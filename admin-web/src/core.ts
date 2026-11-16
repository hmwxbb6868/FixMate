import {contract} from './contract.js';
import type {StaffTokenVO,StaffProfileVO} from '../../packages/api-contract/typescript/dist/index.js';
export type Dict=Record<string,any>;
export type Wire<T>=T extends Date?string:T extends Array<infer U>?Wire<U>[]:T extends object?{[K in keyof T]:Wire<T[K]>}:T;
export type Profile=Wire<StaffProfileVO>;
export const schemas:Dict=contract.schemas;
export const operations:Dict=contract.operations;
export const preview=new URLSearchParams(location.search).get('preview')==='1';
export function schema(s:Dict):Dict {if(s?.$ref)return schema(schemas[s.$ref.split('/').pop()]);if(s?.allOf)return Object.assign({},...s.allOf.map(schema),s);return s||{};}
export function escape(value:unknown):string{return String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]!));}
export function key(){return crypto.randomUUID();}
export function pathFor(op:string,args:Dict={}){let path=operations[op]?.path;if(!path)throw Error('未知操作');return path.replace(/\{(\w+)\}/g,(_:string,k:string)=>{if(!/^[1-9][0-9]{0,18}$/.test(String(args[k])))throw Error('记录编号不正确');return encodeURIComponent(args[k]);});}
export function may(op:string,profile:Profile|null){const roles=operations[op]?.roles;return !!profile&&(!roles?.length||roles.includes(profile.role));}
export class ApiError extends Error{constructor(message:string,public status=0,public code='NETWORK_ERROR'){super(message);}}
export const unknownResult=(e:unknown)=>!(e instanceof ApiError)||e.status===0||e.status>=500||e.code==='DUPLICATE_REQUEST'||e.status===401;
export type Session=Omit<Wire<StaffTokenVO>,'refreshToken'>&{
 sessionId:string;
 sessionExpiresAt:number;
};
export let session:Session|null=null;
let refreshPending:{body:Dict;key:string}|null=null,refreshFlight:Promise<void>|null=null;
let expiryTimer:number|undefined;
export function setSession(value:Session){
 if(value?.principalType!=='STAFF'||value.audience!=='fixmate-admin'||!value.sessionId)throw new ApiError('该登录身份不能进入管理工作区',403);
 if(!Number.isFinite(value.sessionExpiresAt)||value.sessionExpiresAt<=Date.now())throw new ApiError('登录已到期，请重新登录',401);
 session=value;
 if(expiryTimer!==undefined)window.clearTimeout(expiryTimer);
 if(typeof window.setTimeout==='function')expiryTimer=window.setTimeout(forgetSession,value.sessionExpiresAt-Date.now());
}
export function forgetSession(){
 if(expiryTimer!==undefined){window.clearTimeout(expiryTimer);expiryTimer=undefined;}
 session=null;
 refreshPending=null;
 window.dispatchEvent(new Event('session-ended'));
}
export async function raw(path:string,method='GET',body?:unknown,idem?:string,token?:string):Promise<any>{
 if(preview)throw new ApiError('样式预览不连接后台，也不执行业务操作',403);
 const headers:Record<string,string>={'Accept':'application/json','X-Request-Id':key()};if(token)headers.Authorization='Bearer '+token;if(body!==undefined)headers['Content-Type']='application/json';if(method!=='GET')headers['Idempotency-Key']=idem||key();
 const send=()=>fetch(path,{
  method,headers,body:body===undefined?undefined:JSON.stringify(body),
  cache:'no-store',credentials:'same-origin',mode:'same-origin',referrerPolicy:'origin',
  redirect:'error',signal:AbortSignal.timeout(30000),
 });
 const writesSession=path.startsWith('/__session/')||path==='/api/v1/admin/auth/password/change';
 let response:Response;
 try{
  response=writesSession&&typeof navigator!=='undefined'&&navigator.locks
   ?await navigator.locks.request('fixmate-admin-browser-session',send)
   :await send();
 }catch{throw new ApiError('网络连接未完成，请核对原操作结果后恢复');}
 let data:any;try{data=await response.json();}catch{throw new ApiError('服务器响应无法读取，请保留原操作',response.status>=400?response.status:502);}
 if(!response.ok)throw new ApiError(data?.message||'请求未完成',response.status,data?.code);return data.data;
}
export async function restoreSession(){
 const value=await raw('/__session/resume','POST',{});
 setSession(value);
}
async function refresh(){if(!session)throw new ApiError('请重新登录',401);const previous=session;
 if(!refreshPending)refreshPending={body:{sessionId:previous.sessionId},key:key()};
 const result=await raw('/__session/resume','POST',refreshPending.body,refreshPending.key);
 if(session!==previous||result?.sessionId!==previous.sessionId)throw new ApiError('账号已经切换',403);
 setSession(result);refreshPending=null;
}
export async function api(op:string,args:Dict={},body?:Dict,idem?:string,query:Dict={}):Promise<any>{
 if(!session)throw new ApiError('请先登录',401);
 if(session.sessionExpiresAt<=Date.now()){forgetSession();throw new ApiError('登录已到期，请重新登录',401);}
 if(!may(op,session.staff))throw new ApiError('当前岗位没有此操作权限',403);
 const actor=session.staff.id,sessionId=session.sessionId,path=pathFor(op,args),params=new URLSearchParams();for(const[k,v]of Object.entries(query))if(v!==''&&v!=null)params.set(k,String(v));const url=path+(params.size?'?'+params:'');
 const action=()=>raw(url,operations[op].method,body,idem,session?.accessToken);let result:any;
 try{result=await action();}catch(e){if(e instanceof ApiError&&e.status===401){
  if(!refreshFlight)refreshFlight=refresh().finally(()=>{refreshFlight=null;});try{await refreshFlight;}catch(failure){if(failure instanceof ApiError&&[401,403].includes(failure.status)&&session?.sessionId===sessionId)forgetSession();throw failure;}
  if(session?.sessionId!==sessionId||session.staff.id!==actor)throw new ApiError('登录会话已切换',403);
  result=await action();
 }else throw e;}
 if(session?.staff.id!==actor||session.sessionId!==sessionId)throw new ApiError('账号已变更，旧响应已丢弃',403);return result;
}
export async function digest(value:unknown){return [...new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(JSON.stringify(value))))].map(x=>x.toString(16).padStart(2,'0')).join('');}
export interface Command {id:string;owner:string;op:string;args:Dict;body:Dict;key:string;createdAt:number;resource:string}
// 仅保存加密未决命令，AES密钥不可导出；不存会话令牌，不缓存服务端客户详情。
let database:Promise<IDBDatabase>|undefined;
function db(){return database??=new Promise((resolve,reject)=>{const request=indexedDB.open('fixmate-admin-command-journal',1);request.onupgradeneeded=()=>{request.result.createObjectStore('commands',{keyPath:'id'});request.result.createObjectStore('keys');};request.onsuccess=()=>resolve(request.result);request.onerror=()=>reject(Error('未决操作恢复存储不可用，请勿提交新操作'));});}
async function storeRequest(store:string,mode:IDBTransactionMode,action:(s:IDBObjectStore)=>IDBRequest):Promise<any>{const database=await db();return new Promise((resolve,reject)=>{const tx=database.transaction(store,mode),req=action(tx.objectStore(store));let result:any;req.onsuccess=()=>{result=req.result;};tx.oncomplete=()=>resolve(result);tx.onerror=()=>reject(Error('未决操作未能安全保存，请勿刷新页面'));tx.onabort=()=>reject(Error('未决操作未能安全保存'));});}
let secretFlight:Promise<CryptoKey>|undefined;
async function encryptionKey(){return secretFlight??=(async()=>{let value=await storeRequest('keys','readonly',s=>s.get('aes'));if(!value){const candidate=await crypto.subtle.generateKey({name:'AES-GCM',length:256},false,['encrypt','decrypt']);try{await storeRequest('keys','readwrite',s=>s.add(candidate,'aes'));value=candidate;}catch(e){value=await storeRequest('keys','readonly',s=>s.get('aes'));if(!value)throw e;}}return value as CryptoKey;})().catch(e=>{secretFlight=undefined;throw e;});}
export const journal={
 async save(command:Command){const iv=crypto.getRandomValues(new Uint8Array(12)),secret=await encryptionKey();const cipher=await crypto.subtle.encrypt({name:'AES-GCM',iv},secret,new TextEncoder().encode(JSON.stringify(command)));await storeRequest('commands','readwrite',s=>s.put({id:command.id,owner:command.owner,iv,cipher}));},
 async list(owner:string):Promise<Command[]>{const all:any[]=await storeRequest('commands','readonly',s=>s.getAll()),secret=await encryptionKey();return Promise.all(all.filter(x=>x.owner===owner).map(async x=>JSON.parse(new TextDecoder().decode(await crypto.subtle.decrypt({name:'AES-GCM',iv:x.iv},secret,x.cipher)))));},
 async remove(id:string){await storeRequest('commands','readwrite',s=>s.delete(id));}
};
export function commandExpired(command:Command,now=Date.now()){return now-command.createdAt>15*60*1000;}
export function sameActor(command:Command,profile:Profile|null){return command.owner===profile?.id&&may(command.op,profile);}
