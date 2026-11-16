// 合成演示数据：仅 ?preview=1 使用，传输层完全禁止请求后台。
import {Dict} from './core.js';
export const demoProfile={id:'9001',username:'preview-admin',name:'运营管理员',role:'ADMIN',status:'ENABLED',version:0,phone:null} as const;
const statuses=['WAITING_ASSIGNMENT','APPOINTED','WAITING_QUOTE_CONFIRMATION','IN_REPAIR','WAITING_ACCEPTANCE','COMPLETED'];
export const demoOrders=Array.from({length:8},(_,i)=>({id:String(9100+i),workOrderNo:'WO20261106'+String(1008-i),repairNo:'RR20261106'+String(5008-i),applianceType:['AIR_CONDITIONER','WASHING_MACHINE','REFRIGERATOR'][i%3],symptomSummary:['运行时有异常声音，制冷效果变差','洗衣机无法正常排水，已检查过滤网','冷藏室温度偏高，需要现场检测'][i%3],status:statuses[i%6],currentAssigneeName:i%6===0?null:['示例师傅甲','示例师傅乙'][i%2],currentAssigneeId:i%6===0?null:'9201',assignmentStatus:i%6===0?null:'ACTIVE',appointmentStartAt:i%6===0?null:'2026-11-07T05:00:00Z',riskFlag:i===6,createdAt:'2026-11-06T'+String(9-i).padStart(2,'0')+':20:00Z',version:2}));
export const demoDashboard={manualReviewCount:8,unresolvedSafetyCount:2,humanHandoffCount:3,urgeCount:4,assignmentCount:6,quoteConfirmationCount:5,overrideApprovalCount:2,upcomingAppointmentCount:12,missedAppointmentCount:1,returnRequestCount:1,requoteCount:2,statusDistribution:{WAITING_ASSIGNMENT:6,APPOINTED:12,IN_INSPECTION:4,WAITING_QUOTE_CONFIRMATION:5,IN_REPAIR:9,WAITING_ACCEPTANCE:3,COMPLETED:26},generatedAt:'2026-11-06T10:35:00Z',aiEnabled:false,followUps:[{kind:'REPAIR_REVIEW',targetId:'9100',referenceNo:'RR202611067008',waitingFor:'STAFF',waitingSince:'2026-11-06T06:35:00Z',categoryTotal:1},{kind:'DISPATCH',targetId:'9100',referenceNo:'WO202611061008',waitingFor:'STAFF',waitingSince:'2026-11-06T07:35:00Z',categoryTotal:1}]};
const exampleVisit={startAt:'2026-11-08T09:00:00Z',endAt:'2026-11-08T11:00:00Z'};
export function demoList(page:string):Dict[]{switch(page){
 case 'orders':return demoOrders;
 case 'intake':return demoOrders.slice(0,2).map((r,i)=>({...r,repairNo:'RR20261106'+(7008-i),status:i?'HUMAN_HANDOFF':'SUBMITTED',repairStatus:i?'HUMAN_HANDOFF':'SUBMITTED'}));
 case 'intake-reviewed':return [{...demoOrders[2],repairNo:'RR202611067006',status:'TRIAGED',repairStatus:'TRIAGED'}];
 case 'dispatch':return [demoOrders[0]];
 case 'appointments':return [{...demoOrders[1],preferredVisit:exampleVisit,appointmentStatus:'RESCHEDULE_REQUESTED'}];
 case 'coordination':return [{...demoOrders[4],status:'WAITING_MANUAL_REVIEW'}];
 case 'returns':return [{...demoOrders[3],assignmentStatus:'RETURN_REQUESTED'}];
 case 'pool':return [{...demoOrders[6],riskFlag:false,preferredVisit:exampleVisit}];
 case 'pool-blocked':return [{...demoOrders[6],preferredVisit:{startAt:'2026-11-06T09:00:00Z',endAt:'2026-11-06T11:00:00Z'}}];
 case 'pool-claimed':return [{...demoOrders[1],preferredVisit:exampleVisit,latestAssigneeName:'示例师傅甲',latestAssignmentStatus:'ACTIVE'}];
 case 'pool-direct':return [{...demoOrders[0],preferredVisit:exampleVisit,latestAssigneeName:'示例师傅乙',latestAssignmentStatus:'PENDING',assignmentStatus:'PENDING'}];
 case 'pool-withdrawn':return [{...demoOrders[0],preferredVisit:exampleVisit}];
 case 'staff':return [{id:'9201',username:'technician-demo',name:'示例师傅甲',phone:null,role:'TECHNICIAN',status:'ENABLED',version:1,bindingStatus:'ACTIVE',bindingApplicationPhone:'138****0000'},{id:'9202',username:'dispatcher-demo',name:'示例调度员',phone:null,role:'DISPATCHER',status:'ENABLED',version:0,bindingStatus:null,bindingApplicationPhone:null}];
 case 'customers':return [{id:'9701',nickname:'示例用户',maskedPhone:'139****0001',phoneVerified:true,status:'ENABLED',createdAt:'2026-11-06T09:00:00Z',lastLoginAt:'2026-11-07T09:00:00Z'}];
 case 'bindings':return [{id:'9301',staffId:'9201',staffName:'示例师傅甲',applicantName:'示例申请人',applicantContactPhone:'138****0000',contactPhoneVerified:false,status:'PENDING',appliedAt:'2026-11-06T09:00:00Z',version:0}];
 case 'invitations':return [{id:'9401',staffId:'9201',status:'ISSUED',createdAt:'2026-11-06T08:00:00Z',expiresAt:'2026-11-07T08:00:00Z',version:0}];
 case 'safety':return [
  {id:'9501',workOrderId:'9106',status:'OPEN',matchedRules:[{ruleCode:'ELECTRIC_LEAKAGE',title:'漏电风险'}],acknowledgedAt:null,createdAt:'2026-11-06T07:10:00Z',version:0},
  {id:'9500',workOrderId:'9101',status:'RESOLVED',matchedRules:[{ruleCode:'SMOKE_FIRE',title:'冒烟着火风险'}],acknowledgedAt:'2026-11-06T06:20:00Z',resolvedAt:'2026-11-06T06:45:00Z',createdAt:'2026-11-06T06:10:00Z',version:2}
 ];
 case 'approvals':return [{id:'9601',quoteId:'9701',status:'PENDING',overrideReason:'现场设备型号特殊，需独立核对配件范围',requestedBy:'9201',reviewedBy:null,version:0}];
 case 'urges':case 'handoffs':return [{id:'9801',workOrderId:'9101',kind:page==='urges'?'URGE':'HUMAN_HANDOFF',status:'PENDING',reason:'客户希望确认本次上门安排',createdAt:'2026-11-06T08:20:00Z',handledAt:null,version:0}];
 case 'handoff-history':return [...demoList('handoffs'),{id:'9802',workOrderId:'9101',kind:'HUMAN_HANDOFF',status:'HANDLED',reason:'客户希望核实上门时间',handlingConclusion:'双方已确认上门安排',handledBy:{id:'9001',name:'运营管理员'},createdAt:'2026-11-06T08:20:00Z',handledAt:'2026-11-06T09:00:00Z',version:1}];
 case 'services':return [{id:'9901',code:'AC-INSPECT',name:'空调现场检测',applianceType:'AIR_CONDITIONER',description:'检查设备运行状态并记录现场事实',status:'ENABLED',version:0}];
 case 'pricing':return [{id:'9911',serviceItemId:'9901',title:'空调基础检测',feeType:'INSPECTION',clauseNo:'AC-01',ruleVersion:'2026.1',fixedPrice:'60.00',minPrice:null,maxPrice:null,status:'ENABLED',effectiveFrom:'2026-11-01T00:00:00Z',version:0}];
 case 'audit':return [
  {id:'9994',operatorType:'STAFF',operatorId:'9001',action:'STAFF_LOGIN',resourceType:'STAFF_USER',resourceId:'9001',occurredAt:'2026-11-06T10:00:00Z'},
  {id:'9993',operatorType:'STAFF',operatorId:'9201',action:'JOB_POOL_CLAIMED',resourceType:'JOB_POOL_ENTRY',resourceId:'9982',occurredAt:'2026-11-06T09:55:00Z'},
  {id:'9992',operatorType:'STAFF',operatorId:'9001',action:'JOB_POOL_PUBLISHED',resourceType:'JOB_POOL_ENTRY',resourceId:'9982',occurredAt:'2026-11-06T09:50:00Z'},
  {id:'9991',operatorType:'CUSTOMER',operatorId:'9931',action:'REPAIR_CONVERTED',resourceType:'REPAIR_REQUEST',resourceId:'9981',occurredAt:'2026-11-06T09:40:00Z'},
  {id:'9990',operatorType:'STAFF',operatorId:'9001',action:'REPAIR_MANUALLY_TRIAGED',resourceType:'REPAIR_REQUEST',resourceId:'9981',occurredAt:'2026-11-06T09:30:00Z'},
 ];
 default:return [];
 }}
