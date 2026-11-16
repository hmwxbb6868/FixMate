import { allTasks, Task } from '../../../services/technician';
import { requireTechnician, enterUser, invalidateTechnician } from '../../../services/workspace';
import { syncTab, openTaskList } from '../../../utils/navigation';
import { beijingDay, errorText, presentTask } from '../../../utils/technicianPresentation';
Page({
 data:{loading:true,error:'',staffName:'',dateText:'',updatedText:'',pending:[] as ReturnType<typeof presentTask>[],ongoing:[] as ReturnType<typeof presentTask>[],pendingCount:0,ongoingCount:0,activeCount:0,todayCount:0,denied:false},
 _generation:0,_owner:'',
 onShow(){syncTab(this,0);this.refresh();},onPullDownRefresh(){this.refresh();},onHide(){this._generation++;this.setData({pending:[],ongoing:[]});},
 async refresh(){const g=++this._generation;this.setData({loading:true,error:'',denied:false,staffName:''});try{const access=await requireTechnician();const owner=String(wx.getStorageSync('fm.customerId'));if(g!==this._generation)return;this._owner=owner;const tasks=await allTasks();if(g!==this._generation||owner!==String(wx.getStorageSync('fm.customerId')))return;const pending=tasks.filter(x=>x.assignment.status==='PENDING'),ongoing=tasks.filter(x=>x.assignment.status!=='PENDING');this.setData({staffName:access.staffName||'维修师傅',dateText:beijingDay().slice(5).replace('-','月')+'日',updatedText:'刚刚更新',pending:pending.slice(0,3).map(presentTask),ongoing:ongoing.slice(0,4).map(presentTask),pendingCount:pending.length,ongoingCount:ongoing.length,activeCount:ongoing.filter(x=>x.assignment.status==='ACTIVE').length,todayCount:tasks.filter(x=>x.assignment.status==='ACTIVE'&&x.appointmentStartAt&&beijingDay(x.appointmentStartAt)===beijingDay()).length});syncTab(this,0);}catch(error){if(g===this._generation){invalidateTechnician(error);this.setData({error:errorText(error),denied:(error as {status?:number}).status===403,pending:[],ongoing:[]});}}finally{if(g===this._generation)this.setData({loading:false});wx.stopPullDownRefresh();}},
 onTask(event:{detail:{id:string}}){wx.navigateTo({url:'/pages/technician/task/index?id='+encodeURIComponent(event.detail.id)});},
 onPool(){wx.navigateTo({url:"/pages/technician/job-pool/index"});},onSchedule(){wx.navigateTo({url:'/pages/technician/schedule/index'});},onList(event:WechatMiniprogram.TouchEvent){openTaskList(String(event.currentTarget.dataset.filter||'ALL'));},onUser(){enterUser();}
});
