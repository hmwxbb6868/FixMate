import { visitText } from './visitPresentation';
import type { Task, Participation } from '../services/technician';
import { applianceLabel, displayTime, statusLabel } from './recordPresentation';
export { displayTime, applianceLabel };
export const actionsHave=(actions:readonly string[]|undefined,key:string) => !!actions&&actions.includes(key);
export const errorText=(error:unknown) => error instanceof Error?error.message:(error as {message?:string})?.message||'暂时无法连接，请重试';
export const orderStatus=(status:string) => statusLabel('',status);
export const beijingDay=(value:string|number|Date=Date.now()) => displayTime(new Date(value)).slice(0,10);
export function presentTask(row:Task) {
 const state=row.assignment.status;
 const rescheduling=state==='ACTIVE'&&!!row.rescheduleVisit;
 const summary=row.reworkSummary;
 return {...row,title:applianceLabel(row.applianceType),
  reworkSummary:summary?{...summary,
   issueTime:summary.issueAt?displayTime(summary.issueAt):'',
   previousVisitTime:summary.previousVisit?visitText(summary.previousVisit):'',
   previousCompletedTime:summary.previousCompletedAt?displayTime(summary.previousCompletedAt):''}:null,
  stateText:rescheduling?'客户申请改约':state==='PENDING'?'待确认派单':state==='RETURN_REQUESTED'?'退回审核中':row.requoteRequested?'需重新报价':orderStatus(row.status),
  tone:rescheduling||state==='PENDING'?'orange':state==='RETURN_REQUESTED'?'muted':'blue',
  icon:row.applianceType==='REFRIGERATOR'?'freezer':row.applianceType==='WASHING_MACHINE'?'wash':'air',
  areaText:row.serviceArea||'服务区域待补充',
  appointmentText:rescheduling?visitText(row.rescheduleVisit):row.appointmentStartAt?displayTime(row.appointmentStartAt):visitText(row.preferredVisit),
  buttonText:rescheduling?'查看改约':state==='PENDING'?'查看派单':state==='RETURN_REQUESTED'?'查看进度':'继续服务'};
}
export function presentParticipation(row:Participation) {
 const result=row.latestAssignmentResult;
 const declinedReschedule=result==='RESCHEDULE_DECLINED';
 return {...row,title:applianceLabel(row.applianceType),
  stateText:declinedReschedule?'已取消':result==='DECLINED'?'已拒单':orderStatus(row.status),
  acceptanceText:declinedReschedule?'已拒单':result==='ACCEPTED'?'已接单':'',
  timeText:displayTime(row.endedAt||row.participatedAt)};
}
export function nextStep(status:string):string {return ({WAITING_ASSIGNMENT:'核对设备、服务区域与用户期望时段，确认能够履约后再接单。',WAITING_APPOINTMENT:'请客户选择期望时段，再由你确认上门安排。',APPOINTED:'按约上门，准备开始现场检测。',IN_INSPECTION:'记录实际检测结果，提交后才能创建报价。',WAITING_QUOTE:'依据已提交的检测结果填写报价，金额由平台规则计算。',WAITING_QUOTE_CONFIRMATION:'报价已发出，等待客户确认后开始维修。',IN_REPAIR:'按已确认范围维修，完成后记录服务过程。',PAUSED_FOR_REQUOTE:'维修已暂停，等待客户确认追加报价。',WAITING_ACCEPTANCE:'维修已提交完工，等待客户验收。',WAITING_MANUAL_REVIEW:'平台正在人工协调，请等待处理结果。'} as Record<string,string>)[status]||'查看当前任务信息与平台安排。';}

export function staffContact(access:import('../services/technician').WorkspaceAccess,binding:import('../services/technician').Binding|null):{phone:string;source:string}{if(access.staffPhone)return {phone:access.staffPhone,source:'员工档案联系电话'};if(binding?.status==='ACTIVE'&&String(binding.id)===String(access.bindingId)&&String(binding.staffId)===String(access.staffId))return {phone:binding.applicantContactPhone,source:'绑定申请联系电话'};return {phone:'',source:'联系电话待补充'};}