export function demoDetail(page:string,row:Dict):Dict{
 if(page==='audit'){
  const triage=row.action==='REPAIR_MANUALLY_TRIAGED';
  const converted=row.action==='REPAIR_CONVERTED';
  return {
   summary:{...row,traceId:'synthetic-audit-trace',reason:triage?'已记录业务依据（此查询不展示原文）':null},
   beforeSummary:triage?JSON.stringify({version:1}):null,
   afterSummary:triage?JSON.stringify({version:2}):converted?JSON.stringify({workOrderId:'9100'}):null,
   proxyTechnicianId:null,
  };
 }
 if(['orders','appointments'].includes(page))return {visitPolicy:'CUSTOMER_SLOT',preferredVisit:{startAt:'2026-11-07T05:00:00Z',endAt:'2026-11-07T07:00:00Z'},workOrder:{...row,symptomDescription:row.symptomSummary,brandCode:'示例品牌',modelCode:'示例型号',mediaFiles:[],appointment:row.status==='WAITING_ASSIGNMENT'?null:{id:'9951',status:'CONFIRMED',scheduledStartAt:'2026-11-07T05:00:00Z',scheduledEndAt:'2026-11-07T07:00:00Z',note:'客户选择时段，师傅确认履约'},timeline:[{eventType:'WORK_ORDER_STATUS',occurredAt:'2026-11-06T08:20:00Z',title:'工单已创建',detail:'客户确认生成维修工单'},{eventType:'WORK_ORDER_STATUS',occurredAt:'2026-11-06T09:20:00Z',title:'工单进度更新',detail:'平台核对后承接服务'}],quoteHistory:[],serviceRecords:[]},repairRequestId:'9981',repairNo:row.repairNo,safetyBlocked:row.riskFlag||false,appointmentStatus:row.appointmentStatus||null,assigneeName:row.currentAssigneeName,serviceContact:{contactName:row.status==='COMPLETED'?null:'示例联系人',contactPhone:row.status==='COMPLETED'?null:'138****0000',addressText:row.status==='COMPLETED'?null:'示例市示例区 · 合成地址',contactSource:'SUBMISSION_SNAPSHOT'},assignment:row.currentAssigneeId||row.latestAssigneeName?{id:'9971',technicianId:'9201',status:row.assignmentStatus||'ACTIVE',allocationSource:row.assignmentStatus==='PENDING'?'DIRECT':'SELF_CLAIM',version:0}:null,serviceRecords:[],allowedActions:row.status==='WAITING_ASSIGNMENT'?['ASSIGN','CANCEL']:['COORDINATE'],appointmentVersion:0};
 if(page==='intake')return {repairRequest:{...row,symptomDescription:row.symptomSummary,mediaFiles:[],safetyAnswers:{smokeOrBurningSmell:false,electricLeakage:false,gasSmell:false,suspectedRefrigerantLeak:false,severeWaterIngress:false}},serviceContact:{contactName:'示例联系人',contactPhone:'138****0000',addressText:'示例市示例区 · 合成地址',contactSource:'SUBMISSION_SNAPSHOT'},allowedActions:row.status==='TRIAGED'?[]:['MANUAL_TRIAGE']};return row;}
