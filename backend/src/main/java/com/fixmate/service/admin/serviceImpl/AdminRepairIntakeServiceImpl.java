package com.fixmate.service.admin.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.assembler.RepairRecordAssembler;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.dto.user.SafetyAnswersDTO;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.AdminRepairIntakeService;
import com.fixmate.service.common.*;
import com.fixmate.service.user.UserRepairRequestService;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRepairIntakeServiceImpl implements AdminRepairIntakeService {
    private final RepairRequestMapper requests;
    private final AdminRepairRequestQueryMapper query;
    private final WorkOrderMapper orders;
    private final CustomerUserMapper customers;
    private final SafetyEventMapper events;
    private final StaffAccessService access;
    private final UserRepairRequestService userRequests;
    private final RepairRecordAssembler assembler;
    private final ServiceContactProjection contacts;
    private final SafetyRuleEngine safety;
    private final ObjectMapper json;
    private final DatabaseClockMapper clock;
    private final AuditLogService audit;
    public AdminRepairIntakeServiceImpl(RepairRequestMapper requests,AdminRepairRequestQueryMapper query,WorkOrderMapper orders,
            CustomerUserMapper customers,SafetyEventMapper events,StaffAccessService access,UserRepairRequestService userRequests,
            RepairRecordAssembler assembler,ServiceContactProjection contacts,SafetyRuleEngine safety,ObjectMapper json,
            DatabaseClockMapper clock,AuditLogService audit) {
        this.requests=requests;this.query=query;this.orders=orders;this.customers=customers;this.events=events;this.access=access;
        this.userRequests=userRequests;this.assembler=assembler;this.contacts=contacts;this.safety=safety;this.json=json;this.clock=clock;this.audit=audit;
    }
    @Override public RepairRecordListItemVOPageVO list(StaffPrincipal actor,int page,int size,String status,boolean pendingReview,String repairNo,boolean pendingCustomerConfirmation) {
        access.require(actor,"ADMIN","DISPATCHER","AUDITOR");StaffDirectoryServiceImpl.page(page,size);
        if(status!=null&&!Set.of("DRAFT","SUBMITTED","COLLECTING","TRIAGED","HUMAN_HANDOFF","CONVERTED","HIGH_RISK_BLOCKED","CANCELLED").contains(status))
            throw BusinessException.validation("报修状态不正确");
        var result=query.list(new Page<>(page,size),status,pendingReview,repairNo,pendingCustomerConfirmation);
        return new RepairRecordListItemVOPageVO(result.getRecords(),page,size,result.getTotal());
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public AdminRepairRequestDetailVO detail(StaffPrincipal actor,Long id) {
        var staff=access.require(actor,"ADMIN","DISPATCHER","AUDITOR");return view(read(id,false),staff.getRole());
    }
    @Override @Transactional public AdminRepairRequestDetailVO triage(StaffPrincipal actor,Long id,ManualTriageDTO input) {
        var staff=access.requireLocked(actor,"ADMIN","DISPATCHER");var request=read(id,true);
        StaffIdentityGuard.version(request.getVersion(),input.expectedVersion());
        if(!Boolean.TRUE.equals(input.informationConfirmed())||!Boolean.TRUE.equals(input.safetyReviewed()))throw BusinessException.validation("须确认信息与安全核对");
        String note=input.reviewNote()==null?null:contacts.redactNote(StaffIdentityGuard.text(input.reviewNote(),2,1000,"核对依据"),request);
        if(!eligible(request))throw BusinessException.stateConflict("当前报修不符合人工核对前提");
        if(request.getServiceContactSnapshot()==null)request.setServiceContactSnapshot(contacts.confirmLegacy(request,input.expectedAddressVersion()));
        if(!contacts.complete(request))throw BusinessException.stateConflict("服务联系信息不完整，不能完成核对");
        var at=clock.now();int changed=requests.update(null,new UpdateWrapper<RepairRequestEntity>().eq("id",id)
                .eq("version",input.expectedVersion()).in("status",List.of("SUBMITTED","HUMAN_HANDOFF")).isNull("work_order_id")
                .set("status","TRIAGED").set("service_contact_snapshot",request.getServiceContactSnapshot())
                .set("manual_reviewed_by",actor.staffId()).set("manual_reviewed_at",at).set("manual_review_note",note)
                .set("version",input.expectedVersion()+1).set("updated_at",at));
        changed(changed);request.setStatus("TRIAGED");request.setManualReviewedBy(actor.staffId());request.setManualReviewedAt(at);
        request.setManualReviewNote(note);request.setVersion(input.expectedVersion()+1);
        var after=auditResult(request,note,input.expectedAddressVersion());
        after.put("informationConfirmed",true);
        after.put("safetyReviewed",true);
        audit.record("STAFF",actor.staffId(),"REPAIR_MANUALLY_TRIAGED","REPAIR_REQUEST",id,Map.of("version",input.expectedVersion()),after);
        return view(request,staff.getRole());
    }
    @Override @Transactional public AdminRepairRequestDetailVO confirmContact(StaffPrincipal actor,Long id,ServiceContactConfirmationDTO input) {
        var staff=access.requireLocked(actor,"ADMIN","DISPATCHER");var request=read(id,true);
        StaffIdentityGuard.version(request.getVersion(),input.expectedVersion());
        if(!contactEligible(request))throw BusinessException.stateConflict("当前报修无需或不能补齐联系方式");
        String note=contacts.redactNote(StaffIdentityGuard.text(input.reviewNote(),2,1000,"联系方式核对依据"),request);
        String snapshot=contacts.confirmLegacy(request,input.expectedAddressVersion());
        int rows=requests.update(null,new UpdateWrapper<RepairRequestEntity>().eq("id",id).eq("version",input.expectedVersion())
                .isNull("service_contact_snapshot").set("service_contact_snapshot",snapshot).set("version",input.expectedVersion()+1).set("updated_at",clock.now()));
        changed(rows);request.setServiceContactSnapshot(snapshot);request.setVersion(input.expectedVersion()+1);
        audit.record("STAFF",actor.staffId(),"REPAIR_SERVICE_CONTACT_CONFIRMED","REPAIR_REQUEST",id,
                Map.of("version",input.expectedVersion()),auditResult(request,note,input.expectedAddressVersion()));
        return view(request,staff.getRole());
    }
    private RepairRequestEntity read(Long id,boolean lock) {
        var condition=new LambdaQueryWrapper<RepairRequestEntity>().eq(RepairRequestEntity::getId,id);if(lock)condition.last("FOR UPDATE");
        var r=requests.selectOne(condition);if(r==null)throw BusinessException.notFound("报修不存在");
        if(r.getWorkOrderId()!=null) {
            var w=orders.selectById(r.getWorkOrderId());
            if(w==null||!r.getUserId().equals(w.getCustomerId())||!r.getId().equals(w.getRepairRequestId()))throw BusinessException.stateConflict("报修工单关联异常");
            // 旧紧急单的原始地址在工单快照中；仅投影回退，不改报修历史字段。
            if(r.getAddressSnapshot()==null)r.setAddressSnapshot(w.getAddressSnapshot());
        }
        return r;
    }
    private boolean eligible(RepairRequestEntity r) {
        if(!Set.of("SUBMITTED","HUMAN_HANDOFF").contains(r.getStatus())||r.getWorkOrderId()!=null||r.getTriageSessionId()!=null)return false;
        if(orders.selectCount(new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getRepairRequestId,r.getId()))>0)return false;
        var owner=customers.selectById(r.getUserId());if(owner==null||!"ENABLED".equals(owner.getStatus()))return false;
        if(r.getApplianceType()==null||r.getBrandId()==null||r.getAddressId()==null||r.getSymptomDescription()==null
                ||r.getSymptomDescription().strip().length()<10||r.getSymptomDescription().length()>1000
                ||(r.getModelId()==null&&(r.getModelText()==null||r.getModelText().isBlank())))return false;
        if(events.selectCount(new LambdaQueryWrapper<SafetyEventEntity>().eq(SafetyEventEntity::getRepairRequestId,r.getId()).ne(SafetyEventEntity::getStatus,"RESOLVED"))>0)return false;
        try {
            SafetyAnswersDTO a=json.readValue(r.getSafetyAnswers(),SafetyAnswersDTO.class);
            if(a==null||a.getSmokeOrBurningSmell()==null||a.getElectricLeakage()==null||a.getGasSmell()==null||a.getSuspectedRefrigerantLeak()==null||a.getSevereWaterIngress()==null)return false;
            String text=String.join(" ",Objects.toString(r.getSymptomDescription(),""),Objects.toString(r.getErrorCodeText(),""),Objects.toString(r.getModelText(),""));
            return safety.match(a,text).isEmpty();
        } catch(com.fasterxml.jackson.core.JsonProcessingException|IllegalArgumentException ex){return false;}
    }
    private boolean contactEligible(RepairRequestEntity r) {
        if(r.getServiceContactSnapshot()!=null||Set.of("DRAFT","CANCELLED","COLLECTING").contains(r.getStatus()))return false;
        var owner=customers.selectById(r.getUserId());if(owner==null||!"ENABLED".equals(owner.getStatus()))return false;
        if(r.getWorkOrderId()!=null) {
            var order=orders.selectById(r.getWorkOrderId());
            if(order==null||Set.of("COMPLETED","CANCELLED","CLOSED_UNRESOLVED").contains(order.getStatus()))return false;
        }
        return contacts.complete(r);
    }
    private AdminRepairRequestDetailVO view(RepairRequestEntity r,String role) {
        boolean writer=Set.of("ADMIN","DISPATCHER").contains(role);var actions=new ArrayList<String>();
        if(writer&&eligible(r)&&contacts.complete(r))actions.add("MANUAL_TRIAGE");
        if(writer&&contactEligible(r))actions.add("CONFIRM_SERVICE_CONTACT");
        boolean disclose=writer&&!Set.of("DRAFT","CANCELLED").contains(r.getStatus());
        if(disclose&&r.getWorkOrderId()!=null) {
            var w=orders.selectById(r.getWorkOrderId());disclose=w!=null&&!Set.of("COMPLETED","CANCELLED","CLOSED_UNRESOLVED").contains(w.getStatus());
        }
        var snapshot=disclose?userRequests.get(r.getUserId(),r.getId()):userRequests.getWithoutMedia(r.getUserId(),r.getId());
        if(!disclose){snapshot.setAddressId(null);snapshot.setSymptomDescription(contacts.redactPersonalText(snapshot.getSymptomDescription(),r));snapshot.setModelText(contacts.redactPersonalText(snapshot.getModelText(),r));snapshot.setErrorCodeText(contacts.redactPersonalText(snapshot.getErrorCodeText(),r));}
        return new AdminRepairRequestDetailVO(assembler.snapshot(snapshot),access.actor(r.getManualReviewedBy()),
                r.getManualReviewedAt()==null?null:r.getManualReviewedAt().atOffset(java.time.ZoneOffset.UTC),disclose?r.getManualReviewNote():contacts.redactPersonalText(r.getManualReviewNote(),r),actions,contacts.view(r,disclose));
    }
    private Map<String,Object> auditResult(RepairRequestEntity r,String note,Integer addressVersion) {
        var result=new LinkedHashMap<String,Object>();result.put("status",r.getStatus());result.put("version",r.getVersion());result.put("reviewNote",note);
        result.put("contactSource",contacts.view(r,false).contactSource());result.put("addressVersion",addressVersion);return result;
    }
    private void changed(int n){if(n!=1)throw BusinessException.versionConflict("报修已变化，请刷新核对");}
}
