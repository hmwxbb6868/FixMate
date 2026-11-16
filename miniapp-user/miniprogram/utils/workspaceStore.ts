import type { WorkspaceAccess } from '../services/technician';
import { customerId } from './repairRecovery';
let access:WorkspaceAccess|null=null,owner='';
export function workspacePreference(): 'USER'|'TECHNICIAN' { return wx.getStorageSync('fm.workspace.'+customerId())==='TECHNICIAN'?'TECHNICIAN':'USER'; }
export function hasTechnicianAccess(value:WorkspaceAccess|null):boolean { return !!value&&Array.isArray(value.availableWorkspaces)&&value.availableWorkspaces.includes('TECHNICIAN')&&!!value.staffId&&!!value.bindingId&&value.staffRole==='TECHNICIAN'; }
export function rememberAccess(value:WorkspaceAccess):void { access=value;owner=customerId();if(!hasTechnicianAccess(value))setWorkspace('USER'); }
export function cachedAccess():WorkspaceAccess|null { return owner===customerId()?access:null; }
export function technicianMode():boolean { return workspacePreference()==='TECHNICIAN'&&hasTechnicianAccess(cachedAccess()); }
export function setWorkspace(mode:'USER'|'TECHNICIAN'):void { if(customerId())wx.setStorageSync('fm.workspace.'+customerId(),mode); }
export function forgetAccess():void { access=null;owner='';setWorkspace('USER'); }
export function staffScope(value:WorkspaceAccess):string { return [value.staffId,value.bindingId,value.bindingVersion].join(':'); }
