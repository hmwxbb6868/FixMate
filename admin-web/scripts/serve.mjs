// 用户手动启动；同源代理避免改动已验收后端CORS/认证配置。
import {createServer,request} from 'node:http';
import {readFile,stat} from 'node:fs/promises';
import {resolve,dirname,extname,sep} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createAdminSession,loadSessionKey} from './session.mjs';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'../dist');
const port=Number(process.env.FIXMATE_ADMIN_PORT||5173);
const backend=new URL(process.env.FIXMATE_ADMIN_BACKEND||'http://127.0.0.1:8080');
const docker=process.env.FIXMATE_ADMIN_DOCKER==='1';
const permittedHost=docker?backend.host==='backend:8080':['127.0.0.1','localhost','[::1]'].includes(backend.hostname);
if(backend.protocol!=='http:'||!permittedHost||backend.username||backend.password||backend.pathname!=='/'||backend.search||backend.hash)throw Error('管理端代理地址不符合当前运行环境');
const sessions=createAdminSession({secret:await loadSessionKey(process.env.FIXMATE_ADMIN_SESSION_KEY_FILE||resolve(root,'../.admin-session.key')),backend});
const allowedOrigins=new Set(['http://127.0.0.1:'+port,'http://localhost:'+port]);
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8'};

function sendJson(res,status,body){
 res.writeHead(status,{'Content-Type':'application/json; charset=utf-8'});
 res.end(JSON.stringify(body));
}

async function readSessionBody(req){
 const chunks=[];
 let length=0;
 for await(const chunk of req){
  length+=chunk.length;
  if(length>16384)throw Error('会话请求过长');
  chunks.push(chunk);
 }
 return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

const server=createServer(async(req,res)=>{
 res.setHeader('Cache-Control','no-store');res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','no-referrer');
 res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' https:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
 const url=new URL(req.url||'/',`http://127.0.0.1:${port}`);
 if(url.pathname.startsWith('/__session/')){
  if(req.method!=='POST'||!allowedOrigins.has(req.headers.origin)){
   sendJson(res,403,{code:'FORBIDDEN',message:'会话请求来源无效'});return;
  }
  const operation=url.pathname.slice('/__session/'.length);
  if(!['login','resume','logout'].includes(operation)){
   sendJson(res,404,{code:'RESOURCE_NOT_FOUND',message:'会话操作不存在'});return;
  }
  const idempotencyKey=req.headers['idempotency-key'];
  if(typeof idempotencyKey!=='string'||!idempotencyKey.trim()){
   sendJson(res,400,{code:'VALIDATION_ERROR',message:'会话请求缺少操作标识'});return;
  }
  let body;
  try{body=await readSessionBody(req);if(!body||typeof body!=='object'||Array.isArray(body))throw Error();}
  catch{sendJson(res,400,{code:'VALIDATION_ERROR',message:'会话请求格式不正确'});return;}
  try{
   let result;
   if(operation==='login')result=await sessions.login(body,idempotencyKey);
   else if(operation==='resume')result=await sessions.resume(req.headers.cookie,body.sessionId);
   else result=await sessions.logout(req.headers.cookie,body.sessionId,idempotencyKey);
   if(result.cookie)res.setHeader('Set-Cookie',result.cookie);
   sendJson(res,result.status,result.body);
  }catch{sendJson(res,502,{code:'PROXY_UNAVAILABLE',message:'暂时无法连接后台，请稍后恢复会话'});}
  return;
 }
 if(url.pathname.startsWith('/api/')){
  if(!url.pathname.startsWith('/api/v1/admin/')||!['GET','POST','PUT'].includes(req.method||'')){res.writeHead(403).end();return;}
  const origin=req.headers.origin;if(origin&&!allowedOrigins.has(origin)){res.writeHead(403).end();return;}
  if(!sessions.authorizes(req.headers.cookie,req.headers.authorization)){
   sendJson(res,401,{code:'UNAUTHORIZED',message:'登录已失效，请重新登录'});return;
  }
  const headers={...req.headers,host:backend.host};delete headers.cookie;delete headers.connection;
  const proxy=request(new URL(url.pathname+url.search,backend),{method:req.method,headers,timeout:30000},up=>{
   if(url.pathname==='/api/v1/admin/auth/password/change'&&up.statusCode===200)res.setHeader('Set-Cookie',sessions.clearCookie());
   res.statusCode=up.statusCode||502;res.setHeader('Content-Type',up.headers['content-type']||'application/json');up.pipe(res);
  });proxy.on('timeout',()=>proxy.destroy());proxy.on('error',()=>{if(!res.headersSent)res.writeHead(502,{'Content-Type':'application/json; charset=utf-8'});res.end(JSON.stringify({code:'PROXY_UNAVAILABLE',message:'后端暂不可达，请确认本项目后端正在运行'}));});req.pipe(proxy);return;
 }
 try{const name=decodeURIComponent(url.pathname==='/'?'/index.html':url.pathname);const path=resolve(root,'.'+name);if(!path.startsWith(root+sep))throw Error();const info=await stat(path);if(!info.isFile())throw Error();res.setHeader('Content-Type',types[extname(path)]||'application/octet-stream');res.end(await readFile(path));}catch{res.writeHead(404).end('Not found');}
});
server.listen(port,docker?'0.0.0.0':'127.0.0.1',()=>console.log(`安心修管理端 http://127.0.0.1:${port} | 纯样式预览 http://127.0.0.1:${port}/?preview=1 | 后端 ${backend.origin}`));
server.on('error',e=>{console.error(e.code==='EADDRINUSE'?'端口被占用，请关闭本项目旧管理端窗口或设置FIXMATE_ADMIN_PORT':e.message);process.exitCode=1;});
