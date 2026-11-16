import {escape,Dict} from './core.js';
export const words:Record<string,string>={
 INITIAL_QUOTE_REJECTED:'客户拒绝首版报价',ADDITIONAL_QUOTE_REJECTED:'客户拒绝追加报价',ACCEPTANCE_ISSUE:'验收有问题',
 SAFETY_REVIEW:'安全事件后续处理',MANUAL:'人工协调',CANCEL_REJECTED_QUOTE:'确认客户意愿后取消工单',SCHEDULE_REWORK:'安排原师傅返修',
 WAITING_CUSTOMER_VISIT:'待选上门时段',CONTINUE_AFTER_SAFETY:'继续安排维修',
 CANCELLATION_VISIT_FEE:'取消报修上门费',VISIT_FEE_RECORDED:'上门费已登记',
 AIR_SOURCE_HEAT_PUMP:'空气源热泵',REGISTERED:'已登记',PENDING_SOURCE:'待补来源',COVERED:'已覆盖',CONFLICTED:'信息冲突',VISIT:'上门费',OTHER:'其他费用',ACCEPTED:'已接受',CUSTOMER:'客户',STAFF:'员工',SYSTEM:'系统',ADMIN:'管理员',DISPATCHER:'调度人员',TECHNICIAN:'维修人员',KNOWLEDGE_MANAGER:'配置管理员',AUDITOR:'审计员',ENABLED:'启用',DISABLED:'停用',SUBMITTED:'已提交',DRAFT:'草稿',TRIAGED:'已核对',CONVERTED:'已生成工单',CANCELLED:'已取消',HIGH_RISK_BLOCKED:'安全阻断',HUMAN_HANDOFF:'转人工',PENDING:'待处理',ACTIVE:'履约中',RETURN_REQUESTED:'退回待审',DECLINED:'已拒接',FINISHED:'已结束',REASSIGNED:'已改派',REVOKED:'已撤销',ISSUED:'待使用',CONSUMED:'已使用',EXPIRED:'已过期',HANDLED:'已处理',OPEN:'待解除',RESOLVED:'已解除',APPROVED:'已批准',REJECTED:'已拒绝',INVALIDATED:'已失效',PUBLISHED:'大厅可接',CLAIMED:'已接走',WITHDRAWN:'已撤回',AIR_CONDITIONER:'空调',REFRIGERATOR:'冰箱',WASHING_MACHINE:'洗衣机',WAITING_MANUAL_REVIEW:'人工协调',WAITING_ASSIGNMENT:'待分派',WAITING_APPOINTMENT:'待确认预约',APPOINTED:'已预约',IN_INSPECTION:'现场检测',WAITING_QUOTE:'待报价',WAITING_QUOTE_CONFIRMATION:'待客户确认',IN_REPAIR:'维修中',PAUSED_FOR_REQUOTE:'追加待确认',WAITING_ACCEPTANCE:'待客户验收',COMPLETED:'已完成',CLOSED_UNRESOLVED:'未解决关闭',CONFIRMED:'已确认',RESCHEDULE_REQUESTED:'申请改约',MISSED:'已爽约',INITIAL:'初始报价',ADDITIONAL:'追加报价',DIRECT:'平台派单',SELF_CLAIM:'自主接单',INSPECTION:'检测',REPAIR:'维修',URGE:'催办',CUSTOMER_SLOT:'用户时段确认',LEGACY_PLATFORM:'历史预约流程',TECHNICIAN_SELF:'维修人员本人',DISPATCHER_PROXY:'调度代录',ADMIN_TAKEOVER:'管理员接管',REQUOTE:'重新报价',ABANDON_ADDITIONAL:'放弃追加',RESUME_REPAIR:'恢复维修',RETURN_TO_ASSIGNMENT:'转为待分派',ESCALATE_TO_MANUAL:'升级人工处理',CURRENT_ADDRESS:'当前地址候选',SUBMISSION_SNAPSHOT:'提交时联系信息',MANUAL_REVIEW_SNAPSHOT:'人工确认快照',UNAVAILABLE:'未确认',true:'是',false:'否',CNY:'人民币',DIAGNOSIS:'检测费',LABOR:'人工费',PART:'配件费',SERVICE:'服务费',MATERIAL:'材料费'
};
export const labels:Record<string,string>={id:'记录编号',repairStatus:'报修状态',appointmentStatus:'预约状态',latestAssigneeName:'最近分派对象',latestAssignmentStatus:'分派结果',workOrderId:'工单编号',workOrderNo:'维修工单号',repairRequestId:'报修编号',repairNo:'报修单号',status:'状态',applianceType:'设备类型',brandCode:'品牌编码',modelCode:'型号',symptomSummary:'故障摘要',symptomDescription:'故障描述',riskFlag:'安全风险',technicianId:'维修人员',currentAssigneeId:'当前负责人编号',currentAssigneeName:'当前负责人',assignmentStatus:'分派状态',appointmentStartAt:'上门时间',createdAt:'创建时间',updatedAt:'更新时间',version:'数据版本',versionNo:'报价版本',name:'姓名 / 名称',username:'登录账号',phone:'员工联系电话',bindingApplicationPhone:'绑定申请电话（自填）',bindingStatus:'维修身份状态',nickname:'用户昵称',maskedPhone:'手机号（脱敏）',phoneVerified:'手机号已验证',lastLoginAt:'最近登录',role:'岗位角色',applicantName:'申请人姓名',applicantContactPhone:'申请人联系电话',contactPhoneVerified:'电话已验证',staffId:'员工编号',customerId:'用户编号',staffName:'员工姓名',appliedAt:'申请时间',confirmedAt:'确认时间',handledAt:'处理时间',handledBy:'处理人',handlingConclusion:'处理结论',reason:'处理原因',reviewNote:'核对说明',informationConfirmed:'已核对设备、故障与联系信息',safetyReviewed:'已核对安全答案及风险',expectedAddressVersion:'地址候选版本',contactName:'联系人',contactPhone:'联系电话',addressText:'服务地址',contactSource:'联系信息来源',addressVersion:'地址版本',phoneMasked:'电话已脱敏',manualReviewedBy:'核对人员',manualReviewedAt:'核对时间',manualReviewNote:'核对依据',kind:'记录类型',facts:'现场事实',summary:'服务摘要',correctsRecordId:'修正原记录',startAt:'开始时间',endAt:'结束时间',scheduledStartAt:'预约开始',scheduledEndAt:'预约结束',note:'预约备注',confirmed:'双方已确认预约',approved:'同意退回并改派',replacementTechnicianId:'接替维修人员',resolution:'协调处理方式',decisionReason:'审核说明',reviewComment:'审核意见',overrideReason:'超区间申请依据',initialPassword:'初始密码（六位数字）',currentPassword:'当前密码',newPassword:'新密码（六位数字）',temporaryPassword:'临时密码（六位数字）',password:'密码',code:'服务编码',description:'说明',serviceItemId:'服务项目',pricingRuleId:'价格规则',feeType:'费用类型',clauseNo:'条款编号',ruleVersion:'规则版本',title:'标题',unit:'计价单位',fixedPrice:'固定单价',minPrice:'最低单价',maxPrice:'最高单价',refundPolicy:'退费规则',effectiveFrom:'生效时间',effectiveTo:'失效时间',quoteType:'报价类型',previousQuoteId:'前一版报价',baseQuoteId:'最近已接受报价',serviceRecordId:'检测记录依据',submissionSource:'提交方式',expiresAt:'有效期至',items:'报价项目',quantity:'数量（最多三位小数）',selectedUnitPrice:'选定单价',pricingReason:'选价依据',itemName:'项目名称',specification:'规格型号',partSourceNote:'配件来源',additionReason:'追加原因',changeReason:'变更原因',quoteNo:'报价单号',subtotalAmount:'本次报价合计',confirmedAmountBefore:'此前已确认金额',currentAdditionalAmount:'本次追加金额',payableTotalAfterAcceptance:'接受后累计应付',contentHash:'报价内容指纹',quoteContentHash:'申请关联指纹',quoteId:'报价编号',requestedBy:'申请人员',reviewedBy:'审核人员',assignedBy:'分派人员',assignedAt:'分派时间',acceptedAt:'接单时间',endedAt:'结束时间',endedBy:'结束人员',endReason:'结束原因',allocationSource:'接单来源',invitationId:'邀请编号',invitationCode:'维修邀请码',entryPage:'小程序入口',revocationReason:'撤销原因',revokedAt:'撤销时间',createdBy:'创建人员',consumedAt:'使用时间',consumedBindingId:'使用绑定编号',revokedBy:'撤销人员',acknowledgedAt:'确认时间',acknowledgedBy:'确认人员',acknowledgementNote:'风险确认依据',resolvedBy:'解除人员',resolvedAt:'解除时间',resolutionBasis:'解除依据',matchedRules:'命中风险规则',submittedAt:'提交时间',submittedBy:'提交人员',confirmedBy:'确认人员',rejectedBy:'拒绝人员',rejectedAt:'拒绝时间',occurredAt:'故障发生时间',technicianName:'维修人员',visitPolicy:'预约机制',preferredVisit:'客户期望时段',action:'操作',actorType:'操作者类型',actorId:'操作者编号',targetType:'资源类型',targetId:'资源编号',operatorName:'操作人员',operatorType:'操作者类型',operatorId:'操作者编号',operation:'操作',resourceType:'资源类型',resourceId:'资源编号',requestId:'请求标识',proxyTechnicianId:'代录维修人员',beforeSummary:'变更前摘要',afterSummary:'变更后摘要',eventType:'事件',fromStatus:'原状态',toStatus:'新状态',ruleCode:'风险规则',message:'说明',confidence:'置信度',coverageStatus:'覆盖情况',keyword:'工单号 / 设备 / 故障',appointmentDate:'上门日期',workOrder:'工单信息',repairRequest:'报修信息',serviceContact:'服务联系',assignment:'当前分派',appointment:'当前预约',serviceRecords:'服务记录',timeline:'处理时间线',quote:'报价',approval:'超区间审批',currentQuote:'当前报价',quoteHistory:'报价历史',acceptance:'客户验收',addressSnapshot:'地址快照',safetyAnswers:'安全答案',smokeOrBurningSmell:'烟雾或焦味',electricLeakage:'漏电迹象',gasSmell:'燃气气味',suspectedRefrigerantLeak:'疑似制冷剂泄漏',severeWaterIngress:'严重进水',safetyEvents:'安全事件',humanHandoff:'人工处理',manualReview:'人工核对'
};
Object.assign(labels,{amount:'金额',visitFeeAmount:'上门费',recordedAt:'记录时间'});
export const label=(key:string)=>key==='coordinationReason'?'协调原因':labels[key]||key;
export const text=(v:any):string=>v==null||v===''?'—':words[String(v)]||String(v);
export function dateText(value:any){if(!value)return '—';const d=new Date(value);return Number.isNaN(d.valueOf())?text(value):new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hour12:false}).format(d).replaceAll('/','-');}
export function tone(s:any){return ['OPEN','HIGH_RISK_BLOCKED','DISABLED','REJECTED','MISSED'].includes(s)?'red':['COMPLETED','CONFIRMED','ENABLED','HANDLED','ACTIVE','APPROVED'].includes(s)?'green':['CANCELLED','EXPIRED','REVOKED','FINISHED','WITHDRAWN'].includes(s)?'gray':'orange';}
export const badge=(v:any)=>`<span class="badge ${tone(v)}">${escape(text(v))}</span>`;
const auditActions: Record<string, string> = {
 VISIT_CANCELLATION_FEE_RECORDED: '登记取消报修上门费',
 STAFF_LOGIN: '员工登录',
 STAFF_LOGIN_REJECTED: '员工登录未通过',
 STAFF_LOGOUT: '员工退出登录',
 STAFF_RATE_LIMITED: '员工认证请求过多',
 STAFF_PASSWORD_CHANGED: '修改员工密码',
 STAFF_PASSWORD_RESET: '重置员工密码',
 STAFF_ADMIN_BOOTSTRAPPED: '初始化管理员',
 STAFF_ADMIN_OFFLINE_RECOVERED: '离线恢复管理员账号',
 STAFF_CREATED: '创建员工档案',
 STAFF_STATUS_CHANGED: '变更员工状态',
 STAFF_DISABLED_ORDER_COORDINATION: '员工停用后转人工协调',
 STAFF_INVITATION_ISSUED: '签发维修邀请',
 STAFF_INVITATION_REVOKED: '撤销维修邀请',
 STAFF_BINDING_APPLIED: '申请维修身份绑定',
 STAFF_BINDING_ACTIVE: '确认维修身份绑定',
 STAFF_BINDING_REJECTED: '拒绝维修身份绑定',
 STAFF_BINDING_REVOKED: '撤销维修身份绑定',
 CUSTOMER_LOGIN: '客户登录',
 CUSTOMER_LOGOUT: '客户退出登录',
 CUSTOMER_TOKEN_REFRESHED: '续期客户登录',
 CUSTOMER_PROFILE_UPDATED: '更新客户资料',
 ADDRESS_CREATED: '新增服务地址',
 ADDRESS_UPDATED: '更新服务地址',
 ADDRESS_DISABLED: '停用服务地址',
 FILE_UPLOADED: '上传文件',
 REPAIR_DRAFT_CREATED: '新建报修草稿',
 REPAIR_DRAFT_UPDATED: '更新报修草稿',
 REPAIR_SUBMITTED: '提交报修',
 REPAIR_HIGH_RISK_BLOCKED: '报修触发安全阻断',
 REPAIR_MANUALLY_TRIAGED: '人工核对报修',
 REPAIR_SERVICE_CONTACT_CONFIRMED: '确认报修服务联系信息',
 REPAIR_CONVERTED: '报修转为维修工单',
 REPAIR_CANCELLED: '取消报修',
 WORK_ORDER_CREATED: '创建维修工单',
 WORK_ORDER_STATUS_CHANGED: '变更工单状态',
 WORK_ORDER_TAKEOVER: '管理员接管工单',
 WORK_ORDER_TAKEOVER_ENDED: '结束管理员接管',
 WORK_ORDER_COORDINATION: '协调维修工单',
 WORK_ORDER_URGED: '客户催办工单',
 WORK_ORDER_CANCELLATION_REQUESTED: '客户申请取消报修',
 CUSTOMER_RESCHEDULE_DECLINED: '师傅拒绝改约，工单退回待分派',
 HUMAN_HANDOFF_REQUESTED: '申请人工处理',
 BUSINESS_TODO_HANDLED: '处理业务待办',
 WORK_ORDER_COMPLETION_SUBMITTED: '提交完工',
 WORK_ORDER_ACCEPTED: '客户确认验收',
 WORK_ORDER_ACCEPTANCE_ISSUE: '客户提出验收异议',
 WORK_ORDER_CANCELLED: '取消维修工单',
 WORK_ORDER_CLOSED_UNRESOLVED: '工单未解决关闭',
 JOB_POOL_PUBLISHED: '发布到接单大厅',
 JOB_POOL_CLAIMED: '师傅自主接单',
 JOB_POOL_WITHDRAWN: '撤回大厅发布',
 ASSIGNMENT_ASSIGNED: '派发维修任务',
 ASSIGNMENT_ACCEPTED: '确认接单',
 ASSIGNMENT_DECLINED: '拒绝派单',
 ASSIGNMENT_RETURN_REQUESTED: '申请退回任务',
 ASSIGNMENT_RETURN_REJECTED: '拒绝退回申请',
 ASSIGNMENT_REASSIGNED: '改派维修任务',
 ASSIGNMENT_REVOKED: '撤销维修分派',
 ASSIGNMENT_FINISHED: '结束维修分派',
 PREFERRED_VISIT_SELECTED: '客户选择期望上门时段',
 CUSTOMER_VISIT_CONFIRMED: '师傅确认客户上门时段',
 CUSTOMER_RESCHEDULE_CONFIRMED: '师傅确认客户改约',
 APPOINTMENT_CREATED: '创建上门预约',
 APPOINTMENT_CONFIRMED: '确认上门预约',
 APPOINTMENT_RESCHEDULE_REQUESTED: '客户申请改约',
 APPOINTMENT_MISSED: '记录预约爽约',
 SAFETY_ACKNOWLEDGED: '确认安全风险',
 SAFETY_RESOLVED: '解除安全风险',
 INSPECTION_STARTED: '开始现场检测',
 SERVICE_RECORD_SUBMITTED: '提交服务记录',
 QUOTE_DRAFT_CREATED: '创建报价草稿',
 QUOTE_DRAFT_EDITED: '编辑报价草稿',
 QUOTE_SUBMITTED: '提交报价',
 QUOTE_ACCEPTED: '客户接受报价',
 QUOTE_REJECTED: '客户拒绝报价',
 QUOTE_WITHDRAWN: '撤回报价',
 QUOTE_WITHDRAWN_FOR_COORDINATION: '协调工单时撤回报价',
 QUOTE_WITHDRAWN_FOR_REASSIGNMENT: '改派时撤回报价',
 QUOTE_EXPIRED: '报价过期',
 QUOTE_EXPIRATION_SKIPPED: '跳过报价过期处理',
 QUOTE_OVERRIDE_REQUESTED: '申请超区间报价',
 QUOTE_OVERRIDE_APPROVED: '批准超区间报价',
 QUOTE_OVERRIDE_REJECTED: '拒绝超区间报价',
 QUOTE_OVERRIDE_INVALIDATED: '超区间审批失效',
 SERVICE_ITEM_CREATED: '创建服务项目',
 SERVICE_ITEM_UPDATED: '更新服务项目',
 SERVICE_ITEM_STATUS: '变更服务项目状态',
 PRICING_RULE_CREATED: '创建价格规则',
 PRICING_RULE_UPDATED: '更新价格规则',
 PRICING_RULE_STATUS: '变更价格规则状态',
};

