import { USER_TABS, TECHNICIAN_TABS } from '../utils/navigation';
import { technicianMode, setWorkspace } from '../utils/workspaceStore';
Component({
 data:{selected:0,hidden:false,tabs:USER_TABS,workspace:'USER'},
 lifetimes:{attached(){const staff=technicianMode(),tabs=staff?TECHNICIAN_TABS:USER_TABS;const pages=getCurrentPages(),route=pages.length?'/'+pages[pages.length-1].route:'';const index=tabs.findIndex(tab=>tab.path===route);this.setData({tabs,workspace:staff?'TECHNICIAN':'USER',selected:Math.max(0,index)});}},
 methods:{onSelect(event:WechatMiniprogram.TouchEvent){const index=Number(event.currentTarget.dataset.index),tab=this.data.tabs[index];if(!tab||this.data.hidden||index===this.data.selected)return;setWorkspace(this.data.workspace==='TECHNICIAN'?'TECHNICIAN':'USER');wx.switchTab({url:tab.path});}}
});
