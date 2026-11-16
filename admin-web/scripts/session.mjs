import {createCipheriv,createDecipheriv,createHmac,randomBytes,randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';

const cookieName='fixmate_admin_session';
const sessionLifetime=7*24*60*60*1000;
const invalid={code:'UNAUTHORIZED',message:'登录已失效，请重新登录'};

export async function loadSessionKey(file){
 try{await writeFile(file,randomBytes(32),{flag:'wx',mode:0o600});}
 catch(error){if(error.code!=='EEXIST')throw error;}
 const secret=await readFile(file);
 if(secret.length!==32)throw Error('管理台会话密钥无效');
 return secret;
}

function claims(token){
 try{return JSON.parse(Buffer.from(token.split('.')[1],'base64url').toString('utf8'));}
 catch{return null;}
}

export function createAdminSession({secret,backend,fetchBackend=fetch,now=Date.now}){
 function seal(value){
  const iv=randomBytes(12);
  const cipher=createCipheriv('aes-256-gcm',secret,iv);
  const encrypted=Buffer.concat([cipher.update(JSON.stringify(value),'utf8'),cipher.final()]);
  return Buffer.concat([iv,cipher.getAuthTag(),encrypted]).toString('base64url');
 }

 function read(cookieHeader=''){
  const prefix=cookieName+'=';
  const encoded=cookieHeader.split(';').map(value=>value.trim()).find(value=>value.startsWith(prefix))?.slice(prefix.length);
  if(!encoded||encoded.length>4096)return null;
  try{
   const bytes=Buffer.from(encoded,'base64url');
   const decipher=createDecipheriv('aes-256-gcm',secret,bytes.subarray(0,12));
   decipher.setAuthTag(bytes.subarray(12,28));
   const value=JSON.parse(Buffer.concat([decipher.update(bytes.subarray(28)),decipher.final()]).toString('utf8'));
   return value.expiresAt>now()?value:null;
  }catch{return null;}
 }

 function cookie(value){
  const seconds=value?Math.ceil((value.expiresAt-now())/1000):0;
  return `${cookieName}=${value?seal(value):''}; Path=/; HttpOnly; SameSite=Strict; Max-Age=${Math.max(0,seconds)}`;
 }

 async function auth(operation,body,idempotencyKey){
  const response=await fetchBackend(new URL('/api/v1/admin/auth/'+operation,backend),{
   method:'POST',headers:{'Content-Type':'application/json','X-Request-Id':randomUUID(),'Idempotency-Key':idempotencyKey},
   body:JSON.stringify(body),redirect:'error',signal:AbortSignal.timeout(30000),
  });
  return {status:response.status,body:await response.json()};
 }

 function success(result,previous){
  const value=result.body?.data;
  const token=claims(value?.accessToken);
  if(value?.principalType!=='STAFF'||value.audience!=='fixmate-admin'||!value.refreshToken||!token?.familyId||token.tokenUse!=='ACCESS'||!Number.isSafeInteger(token.iat)||token.sub!==value.staff?.id){
   throw Error('员工会话响应不完整');
  }
  if(previous&&(previous.family!==token.familyId||previous.owner!==token.sub))throw Error('员工会话身份已变更');
  const expiresAt=previous?.expiresAt??(token.iat*1000+sessionLifetime);
  if(expiresAt<=now())return {status:401,body:invalid,cookie:cookie(null)};
  const state={expiresAt,family:token.familyId,owner:token.sub,refreshToken:value.refreshToken};
  const {refreshToken,...access}=value;
  return {status:200,body:{...result.body,data:{...access,sessionId:state.family,sessionExpiresAt:expiresAt}},cookie:cookie(state)};
 }

 return {
  clearCookie:()=>cookie(null),
  authorizes(cookieHeader,authorization){
   const state=read(cookieHeader);
   if(!state||!authorization?.startsWith('Bearer '))return false;
   const token=claims(authorization.slice(7));
   // Only bind the request to the cookie here. Spring still authenticates the bearer token.
   return token?.familyId===state.family&&token.sub===state.owner&&token.tokenUse==='ACCESS';
  },
  async login(body,idempotencyKey){
   const result=await auth('login',body,idempotencyKey);
   return result.status===200?success(result,null):result;
  },
  async resume(cookieHeader,expectedSessionId){
   const state=read(cookieHeader);
   if(!state)return {status:401,body:invalid};
   if(expectedSessionId&&expectedSessionId!==state.family)return {status:403,body:{code:'FORBIDDEN',message:'登录会话已切换，请重新打开管理台'}};
   // Concurrent tabs and lost responses reuse the same key for the same refresh token.
   const key=createHmac('sha256',secret).update('refresh\0'+state.refreshToken).digest('hex');
   const result=await auth('refresh',{refreshToken:state.refreshToken},key);
   if(result.status===200)return success(result,state);
   return result;
  },
  async logout(cookieHeader,expectedSessionId,idempotencyKey){
   const state=read(cookieHeader);
   if(!state)return {status:200,body:{code:'OK'},cookie:cookie(null)};
   if(expectedSessionId!==state.family)return {status:403,body:{code:'FORBIDDEN',message:'登录会话已切换，请重新打开管理台'}};
   const result=await auth('logout',{refreshToken:state.refreshToken},idempotencyKey);
   return [200,401,403].includes(result.status)?{...result,cookie:cookie(null)}:result;
  },
 };
}