const auditResources: Record<string, string> = {
 STAFF_USER: '员工账号',
 STAFF_AUTH: '员工认证',
 STAFF_BINDING: '维修身份绑定',
 STAFF_INVITATION: '维修邀请',
 CUSTOMER_USER: '客户账号',
 CUSTOMER_ADDRESS: '服务地址',
 FILE_ASSET: '上传文件',
 REPAIR_REQUEST: '报修申请',
 WORK_ORDER: '维修工单',
 WORK_ASSIGNMENT: '工单分派',
 JOB_POOL_ENTRY: '大厅发布记录',
 SAFETY_EVENT: '安全事件',
 QUOTE: '报价单',
 HUMAN_HANDOFF_RECORD: '人工协调记录',
 URGE_RECORD: '客户催办记录',
 SERVICE_ITEM: '服务项目',
 PRICING_RULE: '价格规则',
};

export function auditFieldLabel(field: string): string {
 if (field === 'action') return '操作事项';
 if (field === 'occurredAt') return '操作时间';
 return label(field);
}

export function auditCell(field: string, value: unknown): string {
 if (field === 'reason' && typeof value === 'string') {
  return escape(value.replace(/\s*[（(]此查询不展示原文[）)]\s*$/, ''));
 }
 if (field !== 'action' && field !== 'resourceType') return cell(field, value);
 if (value == null || value === '') return '—';
 const code = String(value);
 const names = field === 'action' ? auditActions : auditResources;
 const fallback = field === 'action' ? '未归类操作' : '未归类资源';
 const name = Object.hasOwn(names, code) ? names[code] : fallback;
 return `<span title="原始代码 ${escape(code)}">${escape(name)}</span>`;
}

