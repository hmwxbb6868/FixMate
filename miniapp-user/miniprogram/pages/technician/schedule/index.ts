import { allTasks, Task } from '../../../services/technician';
import { requireTechnician } from '../../../services/workspace';
import { beijingDay, presentTask, errorText } from '../../../utils/technicianPresentation';
Page({data:{loading:true,error:'',filter:'TODAY',items:[] as ReturnType<typeof presentTask>[],dateText:'',filters:[{key:'TODAY',text:'今天'},{key:'TOMORROW',text:'明天'},{key:'LATER',text:'后续安排'}]},_g:0,_tasks:[] as Task[],
 onShow(){this.load();},onPullDownRefresh(){this.load();},onHide(){this._g++;this._tasks=[];this.setData({items:[]});},
 async load(){const g=++this._g;this.setData({loading:true,error:''});try{await requireTechnician();const tasks=await allTasks();if(g!==this._g)return;this._tasks=tasks.filter(x=>x.assignment.status==='ACTIVE'&&!!x.appointmentStartAt).sort((a,b)=>a.appointmentStartAt!.localeCompare(b.appointmentStartAt!));this.apply();}catch(error){if(g===this._g)this.setData({error:errorText(error),items:[]});}finally{if(g===this._g)this.setData({loading:false});wx.stopPullDownRefresh();}},
 apply(){const today=beijingDay(),tomorrow=beijingDay(Date.now()+86400000),filter=this.data.filter;this.setData({dateText:filter==='TODAY'?today:filter==='TOMORROW'?tomorrow:'后续预约',items:this._tasks.filter(x=>{const day=beijingDay(x.appointmentStartAt!);return filter==='TODAY'?day===today:filter==='TOMORROW'?day===tomorrow:day>tomorrow;}).map(presentTask)});},
 onFilter(event:WechatMiniprogram.TouchEvent){this.setData({filter:String(event.currentTarget.dataset.filter)});this.apply();},onTask(event:{detail:{id:string}}){wx.navigateTo({url:'/pages/technician/task/index?id='+event.detail.id});}
});
