package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.StaffIdentityGuard;
import com.fixmate.service.user.UserProfileService;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 客户期望不是正式预约。创建CONFIRMED之前，调用方必须持有师傅身份及工单根锁。 */
@Service
public class VisitSchedulingService {
    private static final List<String> CURRENT=List.of("PENDING","CONFIRMED","RESCHEDULE_REQUESTED");
    private static final String RESCHEDULE_DECLINE_REASON="原维修人员无法按客户的新时段上门，工单退回待分派";
    private final WorkOrderMapper orders; private final WorkOrderAssignmentMapper assignments;
    private final AppointmentMapper appointments; private final CustomerUserMapper customers;
    private final RepairRequestMapper repairs; private final UserProfileService profiles;
    private final EmployeeWechatAccessService employees; private final WorkOrderSafetyGate safety;
    private final ServiceContactProjection contacts; private final DatabaseClockMapper clock;
    private final WorkOrderStateMachine machine; private final AuditLogService audit;
    private final CustomerCancellationPolicy cancellations;
    private final WorkOrderFollowUp followUp;
    public VisitSchedulingService(WorkOrderMapper orders,WorkOrderAssignmentMapper assignments,AppointmentMapper appointments,
            CustomerUserMapper customers,RepairRequestMapper repairs,UserProfileService profiles,EmployeeWechatAccessService employees,
            WorkOrderSafetyGate safety,ServiceContactProjection contacts,DatabaseClockMapper clock,WorkOrderStateMachine machine,AuditLogService audit,
            CustomerCancellationPolicy cancellations,WorkOrderFollowUp followUp){
        this.orders=orders;this.assignments=assignments;this.appointments=appointments;this.customers=customers;this.repairs=repairs;
        this.profiles=profiles;this.employees=employees;this.safety=safety;this.contacts=contacts;this.clock=clock;this.machine=machine;this.audit=audit;
        this.cancellations=cancellations;
        this.followUp=followUp;
    }
    public static PreferredVisitVO preferred(WorkOrderEntity w){return w==null||w.getPreferredVisitStartAt()==null||w.getPreferredVisitEndAt()==null?null:new PreferredVisitVO(offset(w.getPreferredVisitStartAt()),offset(w.getPreferredVisitEndAt()));}
    public static String policy(WorkOrderEntity w){return w==null?"LEGACY_PLATFORM":w.getVisitPolicy();}
    private static OffsetDateTime offset(LocalDateTime t){return t.atOffset(ZoneOffset.UTC);}
    public static boolean wasRescheduleDeclined(WorkOrderAssignmentEntity assignment) {
        return "REVOKED".equals(assignment.getStatus()) && assignment.getAcceptedAt()!=null
                && Objects.equals(assignment.getTechnicianId(),assignment.getEndedBy())
                && RESCHEDULE_DECLINE_REASON.equals(assignment.getEndReason());
    }
    public static void validate(LocalDateTime start,LocalDateTime end,LocalDateTime now){
        if(start==null||end==null||!start.isAfter(now)||!end.isAfter(start)||Duration.between(start,end).compareTo(Duration.ofHours(4))>0||start.isAfter(now.plusDays(30)))
            throw BusinessException.validation("请选择未来30天内、开始早于结束且不超过4小时的期望上门时段");
    }
    public boolean hasValidPreference(WorkOrderEntity w){try{validate(w.getPreferredVisitStartAt(),w.getPreferredVisitEndAt(),clock.now());return true;}catch(BusinessException e){return false;}}
    public boolean hasReworkPreference(WorkOrderEntity w){
        return hasValidPreference(w)&&!matchesCompletedVisit(w.getId(),w.getPreferredVisitStartAt(),w.getPreferredVisitEndAt());
    }
    private boolean matchesCompletedVisit(Long id,LocalDateTime start,LocalDateTime end){
        var previous=latest(id,false);
        return previous!=null&&"COMPLETED".equals(previous.getStatus())
                &&start.equals(previous.getScheduledStartAt())&&end.equals(previous.getScheduledEndAt());
    }
    public void requirePreference(WorkOrderEntity w){validate(w.getPreferredVisitStartAt(),w.getPreferredVisitEndAt(),clock.now());}
    public PreferredVisitResultVO get(Long customerId,Long id){profiles.current(customerId);return result(owned(customerId,id,false));}
    public boolean canSelect(WorkOrderEntity w){
        if(w==null)return false;
        var cause=followUp.manualCause(w);
        boolean awaitingRework=cause!=null&&WorkOrderFollowUp.ACCEPTANCE_ISSUE.equals(cause.type());
        if(!awaitingRework&&w.getTakeoverBy()!=null)return false;
        if(!awaitingRework&&!Set.of("WAITING_CUSTOMER_VISIT","WAITING_ASSIGNMENT","WAITING_APPOINTMENT").contains(w.getStatus()))return false;
        boolean continuingAfterSafety="WAITING_CUSTOMER_VISIT".equals(w.getStatus());
        if((continuingAfterSafety||awaitingRework)&&!safety.clear(w.getId()))return false;
        var ap=latest(w.getId(),false);if(ap!=null&&CURRENT.contains(ap.getStatus()))return false;
        if(w.getCurrentAssignmentId()==null)return (continuingAfterSafety||awaitingRework||"WAITING_ASSIGNMENT".equals(w.getStatus()))&&w.getTechnicianId()==null&&assignments.selectCount(new LambdaQueryWrapper<WorkOrderAssignmentEntity>().eq(WorkOrderAssignmentEntity::getWorkOrderId,w.getId()).in(WorkOrderAssignmentEntity::getStatus,List.of("PENDING","ACTIVE","RETURN_REQUESTED")))==0;
        if(continuingAfterSafety)return false;
        var a=assignments.selectById(w.getCurrentAssignmentId());if(a==null||!w.getId().equals(a.getWorkOrderId()))return false;
        if(awaitingRework&&"FINISHED".equals(a.getStatus()))return true;
        if(w.getTakeoverBy()!=null)return false;
        return ("PENDING".equals(a.getStatus())&&w.getTechnicianId()==null&&a.getAcceptedAt()==null)
            ||("ACTIVE".equals(a.getStatus())&&a.getAcceptedAt()!=null&&a.getTechnicianId().equals(w.getTechnicianId()));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public PreferredVisitResultVO select(Long customerId,Long id,PreferredVisitDTO input){
        var customer=customers.selectOne(new LambdaQueryWrapper<CustomerUserEntity>().eq(CustomerUserEntity::getId,customerId).last("FOR UPDATE"));
        if(customer==null||!"ENABLED".equals(customer.getStatus()))throw BusinessException.forbidden("当前用户不可选择上门时段");
        var w=owned(customerId,id,true);StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());
        if(!canSelect(w))throw BusinessException.stateConflict("任务已接单约定或正在履约，请按正式预约改约流程处理");
        var start=input.startAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);var end=input.endAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);var now=clock.now();validate(start,end,now);
        if(followUp.isRework(w)&&matchesCompletedVisit(id,start,end))
            throw BusinessException.validation("该时段已用于上一轮上门，请选择新的返修时段");
        var before=preferred(w);
        boolean continuingAfterSafety="WAITING_CUSTOMER_VISIT".equals(w.getStatus());
        if(continuingAfterSafety)safety.requireClear(id);
        var slotUpdate=new UpdateWrapper<WorkOrderEntity>().eq("id",id).eq("version",w.getVersion())
                .set("visit_policy","CUSTOMER_SLOT").set("preferred_visit_start_at",start).set("preferred_visit_end_at",end);
        if(!continuingAfterSafety)slotUpdate.set("version",w.getVersion()+1).set("updated_at",now);
        changed(orders.update(null,slotUpdate));
        if(continuingAfterSafety) {
            w=machine.transition(id,WorkOrderStatus.WAITING_ASSIGNMENT,w.getVersion(),
                    "客户确认继续维修并选定上门时段","CUSTOMER",customerId);
        } else {
            w.setVisitPolicy("CUSTOMER_SLOT");w.setPreferredVisitStartAt(start);w.setPreferredVisitEndAt(end);w.setVersion(w.getVersion()+1);
        }
        audit.recordCustomer(customerId,"PREFERRED_VISIT_SELECTED","WORK_ORDER",id,before,Map.of("startAt",start.toString(),"endAt",end.toString(),"version",w.getVersion()));return result(w);
    }
    private PreferredVisitResultVO result(WorkOrderEntity w){return new PreferredVisitResultVO(w.getId(),w.getVersion(),policy(w),preferred(w),canSelect(w)?List.of("SELECT_PREFERRED_VISIT"):List.of());}
    /** 同一员工行锁串行化不同工单的确认；FOR UPDATE保证即使外层事务较早建立快照也读到最新预约。 */
    public void requireAvailable(WorkOrderEntity w,Long staffId,LocalDateTime start,LocalDateTime end){
        validate(start,end,clock.now());
        var overlap=appointments.selectList(new LambdaQueryWrapper<AppointmentEntity>().select(AppointmentEntity::getId)
            .eq(AppointmentEntity::getTechnicianId,staffId).ne(AppointmentEntity::getWorkOrderId,w.getId()).in(AppointmentEntity::getStatus,CURRENT)
            .and(q->q.nested(t->t.lt(AppointmentEntity::getScheduledStartAt,end).gt(AppointmentEntity::getScheduledEndAt,start))
                .or(t->t.lt(AppointmentEntity::getPreviousConfirmedStartAt,end).gt(AppointmentEntity::getPreviousConfirmedEndAt,start))).last("FOR UPDATE"));
        if(!overlap.isEmpty())throw BusinessException.stateConflict("该时段与你已有预约冲突，请选择其他任务或协调改约");
    }
    /** 在分派已写ACTIVE、根已到WAITING_APPOINTMENT之后调用；任何异常回滚整个接单。 */
    public void afterAcceptance(WorkOrderEntity w,Long staffId){
        if(!"CUSTOMER_SLOT".equals(w.getVisitPolicy())&&!followUp.isRework(w))return;
        requirePreference(w);requireAvailable(w,staffId,w.getPreferredVisitStartAt(),w.getPreferredVisitEndAt());
        var ap=latest(w.getId(),true);if(ap!=null&&CURRENT.contains(ap.getStatus()))throw BusinessException.stateConflict("已有当前预约，请刷新核对");
        createConfirmed(w,staffId);
    }
    private void createConfirmed(WorkOrderEntity w,Long staffId){
        var ap=new AppointmentEntity();var now=clock.now();ap.setWorkOrderId(w.getId());ap.setTechnicianId(staffId);ap.setStatus("CONFIRMED");
        ap.setScheduledStartAt(w.getPreferredVisitStartAt());ap.setScheduledEndAt(w.getPreferredVisitEndAt());ap.setNote("用户选择时段，维修人员确认履约");ap.setVersion(0);ap.setCreatedAt(now);ap.setUpdatedAt(now);changed(appointments.insert(ap));
        appointed(w,staffId);audit.record("STAFF",staffId,"CUSTOMER_VISIT_CONFIRMED","WORK_ORDER",w.getId(),null,Map.of("appointmentId",ap.getId(),"startAt",ap.getScheduledStartAt().toString(),"endAt",ap.getScheduledEndAt().toString()));
    }
    public boolean canConfirm(WorkOrderEntity w,Long staffId){
        if(w.getTakeoverBy()!=null||!Set.of("WAITING_APPOINTMENT","APPOINTED").contains(w.getStatus()))return false;
        var a=own(w,staffId,false);if(a==null||!safety.clear(w.getId()))return false;
        if(!contacts.confirmedForOrder(repairs.selectById(w.getRepairRequestId()),w))return false;
        var ap=latest(w.getId(),false);
        if(ap!=null&&"RESCHEDULE_REQUESTED".equals(ap.getStatus())){try{validate(ap.getScheduledStartAt(),ap.getScheduledEndAt(),clock.now());return ap.getTechnicianId()==null||staffId.equals(ap.getTechnicianId());}catch(BusinessException e){return false;}}
        return (ap==null||!CURRENT.contains(ap.getStatus()))&&"WAITING_APPOINTMENT".equals(w.getStatus())&&hasValidPreference(w);
    }
    public boolean canDeclineReschedule(WorkOrderEntity w,Long staffId) {
        if(w.getTakeoverBy()!=null||!Set.of("WAITING_APPOINTMENT","APPOINTED").contains(w.getStatus())
                ||own(w,staffId,false)==null)return false;
        var ap=latest(w.getId(),false);
        return ap!=null&&"RESCHEDULE_REQUESTED".equals(ap.getStatus())
                &&(ap.getTechnicianId()==null||staffId.equals(ap.getTechnicianId()))&&cancellations.canCancelNow(w);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void declineReschedule(Long customerId,Long id,VersionCommandDTO input) {
        var person=employees.requireLocked(customerId);var w=order(id,true);
        StaffIdentityGuard.version(w.getVersion(),input.expectedVersion());
        if(!canDeclineReschedule(w,person.staffId()))throw BusinessException.stateConflict("当前没有可由本人拒绝的改约申请");
        var a=own(w,person.staffId(),true);var ap=latest(id,true);var now=clock.now();
        String reason=RESCHEDULE_DECLINE_REASON;
        changed(assignments.update(null,new UpdateWrapper<WorkOrderAssignmentEntity>().eq("id",a.getId())
                .eq("version",a.getVersion()).eq("status","ACTIVE").set("status","REVOKED")
                .set("ended_by",person.staffId()).set("ended_at",now).set("end_reason",reason)
                .set("version",a.getVersion()+1).set("updated_at",now)));
        changed(appointments.update(null,new UpdateWrapper<AppointmentEntity>().eq("id",ap.getId())
                .eq("version",ap.getVersion()).eq("status","RESCHEDULE_REQUESTED").set("status","CANCELLED")
                .set("previous_confirmed_start_at",null).set("previous_confirmed_end_at",null)
                .set("version",ap.getVersion()+1).set("updated_at",now)));
        changed(orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",id).eq("version",w.getVersion())
                .set("current_assignment_id",null).set("technician_id",null).set("assigned_at",null)
                .set("visit_policy","CUSTOMER_SLOT").set("preferred_visit_start_at",ap.getScheduledStartAt())
                .set("preferred_visit_end_at",ap.getScheduledEndAt())));
        machine.transition(id,WorkOrderStatus.WAITING_ASSIGNMENT,w.getVersion(),reason,"STAFF",person.staffId());
        audit.record("STAFF",person.staffId(),"ASSIGNMENT_REVOKED","WORK_ASSIGNMENT",a.getId(),null,
                Map.of("workOrderId",id,"eventType","REVOKED","reason",reason,"version",a.getVersion()+1));
        audit.record("STAFF",person.staffId(),"CUSTOMER_RESCHEDULE_DECLINED","WORK_ORDER",id,null,
                Map.of("appointmentId",ap.getId(),"startAt",ap.getScheduledStartAt().toString(),"endAt",ap.getScheduledEndAt().toString()));
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void confirm(Long customerId,Long id,WorkOrderCommandDTO input){
        var person=employees.requireLocked(customerId);var w=order(id,true);StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());
        if(!canConfirm(w,person.staffId())||own(w,person.staffId(),true)==null)throw BusinessException.stateConflict("当前任务没有可由本人确认的用户时段");
        safety.requireClear(id);var ap=latest(id,true);
        if(ap==null||!CURRENT.contains(ap.getStatus())){requireAvailable(w,person.staffId(),w.getPreferredVisitStartAt(),w.getPreferredVisitEndAt());createConfirmed(w,person.staffId());}
        else{
            requireAvailable(w,person.staffId(),ap.getScheduledStartAt(),ap.getScheduledEndAt());
            changed(appointments.update(null,new UpdateWrapper<AppointmentEntity>().eq("id",ap.getId()).eq("version",ap.getVersion()).eq("status","RESCHEDULE_REQUESTED").set("status","CONFIRMED").set("previous_confirmed_start_at",null).set("previous_confirmed_end_at",null).set("technician_id",person.staffId()).set("version",ap.getVersion()+1).set("updated_at",clock.now())));
            appointed(w,person.staffId());audit.record("STAFF",person.staffId(),"CUSTOMER_RESCHEDULE_CONFIRMED","WORK_ORDER",id,null,Map.of("appointmentId",ap.getId(),"startAt",ap.getScheduledStartAt().toString(),"endAt",ap.getScheduledEndAt().toString()));
        }
    }
    private void appointed(WorkOrderEntity w,Long actor){
        if("WAITING_APPOINTMENT".equals(w.getStatus())){var updated=machine.transition(w.getId(),WorkOrderStatus.APPOINTED,w.getVersion(),"维修人员确认用户所选上门时段","STAFF",actor);w.setStatus(updated.getStatus());w.setVersion(updated.getVersion());}
        else{changed(orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",w.getId()).eq("version",w.getVersion()).set("version",w.getVersion()+1).set("updated_at",clock.now())));w.setVersion(w.getVersion()+1);}
    }
    private WorkOrderAssignmentEntity own(WorkOrderEntity w,Long id,boolean lock){
        if(w.getCurrentAssignmentId()==null||!id.equals(w.getTechnicianId()))return null;
        var q=new LambdaQueryWrapper<WorkOrderAssignmentEntity>().eq(WorkOrderAssignmentEntity::getId,w.getCurrentAssignmentId());if(lock)q.last("FOR UPDATE");var a=assignments.selectOne(q);
        return a!=null&&w.getId().equals(a.getWorkOrderId())&&id.equals(a.getTechnicianId())&&"ACTIVE".equals(a.getStatus())&&a.getAcceptedAt()!=null?a:null;
    }
    private WorkOrderEntity owned(Long user,Long id,boolean lock){var w=order(id,lock);if(!user.equals(w.getCustomerId()))throw BusinessException.forbidden("无权读取或选择该工单时段");return w;}
    private WorkOrderEntity order(Long id,boolean lock){var q=new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,id);if(lock)q.last("FOR UPDATE");var w=orders.selectOne(q);if(w==null)throw BusinessException.notFound("工单不存在");return w;}
    private AppointmentEntity latest(Long id,boolean lock){return appointments.selectOne(new LambdaQueryWrapper<AppointmentEntity>().eq(AppointmentEntity::getWorkOrderId,id).orderByDesc(AppointmentEntity::getId).last(lock?"LIMIT 1 FOR UPDATE":"LIMIT 1"));}
    private void changed(int n){if(n!=1)throw BusinessException.versionConflict("上门安排已变化，请刷新后核对");}
}