// Present the public audit metadata as labeled values. IDs remain strings.
const auditMetadataLabels: Record<string, string> = {
 workOrderId: '关联工单编号',
 status: '状态',
 fromStatus: '原状态',
 toStatus: '新状态',
 workOrderStatus: '工单状态',
 appointmentStatus: '预约状态',
 version: '数据版本',
 expectedVersion: '预期数据版本',
 quoteVersion: '报价数据版本',
 versionNo: '报价版本',
 bindingVersion: '绑定版本',
 itemRevision: '报价项目修订版本',
 count: '数量',
 workload: '当前任务数',
 quoteId: '报价编号',
 assignmentId: '分派编号',
 appointmentId: '预约编号',
 recordId: '服务记录编号',
 serviceRecordId: '服务记录依据编号',
 correctsRecordId: '被修正的记录编号',
 technicianId: '维修人员编号',
 actualTechnicianId: '实际维修人员编号',
 actualOperator: '实际操作人编号',
 submittedBy: '提交人编号',
 requestedBy: '申请人编号',
 reviewedBy: '审核人编号',
 approvalId: '审批编号',
 todoId: '待办编号',
 handoffId: '人工协调编号',
 urgeRecordId: '催办记录编号',
 handoffRecordId: '人工协调记录编号',
 staffId: '员工编号',
 bindingId: '身份绑定编号',
 assignedBy: '分派人编号',
 endedBy: '结束分派人编号',
 takeoverBy: '接管人编号',
 addressId: '地址编号',
 fileId: '文件编号',
 role: '岗位角色',
 quoteType: '报价类型',
 kind: '记录类型',
 submissionSource: '提交方式',
 resolution: '处理方式',
 source: '来源',
 eventType: '事件类型',
 subtotalAmount: '本次报价合计',
 confirmedAmountBefore: '此前已确认金额',
 totalAmount: '总金额',
 payableTotalAfterAcceptance: '接受后累计应付',
 fixedPrice: '固定单价',
 minPrice: '最低单价',
 maxPrice: '最高单价',
 contentHash: '报价内容指纹',
 quoteContentHash: '审批关联报价指纹',
 reasonRecorded: '已记录处理依据',
 handlingConclusionRecorded: '已记录处理结论',
 informationConfirmed: '已核对报修信息',
 safetyReviewed: '已核对安全情况',
 startAt: '开始时间',
 endAt: '结束时间',
 scheduledStartAt: '预约开始',
 scheduledEndAt: '预约结束',
 acceptedAt: '接单时间',
 endedAt: '结束时间',
 submittedAt: '提交时间',
 reviewedAt: '审核时间',
 handledAt: '处理时间',
 effectiveFrom: '生效时间',
 effectiveTo: '失效时间',
 takeoverAt: '接管时间',
 'staff.id': '员工编号',
 'staff.role': '员工岗位',
 'staff.status': '员工状态',
 'staff.version': '员工资料版本',
 acceptedQuoteIds: '已接受报价编号',
 summaryNote: '摘要说明',
};

