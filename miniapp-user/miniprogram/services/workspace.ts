import { redactPendingBodies } from '../utils/technicianCommand';
import { getCurrentProfile } from './profile';
import { fetchWorkspace, WorkspaceAccess } from './technician';
import { cachedAccess, forgetAccess, hasTechnicianAccess, rememberAccess, setWorkspace, workspacePreference } from '../utils/workspaceStore';
import { ApiError } from '../utils/request';
import { customerId } from '../utils/repairRecovery';
let active:Promise<WorkspaceAccess>|null=null;
export function loadWorkspaceAccess():Promise<WorkspaceAccess> {
  if(active)return active;
  active=(async()=>{await getCurrentProfile();const result=await fetchWorkspace();rememberAccess(result);return result;})().finally(()=>{active=null;});
  return active;
}
export async function requireTechnician():Promise<WorkspaceAccess> {
  const value=await loadWorkspaceAccess();if(!hasTechnicianAccess(value)){redactPendingBodies();forgetAccess();throw new ApiError('STAFF_ACCESS_REQUIRED','当前维修授权已失效，请返回用户工作区',403,'',null);}return value;
}
export async function enterTechnician():Promise<void> {await requireTechnician();setWorkspace('TECHNICIAN');await wx.switchTab({url:'/pages/technician/workbench/index'});}
export function enterUser():void {setWorkspace('USER');wx.switchTab({url:'/pages/repair/new/index'});}
export async function restoreLastWorkspace():Promise<void> {if(!customerId()||workspacePreference()!=='TECHNICIAN')return;await enterTechnician();}
export function invalidateTechnician(error:unknown):boolean {const e=error as {status?:number;code?:string};if(e.status===401||e.code==='STAFF_ACCESS_REQUIRED'){forgetAccess();return true;}return false;}
export { cachedAccess };
