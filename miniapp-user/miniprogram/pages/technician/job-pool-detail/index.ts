import { visitText } from '../../../utils/visitPresentation';
import { getJobPool, PoolEntry } from '../../../services/technician';
import type { AssignmentVO } from '@fixmate/api-contract';
import { requireTechnician } from '../../../services/workspace';
import { staffScope } from '../../../utils/workspaceStore';
import { applianceLabel, displayTime, errorText } from '../../../utils/technicianPresentation';
import { pendingCommand, staffCommand, retryStaffCommand, commandUnknown } from '../../../utils/technicianCommand';
import { confirmAction } from '../../../utils/technicianDialogs';
Page({data:{id:'',orderId:'',loading:true,error:'',entry:null as PoolEntry|null,title:'',timeText:'',slotText:'',canClaim:false,busy:false,commandError:'',pendingLabel:''},_g:0,_scope:'',_owner:'',
 onLoad(o:Record<string,string>){this.setData({id:o.id||'',orderId:o.order||''});},onShow(){this.load();},onPullDownRefresh(){this.load();},onHide(){this._g++;this.setData({entry:null});},
 async load(){const g=++this._g;this.setData({loading:true,error:'',entry:null,canClaim:false});try{if(!/^[1-9][0-9]*$/.test(this.data.id)||!/^[1-9][0-9]*$/.test(this.data.orderId))throw new Error('订单编号不正确');const access=await requireTechnician();if(g!==this._g)return;this._scope=staffScope(access);this._owner=String(wx.getStorageSync('fm.customerId'));this.setData({pendingLabel:pendingCommand('order:'+this.data.orderId)?.label||''});const row=await getJobPool(this.data.id);if(g!==this._g||this._owner!==String(wx.getStorageSync('fm.customerId')))return;if(row.workOrderId!==this.data.orderId)throw new Error('订单已变化，请返回大厅');this.setData({entry:row,canClaim:row.allowedActions.includes('CLAIM_JOB')&&!!row.preferredVisit,slotText:visitText(row.preferredVisit),title:applianceLabel(row.applianceType),timeText:displayTime(row.publishedAt)});}catch(e){if(g===this._g)this.setData({error:errorText(e)});}finally{if(g===this._g)this.setData({loading:false});wx.stopPullDownRefresh();}},
 async onClaim(){if(this.data.busy||this.data.pendingLabel||!this.data.entry||!this.data.canClaim)return;const g=this._g;if(!await confirmAction('确认时段并接单','用户期望：'+this.data.slotText+'。确认能够在该时段履约，接单成功后预约成立；报价仍需客户单独确认。')||g!==this._g)return;await this.perform(false);},
 async onRetry(){if(!this.data.busy)await this.perform(true);},
 async perform(retry:boolean){const row=this.data.entry;if(!retry&&!row)return;const g=this._g;this.setData({busy:true,commandError:''});try{const access=await requireTechnician();if(g!==this._g||staffScope(access)!==this._scope||this._owner!==String(wx.getStorageSync('fm.customerId')))throw new Error('维修身份已变化，请重新读取');const slot='order:'+this.data.orderId;const result=retry?await retryStaffCommand<AssignmentVO>(slot,this._scope):await staffCommand<AssignmentVO>(slot,this._scope,'自主接单','POST','/api/v1/technician/job-pool/'+this.data.id+'/claim',{expectedWorkOrderVersion:row!.workOrderVersion,expectedPublicationVersion:row!.version});if(g!==this._g)return;this.setData({pendingLabel:'',entry:null});wx.showToast({title:'接单成功',icon:'success'});wx.redirectTo({url:'/pages/technician/task/index?id='+result.workOrderId});}catch(e){
  if(g!==this._g||this._owner!==String(wx.getStorageSync('fm.customerId')))return;
  const unknown=commandUnknown(e);
  const message=errorText(e);
  const appointmentConflict=!unknown&&(e as {status?:number}).status===409&&message.includes('已有预约冲突');
  this.setData({
   commandError:unknown?'接单结果待确认，请恢复原操作；不会重复抢单。':appointmentConflict?'':message,
   pendingLabel:pendingCommand('order:'+this.data.orderId)?.label||''
  });
  if(appointmentConflict)wx.showToast({title:'该时段与你已有预约冲突',icon:'none',duration:2000});
 }finally{if(g===this._g)this.setData({busy:false});}},
 onBack(){wx.navigateBack();}
});
