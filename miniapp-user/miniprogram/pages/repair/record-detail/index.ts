import type { RepairRecordDetailVO } from '@fixmate/api-contract';
import Toast from '@vant/weapp/toast/toast';
import { getRepairRecord, cancelRepairRequest, convertRepairToWorkOrder } from '../../../services/repair';
import { getCurrentProfile } from '../../../services/profile';
import { applianceLabel, brandLabel, recordStatusLabel, displayTime } from '../../../utils/recordPresentation';
import { cancelWorkOrder } from '../../../services/workOrder';
import { pendingSubmit, openRepair, customerId, readRecovery, writeRecovery, removeRecovery } from '../../../utils/repairRecovery';
import { visitText } from '../../../utils/visitPresentation';
import { newIdempotencyKey } from '../../../utils/id';
interface PendingConvert {key:string;version:number;at:number}
const convertSlot=(id:string)=>'fm.convert.'+id;
Page({
  data: {id:'',loading:true,error:'',record:null as RepairRecordDetailVO|null,statusText:'',deviceText:'',timeText:'',occurredText:'',
    canSelectVisit:false,preferredText:'',canConvert:false,convertVisible:false,converting:false,convertPending:false,convertError:'',canContinue:false,canCancel:false,withVisitFee:false,canView:false,isPending:false,cancelVisible:false,cancelling:false,failedImages:{} as Record<string,boolean>},
  _generation:0,_owner:'',_convert:null as PendingConvert|null,
  onLoad(options: Record<string,string|undefined>) { this.setData({id:options.id||''}); },
  onShow() { this.load(); },
  onUnload() { this._generation++; },
  onPullDownRefresh() { this.load(); },
  async load() {
    if(this.data.converting){wx.stopPullDownRefresh();return;}
    if(!this.data.id) {this.setData({loading:false,error:'缺少报修编号'});return;}
    const generation=++this._generation;this.setData({loading:true,error:'',record:null,canConvert:false,canCancel:false,canView:false,canContinue:false});
    try {
      await getCurrentProfile();const owner=customerId();
      const stored=readRecovery<PendingConvert>(convertSlot(this.data.id));
      if(stored&&(!Number.isInteger(stored.version)||typeof stored.key!=='string'||typeof stored.at!=='number'))throw new Error('本机确认恢复记录异常，请联系平台核对，不能重新发起');
      const record=await getRepairRecord(this.data.id);
      if(generation!==this._generation||owner!==customerId())return;
      this._owner=owner;this._convert=stored;
      // 服务端已校验报修与工单同属本人；权威关联可收口响应丢失后的结果。
      if(this._convert&&record.workOrderId){removeRecovery(convertSlot(this.data.id));this._convert=null;}
      const conversionPending=!!this._convert;
      const r=record.repairRequest,actions=Array.from(record.allowedActions||[]),isPending=pendingSubmit()?.draftId===r.id;
      this.setData({record,isPending,canSelectVisit:!isPending&&!conversionPending&&actions.includes('SELECT_PREFERRED_VISIT'),preferredText:record.preferredVisit?visitText(record.preferredVisit as unknown as {startAt:string;endAt:string}):'',convertPending:conversionPending,convertError:conversionPending?'上次确认结果待核对，请恢复原操作。':'',canConvert:!isPending&&!conversionPending&&actions.includes('CONVERT_TO_WORK_ORDER'),statusText:isPending?'提交结果待确认':recordStatusLabel(r.status,record.workOrderStatus,record.preferredVisit?.startAt),
        deviceText:[applianceLabel(r.applianceType),brandLabel(r.brandCode),r.modelCode||r.modelText].filter(Boolean).join(' · '),
        timeText:displayTime(r.createdAt),occurredText:r.occurredAt?displayTime(r.occurredAt):'',failedImages:{},
        canContinue:!isPending&&!conversionPending&&actions.indexOf('CONTINUE_DRAFT')>=0,
        canCancel:!isPending&&!conversionPending&&(actions.includes('CANCEL')||actions.includes('CANCEL_WORK_ORDER')||actions.includes('CANCEL_WITH_VISIT_FEE')),
        withVisitFee:actions.includes('CANCEL_WITH_VISIT_FEE'),
        canView:!isPending&&!conversionPending&&actions.indexOf('VIEW_WORK_ORDER')>=0});
    } catch(error) {
      if(generation===this._generation)this.setData({error:error instanceof Error?error.message:'读取失败，请重试'});
    } finally { if(generation===this._generation)this.setData({loading:false});wx.stopPullDownRefresh(); }
  },
  onConvert(){if(this.data.canConvert&&!this.data.converting&&!this.data.cancelling)this.setData({convertVisible:true});},
  onCloseConvert(){if(!this.data.converting)this.setData({convertVisible:false});},
  async onConfirmConvert(){
    if(this.data.converting||this.data.cancelling||!this._owner||this._owner!==customerId())return;
    if(!this._convert){
      if(!this.data.canConvert||!this.data.record||!Number.isInteger(this.data.record.repairRequest.version))return;
      this._convert={key:newIdempotencyKey(),version:this.data.record.repairRequest.version,at:Date.now()};
      writeRecovery(convertSlot(this.data.id),this._convert);
    }
    const pending=this._convert;
    if(Date.now()-pending.at>30*60*1000){this.setData({convertVisible:false,convertPending:true,canConvert:false,convertError:'确认结果仍需核对，请先下拉读取工单关联；不会自动重新发起。'});return;}
    const owner=this._owner,generation=this._generation;
    this.setData({converting:true,convertVisible:false,convertPending:true,convertError:'',canConvert:false,canCancel:false,canContinue:false,canView:false});
    try{
      const order=await convertRepairToWorkOrder(this.data.id,pending.version,pending.key,owner);
      if(owner!==customerId()||generation!==this._generation)return;
      removeRecovery(convertSlot(this.data.id));this._convert=null;this.setData({convertPending:false});Toast.success('工单已生成');
      wx.navigateTo({url:'/pages/repair/visit-slot/index?id='+encodeURIComponent(order.id)+'&repair='+this.data.id});
    }catch(error){
      if(owner!==customerId()||generation!==this._generation)return;
      const e=error as {status?:number;code?:string;message?:string};
      if(!e.status||e.status>=500||e.status===401||e.code==='DUPLICATE_REQUEST')this.setData({convertError:'确认结果待核对，请恢复原操作或下拉核对已关联工单。'});
      else{removeRecovery(convertSlot(this.data.id));this._convert=null;this.setData({convertPending:false,convertError:(e.message||'确认未完成')+'，请下拉刷新后重新核对。'});}
    }finally{if(owner===customerId()&&generation===this._generation)this.setData({converting:false});}
  },
  onSelectVisit(){
    const record=this.data.record,id=record?.workOrderId;
    if(!this.data.canSelectVisit||!id||this.data.converting)return;
    const mode=record.workOrderStatus==='WAITING_CUSTOMER_VISIT'?'&continuation=1':
      record.workOrderStatus==='WAITING_MANUAL_REVIEW'?'&rework=1':'';
    wx.navigateTo({url:'/pages/repair/visit-slot/index?id='+encodeURIComponent(id)+'&repair='+this.data.id+mode});
  },
  onContinue() { if(this.data.canContinue)openRepair(this.data.id,'draft'); },
  onRecover() { if(this.data.isPending)openRepair(this.data.id,'pending'); },
  onView() { const id=this.data.record?.workOrderId;if(this.data.canView&&id)wx.navigateTo({url:'/pages/work-order/detail/index?id='+encodeURIComponent(id)}); },
  onCancel() { if(this.data.canCancel&&!this.data.cancelling)this.setData({cancelVisible:true}); },
  onCloseDialog() { this.setData({cancelVisible:false}); },
  async onConfirmCancel() {
    const record=this.data.record;
    if(!record||!this.data.canCancel||this.data.cancelling||this.data.converting||this.data.convertPending)return;
    this.setData({cancelVisible:false,cancelling:true});
    try {
      if(record.workOrderId){
        if(typeof record.workOrderVersion!=='number')throw new Error('请刷新后重新读取工单');
        const fee=this.data.withVisitFee?record.cancellationFeeAmount:undefined;
        if(this.data.withVisitFee&&!fee)throw new Error('请刷新工单后核对上门费');
        await cancelWorkOrder(record.workOrderId,record.workOrderVersion,fee??undefined);
      }else await cancelRepairRequest(record.repairRequest.id,{expectedVersion:record.repairRequest.version});
      Toast.success('已取消报修');
      await this.load();
    }
    catch(error) {Toast.fail(error instanceof Error?error.message:'操作未完成，请重试');}
    finally {this.setData({cancelling:false});}
  },
  onImageError(event: WechatMiniprogram.TouchEvent) { const id=String(event.currentTarget.dataset.id);this.setData({['failedImages.'+id]:true}); },
  onPreview(event: WechatMiniprogram.TouchEvent) {
    const current=String(event.currentTarget.dataset.url||''),urls=(this.data.record?.repairRequest.mediaFiles||[]).map(f=>f.url).filter(Boolean);
    if(current&&urls.length)wx.previewImage({current,urls});
  },
  onBackToRecords() {wx.switchTab({url:'/pages/repair/records/index'});}
});