function readAuditSummary(raw: unknown): Record<string, unknown> {
 if (typeof raw !== 'string' || !raw.trim()) return {};
 try {
  const value = JSON.parse(raw);
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {};
  const staff = value.staff;
  return {
   ...value,
   'staff.id': staff?.id,
   'staff.role': staff?.role,
   'staff.status': staff?.status,
   'staff.version': staff?.version,
  };
 } catch {
  const summaryNote = raw === '可公开审计元数据超出单次摘要范围'
   ? '摘要内容超出显示范围'
   : '摘要暂不可用';
  return {summaryNote};
 }
}

function auditMetadataText(field: string, value: unknown): string | undefined {
 if (field === 'acceptedQuoteIds' && Array.isArray(value)) {
  const ids = value.filter(id => typeof id === 'string' && /^[1-9][0-9]{0,18}$/.test(id));
  return ids.length ? ids.join('、') : undefined;
 }
 if (typeof value !== 'string' && typeof value !== 'number' && typeof value !== 'boolean') return undefined;
 if (value === '') return undefined;
 if (field.endsWith('At') || field === 'effectiveFrom' || field === 'effectiveTo') return dateText(value);
 if (value === 'USER_ACCEPTANCE_ISSUE') return '客户验收异议';
 if (value === 'ASSIGNED') return '已派单';
 if (value === 'RETURN_REJECTED') return '退回申请已拒绝';
 return text(value);
}

