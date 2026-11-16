import { request, ApiError } from './request';
import { newIdempotencyKey } from './id';
import { customerId } from './repairRecovery';
export interface StaffPending {path:string;method:'POST'|'PUT';body?:unknown;key:string;at:number;scope:string;label:string}
const BODY_TTL=10*60*1000;
function key(slot:string):string {if(!customerId())throw new Error('请先确认登录身份');return 'fm.staff.pending.'+customerId()+'.'+slot;}
export function pendingCommand(slot:string):StaffPending|null {
  const name=key(slot),raw=wx.getStorageSync(name) as StaffPending|null;
  if(!raw||!raw.key||!raw.path||!raw.scope)return null;
  if(Date.now()-raw.at>BODY_TTL&&raw.body!==undefined){delete raw.body;wx.setStorageSync(name,raw);}return raw;
}
export function dropCommand(slot:string):void {wx.removeStorageSync(key(slot));}
export function commandUnknown(error:unknown):boolean {const e=error as {status?:number;code?:string};return !e.status||e.status>=500||e.code==='DUPLICATE_REQUEST';}
async function send<T>(slot:string,scope:string,snapshot:StaffPending):Promise<T> {
  if(snapshot.scope!==scope||snapshot.body===undefined)throw new ApiError('PENDING_REVIEW_REQUIRED','上次操作需由平台核对，请勿重新提交',0,'',null);
  const owner=customerId();
  try {const result=await request<T>({method:snapshot.method,path:snapshot.path,data:snapshot.body,idempotencyKey:snapshot.key,expectedOwner:owner});if(owner===customerId())dropCommand(slot);return result;}
  catch(error){if(owner===customerId()&&!commandUnknown(error))dropCommand(slot);throw error;}
}
export function staffCommand<T>(slot:string,scope:string,label:string,method:'POST'|'PUT',path:string,body:unknown):Promise<T> {
  if(pendingCommand(slot))return Promise.reject(new ApiError('PENDING_RESULT','请先恢复上次操作结果，再进行新操作',0,'',null));
  const snapshot:StaffPending={path,method,body,key:newIdempotencyKey(),at:Date.now(),scope,label};wx.setStorageSync(key(slot),snapshot);return send<T>(slot,scope,snapshot);
}
export function retryStaffCommand<T>(slot:string,scope:string):Promise<T> {const snapshot=pendingCommand(slot);if(!snapshot)return Promise.reject(new Error('没有待恢复的操作'));return send<T>(slot,scope,snapshot);}
/** 退出/失权时清除输入正文，保留不含客户资料的未决凭证，防止改键重复写。 */
export function redactPendingBodies():void {for(const name of wx.getStorageInfoSync().keys)if(name.startsWith('fm.staff.pending.'+customerId()+'.')){const value=wx.getStorageSync(name) as StaffPending;delete value.body;wx.setStorageSync(name,value);}}

export function pruneExpiredPendingBodies():void {for(const name of wx.getStorageInfoSync().keys)if(name.startsWith('fm.staff.pending.')){const value=wx.getStorageSync(name) as StaffPending;if(value&&Date.now()-value.at>BODY_TTL&&value.body!==undefined){delete value.body;wx.setStorageSync(name,value);}}}

/** Only this customer's unresolved claim receipts; never expose stored command bodies. */
export function pendingPoolClaims():Array<{id:string;orderId:string;label:string}>{const prefix='fm.staff.pending.'+customerId()+'.order:';return wx.getStorageInfoSync().keys.filter(k=>k.startsWith(prefix)).flatMap(k=>{const value=wx.getStorageSync(k) as StaffPending;const match=value?.path?.match(/^\/api\/v1\/technician\/job-pool\/([1-9][0-9]*)\/claim$/);return match?[{id:match[1],orderId:k.slice(prefix.length),label:value.label}]:[];});}
