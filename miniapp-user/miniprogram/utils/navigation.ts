import { technicianMode, setWorkspace, cachedAccess, hasTechnicianAccess } from './workspaceStore';
export const USER_TABS=[{path:'/pages/repair/new/index',text:'发起报修',icon:'edit'},{path:'/pages/repair/records/index',text:'报修记录',icon:'records'},{path:'/pages/workspace/me/index',text:'我的',icon:'contact'}];
export const TECHNICIAN_TABS=[{path:'/pages/technician/workbench/index',text:'工作台',icon:'wap-home-o'},{path:'/pages/technician/work-orders/index',text:'我的工单',icon:'orders-o'},{path:'/pages/workspace/me/index',text:'我的',icon:'contact'}];
export function syncTab(page:WechatMiniprogram.Page.Instance<WechatMiniprogram.IAnyObject,WechatMiniprogram.IAnyObject>,selected:number,hidden=false):void {
  const staff=hasTechnicianAccess(cachedAccess())&&(!!page.route?.startsWith('pages/technician/')||page.route==='pages/workspace/me/index'&&technicianMode());
  if(page.route&&page.route!=='pages/workspace/me/index')setWorkspace(staff?'TECHNICIAN':'USER');
  const tab=page.getTabBar&&page.getTabBar();if(tab)tab.setData({selected,hidden,tabs:staff?TECHNICIAN_TABS:USER_TABS,workspace:staff?'TECHNICIAN':'USER'});
}
let taskIntent:{owner:string;filter:string}|null=null;
export function openTaskList(filter='ALL'):void {taskIntent={owner:String(wx.getStorageSync('fm.customerId')||''),filter};wx.switchTab({url:'/pages/technician/work-orders/index'});}
export function takeTaskIntent():string {const value=taskIntent;taskIntent=null;return value?.owner===String(wx.getStorageSync('fm.customerId')||'')?value.filter:'ALL';}
export function backToWorkbench():void {if(getCurrentPages().length>1)wx.navigateBack();else wx.switchTab({url:'/pages/technician/workbench/index'});}