export function auditChanges(beforeSummary: unknown, afterSummary: unknown) {
 const before = readAuditSummary(beforeSummary);
 const after = readAuditSummary(afterSummary);
 const rows: Array<{label: string; value: string}> = [];
 for (const [field, caption] of Object.entries(auditMetadataLabels)) {
  const oldValue = auditMetadataText(field, before[field]);
  const newValue = auditMetadataText(field, after[field]);
  if (newValue !== undefined) {
   const value = oldValue !== undefined && oldValue !== newValue ? `${oldValue} → ${newValue}` : newValue;
   rows.push({label: caption, value});
  } else if (oldValue !== undefined) {
   rows.push({label: caption, value: `${oldValue}（变更前）`});
  }
 }
 return rows;
}

export function cell(k:string,v:any){if(k==='status'||k==='assignmentStatus')return badge(v);if(k==='riskFlag')return v?'<span class="badge red">待核实</span>':'<span class="muted">正常</span>';if(k.endsWith('At')||k==='effectiveFrom'||k==='effectiveTo')return escape(dateText(v));return escape(text(v));}

export function listCell(field: string, value: unknown): string {
 if (field==='assignmentStatus'||field==='latestAssignmentStatus') return value?badge(assignmentDisplayStatus(String(value))):'—';
 if (field === 'preferredVisit') {
  const visit = value as {startAt?: string; endAt?: string} | null;
  return visit?.startAt && visit.endAt ? escape(dateText(visit.startAt) + ' 至 ' + dateText(visit.endAt)) : '—';
 }
 if (field === 'appointmentStatus') return value ? value === 'PENDING' ? '待确认' : badge(value as string) : '—';
 if (field === 'repairStatus') return cell('status', value);
 if (field === 'matchedRules' && Array.isArray(value)) {
  const rules = value.map((rule: unknown) => {
   if (typeof rule === 'string') return rule.trim();
   if (!rule || typeof rule !== 'object') return '';
   const {title, ruleCode, templateText} = rule as Record<string, unknown>;
   return [title, ruleCode, templateText]
    .find((part): part is string => typeof part === 'string' && part.trim() !== '')?.trim();
  }).filter(Boolean);
  return escape(rules.join('、') || '—');
 }
 if (Array.isArray(value)) return escape(value.map(text).join('、'));
 if (value && typeof value === 'object') {
  const item = value as {name?: string; id?: string};
  return escape(item.name || item.id || '已记录');
 }
 return cell(field, value);
}
export const assignmentDisplayStatus=(status:string)=>status==='PENDING'?'待师傅接单':status;
export function orderDisplayStatus(status:string,assignmentStatus?:string|null):string {
 return assignmentStatus==='PENDING'&&['WAITING_ASSIGNMENT','WAITING_APPOINTMENT'].includes(status)?'待师傅接单':status;
}
export function timelineItems(value:unknown):Array<{time:string;title:string;detail:string}> {
 if(!Array.isArray(value))return [];
 return value.filter(event=>event?.occurredAt||event?.title).map(event=>({
  time:dateText(event.occurredAt),title:event.title||'工单进度更新',detail:event.detail||''
 }));
}
export function publicationContactReady(contact:Record<string,any>|null|undefined):boolean {
 const phone=contact?.contactPhone;
 return ['SUBMISSION_SNAPSHOT','MANUAL_REVIEW_SNAPSHOT'].includes(contact?.contactSource)
  &&!!contact?.contactName?.trim()&&!!contact?.addressText?.trim()
  &&typeof phone==='string'&&/^[0-9+() -]{6,20}$/.test(phone)&&phone.replace(/[^0-9]/g,'').length>=6;
}
function contactNeedsReview(contact:Record<string,any>|null|undefined):boolean {
 if(contact?.phoneMasked&&['SUBMISSION_SNAPSHOT','MANUAL_REVIEW_SNAPSHOT'].includes(contact.contactSource))return false;
 return !publicationContactReady(contact);
}
export function preferredVisitIssue(detail:Record<string,any>,now=Date.now()):string {
 const start=Date.parse(detail.preferredVisit?.startAt),end=Date.parse(detail.preferredVisit?.endAt);
 if(detail.visitPolicy!=='CUSTOMER_SLOT'||!Number.isFinite(start)||!Number.isFinite(end))
  return '等待客户选择有效的期望上门时段。';
 if(start<=now)return '客户选的期望上门时段已过，请客户在报修详情中调整时段。';
 if(start>now+30*86400000||end<=start||end-start>4*3600000)
  return '客户选的期望上门时段不符合要求，请客户重新选择。';
 return '';
}
export function publicationRequirements(detail:Record<string,any>,now=Date.now()):string[] {
 if(detail.workOrder?.status==='WAITING_CUSTOMER_VISIT')return ['等待客户确认继续维修并选择期望上门时段。'];
 const issue=preferredVisitIssue(detail,now);
 if(detail.assignment?.status==='PENDING')return ['已定向派出，等待师傅接单。可到「定向派单进度」查看。',
  ...(detail.visitPolicy==='CUSTOMER_SLOT'&&['WAITING_ASSIGNMENT','WAITING_APPOINTMENT'].includes(detail.workOrder?.status)&&issue
   ?['客户需先选择有效上门时段，师傅才能确认接单。'+(detail.preferredVisit?issue:'')]:[])];
 if(detail.workOrder?.status!=='WAITING_ASSIGNMENT'||detail.assignment)return ['当前工单已进入其他处理阶段。'];
 const contact=detail.serviceContact;
 const missing:string[]=[];
 if(issue)missing.push(issue);
 if(contactNeedsReview(contact))missing.push(contact?.contactSource==='CURRENT_ADDRESS'
  ?'客户已留联系方式，需在关联报修中确认联系人、电话和上门地址。'
  :'服务联系信息或上门地址不完整，请核对关联报修。');
 return missing;
}
type NextStep={owner:string;message:string;tab?:string;link?:string};
export function repairNextStep(repair:Record<string,any>,actions:string[]=[]):NextStep {
 if(repair.workOrderId)return {owner:'维修工单',message:'已关联维修工单，打开工单查看下一步。'};
 if(repair.status==='TRIAGED')return {owner:'客户',message:'请客户在小程序报修详情确认生成维修工单。'};
 if(repair.status==='CANCELLED')return {owner:'已结束',message:'这笔报修已取消。'};
 if(repair.status==='HIGH_RISK_BLOCKED')return {owner:'平台',message:'请到安全事件查看并处理风险。',link:'safety'};
 if(actions.includes('MANUAL_TRIAGE'))return {owner:'平台',message:'核对设备、故障、服务联系和安全信息后，确认核对报修。'};
 if(['SUBMITTED','HUMAN_HANDOFF'].includes(repair.status))return {owner:'平台',message:'报修等待员工核对，请打开资料查看可用操作。'};
 return {owner:'平台',message:'查看报修资料和当前可用操作，确认是否需要补齐信息。'};
}
export function orderNextStep(detail:Record<string,any>,pool:Record<string,any>|null|undefined,now=Date.now()):NextStep {
 const w=detail.workOrder||{},assignment=detail.assignment;
 if(['COMPLETED','CANCELLED','CLOSED_UNRESOLVED'].includes(w.status))
  return {owner:'已结束',message:'可查看服务记录与处理时间线。',tab:'timeline'};
 if(detail.safetyBlocked===true)
  return {owner:'平台',message:'安全事件尚未解除。请到安全事件核实并处理。',link:'safety'};
 if(w.status==='WAITING_CUSTOMER_VISIT')
  return {owner:'客户',message:'请客户在小程序确认继续维修并选定期望上门时段。',tab:'appointment'};
 if(assignment?.status==='RETURN_REQUESTED')
  return {owner:'平台',message:'师傅已申请退回任务，请审核是否继续履约或改派。'};
 const issue=preferredVisitIssue(detail,now);
 if(assignment?.status==='PENDING'&&['WAITING_ASSIGNMENT','WAITING_APPOINTMENT'].includes(w.status)){
  if(detail.visitPolicy==='CUSTOMER_SLOT'&&issue)return {owner:'客户',message:issue+' 师傅随后可确认接单。',tab:'appointment'};
  return {owner:'维修人员',message:detail.assigneeName?'已定向派出，等待 '+detail.assigneeName+' 确认接单。':'已定向派出，等待师傅确认接单。',tab:'appointment'};
 }
 if(w.status==='WAITING_ASSIGNMENT'){
  if(issue)return {owner:'客户',message:issue,tab:'appointment'};
  if(contactNeedsReview(detail.serviceContact))
   return {owner:'平台',message:'请先核对联系人、联系电话和上门地址，再发布到接单大厅。',tab:'appointment'};
  if(pool===undefined)return {owner:'平台',message:'请到预约与分派查看当前大厅发布状态。',tab:'appointment'};
  if(pool?.status==='PUBLISHED')return {owner:'维修人员',message:'工单已发布，等待师傅在接单大厅接单。',tab:'appointment'};
  return {owner:'平台',message:'上门时段和服务联系已齐备，可在预约与分派核对发布条件，或使用定向派单。',tab:'appointment'};
 }
 if(w.status==='WAITING_APPOINTMENT'){
  if(detail.appointmentStatus==='MISSED')return {owner:'平台',message:'原预约已超时，请核实上门情况并协调重新预约。',tab:'appointment'};
  if(detail.visitPolicy==='CUSTOMER_SLOT'&&issue)return {owner:'客户',message:issue,tab:'appointment'};
  return detail.visitPolicy==='CUSTOMER_SLOT'&&assignment?.status==='ACTIVE'
   ?{owner:'维修人员',message:'等待师傅确认客户选择的上门时段。',tab:'appointment'}
   :{owner:'平台',message:'请核对当前分派及双方上门时间，协调确认预约。',tab:'appointment'};
 }
 if(w.status==='APPOINTED'&&detail.appointmentStatus==='RESCHEDULE_REQUESTED')
  return {owner:'维修人员',message:'客户申请改约，等待师傅确认新时段。平台可查看预约或协助协调。',tab:'appointment'};
 if(['WAITING_QUOTE_CONFIRMATION','PAUSED_FOR_REQUOTE'].includes(w.status)&&Date.parse(w.currentQuote?.expiresAt)<=now)
  return {owner:'平台',message:'当前报价已过有效期，请核对报价记录和可用处理方式。',tab:'quotes'};
 const steps:Record<string,NextStep>={
  WAITING_MANUAL_REVIEW:{owner:'平台',message:'请查看协调原因和处理时间线，再选择工单可用的协调方式。',tab:'timeline'},
  APPOINTED:{owner:'维修人员',message:'预约已成立，等待师傅上门并开始检测。',tab:'appointment'},
  IN_INSPECTION:{owner:'维修人员',message:'等待师傅完成现场检测并提交检测记录。',tab:'records'},
  WAITING_QUOTE:{owner:'维修人员',message:'等待师傅按检测结果填写并提交报价。',tab:'quotes'},
  WAITING_QUOTE_CONFIRMATION:{owner:'客户',message:'等待客户确认当前报价，确认后继续维修。',tab:'quotes'},
  PAUSED_FOR_REQUOTE:{owner:'客户',message:'维修已暂停，等待客户确认追加报价。',tab:'quotes'},
  IN_REPAIR:{owner:'维修人员',message:'等待师傅完成维修、提交服务记录和完工。',tab:'records'},
  WAITING_ACCEPTANCE:{owner:'客户',message:'师傅已提交完工，等待客户在小程序确认验收。',tab:'records'}
 };
 return steps[w.status]||{owner:'平台',message:'请查看工单状态和处理时间线。',tab:'timeline'};
}
export function icon(name:string){const paths:Record<string,string>={home:'M3 10l9-7 9 7v11H3z M9 21v-8h6v8',orders:'M6 3h12v18H6z M9 7h6 M9 11h6 M9 15h4',intake:'M4 4h16v16H4z M8 8h8 M8 12h8 M8 16h4',calendar:'M4 5h16v16H4z M7 3v4 M17 3v4 M4 10h16 M8 14h2 M14 14h2',users:'M16 21v-3a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v3 M9 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M22 21v-3a4 4 0 0 0-3-3.9 M16 2a4 4 0 0 1 0 8',shield:'M12 2l8 4v6c0 5-8 10-8 10S4 17 4 12V6z M8 12l3 3 5-6',price:'M4 3h16v18H4z M8 7h8 M8 11h8 M8 15h3 M8 18h6',clock:'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18 M12 7v6l4 2',audit:'M4 3h12l4 4v14H4z M8 10h8 M8 14h8 M8 18h5',settings:'M4 6h16 M4 12h16 M4 18h16 M8 3v6 M16 9v6 M10 15v6',arrow:'M9 5l7 7-7 7',refresh:'M20 8a8 8 0 1 0 0 8 M20 3v5h-5',search:'M10 3a7 7 0 1 0 0 14 7 7 0 0 0 0-14 M15 15l6 6',logout:'M9 4H3v16h6 M13 7l5 5-5 5 M7 12h14',menu:'M4 6h16 M4 12h16 M4 18h16',check:'M4 12l5 5L20 6',plus:'M12 4v16 M4 12h16',close:'M5 5l14 14 M19 5L5 19',bell:'M5 17h14l-2-3V9a5 5 0 0 0-10 0v5z M10 21h4',tool:'M14 5a5 5 0 0 0-6 6l-6 6 5 5 6-6a5 5 0 0 0 6-6l-3 3-5-5z'};return `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${paths[name]||paths.orders}"/></svg>`;}
