import {ApiError,Dict,may,Profile} from './core.js';
export const workspaces={tasks:'员工待办',pool:'接单大厅管理',people:'人员与授权',settings:'服务与定价',cancellations:'客户取消记录'} as const;
export interface PageDef{id:string;title:string;group:string;icon:string;description:string;op:string;columns:string[];detail?:string;filters?:string[];create?:string;workspace?:keyof typeof workspaces;fixedQuery?:Dict}
const queuePage=(id:string,title:string,description:string,queue:string,workspace:'tasks'|'pool',columns:string[],filters=['keyword']):PageDef=>({
 id,title,description,workspace,group:'服务运营',icon:workspace==='pool'?'orders':'bell',op:'listAdminWorkOrders',detail:'getAdminWorkOrder',columns,filters,fixedQuery:{queue}
});
const dispatchColumns=['workOrderNo','applianceType','symptomSummary','preferredVisit','status'];
const poolHistoryColumns=['workOrderNo','applianceType','preferredVisit','currentAssigneeName','status'];
const assignmentProgressColumns=['workOrderNo','applianceType','latestAssigneeName','latestAssignmentStatus','status','preferredVisit','appointmentStartAt'];
export const pages:PageDef[]=[
 {id:'dashboard',title:'运营工作台',group:'总览',icon:'home',description:'掌握服务进度，及时处理每一项待办',op:'getAdminDashboard',columns:[]},
 {id:'orders',title:'工单中心',group:'服务运营',icon:'orders',description:'从接单到交付，跟进每一笔维修服务',op:'listAdminWorkOrders',detail:'getAdminWorkOrder',columns:['workOrderNo','applianceType','symptomSummary','status','currentAssigneeName','assignmentStatus','appointmentStartAt','riskFlag','createdAt'],filters:['keyword','status','applianceType','technicianId','riskFlag','appointmentDate','brandCode','coverageStatus']},
 {id:'cancelled-repairs',title:'已取消报修',workspace:'cancellations',group:'服务运营',icon:'orders',description:'查看客户在生成维修工单前取消的报修。',op:'listAdminRepairRequests',detail:'getAdminRepairRequest',columns:['repairNo','applianceType','symptomSummary','repairStatus','createdAt'],fixedQuery:{status:'CANCELLED'}},
 {id:'cancelled-orders',title:'已取消工单',workspace:'cancellations',group:'服务运营',icon:'orders',description:'查看客户取消的维修工单和原处理记录。',op:'listAdminWorkOrders',detail:'getAdminWorkOrder',columns:['workOrderNo','repairNo','applianceType','status','createdAt'],filters:['keyword'],fixedQuery:{queue:'CUSTOMER_CANCELLED'}},
 {id:'quote-records',title:'报价记录',group:'服务运营',icon:'price',description:'查看已发给客户的报价和取消报修产生的上门费。「已完成」表示客户已确认该版报价。「上门费已登记」表示已记录应收金额。',op:'listAdminQuoteRecords',columns:['workOrderNo','quoteNo','quoteType','amount','visitFeeAmount','status','recordedAt'],filters:['keyword','status']},
 {id:'intake',title:'报修核对',workspace:'tasks',group:'服务运营',icon:'bell',description:'核对设备、故障、服务联系和安全信息。核对完成后，报修移到「待客户确认」。',op:'listAdminRepairRequests',detail:'getAdminRepairRequest',columns:['repairNo','applianceType','symptomSummary','repairStatus','createdAt'],fixedQuery:{pendingReview:true}},
 {id:'intake-reviewed',title:'待客户确认',workspace:'tasks',group:'服务运营',icon:'bell',description:'普通报修等待客户确认生成工单。安全解除后继续维修的工单等待客户选上门时段，选定后进入「待分派」。',op:'listAdminRepairRequests',detail:'getAdminRepairRequest',columns:['repairNo','applianceType','symptomSummary','repairStatus','createdAt'],fixedQuery:{pendingCustomerConfirmation:true}},
 queuePage('dispatch','待分派','工单生成后，可在详情定向派单，或在条件齐全时发布到接单大厅。定向派出后到「定向派单进度」查看师傅是否接单。','DISPATCH','tasks',dispatchColumns),
 queuePage('appointments','预约待确认','处理已接单后尚未确认的上门安排或客户改约。师傅接单时已确认预约的工单可在工单中心查看。','APPOINTMENT','tasks',['workOrderNo','currentAssigneeName','preferredVisit','appointmentStartAt','appointmentStatus','status'],['keyword','technicianId','appointmentDate']),
 queuePage('coordination','工单协调','查看协调原因。验收异议由客户先选择返修时段，管理员再定向派给原维修人员。','COORDINATION','tasks',['workOrderNo','coordinationReason','currentAssigneeName','status','createdAt']),
 queuePage('quote-rejected','报价被拒','查看待平台协调及待师傅重报的工单。新报价发给客户确认后，本待办结束。','QUOTE_REJECTED','tasks',['workOrderNo','coordinationReason','currentAssigneeName','status','createdAt']),
 queuePage('returns','退回审核','查看师傅提交的退回申请，在工单详情中决定继续履约或改派。','RETURN','tasks',['workOrderNo','currentAssigneeName','symptomSummary','assignmentStatus','status']),
 {id:'handoffs',title:'转人工请求',workspace:'tasks',group:'服务运营',icon:'bell',description:'处理转人工、验收异议等事项。可在「人工请求记录」查看各状态的请求。',op:'listHumanHandoffs',columns:['id','workOrderId','status','reason','createdAt'],fixedQuery:{status:'PENDING'}},
 {id:'approvals',title:'价格例外审批',workspace:'tasks',group:'服务运营',icon:'price',description:'审核超出平台价格区间的申请。区间内报价由师傅直接发给客户；例外获批后仍需客户确认。',op:'listPendingOverrideApprovals',columns:['id','quoteId','status','overrideReason','requestedBy','reviewedBy','version'],filters:['status']},
 queuePage('pool','大厅可接','这里显示已发布且师傅当前可接的工单。发布新工单请到「员工待办 → 待分派」。','POOL_LIVE','pool',dispatchColumns),
 queuePage('pool-blocked','发布需跟进','发布仍在，但因时段失效或工单条件变化，师傅当前无法接单。可核对条件或撤回。','POOL_BLOCKED','pool',dispatchColumns),
 queuePage('pool-claimed','自主接单进度','跟进师傅从大厅接走的工单，查看接单人、预约及履约状态。改派后按最新分派方式归类。','SELF_CLAIM_PROGRESS','pool',assignmentProgressColumns,['keyword','status']),
 queuePage('pool-direct','定向派单进度','跟进平台指定师傅的工单，包括待接单、已接单、拒接和履约结果。查看分派历史可追溯每次派单。','DIRECT_PROGRESS','pool',assignmentProgressColumns,['keyword','status']),
 queuePage('pool-withdrawn','已撤回','每张工单的最新大厅发布已撤回。仍需分派的工单可从「员工待办 → 待分派」继续处理。','POOL_WITHDRAWN','pool',poolHistoryColumns),
 {id:'safety',title:'安全事件',group:'待办与审核',icon:'shield',description:'先确认命中的风险，再根据实际核实依据解除。解除后可打开关联工单继续协调。',op:'listSafetyEvents',columns:['id','workOrderId','status','matchedRules','acknowledgedAt','resolvedAt','createdAt'],filters:['status']},
 {id:'urges',title:'客户催办',group:'待办与审核',icon:'clock',description:'核对客户诉求并记录实际处理结论',op:'listUrges',columns:['id','workOrderId','status','reason','createdAt','handledAt'],filters:['status']},
 {id:'handoff-history',title:'人工请求记录',group:'待办与审核',icon:'bell',description:'查看新提交和已处理的人工请求，可按状态筛选。待处理请求可在详情中记录协调结论。',op:'listHumanHandoffs',columns:['id','workOrderId','status','reason','createdAt','handlingConclusion','handledBy','handledAt'],filters:['status']},
 {id:'staff',title:'员工档案',workspace:'people',group:'系统管理',icon:'users',description:'查看员工岗位、联系方式与账号状态。绑定申请电话由师傅本人填写。',op:'listStaff',columns:['username','name','role','phone','status'],create:'createStaff'},
 {id:'customers',title:'用户档案',workspace:'people',group:'系统管理',icon:'users',description:'按用户编号或昵称查询小程序用户。手机号脱敏显示。',op:'listAdminCustomers',columns:['id','nickname','maskedPhone','phoneVerified','status','createdAt','lastLoginAt'],filters:['keyword','status']},
 {id:'bindings',title:'维修身份审核',workspace:'people',group:'系统管理',icon:'shield',description:'核实申请人与员工身份后，授予微信维修权限',op:'listStaffBindings',detail:'getStaffBinding',columns:['staffName','applicantName','applicantContactPhone','contactPhoneVerified','status','appliedAt'],filters:['status']},
 {id:'invitations',title:'维修邀请',workspace:'people',group:'系统管理',icon:'users',description:'私发一次性邀请给新人。新人提交申请后，到「维修身份审核」核实并开通权限。',op:'listStaffInvitations',detail:'getStaffInvitation',columns:['id','staffId','status','createdAt','expiresAt','consumedAt'],filters:['staffId','status']},
 {id:'services',title:'服务项目',workspace:'settings',group:'系统管理',icon:'tool',description:'维护可提供的设备维修服务与启用状态',op:'listServiceItems',detail:'getServiceItems',columns:['code','name','applianceType','description','status'],create:'createServiceItems'},
 {id:'pricing',title:'价格规则',workspace:'settings',group:'系统管理',icon:'price',description:'维护固定价、价格区间、有效期与退费条款',op:'listPricingRules',detail:'getPricingRules',columns:['title','serviceItemId','feeType','clauseNo','ruleVersion','fixedPrice','minPrice','maxPrice','status','effectiveFrom'],create:'createPricingRules'},
 {id:'audit',title:'操作审计',group:'系统管理',icon:'audit',description:'只读回顾操作主体、业务对象及变更摘要',op:'listAuditLogs',detail:'getAuditLogDetail',columns:['id','operatorType','operatorId','action','resourceType','resourceId','occurredAt'],filters:['workOrderId']}
];
export const visiblePages=(p:Profile)=>pages.filter(x=>may(x.op,p));
export const sidebarPages=(available:PageDef[])=>available.filter((page,index)=>!page.workspace||available.findIndex(x=>x.workspace===page.workspace)===index);
export function pageQuery(page:PageDef,query:Dict):Dict {
 const filters={...query,...page.fixedQuery};
 if(['safety','handoff-history'].includes(page.id)&&!filters.status)filters.status='ALL';
 return filters;
}
export const queryMismatchCode='ADMIN_QUERY_MISMATCH';
const queryMismatch=()=>new ApiError('后端未返回与当前筛选一致的数据。请按项目启动脚本重启新版后端，再重新读取。',409,queryMismatchCode);
export function validatePageRows(filters:Dict,items:Dict[]):void {
 if(filters.queue){
  // 队列筛选与这两个可空字段同版交付。旧接口会忽略 queue 并返回全量工单。
  if(items.some(row=>!('preferredVisit' in row)||!('appointmentStatus' in row)))throw queryMismatch();
  if(['POOL_READY','POOL_LIVE'].includes(filters.queue)&&items.some(row=>row.status!=='WAITING_ASSIGNMENT'||!row.preferredVisit))throw queryMismatch();
  if(['DIRECT_PROGRESS','SELF_CLAIM_PROGRESS'].includes(filters.queue)&&items.some(row=>!row.latestAssignmentStatus))throw queryMismatch();
  if(filters.queue==='CUSTOMER_CANCELLED'&&items.some(row=>row.status!=='CANCELLED'))throw queryMismatch();
  if(filters.queue==='QUOTE_REJECTED'&&items.some(row=>!['WAITING_MANUAL_REVIEW','WAITING_QUOTE'].includes(row.status)||
     !['INITIAL_QUOTE_REJECTED','ADDITIONAL_QUOTE_REJECTED'].includes(row.coordinationCause)))throw queryMismatch();
 }
 if(filters.pendingReview&&items.some(row=>!['SUBMITTED','HUMAN_HANDOFF'].includes(row.repairStatus)||row.workOrderId!=null))throw queryMismatch();
 if(filters.pendingCustomerConfirmation&&items.some(row=>!(
  (row.repairStatus==='TRIAGED'&&row.workOrderId==null)
  ||(row.repairStatus==='HIGH_RISK_BLOCKED'&&row.workOrderId!=null&&row.workOrderStatus==='WAITING_CUSTOMER_VISIT')
 )))throw queryMismatch();
}
export function pageReadError(page:PageDef,filters:Dict,error:unknown):unknown {
 if(filters.queue&&error instanceof ApiError&&error.status===400&&error.code==='VALIDATION_ERROR'&&error.message==='工单队列不正确')return queryMismatch();
 if(page.id==='safety'&&filters.status==='ALL'&&error instanceof ApiError&&error.status===400&&error.code==='VALIDATION_ERROR'&&error.message==='安全状态不正确'){
  return new ApiError('当前后端版本不支持查询全部安全事件。请重启新版后端，或先分别查询「待解除」和「已解除」。',409,queryMismatchCode);
 }
 return error;
}
export interface Action {op:string;label:string;action?:string;args?:Dict;seed?:Dict;danger?:boolean}
export const canInviteStaff=(row:Dict)=>row.role==='TECHNICIAN'&&row.status==='ENABLED'&&row.bindingStatus===null;
export const orderActions:Action[]=[
 {op:'assignWorkOrder',label:'派单 / 改派',action:'ASSIGN'},
 {op:'adminTakeover',label:'管理员接管',action:'TAKEOVER'},
 {op:'adminCoordination',label:'协调处理',action:'COORDINATE'},
 {op:'createWorkOrderAppointment',label:'协调预约',action:'APPOINT'},
 {op:'confirmWorkOrderAppointment',label:'确认当前预约',action:'CONFIRM_APPOINTMENT'},
 {op:'adminStartInspection',label:'开始现场检测',action:'START_INSPECTION'},
 {op:'adminCreateServiceRecord',label:'提交检测记录',action:'SUBMIT_INSPECTION',seed:{kind:'INSPECTION'}},
 {op:'adminCreateServiceRecord',label:'记录维修服务',action:'RECORD_SERVICE',seed:{kind:'REPAIR'}},
 {op:'adminCreateQuote',label:'创建报价',action:'CREATE_QUOTE',seed:{quoteType:'INITIAL'}},
 {op:'adminCreateQuote',label:'创建追加报价',action:'CREATE_ADDITIONAL_QUOTE',seed:{quoteType:'ADDITIONAL'}},
 {op:'adminCompletion',label:'提交完工',action:'SUBMIT_COMPLETION'},
 {op:'adminCancel',label:'取消工单',action:'CANCEL',danger:true},
 {op:'adminCloseUnresolved',label:'未解决关闭',action:'CLOSE_UNRESOLVED',danger:true}
];
export function resourceActions(page:string,row:Dict):Action[]{
 switch(page){
 case 'intake':case 'intake-reviewed':return [{op:'manualTriageRepairRequest',label:'核对报修',action:'MANUAL_TRIAGE'},{op:'confirmRepairServiceContact',label:'确认服务联系',action:'CONFIRM_SERVICE_CONTACT'}];
 case 'staff':return [{op:'setStaffStatus',label:row.status==='ENABLED'?'停用员工':'启用员工',seed:{status:row.status==='ENABLED'?'DISABLED':'ENABLED'},danger:row.status==='ENABLED'},{op:'issueStaffInvitation',label:'签发维修邀请'},{op:'resetStaffPassword',label:'重置登录密码',danger:true}].filter(x=>x.op!=='issueStaffInvitation'||canInviteStaff(row));
 case 'bindings':return row.status==='PENDING'?[{op:'confirmStaffBinding',label:'确认身份'},{op:'rejectStaffBinding',label:'拒绝申请',danger:true}]:row.status==='ACTIVE'?[{op:'revokeStaffBinding',label:'撤销授权',danger:true}]:[];
 case 'invitations':return row.status==='ISSUED'?[{op:'revokeStaffInvitation',label:'撤销邀请',danger:true}]:[];
 case 'safety':return row.status!=='OPEN'?[]:row.acknowledgedAt?[{op:'resolveSafetyEvent',label:'核实解除风险',danger:true}]:[{op:'acknowledgeSafetyEvent',label:'确认风险'}];
 case 'urges':return row.status==='PENDING'?[{op:'resolveUrges',label:'记录催办结论'}]:[];
 case 'handoffs':case 'handoff-history':return row.status==='PENDING'?[{op:'resolveHumanHandoffs',label:'记录协调结论'}]:[];
 case 'approvals':return row.status==='PENDING'?[{op:'approveQuoteOverride',label:'批准申请'},{op:'rejectQuoteOverride',label:'拒绝申请',danger:true}]:[];
 case 'services':return [{op:'updateServiceItems',label:'编辑服务项目'},{op:'statusServiceItems',label:row.status==='ENABLED'?'停用项目':'启用项目',seed:{status:row.status==='ENABLED'?'DISABLED':'ENABLED'}}];
 case 'pricing':return [{op:'updatePricingRules',label:'编辑价格规则'},{op:'statusPricingRules',label:row.status==='ENABLED'?'停用规则':'启用规则',seed:{status:row.status==='ENABLED'?'DISABLED':'ENABLED'}}];
 default:return [];
 }
}
export function permitted(actions:Action[],row:Dict,profile:Profile|null){return actions.filter(x=>may(x.op,profile)&&(!x.action||Array.isArray(row.allowedActions)&&row.allowedActions.includes(x.action)));}
