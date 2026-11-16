package com.fixmate.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.*;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.assembler.QuoteAssembler;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.common.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 报价工作聚合：身份→工单→分派→报价/审批→检测；成功结果由外层幂等事务原子保存。 */
@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class StaffQuoteService {
    public record Actor(Long id,String role,boolean wechat) {}
    private final WorkOrderIdentityLocker locker;
    private final QuoteMapper quotes;private final QuoteItemMapper items;private final QuoteOverrideApprovalMapper approvals;
    private final WorkOrderMapper orders;private final ServiceRecordMapper records;private final ServiceItemMapper services;
    private final PricingRuleMapper rules;private final StaffWechatBindingMapper bindings;
    private final StaffAccessService access;private final StaffIdentityGuard identities;private final EmployeeWechatAccessService employees;
    private final WorkAssignmentService assignments;private final WorkOrderSafetyGate safety;private final AcceptedQuoteScope accepted;
    private final PricingCalculationService pricing;private final WorkOrderStateMachine machine;private final QuoteAssembler assembler;
    private final DatabaseClockMapper clock;private final AuditLogService audit;private final ObjectMapper json;
    private final RepairRequestMapper repairs;private final ServiceContactProjection contacts;
    private final WorkOrderFollowUp followUp;
    public StaffQuoteService(QuoteMapper quotes,QuoteItemMapper items,QuoteOverrideApprovalMapper approvals,WorkOrderMapper orders,
            ServiceRecordMapper records,ServiceItemMapper services,PricingRuleMapper rules,StaffWechatBindingMapper bindings,
            StaffAccessService access,StaffIdentityGuard identities,EmployeeWechatAccessService employees,WorkAssignmentService assignments,
            WorkOrderSafetyGate safety,AcceptedQuoteScope accepted,PricingCalculationService pricing,WorkOrderStateMachine machine,
            QuoteAssembler assembler,DatabaseClockMapper clock,AuditLogService audit,ObjectMapper json,RepairRequestMapper repairs,ServiceContactProjection contacts,WorkOrderIdentityLocker locker,WorkOrderFollowUp followUp){
        this.locker=locker;
        this.repairs=repairs;this.contacts=contacts;
        this.followUp=followUp;
        this.quotes=quotes;this.items=items;this.approvals=approvals;this.orders=orders;this.records=records;this.services=services;
        this.rules=rules;this.bindings=bindings;this.access=access;this.identities=identities;this.employees=employees;this.assignments=assignments;
        this.safety=safety;this.accepted=accepted;this.pricing=pricing;this.machine=machine;this.assembler=assembler;this.clock=clock;this.audit=audit;this.json=json;
    }
    private Actor actor(StaffPrincipal staff,Long customer,boolean write,Long orderId){
        if(customer!=null){var e=employees.requireLocked(customer);return new Actor(e.staffId(),"TECHNICIAN",true);}
        var s=write?locker.lock(staff,orderId,List.of(),"ADMIN","DISPATCHER").actor():
                access.requireLocked(staff,"ADMIN","DISPATCHER","AUDITOR");
        return new Actor(s.getId(),s.getRole(),false);
    }
    public Long quoteOrder(Long id){var q=quotes.selectById(id);if(q==null)throw BusinessException.notFound("报价不存在");return q.getWorkOrderId();}
    public Long approvalOrder(Long id){var a=approvals.selectById(id);if(a==null)throw BusinessException.notFound("审批不存在");return quoteOrder(a.getQuoteId());}
    public void guard(StaffPrincipal staff,Long customer,Long orderId,boolean decision){
        if(customer!=null){var e=employees.require(customer);var w=order(orderId,false);readScope(w,new Actor(e.staffId(),"TECHNICIAN",true));var assignment=assignments.currentAssignment(w,false);if(assignment==null||!"ACTIVE".equals(assignment.getStatus()))throw BusinessException.forbidden("当前分派禁止报价写入或重放");return;}
        var s=access.require(staff,decision?new String[]{"ADMIN"}:new String[]{"ADMIN","DISPATCHER"});
        var w=order(orderId,false);if(!decision&&w.getTakeoverBy()!=null&&!s.getId().equals(w.getTakeoverBy()))throw BusinessException.forbidden("工单已由其他管理员接管");
    }
    public List<StaffQuoteVO> list(StaffPrincipal staff,Long customer,Long orderId){
        var a=actor(staff,customer,false,orderId);var w=order(orderId,true);readScope(w,a);
        var result=new ArrayList<StaffQuoteVO>();for(var q:quoteRows(orderId,true)){refreshApproval(w,q);result.add(view(w,q,a));}return result;
    }
    public StaffQuoteVO create(StaffPrincipal staff,Long customer,Long orderId,StaffQuoteCreateDTO in){
        var a=actor(staff,customer,true,orderId);var w=work(orderId,a);writeScope(w,a);StaffIdentityGuard.version(w.getVersion(),in.expectedWorkOrderVersion());
        draftState(w);var all=quoteRows(orderId,true);if(all.stream().anyMatch(q->Set.of("DRAFT","SUBMITTED").contains(q.getStatus())))throw BusinessException.stateConflict("须先处理当前未决报价");
        validateHistory(all);var previous=all.isEmpty()?null:all.getLast();
        if(!Objects.equals(parse(in.previousQuoteId()),previous==null?null:previous.getId()))throw BusinessException.stateConflict("上一版报价必须连续");
        var base=base(all);var additional=base!=null;
        if(!additional&&!"WAITING_QUOTE".equals(w.getStatus()))throw BusinessException.stateConflict("初始报价须在待报价状态建立");
        if(!Objects.equals(in.quoteType(),additional?"ADDITIONAL":"INITIAL")||!Objects.equals(parse(in.baseQuoteId()),base==null?null:base.getId()))throw BusinessException.stateConflict("报价类型或最近已接受基准不正确");
        if(additional&&!accepted.valid(all))throw BusinessException.stateConflict("已接受报价链异常");
        var q=new QuoteEntity();q.setQuoteNo("Q"+UUID.randomUUID().toString().replace("-","").substring(0,30));q.setWorkOrderId(orderId);
        q.setVersionNo(previous==null?1:previous.getVersionNo()+1);q.setPreviousQuoteId(previous==null?null:previous.getId());q.setBaseQuoteId(base==null?null:base.getId());
        q.setQuoteType(in.quoteType());q.setStatus("DRAFT");q.setCurrency("CNY");q.setVersion(0);q.setItemRevision(0);
        q.setCreatedBy(a.id());q.setSubmissionSource(in.submissionSource());q.setServiceRecordId(in.serviceRecordId());q.setTechnicianId(in.technicianId());
        source(w,q,a);inspection(w,q);q.setChangeReason(reason(in.changeReason(),additional));q.setExpiresAt(expiry(in.expiresAt()));
        q.setConfirmedAmountBefore(base==null?BigDecimal.ZERO.setScale(2):base.getTotalAmount());
        var lines=build(w,q,in.items(),a.id());amounts(q,lines);q.setCreatedAt(clock.now());q.setUpdatedAt(q.getCreatedAt());quotes.insert(q);append(q,lines);
        bump(w);event(a,"QUOTE_DRAFT_CREATED",q,Map.of("serviceRecordId",q.getServiceRecordId(),"submissionSource",q.getSubmissionSource()));return view(w,q,a);
    }
    public StaffQuoteVO update(StaffPrincipal staff,Long customer,Long id,StaffQuoteUpdateDTO in){
        var a=actor(staff,customer,true,quoteOrder(id));var w=work(quoteOrder(id),a);writeScope(w,a);var q=quote(id,true);
        StaffIdentityGuard.version(w.getVersion(),in.expectedWorkOrderVersion());StaffIdentityGuard.version(q.getVersion(),in.expectedVersion());editable(w,q,a);
        invalidate(q,"报价重新编辑");q.setChangeReason(reason(in.changeReason(),"ADDITIONAL".equals(q.getQuoteType())));q.setExpiresAt(expiry(in.expiresAt()));
        q.setItemRevision(q.getItemRevision()+1);var lines=build(w,q,in.items(),a.id());amounts(q,lines);save(q);append(q,lines);bump(w);
        event(a,"QUOTE_DRAFT_EDITED",q,Map.of("itemRevision",q.getItemRevision()));return view(w,q,a);
    }
    public QuoteSubmitResultVO submit(StaffPrincipal staff,Long customer,Long id,QuoteSubmitCommandDTO in){
        var a=actor(staff,customer,true,quoteOrder(id));var w=work(quoteOrder(id),a);writeScope(w,a);var q=quote(id,true);
        StaffIdentityGuard.version(w.getVersion(),in.expectedWorkOrderVersion());StaffIdentityGuard.version(q.getVersion(),in.expectedVersion());editable(w,q,a);hash(q,in.expectedContentHash());
        var lines=lines(q);validateCurrent(w,q,lines);var approval=latestApproval(q,true);
        if(outside(lines)){
            if(!hasCurrentApproval(q,approval))
                throw BusinessException.stateConflict("报价超出平台价格区间，请先申请超区间审批，审批通过后再提交客户确认");
            if(!a.id().equals(approval.getRequestedBy())||a.id().equals(approval.getReviewedBy()))throw BusinessException.forbidden("须由原申请人最终提交，且不得自批");
        }
        q.setStatus("SUBMITTED");q.setSubmittedBy(a.id());q.setSubmittedAt(clock.now());save(q);
        machine.transition(w.getId(),"ADDITIONAL".equals(q.getQuoteType())?WorkOrderStatus.PAUSED_FOR_REQUOTE:WorkOrderStatus.WAITING_QUOTE_CONFIRMATION,
                w.getVersion(),"提交报价 "+q.getQuoteNo(),"STAFF",a.id());w=order(w.getId(),true);
        event(a,"QUOTE_SUBMITTED",q,Map.of("contentHash",in.expectedContentHash(),"technicianId",q.getTechnicianId()));return new QuoteSubmitResultVO("SUBMITTED",view(w,q,a));
    }
    public StaffQuoteVO withdraw(StaffPrincipal staff,Long customer,Long id,QuoteWithdrawCommandDTO in){
        var a=actor(staff,customer,true,quoteOrder(id));var w=work(quoteOrder(id),a);writeScope(w,a);var q=quote(id,true);
        StaffIdentityGuard.version(w.getVersion(),in.expectedWorkOrderVersion());StaffIdentityGuard.version(q.getVersion(),in.expectedQuoteVersion());
        requireMetadata(q);source(w,q,a);current(w,q);if(!Set.of("DRAFT","SUBMITTED").contains(q.getStatus()))throw BusinessException.stateConflict("只可撤回未接受报价");
        String reason=reason(in.reason(),true);boolean submitted="SUBMITTED".equals(q.getStatus());q.setStatus("WITHDRAWN");save(q);invalidate(q,"报价已撤回");
        if(submitted){machine.transition(w.getId(),WorkOrderStatus.WAITING_QUOTE,w.getVersion(),"撤回报价 "+q.getQuoteNo(),"STAFF",a.id());w=order(w.getId(),true);}else bump(w);
        event(a,"QUOTE_WITHDRAWN",q,Map.of("reason",reason));return view(w,q,a);
    }
    public QuoteOverrideApprovalVO requestOverride(StaffPrincipal staff,Long customer,Long id,QuoteOverrideRequestDTO in){
        var a=actor(staff,customer,true,quoteOrder(id));var w=work(quoteOrder(id),a);writeScope(w,a);var q=quote(id,true);
        StaffIdentityGuard.version(q.getVersion(),in.expectedVersion());editable(w,q,a);hash(q,in.expectedContentHash());validateCurrent(w,q,lines(q));
        if(!outside(lines(q)))throw BusinessException.stateConflict("区间内报价无需超区间审批");return approvalView(request(w,q,a,reason(in.overrideReason(),true)));
    }
    public QuoteOverrideApprovalVO decide(StaffPrincipal staff,Long id,QuoteOverrideDecisionDTO in,boolean approve){
        var s=locker.lock(staff,approvalOrder(id),List.of(),"ADMIN").actor();var a=new Actor(s.getId(),s.getRole(),false);
        var w=work(approvalOrder(id),a);var candidate=approvals.selectById(id);var q=quote(candidate.getQuoteId(),true);var row=approvals.selectOne(new QueryWrapper<QuoteOverrideApprovalEntity>().eq("id",id).last("FOR UPDATE"));
        StaffIdentityGuard.version(row.getVersion(),in.expectedApprovalVersion());
        if(!"PENDING".equals(row.getStatus()))throw BusinessException.stateConflict("审批已处理或失效");
        if(a.id().equals(row.getRequestedBy()))throw BusinessException.forbidden("管理员不得审批本人请求");
        if(!"DRAFT".equals(q.getStatus())||!row.getQuoteContentHash().equals(contentHash(q))||!currentRules(w,q,lines(q))||!validExpiry(q)){
            invalidate(q,"报价内容、规则或有效期已变化");return approvalView(approvals.selectById(id));
        }
        // 审批不取得接管权；仍要求当前安全、人员和原检测有效。
        decisionPolicy(w,q,row,a);
        row.setStatus(approve?"APPROVED":"REJECTED");row.setReviewedBy(a.id());row.setReviewedAt(clock.now());row.setReviewComment(reason(in.reviewComment(),true));row.setVersion(row.getVersion()+1);row.setUpdatedAt(clock.now());approvals.updateById(row);
        event(a,approve?"QUOTE_OVERRIDE_APPROVED":"QUOTE_OVERRIDE_REJECTED",q,Map.of("approvalId",id,"requestedBy",row.getRequestedBy(),"contentHash",row.getQuoteContentHash()));return approvalView(row);
    }
    public Map<String,Object> approvalList(StaffPrincipal staff,int page,int size,String status){
        access.requireLocked(staff,"ADMIN");if(page<1||size<1||size>100||!Set.of("PENDING","APPROVED","REJECTED","INVALIDATED").contains(status))throw BusinessException.validation("审批筛选参数不正确");
        var roots=orders.selectList(new QueryWrapper<WorkOrderEntity>().apply("id IN (SELECT q.work_order_id FROM quote q JOIN quote_override_approval a ON a.quote_id=q.id WHERE q.status='DRAFT' AND a.status IN ('PENDING','APPROVED'))").orderByAsc("id").last("FOR UPDATE"));
        for(var w:roots)for(var q:quoteRows(w.getId(),true))if("DRAFT".equals(q.getStatus()))refreshApproval(w,q);
        var result=approvals.selectPage(new Page<>(page,size),new QueryWrapper<QuoteOverrideApprovalEntity>().eq("status",status).orderByDesc("id"));
        return Map.of("items",result.getRecords().stream().map(this::approvalView).toList(),"page",page,"size",size,"total",result.getTotal());
    }
    private QuoteOverrideApprovalEntity request(WorkOrderEntity w,QuoteEntity q,Actor a,String reason){
        var old=latestApproval(q,true);
        if(hasCurrentApproval(q,old))throw BusinessException.stateConflict("当前报价已通过审批，请刷新后提交客户确认");
        if(old!=null&&"PENDING".equals(old.getStatus())){
            if(old.getRequestedBy().equals(a.id())&&old.getQuoteContentHash().equals(contentHash(q)))return old;
            throw BusinessException.stateConflict("已有其他提交人的待处理审批");
        }
        invalidate(q,"重新申请审批");var row=new QuoteOverrideApprovalEntity();row.setQuoteId(q.getId());row.setRequestedBy(a.id());row.setQuoteContentHash(contentHash(q));
        row.setStatus("PENDING");row.setOverrideReason(reason);row.setVersion(0);row.setCreatedAt(clock.now());row.setUpdatedAt(row.getCreatedAt());approvals.insert(row);
        event(a,"QUOTE_OVERRIDE_REQUESTED",q,Map.of("approvalId",row.getId(),"contentHash",row.getQuoteContentHash()));return row;
    }
    public String creationAction(WorkOrderEntity w,Long actor,String role){
        try{var a=new Actor(actor,role,"TECHNICIAN".equals(role));if("AUDITOR".equals(role))return null;readScope(w,a);active(w,false);if(!safety.clear(w.getId()))return null;draftState(w);
            if(a.wechat()&&!Objects.equals(actor,w.getTechnicianId())||!a.wechat()&&!("DISPATCHER".equals(role)&&w.getTakeoverBy()==null||"ADMIN".equals(role)&&Objects.equals(actor,w.getTakeoverBy())))return null;
            var all=quoteRows(w.getId(),false);validateHistory(all);if(all.stream().anyMatch(q->Set.of("DRAFT","SUBMITTED").contains(q.getStatus())))return null;
            if(records.selectCount(new QueryWrapper<ServiceRecordEntity>().eq("work_order_id",w.getId()).eq("record_kind","INSPECTION").isNotNull("submitted_at"))==0)return null;
            return base(all)==null?("WAITING_QUOTE".equals(w.getStatus())?"CREATE_QUOTE":null):accepted.valid(all)?"CREATE_ADDITIONAL_QUOTE":null;
        }catch(BusinessException denied){return null;}
    }
    private void decisionPolicy(WorkOrderEntity w,QuoteEntity q,QuoteOverrideApprovalEntity row,Actor a){
        if(a.wechat()||!"ADMIN".equals(a.role())||!"PENDING".equals(row.getStatus())||a.id().equals(row.getRequestedBy()))throw BusinessException.forbidden("须由另一管理员审批");
        requireMetadata(q);if(!"DRAFT".equals(q.getStatus())||!validExpiry(q)||!Objects.equals(row.getQuoteContentHash(),contentHash(q))||!currentRules(w,q,lines(q)))throw BusinessException.stateConflict("审批内容已失效");
        active(w);safety.requireClear(w.getId());inspection(w,q);current(w,q);draftState(w);
    }
    private void refreshApproval(WorkOrderEntity w,QuoteEntity q){var a=latestApproval(q,true);if(a!=null&&Set.of("PENDING","APPROVED").contains(a.getStatus())&&"DRAFT".equals(q.getStatus())&&(!validExpiry(q)||!currentRules(w,q,lines(q))||!Objects.equals(a.getQuoteContentHash(),contentHash(q))))invalidate(q,"报价规则、有效期或内容已变化");}
    private void readScope(WorkOrderEntity w,Actor a){
        if(!a.wechat())return;var assignment=assignments.currentAssignment(w,false);
        if(WorkOrderStatus.valueOf(w.getStatus()).isTerminal()||w.getTakeoverBy()!=null||assignment==null||!a.id().equals(assignment.getTechnicianId())||!"ACTIVE".equals(assignment.getStatus()))throw BusinessException.forbidden("无权读取当前报价，请查看本人参与摘要");
    }
    private void writeScope(WorkOrderEntity w,Actor a){
        readScope(w,a);active(w);safety.requireClear(w.getId());
        if(a.wechat()&&w.getTakeoverBy()!=null||!a.wechat()&&w.getTakeoverBy()!=null&&!a.id().equals(w.getTakeoverBy()))throw BusinessException.forbidden("须由当前接管管理员操作");
        if(WorkOrderStatus.valueOf(w.getStatus()).isTerminal()||"WAITING_ACCEPTANCE".equals(w.getStatus()))throw BusinessException.stateConflict("工单不支持报价写入");
    }
    private void active(WorkOrderEntity w){active(w,true);}
    private void active(WorkOrderEntity w,boolean lock){
        var assignment=assignments.currentAssignment(w,lock);if(assignment==null||!"ACTIVE".equals(assignment.getStatus()))throw BusinessException.stateConflict("须有当前有效已接人员");
        var b=bindings.selectOne(new QueryWrapper<StaffWechatBindingEntity>().eq("staff_id",assignment.getTechnicianId()).eq("status","ACTIVE"));
        if(b==null||!employees.require(b.getCustomerId()).staffId().equals(assignment.getTechnicianId()))throw BusinessException.stateConflict("当前维修人员授权已失效");
    }
    private void source(WorkOrderEntity w,QuoteEntity q,Actor a){
        if(!Objects.equals(w.getTechnicianId(),q.getTechnicianId()))throw BusinessException.forbidden("报价实际维修人已变化");
        boolean valid=a.wechat()?"TECHNICIAN_SELF".equals(q.getSubmissionSource())&&a.id().equals(q.getTechnicianId()):
                "DISPATCHER".equals(a.role())?"DISPATCHER_PROXY".equals(q.getSubmissionSource())&&w.getTakeoverBy()==null:
                "ADMIN".equals(a.role())&&"ADMIN_TAKEOVER".equals(q.getSubmissionSource())&&a.id().equals(w.getTakeoverBy());
        if(!valid)throw BusinessException.forbidden("报价来源与实际操作者或接管依据不匹配");
    }
    private void inspection(WorkOrderEntity w,QuoteEntity q){
        var r=records.selectOne(new QueryWrapper<ServiceRecordEntity>().eq("id",q.getServiceRecordId()).last("FOR UPDATE"));
        if(r==null||!w.getId().equals(r.getWorkOrderId())||!"INSPECTION".equals(r.getRecordKind())||r.getSubmittedAt()==null)throw BusinessException.stateConflict("须引用同工单已提交检测事实");
    }
    private void editable(WorkOrderEntity w,QuoteEntity q,Actor a){requireMetadata(q);source(w,q,a);current(w,q);draftState(w);inspection(w,q);if(!"DRAFT".equals(q.getStatus()))throw BusinessException.stateConflict("只能编辑或提交当前草稿");}
    private void current(WorkOrderEntity w,QuoteEntity q){var all=quoteRows(w.getId(),true);validateHistory(all);if(all.isEmpty()||!all.getLast().getId().equals(q.getId()))throw quoteConflict("报价不是当前版本");
        var base=base(all);if(!"CNY".equals(q.getCurrency())||"INITIAL".equals(q.getQuoteType())&&(base!=null||q.getBaseQuoteId()!=null||q.getConfirmedAmountBefore().signum()!=0)||"ADDITIONAL".equals(q.getQuoteType())&&base!=null&&q.getConfirmedAmountBefore().compareTo(base.getTotalAmount())!=0)throw quoteConflict("报价基准金额已变化");if("ADDITIONAL".equals(q.getQuoteType())&&(base==null||!base.getId().equals(q.getBaseQuoteId())||!accepted.valid(all)))throw BusinessException.stateConflict("追加基准或已接受报价链已变化");}
    private void draftState(WorkOrderEntity w){
        if(!Set.of("WAITING_QUOTE","IN_REPAIR").contains(w.getStatus()))throw BusinessException.stateConflict("当前工单状态不支持建立或提交报价草稿");
        if("IN_REPAIR".equals(w.getStatus())&&followUp.quoteRejection(w.getId())!=null)
            throw BusinessException.stateConflict("客户已拒绝报价，请等待管理端协调后重新报价");
    }
    private List<QuoteItemEntity> build(WorkOrderEntity w,QuoteEntity q,List<QuoteItemInputDTO> inputs,Long actor){
        if(inputs==null||inputs.isEmpty()||inputs.size()>100)throw BusinessException.validation("报价须有1至100项");var result=new ArrayList<QuoteItemEntity>();int position=0;
        for(var in:inputs){
            var service=services.selectById(in.serviceItemId());var rule=rules.selectById(in.pricingRuleId());
            if(service==null||!"ENABLED".equals(service.getStatus())||!Objects.equals(service.getApplianceType(),w.getApplianceType())||rule==null||!service.getId().equals(rule.getServiceItemId())||!Objects.equals(rule.getFeeType(),in.feeType())||!effective(rule))throw BusinessException.validation("服务项目、设备或当前价格规则不匹配");
            PricingConfigurationService.validatePrices(rule.getFixedPrice(),rule.getMinPrice(),rule.getMaxPrice());
            var row=new QuoteItemEntity();row.setServiceItemId(service.getId());row.setFeeType(in.feeType());row.setPricingRuleId(rule.getId());
            var price=rule.getFixedPrice();if(price!=null){if(in.selectedUnitPrice()!=null&&price.compareTo(money(in.selectedUnitPrice()))!=0)throw BusinessException.validation("固定价格不可修改");}
            else price=money(in.selectedUnitPrice());
            row.setUnitPrice(price);row.setPricingReason(reason(in.pricingReason(),rule.getFixedPrice()==null||"OTHER".equals(in.feeType())));
            row.setQuantity(quantity(in.quantity()));row.setAmount(bounded(pricing.lineAmount(price,row.getQuantity())));
            boolean part="PART".equals(in.feeType());row.setItemName(part?text(in.itemName(),1,200,"配件名称"):in.itemName()==null?service.getName():text(in.itemName(),1,200,"项目名称"));
            row.setSpecification(in.specification()==null&&!part?null:text(in.specification(),part?1:0,200,"规格"));row.setPartSourceNote(in.partSourceNote()==null&&!part?null:text(in.partSourceNote(),part?1:0,1000,"配件来源"));
            row.setAdditionReason(reason(in.additionReason(),"ADDITIONAL".equals(q.getQuoteType())));row.setUnit(rule.getUnit());row.setPricingClauseNo(rule.getClauseNo());row.setPricingClauseTitle(rule.getTitle());
            row.setPricingRuleVersion(rule.getRuleVersion());row.setPricingBasisSnapshot(serialize(new TreeMap<>(Map.of("description",Objects.toString(rule.getDescription(),""),"refundPolicy",Objects.toString(rule.getRefundPolicy(),""),"pricingReason",Objects.toString(row.getPricingReason(),"")))));
            row.setPricingRuleFingerprint(ruleFingerprint(service,rule));row.setCreatedBy(actor);row.setCreatedAt(clock.now());row.setSortOrder(position++);row.setContentRevision(q.getItemRevision());result.add(row);
        }
        validateVisitCharge(w,result);
        return result;
    }
    private void amounts(QuoteEntity q,List<QuoteItemEntity> lines){q.setSubtotalAmount(bounded(pricing.subtotal(lines.stream().map(QuoteItemEntity::getAmount).toList())));q.setTotalAmount(bounded(pricing.total(q.getConfirmedAmountBefore(),q.getSubtotalAmount())));}
    private boolean currentRules(WorkOrderEntity w,QuoteEntity q,List<QuoteItemEntity> lines){
        if(lines.isEmpty())return false;for(var line:lines){var service=services.selectById(line.getServiceItemId());var rule=rules.selectById(line.getPricingRuleId());
            if(service==null||rule==null||!"ENABLED".equals(service.getStatus())||!Objects.equals(service.getApplianceType(),w.getApplianceType())||!effective(rule)||!Objects.equals(line.getPricingRuleFingerprint(),ruleFingerprint(service,rule)))return false;
        }return true;
    }
    private void validateCurrent(WorkOrderEntity w,QuoteEntity q,List<QuoteItemEntity> lines){
        if(!validExpiry(q))throw BusinessException.stateConflict("报价已过有效期，请编辑后重报");if(!currentRules(w,q,lines))throw BusinessException.stateConflict("价格规则已变化或失效，请编辑草稿重新引用");
        validateVisitCharge(w,lines);
        var subtotal=pricing.subtotal(lines.stream().map(i->pricing.lineAmount(i.getUnitPrice(),i.getQuantity())).toList());
        if(subtotal.compareTo(q.getSubtotalAmount())!=0||pricing.total(q.getConfirmedAmountBefore(),subtotal).compareTo(q.getTotalAmount())!=0)throw BusinessException.stateConflict("报价金额快照不一致");
    }
    private void validateVisitCharge(WorkOrderEntity order,List<QuoteItemEntity> items) {
        if(followUp.isRework(order)&&items.stream().anyMatch(item->!Set.of("PART","MATERIAL").contains(item.getFeeType())))
            throw BusinessException.validation("返修检测和原维修范围免费，追加报价仅可包含新增配件或材料");
        var visits=items.stream().filter(item->"VISIT".equals(item.getFeeType())).toList();
        if(visits.isEmpty())return;
        if(visits.size()!=1||visits.getFirst().getQuantity().compareTo(BigDecimal.ONE)!=0
                ||visits.getFirst().getUnitPrice().compareTo(CustomerCancellationPolicy.STANDARD_VISIT_FEE)!=0)
            throw BusinessException.validation("上门费最多一项，数量为 1，固定 50 元");
        if(quoteRows(order.getId(),false).stream().filter(quote->"ACCEPTED".equals(quote.getStatus()))
                .anyMatch(quote->lines(quote).stream().anyMatch(item->"VISIT".equals(item.getFeeType()))))
            throw BusinessException.validation("该工单已确认上门费，不能重复计费");
    }
    private boolean outside(List<QuoteItemEntity> lines){for(var i:lines){var r=rules.selectById(i.getPricingRuleId());if(r!=null&&r.getFixedPrice()==null&&(i.getUnitPrice().compareTo(r.getMinPrice())<0||i.getUnitPrice().compareTo(r.getMaxPrice())>0))return true;}return false;}
    private boolean effective(PricingRuleEntity r){var now=clock.now();return "ENABLED".equals(r.getStatus())&&r.getEffectiveFrom()!=null&&!r.getEffectiveFrom().isAfter(now)&&(r.getEffectiveTo()==null||r.getEffectiveTo().isAfter(now));}
    private String ruleFingerprint(ServiceItemEntity service,PricingRuleEntity rule){return digest(serialize(List.of(service,rule)));}
    /** 内容摘要不包含提交/接受状态、流程时间、行主键；编辑修订号绑定本次内容版本。 */
    public String contentHash(QuoteEntity q){
        var data=new TreeMap<String,Object>();data.put("workOrderId",q.getWorkOrderId());data.put("quoteType",q.getQuoteType());data.put("versionNo",q.getVersionNo());data.put("previousQuoteId",q.getPreviousQuoteId());data.put("baseQuoteId",q.getBaseQuoteId());data.put("currency",q.getCurrency());
        data.put("subtotal",q.getSubtotalAmount());data.put("confirmedBefore",q.getConfirmedAmountBefore());data.put("total",q.getTotalAmount());data.put("changeReason",q.getChangeReason());data.put("expiresAt",q.getExpiresAt());data.put("source",q.getSubmissionSource());data.put("technicianId",q.getTechnicianId());data.put("serviceRecordId",q.getServiceRecordId());data.put("createdBy",q.getCreatedBy());data.put("itemRevision",q.getItemRevision());
        data.put("items",lines(q).stream().map(i->{var row=new TreeMap<String,Object>();row.put("feeType",i.getFeeType());row.put("serviceItemId",i.getServiceItemId());row.put("pricingRuleId",i.getPricingRuleId());row.put("name",i.getItemName());row.put("specification",i.getSpecification());row.put("unit",i.getUnit());row.put("unitPrice",i.getUnitPrice());row.put("quantity",i.getQuantity());row.put("amount",i.getAmount());row.put("clauseNo",i.getPricingClauseNo());row.put("clauseTitle",i.getPricingClauseTitle());row.put("ruleVersion",i.getPricingRuleVersion());row.put("basis",i.getPricingBasisSnapshot());row.put("partSource",i.getPartSourceNote());row.put("additionReason",i.getAdditionReason());row.put("pricingReason",i.getPricingReason());row.put("ruleFingerprint",i.getPricingRuleFingerprint());row.put("sortOrder",i.getSortOrder());return row;}).toList());return digest(serialize(data));
    }
    private void hash(QuoteEntity q,String hash){if(!Objects.equals(contentHash(q),hash))throw quoteConflict("报价内容已变化，请刷新");}
    private boolean hasCurrentApproval(QuoteEntity quote,QuoteOverrideApprovalEntity approval){
        return approval!=null&&"APPROVED".equals(approval.getStatus())
                &&Objects.equals(approval.getQuoteContentHash(),contentHash(quote));
    }
    private StaffQuoteVO view(WorkOrderEntity w,QuoteEntity q,Actor a){
        var row=latestApproval(q,false);List<String> actions=new ArrayList<>();boolean metadata=metadata(q);
        if(metadata&&!"AUDITOR".equals(a.role()))try{
            writeScope(w,a);source(w,q,a);current(w,q);
            if(Set.of("DRAFT","SUBMITTED").contains(q.getStatus()))actions.add("WITHDRAW");
            if("DRAFT".equals(q.getStatus())&&Set.of("WAITING_QUOTE","IN_REPAIR").contains(w.getStatus())){
                draftState(w);
                actions.add("EDIT");
                var lines=lines(q);
                if(validExpiry(q)&&currentRules(w,q,lines)){
                    if(!outside(lines)){
                        actions.add("SUBMIT");
                    }else if(hasCurrentApproval(q,row)){
                        if(a.id().equals(row.getRequestedBy())&&!a.id().equals(row.getReviewedBy()))actions.add("SUBMIT");
                    }else if(row==null||!"PENDING".equals(row.getStatus())){
                        actions.add("REQUEST_OVERRIDE");
                    }
                }
            }
        }catch(BusinessException denied){/* 单一动作投影采用与写端相同守卫，失效不开放按钮。 */}
        if(row!=null&&metadata)try{decisionPolicy(w,q,row,a);actions.add("APPROVE_OVERRIDE");actions.add("REJECT_OVERRIDE");}catch(BusinessException denied){}
        Long technician=q.getTechnicianId();if(technician==null&&q.getServiceRecordId()!=null){var r=records.selectById(q.getServiceRecordId());if(r!=null&&q.getWorkOrderId().equals(r.getWorkOrderId()))technician=r.getTechnicianId();}
        return new StaffQuoteVO(assembler.toVO(q),contentHash(q),q.getSubmissionSource(),q.getCreatedBy(),str(q.getSubmittedBy()),str(q.getServiceRecordId()),row==null?null:approvalView(row),actions,str(technician));
    }
    private boolean metadata(QuoteEntity q){return q.getTechnicianId()!=null&&q.getServiceRecordId()!=null&&Set.of("TECHNICIAN_SELF","DISPATCHER_PROXY","ADMIN_TAKEOVER").contains(Objects.toString(q.getSubmissionSource(),""));}
    private void requireMetadata(QuoteEntity q){if(!metadata(q))throw BusinessException.stateConflict("历史履约元数据不足，仅可只读查看");}
    /** Called under the work-order lock so acceptance and withdrawal cannot race. */
    public void withdrawForLifecycle(Long orderId,String actorType,Long actor,String reason){
        for(var q:quotes.selectList(new QueryWrapper<QuoteEntity>().eq("work_order_id",orderId)
                .in("status",List.of("DRAFT","SUBMITTED")).orderByAsc("id").last("FOR UPDATE"))){
            if(quotes.update(null,new UpdateWrapper<QuoteEntity>().eq("id",q.getId()).eq("version",q.getVersion())
                    .set("status","WITHDRAWN").set("version",q.getVersion()+1).set("updated_at",clock.now()))!=1)
                throw BusinessException.versionConflict("报价已更新");
            invalidate(q,reason);
            audit.record(actorType,actor,"QUOTE_WITHDRAWN_FOR_COORDINATION","QUOTE",q.getId(),null,Map.of("reason",reason));
        }
    }
    private void invalidate(QuoteEntity q,String reason){for(var a:approvals.selectList(new QueryWrapper<QuoteOverrideApprovalEntity>().eq("quote_id",q.getId()).in("status",List.of("PENDING","APPROVED")).orderByAsc("id").last("FOR UPDATE"))){a.setStatus("INVALIDATED");a.setInvalidatedAt(clock.now());a.setVersion(a.getVersion()+1);a.setUpdatedAt(clock.now());approvals.updateById(a);audit.recordSystem("QUOTE_OVERRIDE_INVALIDATED","QUOTE",q.getId(),Map.of("approvalId",a.getId()),Map.of("reason",reason));}}
    private QuoteOverrideApprovalEntity latestApproval(QuoteEntity q,boolean lock){return approvals.selectOne(new QueryWrapper<QuoteOverrideApprovalEntity>().eq("quote_id",q.getId()).orderByDesc("id").last(lock?"LIMIT 1 FOR UPDATE":"LIMIT 1"));}
    private QuoteOverrideApprovalVO approvalView(QuoteOverrideApprovalEntity a){return new QuoteOverrideApprovalVO(a.getId(),a.getQuoteId(),a.getRequestedBy(),str(a.getReviewedBy()),a.getQuoteContentHash(),a.getStatus(),a.getVersion(),a.getOverrideReason(),a.getReviewComment());}
    private WorkOrderEntity work(Long id,Actor actor){
        if(!actor.wechat()){var candidate=order(id,false);if(candidate.getTechnicianId()!=null){var binding=bindings.selectOne(new QueryWrapper<StaffWechatBindingEntity>().eq("staff_id",candidate.getTechnicianId()).eq("status","ACTIVE"));if(binding!=null)employees.requireLocked(binding.getCustomerId());}}
        return order(id,true);
    }
    private WorkOrderEntity order(Long id,boolean lock){var r=orders.selectOne(new QueryWrapper<WorkOrderEntity>().eq("id",id).last(lock?"FOR UPDATE":""));if(r==null)throw BusinessException.notFound("工单不存在");return r;}
    private QuoteEntity quote(Long id,boolean lock){var q=quotes.selectOne(new QueryWrapper<QuoteEntity>().eq("id",id).last(lock?"FOR UPDATE":""));if(q==null)throw BusinessException.notFound("报价不存在");return q;}
    private List<QuoteEntity> quoteRows(Long id,boolean lock){return quotes.selectList(new QueryWrapper<QuoteEntity>().eq("work_order_id",id).orderByAsc("version_no").last(lock?"FOR UPDATE":""));}
    private void validateHistory(List<QuoteEntity> all){QuoteEntity prior=null;int version=1;for(var q:all){if(q.getVersionNo()==null||q.getVersionNo()!=version++||!Objects.equals(q.getPreviousQuoteId(),prior==null?null:prior.getId()))throw BusinessException.stateConflict("历史报价版本链不连续");prior=q;}}
    private QuoteEntity base(List<QuoteEntity> all){return all.stream().filter(q->"ACCEPTED".equals(q.getStatus())).max(Comparator.comparing(QuoteEntity::getVersionNo)).orElse(null);}
    private List<QuoteItemEntity> lines(QuoteEntity q){return items.selectList(new QueryWrapper<QuoteItemEntity>().eq("quote_id",q.getId()).eq("content_revision",q.getItemRevision()==null?0:q.getItemRevision()).orderByAsc("sort_order","id"));}
    private void append(QuoteEntity q,List<QuoteItemEntity> lines){for(var i:lines){i.setQuoteId(q.getId());items.insert(i);}}
    private void save(QuoteEntity q){int old=q.getVersion();q.setVersion(old+1);q.setUpdatedAt(clock.now());if(quotes.update(q,new UpdateWrapper<QuoteEntity>().eq("id",q.getId()).eq("version",old))!=1)throw BusinessException.versionConflict("报价已变化");}
    private void bump(WorkOrderEntity w){if(orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",w.getId()).eq("version",w.getVersion()).set("version",w.getVersion()+1).set("updated_at",clock.now()))!=1)throw BusinessException.versionConflict("工单已变化");w.setVersion(w.getVersion()+1);}
    private void event(Actor a,String action,QuoteEntity q,Map<String,Object> extra){var data=new TreeMap<String,Object>(extra);if(data.get("reason") instanceof String reason){var w=orders.selectById(q.getWorkOrderId());var r=w==null?null:repairs.selectById(w.getRepairRequestId());data.put("reason",r==null?reason.replaceAll("(?<![0-9])1[0-9]{10}(?![0-9])","[电话已脱敏]"):contacts.redactNote(reason,r));}data.put("quoteVersion",q.getVersion());data.put("versionNo",q.getVersionNo());data.put("technicianId",q.getTechnicianId());data.put("actualOperator",a.id());audit.record("STAFF",a.id(),action,"QUOTE",q.getId(),null,data);}
    private String text(String s,int min,int max,String label){return StaffIdentityGuard.text(s,min,max,label);}
    private String reason(String value,boolean required){return text(value,required?2:0,1000,"报价依据");}
    private LocalDateTime expiry(OffsetDateTime v){if(v==null)throw BusinessException.validation("报价有效期不能为空");var t=v.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);if(!t.isAfter(clock.now()))throw BusinessException.validation("报价有效期须晚于当前时间");return t;}
    private boolean validExpiry(QuoteEntity q){return q.getExpiresAt()!=null&&q.getExpiresAt().isAfter(clock.now());}
    private BigDecimal money(String s){if(s==null||!s.matches("^[0-9]{1,10}\\.[0-9]{2}$"))throw BusinessException.validation("须明确选择有效单价");return new BigDecimal(s);}
    private BigDecimal quantity(String s){if(s==null||!s.matches("^(?:0\\.(?!000$)[0-9]{3}|[1-9][0-9]{0,6}\\.[0-9]{3})$"))throw BusinessException.validation("数量须大于零且为三位小数");return new BigDecimal(s);}
    private BigDecimal bounded(BigDecimal v){if(v.signum()<0||v.compareTo(new BigDecimal("9999999999.99"))>0)throw BusinessException.validation("报价金额超出范围");return v;}
    private Long parse(String v){if(v==null)return null;try{long n=Long.parseLong(v);if(n>0)return n;}catch(NumberFormatException ignored){}throw BusinessException.validation("报价ID不正确");}
    private BusinessException quoteConflict(String message){return new BusinessException(com.fixmate.common.api.ErrorCode.Codes.QUOTE_NOT_CURRENT,org.springframework.http.HttpStatus.CONFLICT,message);}
    private String str(Long v){return v==null?null:v.toString();}
    private String serialize(Object v){try{return json.writeValueAsString(v);}catch(Exception e){throw new IllegalStateException("报价快照序列化失败",e);}}
    private String digest(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
