package com.fixmate.service.admin.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.WorkAssignmentService;
import com.fixmate.service.common.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkAssignmentServiceImpl implements WorkAssignmentService {
    private static final List<String> LIVE=List.of("PENDING","ACTIVE","RETURN_REQUESTED");
    private static final Set<String> REASSIGNABLE=Set.of("WAITING_MANUAL_REVIEW","WAITING_ASSIGNMENT","WAITING_APPOINTMENT","APPOINTED","IN_INSPECTION","WAITING_QUOTE","WAITING_QUOTE_CONFIRMATION","PAUSED_FOR_REQUOTE","IN_REPAIR");
    private final WorkOrderIdentityLocker locker;
    private final WorkOrderMapper orders;
    private final VisitSchedulingService visits;
    private final JobPoolEntryMapper poolEntries;
    private final WorkOrderAssignmentMapper assignments;
    private final StaffIdentityGuard guard;
    private final StaffAccessService access;
    private final StaffWechatBindingMapper bindings;
    private final EmployeeWechatAccessService employees;
    private final WorkOrderSafetyGate safety;
    private final AppointmentMapper appointments;
    private final QuoteMapper quotes;
    private final RepairRequestMapper repairs;
    private final WorkOrderStatusLogMapper statusLogs;
    private final HumanHandoffRecordMapper todos;
    private final AuditLogService audit;
    private final AuditLogMapper auditRows;
    private final DatabaseClockMapper clock;
    private final ObjectMapper json;
    private final ServiceContactProjection contacts;
    private final TaskSummaryProjection summaries;
    private final AcceptedQuoteScope acceptedScope;
    private final WorkOrderFollowUp followUp;
    private final ServiceRecordMapper serviceRecords;
    private final CustomerCancellationPolicy cancellations;
    public WorkAssignmentServiceImpl(WorkOrderMapper orders,WorkOrderAssignmentMapper assignments,StaffIdentityGuard guard,
            StaffAccessService access,StaffWechatBindingMapper bindings,EmployeeWechatAccessService employees,WorkOrderSafetyGate safety,
            AppointmentMapper appointments,QuoteMapper quotes,RepairRequestMapper repairs,WorkOrderStatusLogMapper statusLogs,
            HumanHandoffRecordMapper todos,AuditLogService audit,AuditLogMapper auditRows,DatabaseClockMapper clock,ObjectMapper json,ServiceContactProjection contacts,TaskSummaryProjection summaries,AcceptedQuoteScope acceptedScope,WorkOrderIdentityLocker locker,JobPoolEntryMapper poolEntries,VisitSchedulingService visits,WorkOrderFollowUp followUp,ServiceRecordMapper serviceRecords,CustomerCancellationPolicy cancellations) {
        this.visits=visits;this.locker=locker;this.poolEntries=poolEntries;
        this.orders=orders;this.assignments=assignments;this.guard=guard;this.access=access;this.bindings=bindings;this.employees=employees;this.safety=safety;
        this.appointments=appointments;this.quotes=quotes;this.repairs=repairs;this.statusLogs=statusLogs;this.todos=todos;this.audit=audit;
        this.auditRows=auditRows;this.clock=clock;this.json=json;this.contacts=contacts;this.summaries=summaries;
        this.acceptedScope=acceptedScope;
        this.followUp=followUp;
        this.serviceRecords=serviceRecords;
        this.cancellations=cancellations;
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public AssignmentVO assign(StaffPrincipal actor,Long orderId,AssignmentCreateDTO input) {
        var locked=locker.lock(actor,orderId,List.of(input.technicianId()),"ADMIN","DISPATCHER");var directory=locked.staff();var operator=locked.actor();
        target(directory,input.technicianId());var order=locked.order();
        StaffIdentityGuard.version(order.getVersion(),input.expectedWorkOrderVersion());
        String reason=input.reason()==null?"管理端确认分派":reason(order,input.reason());
        return view(assignLocked(actor.staffId(),operator.getRole(),order,input.technicianId(),reason));
    }
    private WorkOrderAssignmentEntity assignLocked(Long actor,String role,WorkOrderEntity order,Long technicianId,String reason) {
        if(poolEntries.selectCount(new LambdaQueryWrapper<JobPoolEntryEntity>().eq(JobPoolEntryEntity::getWorkOrderId,order.getId()).eq(JobPoolEntryEntity::getStatus,"PUBLISHED"))>0)throw BusinessException.stateConflict("请先撤回接单池发布，再定向派单");
        if(order.getTakeoverBy()!=null&&!"ADMIN".equals(role))throw BusinessException.forbidden("接管中的工单须管理员交接");
        if(!REASSIGNABLE.contains(order.getStatus())&&!"WAITING_MANUAL_REVIEW".equals(order.getStatus()))throw BusinessException.stateConflict("当前工单不可派单或改派");
        if("IN_REPAIR".equals(order.getStatus())&&!"ADMIN".equals(role))throw BusinessException.forbidden("维修中交接须管理员核实并记录");
        if("IN_REPAIR".equals(order.getStatus())&&!acceptedScope.valid(quotes.selectList(new LambdaQueryWrapper<QuoteEntity>()
                .eq(QuoteEntity::getWorkOrderId,order.getId()).orderByAsc(QuoteEntity::getVersionNo))))
            throw BusinessException.stateConflict("维修中交接必须保留有效已接受范围");
        boolean rework=followUp.isRework(order);
        boolean dispatchRework=rework&&"WAITING_MANUAL_REVIEW".equals(order.getStatus());
        if(rework&&!Objects.equals(technicianId,followUp.originalTechnician(order.getId())))
            throw BusinessException.stateConflict("返修须定向派给上一轮完工的维修人员");
        if("WAITING_MANUAL_REVIEW".equals(order.getStatus())){
            displayAssignment(order,true);
            if(!"ADMIN".equals(role)||!canAssignManual(order))throw BusinessException.stateConflict("异议返修须管理员核实已结束分派及已接受范围后重新派单");
        }
        safety.requireClear(order.getId());var previous=current(order,true);
        if(previous!=null&&previous.getTechnicianId().equals(technicianId))throw BusinessException.stateConflict("已分派同一维修人员，不能重复派单");
        if(previous!=null)end(previous,actor,"REASSIGNED",reason);
        String next=prepareReassignment(order,actor,reason);
        WorkOrderAssignmentEntity assignment=new WorkOrderAssignmentEntity();assignment.setWorkOrderId(order.getId());assignment.setTechnicianId(technicianId);
        assignment.setStatus("PENDING");assignment.setAllocationSource("DIRECT");assignment.setAssignedBy(actor);assignment.setAssignedAt(clock.now());assignment.setCreatedAt(assignment.getAssignedAt());
        assignment.setUpdatedAt(assignment.getAssignedAt());assignment.setVersion(0);assignments.insert(assignment);
        move(order,dispatchRework?"WAITING_APPOINTMENT":next,assignment.getId(),null,actor,reason);
        event(actor,assignment,"ASSIGNED",reason,null);return assignment;
    }
    /** Caller holds identity and order root locks; creates one already accepted assignment atomically. */
    @Override public AssignmentVO claimLocked(EmployeeWechatAccessService.EmployeeIdentity employee,WorkOrderEntity order) {
        if(!"WAITING_ASSIGNMENT".equals(order.getStatus())||order.getTakeoverBy()!=null||order.getCurrentAssignmentId()!=null||order.getTechnicianId()!=null||current(order,true)!=null)
            throw BusinessException.stateConflict("订单已被接走或不再可接");
        safety.requireClear(order.getId());var now=clock.now();
        var a=new WorkOrderAssignmentEntity();a.setWorkOrderId(order.getId());a.setTechnicianId(employee.staffId());a.setStatus("ACTIVE");a.setAllocationSource("SELF_CLAIM");
        a.setAssignedBy(employee.staffId());a.setAssignedAt(now);a.setAcceptedAt(now);a.setCreatedAt(now);a.setUpdatedAt(now);a.setVersion(0);
        if(assignments.insert(a)!=1)throw BusinessException.versionConflict("接单分派未保存");
        move(order,"WAITING_APPOINTMENT",a.getId(),employee.staffId(),employee.staffId(),"维修人员自主接单");
        visits.afterAcceptance(order,employee.staffId());
        event(employee.staffId(),a,"ACCEPTED","维修人员自主接单并确认用户时段",employee);return view(a);
    }
    private String prepareReassignment(WorkOrderEntity order,Long actor,String reason) {
        String next=order.getStatus();
        if("APPOINTED".equals(next))next="WAITING_APPOINTMENT";
        if(Set.of("WAITING_QUOTE_CONFIRMATION","PAUSED_FOR_REQUOTE").contains(next))next="WAITING_QUOTE";
        for(var ap:appointments.selectList(new LambdaQueryWrapper<AppointmentEntity>().eq(AppointmentEntity::getWorkOrderId,order.getId())
                .in(AppointmentEntity::getStatus,List.of("PENDING","CONFIRMED","RESCHEDULE_REQUESTED")).orderByAsc(AppointmentEntity::getId).last("FOR UPDATE"))) {
            int appointmentRows=appointments.update(null,new UpdateWrapper<AppointmentEntity>().eq("id",ap.getId()).eq("version",ap.getVersion())
                    .set("status","CANCELLED").set("version",ap.getVersion()+1).set("updated_at",clock.now()));
            if(appointmentRows!=1)throw BusinessException.versionConflict("预约已变化");
        }
        for(var quote:quotes.selectList(new LambdaQueryWrapper<QuoteEntity>().eq(QuoteEntity::getWorkOrderId,order.getId())
                .in(QuoteEntity::getStatus,List.of("DRAFT","SUBMITTED")).orderByAsc(QuoteEntity::getId).last("FOR UPDATE"))) {
            if(quotes.update(null,new UpdateWrapper<QuoteEntity>().eq("id",quote.getId()).eq("version",quote.getVersion())
                    .set("status","WITHDRAWN").set("version",quote.getVersion()+1).set("updated_at",clock.now()))!=1)throw BusinessException.versionConflict("报价已更新");
            audit.record("STAFF",actor,"QUOTE_WITHDRAWN_FOR_REASSIGNMENT","QUOTE",quote.getId(),null,Map.of("reason",reason));
        }
        return next;
    }
    private void target(List<StaffUserEntity> directory,Long id) {
        var employee=guard.target(directory,id);
        if(!"ENABLED".equals(employee.getStatus())||!"TECHNICIAN".equals(employee.getRole()))throw BusinessException.stateConflict("目标不是有效维修人员");
        var binding=bindings.selectOne(new LambdaQueryWrapper<StaffWechatBindingEntity>().eq(StaffWechatBindingEntity::getStaffId,id).eq(StaffWechatBindingEntity::getStatus,"ACTIVE"));
        if(binding==null||!id.equals(employees.requireLocked(binding.getCustomerId()).staffId()))throw BusinessException.stateConflict("维修人员尚无有效微信授权");
    }
    @Override public AssignmentVOPageVO history(StaffPrincipal actor,Long orderId,int page,int size) {
        access.require(actor,"ADMIN","DISPATCHER","AUDITOR");order(orderId,false);StaffDirectoryServiceImpl.page(page,size);
        var result=assignments.selectPage(new Page<>(page,size),new LambdaQueryWrapper<WorkOrderAssignmentEntity>()
                .eq(WorkOrderAssignmentEntity::getWorkOrderId,orderId).orderByDesc(WorkOrderAssignmentEntity::getId));
        return new AssignmentVOPageVO(result.getRecords().stream().map(this::view).toList(),page,size,result.getTotal());
    }
    @Override public AssignmentHistoryDetailVO historyDetail(StaffPrincipal actor,Long id) {
        access.require(actor,"ADMIN","DISPATCHER","AUDITOR");var assignment=assignment(id,false);order(assignment.getWorkOrderId(),false);
        var events=new ArrayList<AssignmentEventVO>();
        for(var row:auditRows.selectList(new LambdaQueryWrapper<AuditLogEntity>().eq(AuditLogEntity::getTargetType,"WORK_ASSIGNMENT")
                .eq(AuditLogEntity::getTargetId,id.toString()).orderByAsc(AuditLogEntity::getId))) {
            try {var data=json.readTree(row.getAfterState());String type=data.path("eventType").asText();
                if(!Set.of("ASSIGNED","ACCEPTED","DECLINED","RETURN_REQUESTED","RETURN_REJECTED","REASSIGNED","REVOKED","FINISHED").contains(type))continue;
                events.add(new AssignmentEventVO(row.getId(),id,type,access.actor(row.getActorId()),data.path("reason").asText(null),offset(row.getCreatedAt())));
            }catch(com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException("分派审计数据异常");}
        }
        return new AssignmentHistoryDetailVO(view(assignment),events);
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public AssignmentVO decideReturn(StaffPrincipal actor,Long id,AssignmentReturnDecisionDTO input) {
        var candidate=assignment(id,false);var extra=Boolean.TRUE.equals(input.approved())?List.of(parseId(input.replacementTechnicianId())):List.<Long>of();var locked=locker.lock(actor,candidate.getWorkOrderId(),extra,"ADMIN","DISPATCHER");var directory=locked.staff();var operator=locked.actor();
        if(Boolean.TRUE.equals(input.approved())) {
            Long replacement=parseId(input.replacementTechnicianId());target(directory,replacement);
        } else target(directory,candidate.getTechnicianId());
        var order=locked.order();var assignment=current(order,true);
        if(!REASSIGNABLE.contains(order.getStatus())&&!"WAITING_MANUAL_REVIEW".equals(order.getStatus()))throw BusinessException.stateConflict("当前工单不可处理退回");
        if(assignment==null||!id.equals(assignment.getId())||!"RETURN_REQUESTED".equals(assignment.getStatus()))throw BusinessException.stateConflict("当前没有待审核退回");
        StaffIdentityGuard.version(order.getVersion(),input.expectedWorkOrderVersion());StaffIdentityGuard.version(assignment.getVersion(),input.expectedAssignmentVersion());
        String reason=reason(order,input.reason());
        if(Boolean.TRUE.equals(input.approved()))return view(assignLocked(actor.staffId(),operator.getRole(),order,parseId(input.replacementTechnicianId()),reason));
        if(input.approved()==null)throw BusinessException.validation("须明确退回处理结论");
        updateAssignment(assignment,"ACTIVE");move(order,order.getStatus(),assignment.getId(),assignment.getTechnicianId(),actor.staffId(),reason);
        event(actor.staffId(),assignment,"RETURN_REJECTED",reason,null);return view(assignment);
    }
    @Override
    public boolean canReleaseReturnForPublication(WorkOrderEntity order) {
        return Set.of("APPOINTED","WAITING_APPOINTMENT").contains(order.getStatus())
                &&order.getTakeoverBy()==null&&!followUp.isRework(order)&&cancellations.canCancelNow(order);
    }
    @Override
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void releaseReturnForPublication(WorkOrderEntity order,Long actor,Integer expectedAssignmentVersion,String reason) {
        var assignment=current(order,true);
        if(assignment==null||!"RETURN_REQUESTED".equals(assignment.getStatus()))
            throw BusinessException.stateConflict("当前没有待审核的退回申请");
        StaffIdentityGuard.version(assignment.getVersion(),expectedAssignmentVersion);
        if(!canReleaseReturnForPublication(order))
            throw BusinessException.stateConflict("仅尚未开始上门服务的普通工单可在批准退回后发布到大厅");
        end(assignment,actor,"REVOKED",reason);
        prepareReassignment(order,actor,reason);
        move(order,"WAITING_ASSIGNMENT",null,null,actor,reason);
    }
    @Override public StaffTaskListItemVOPageVO tasks(Long customerId,int page,int size) {
        var employee=employees.require(customerId);StaffDirectoryServiceImpl.page(page,size);
        var result=assignments.selectPage(new Page<>(page,size),new LambdaQueryWrapper<WorkOrderAssignmentEntity>()
                .eq(WorkOrderAssignmentEntity::getTechnicianId,employee.staffId()).in(WorkOrderAssignmentEntity::getStatus,LIVE)
                .inSql(WorkOrderAssignmentEntity::getId,"SELECT current_assignment_id FROM work_order WHERE current_assignment_id IS NOT NULL AND status NOT IN ('COMPLETED','CANCELLED','CLOSED_UNRESOLVED')").orderByDesc(WorkOrderAssignmentEntity::getId));
        // 每条都核验权威指针及镜像；不从全客户工单过滤或泄漏被改派任务。
        return new StaffTaskListItemVOPageVO(result.getRecords().stream().map(a->taskView(order(a.getWorkOrderId(),false),a,false)).toList(),page,size,result.getTotal());
    }
    @Override public StaffTaskListItemVO task(Long customerId,Long orderId) {
        var employee=employees.require(customerId);var order=order(orderId,false);var assignment=current(order,false);
        if(terminal(order)||assignment==null||!employee.staffId().equals(assignment.getTechnicianId()))throw BusinessException.forbidden("任务不在当前分派范围");
        return taskView(order,assignment,true);
    }
    @Override public void requireOwn(Long customerId,Long id,String action) {
        var employee=employees.require(customerId);var assignment=assignment(id,false);var order=order(assignment.getWorkOrderId(),false);
        if(terminal(order)||order.getTakeoverBy()!=null||!employee.staffId().equals(assignment.getTechnicianId()))throw BusinessException.forbidden("无权处理该分派");
        boolean declinedRetry="DECLINE".equals(action)&&"DECLINED".equals(assignment.getStatus())&&order.getCurrentAssignmentId()==null;
        if(!declinedRetry&&(!id.equals(order.getCurrentAssignmentId())||!LIVE.contains(assignment.getStatus())))throw BusinessException.forbidden("分派已失效，请刷新任务");
        if(!declinedRetry)current(order,false);
    }
    @Override @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public AssignmentVO act(Long customerId,Long id,AssignmentActionDTO input,String action) {
        var employee=employees.requireLocked(customerId);var candidate=assignment(id,false);var order=order(candidate.getWorkOrderId(),true);var assignment=current(order,true);
        if(terminal(order)||order.getTakeoverBy()!=null||assignment==null||!id.equals(assignment.getId())||!employee.staffId().equals(assignment.getTechnicianId()))throw BusinessException.forbidden("任务已改派或不属于本人");
        StaffIdentityGuard.version(order.getVersion(),input.expectedWorkOrderVersion());StaffIdentityGuard.version(assignment.getVersion(),input.expectedAssignmentVersion());
        String actionName=switch(action){case "ACCEPT"->"ACCEPT_ASSIGNMENT";case "DECLINE"->"DECLINE_ASSIGNMENT";case "RETURN"->"REQUEST_RETURN";default->throw BusinessException.validation("分派动作不正确");};
        if(!allowedActions(order,assignment,true).contains(actionName))throw BusinessException.stateConflict("当前分派不允许该动作，请刷新任务");
        String reason=reason(order,input.reason());
        switch(action) {
            case "ACCEPT" -> {
                if(!"PENDING".equals(assignment.getStatus()))throw BusinessException.stateConflict("不能重复接单");safety.requireClear(order.getId());
                if(!REASSIGNABLE.contains(order.getStatus())&&!"WAITING_MANUAL_REVIEW".equals(order.getStatus()))throw BusinessException.stateConflict("当前状态不可接单");
                assignment.setAcceptedAt(clock.now());updateAssignment(assignment,"ACTIVE");
                boolean legacyRework="WAITING_MANUAL_REVIEW".equals(order.getStatus())&&followUp.isRework(order);
                move(order,legacyRework||"WAITING_ASSIGNMENT".equals(order.getStatus())?"WAITING_APPOINTMENT":order.getStatus(),id,employee.staffId(),employee.staffId(),reason);
                if("WAITING_APPOINTMENT".equals(order.getStatus()))visits.afterAcceptance(order,employee.staffId());
                event(employee.staffId(),assignment,"ACCEPTED",reason,employee);
            }
            case "DECLINE" -> {
                if(!"PENDING".equals(assignment.getStatus()))throw BusinessException.stateConflict("已接单不能按拒接处理");
                String next=order.getStatus();
                if("WAITING_APPOINTMENT".equals(next)){
                    if(followUp.isRework(order))next="WAITING_MANUAL_REVIEW";
                    else if(cancellations.canCancelNow(order))next="WAITING_ASSIGNMENT";
                }
                end(assignment,employee.staffId(),"DECLINED",reason);
                move(order,next,null,null,employee.staffId(),reason);
            }
            case "RETURN" -> {
                if(!"ACTIVE".equals(assignment.getStatus())||Set.of("WAITING_ACCEPTANCE","COMPLETED","CANCELLED","CLOSED_UNRESOLVED").contains(order.getStatus()))
                    throw BusinessException.stateConflict("当前不可申请退回");
                updateAssignment(assignment,"RETURN_REQUESTED");move(order,order.getStatus(),id,employee.staffId(),employee.staffId(),reason);
                event(employee.staffId(),assignment,"RETURN_REQUESTED",reason,employee);
            }
            default -> throw BusinessException.validation("分派动作不正确");
        }
        return view(assignment);
    }
    private StaffTaskListItemVO taskView(WorkOrderEntity order,WorkOrderAssignmentEntity assignment,boolean includeReworkSummary) {
        var actual=current(order,false);if(actual==null||!assignment.getId().equals(actual.getId()))throw BusinessException.forbidden("任务分派已变化");
        var repair=repairs.selectById(order.getRepairRequestId());
        String symptom=summaries.summary(order,repair);
        var issue=followUp.reworkIssue(order);
        var reworkSummary=includeReworkSummary&&issue!=null?reworkSummary(order,actual,repair,issue):null;
        var appointment=appointments.selectOne(new LambdaQueryWrapper<AppointmentEntity>().eq(AppointmentEntity::getWorkOrderId,order.getId())
                .in(AppointmentEntity::getStatus,List.of("PENDING","CONFIRMED","RESCHEDULE_REQUESTED")).orderByDesc(AppointmentEntity::getId).last("LIMIT 1"));
        return new StaffTaskListItemVO(VisitSchedulingService.preferred(order),VisitSchedulingService.policy(order),order.getId(),order.getWorkOrderNo(),order.getStatus(),order.getApplianceType(),symptom,area(order.getAddressSnapshot()),
                !safety.clear(order.getId()),appointment==null?null:offset("CUSTOMER_SLOT".equals(order.getVisitPolicy())&&"RESCHEDULE_REQUESTED".equals(appointment.getStatus())?appointment.getPreviousConfirmedStartAt():appointment.getScheduledStartAt()),view(actual),offset(order.getCreatedAt()),order.getVersion(),allowedActions(order,actual),
                appointment!=null&&"RESCHEDULE_REQUESTED".equals(appointment.getStatus())&&"ACTIVE".equals(actual.getStatus())
                        ?new PreferredVisitVO(offset(appointment.getScheduledStartAt()),offset(appointment.getScheduledEndAt())):null,
                followUp.requoteRequested(order),issue!=null,reworkSummary);
    }
    private ReworkSummaryVO reworkSummary(WorkOrderEntity order,WorkOrderAssignmentEntity current,
                                          RepairRequestEntity repair,WorkOrderStatusLogEntity issue) {
        var previous=followUp.lastCompletedAssignment(order.getId());
        if(previous==null||!current.getTechnicianId().equals(previous.getTechnicianId())
                ||previous.getEndedAt().isAfter(issue.getCreatedAt()))return null;

        var visit=appointments.selectOne(new LambdaQueryWrapper<AppointmentEntity>()
                .eq(AppointmentEntity::getWorkOrderId,order.getId())
                .eq(AppointmentEntity::getTechnicianId,previous.getTechnicianId())
                .eq(AppointmentEntity::getStatus,"COMPLETED")
                .between(AppointmentEntity::getUpdatedAt,previous.getAcceptedAt(),previous.getEndedAt())
                .orderByDesc(AppointmentEntity::getId).last("LIMIT 1"));
        var visitWindow=visit==null||visit.getScheduledStartAt()==null||visit.getScheduledEndAt()==null?null:
                new PreferredVisitVO(offset(visit.getScheduledStartAt()),offset(visit.getScheduledEndAt()));
        var repairRecord=serviceRecords.selectOne(new LambdaQueryWrapper<ServiceRecordEntity>()
                .eq(ServiceRecordEntity::getWorkOrderId,order.getId())
                .eq(ServiceRecordEntity::getTechnicianId,previous.getTechnicianId())
                .eq(ServiceRecordEntity::getRecordKind,"REPAIR")
                .between(ServiceRecordEntity::getSubmittedAt,previous.getAcceptedAt(),previous.getEndedAt())
                .orderByDesc(ServiceRecordEntity::getId).last("LIMIT 1"));
        return new ReworkSummaryVO(summaries.reworkText(issue.getReason(),order,repair),offset(issue.getCreatedAt()),
                summaries.reworkText(order.getSymptomDescription(),order,repair),visitWindow,offset(previous.getEndedAt()),
                repairRecord==null?null:summaries.reworkText(repairRecord.getSummary(),order,repair));
    }
    private List<String> allowedActions(WorkOrderEntity order,WorkOrderAssignmentEntity assignment) {
        return allowedActions(order,assignment,false);
    }
    private List<String> allowedActions(WorkOrderEntity order,WorkOrderAssignmentEntity assignment,boolean locked) {
        if(terminal(order)||order.getTakeoverBy()!=null)return List.of();
        if("PENDING".equals(assignment.getStatus())) {
            var actions=new ArrayList<String>();
            boolean legacyRework="WAITING_MANUAL_REVIEW".equals(order.getStatus())&&followUp.isRework(order);
            boolean needsPreference=legacyRework||"CUSTOMER_SLOT".equals(order.getVisitPolicy())&&Set.of("WAITING_ASSIGNMENT","WAITING_APPOINTMENT").contains(order.getStatus());
            boolean validPreference=legacyRework?visits.hasReworkPreference(order):!needsPreference||visits.hasValidPreference(order);
            if(validPreference&&REASSIGNABLE.contains(order.getStatus())&&(locked?safety.clearLocked(order.getId()):safety.clear(order.getId())))actions.add("ACCEPT_ASSIGNMENT");
            actions.add("DECLINE_ASSIGNMENT");return actions;
        }
        if("ACTIVE".equals(assignment.getStatus())&&!"WAITING_ACCEPTANCE".equals(order.getStatus()))return List.of("REQUEST_RETURN");
        return List.of();
    }
    private String area(String snapshot){return CoarseServiceArea.fromSnapshot(json,snapshot);}
    private boolean terminal(WorkOrderEntity order){return Set.of("COMPLETED","CANCELLED","CLOSED_UNRESOLVED").contains(order.getStatus());}
    private WorkOrderEntity order(Long id,boolean lock) {
        var q=new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,id);if(lock)q.last("FOR UPDATE");var w=orders.selectOne(q);
        if(w==null)throw BusinessException.notFound("工单不存在");return w;
    }
    private WorkOrderAssignmentEntity assignment(Long id,boolean lock) {
        var q=new LambdaQueryWrapper<WorkOrderAssignmentEntity>().eq(WorkOrderAssignmentEntity::getId,id);if(lock)q.last("FOR UPDATE");var a=assignments.selectOne(q);
        if(a==null)throw BusinessException.notFound("分派不存在");return a;
    }
    private WorkOrderAssignmentEntity current(WorkOrderEntity order,boolean lock) {
        if(order.getCurrentAssignmentId()==null) {
            if(assignments.selectCount(new LambdaQueryWrapper<WorkOrderAssignmentEntity>().eq(WorkOrderAssignmentEntity::getWorkOrderId,order.getId())
                    .in(WorkOrderAssignmentEntity::getStatus,LIVE))>0)throw BusinessException.stateConflict("分派指针异常，请后台协调");
            return null;
        }
        var a=assignment(order.getCurrentAssignmentId(),lock);
        if("FINISHED".equals(a.getStatus())){retained(order,a);return null;}
        Long mirror="PENDING".equals(a.getStatus())?null:a.getTechnicianId();
        boolean acceptedAtValid="PENDING".equals(a.getStatus())?a.getAcceptedAt()==null:a.getAcceptedAt()!=null;
        if(!order.getId().equals(a.getWorkOrderId())||!LIVE.contains(a.getStatus())||!Objects.equals(mirror,order.getTechnicianId())||!acceptedAtValid)
            throw BusinessException.stateConflict("分派指针或人员镜像异常，请后台协调");return a;
    }
    private void updateAssignment(WorkOrderAssignmentEntity a,String status) {
        int n=assignments.update(null,new UpdateWrapper<WorkOrderAssignmentEntity>().eq("id",a.getId()).eq("version",a.getVersion()).eq("status",a.getStatus())
                .set("status",status).set("accepted_at",a.getAcceptedAt()).set("version",a.getVersion()+1).set("updated_at",clock.now()));
        if(n!=1)throw BusinessException.versionConflict("分派已变化");a.setStatus(status);a.setVersion(a.getVersion()+1);
    }
    private void end(WorkOrderAssignmentEntity a,Long actor,String state,String reason) {
        markEnded(a,actor,state,reason);event(actor,a,state,reason,null);
    }
    private void markEnded(WorkOrderAssignmentEntity a,Long actor,String state,String reason) {
        a.setEndedBy(actor);a.setEndedAt(clock.now());a.setEndReason(reason);
        int n=assignments.update(null,new UpdateWrapper<WorkOrderAssignmentEntity>().eq("id",a.getId()).eq("version",a.getVersion())
                .set("status",state).set("ended_by",actor).set("ended_at",a.getEndedAt()).set("end_reason",reason).set("version",a.getVersion()+1).set("updated_at",clock.now()));
        if(n!=1)throw BusinessException.versionConflict("分派已变化");a.setStatus(state);a.setVersion(a.getVersion()+1);
    }
    private void move(WorkOrderEntity order,String next,Long assignmentId,Long technicianId,Long actor,String reason) {
        String before=order.getStatus();
        if(order.getTakeoverBy()!=null)audit.record("STAFF",actor,"WORK_ORDER_TAKEOVER_ENDED","WORK_ORDER",order.getId(),Map.of("takeoverBy",order.getTakeoverBy()),Map.of("reason",reason));
        LocalDateTime acceptedAt=null;
        if(technicianId!=null) {
            var accepted=assignmentId==null?null:assignments.selectById(assignmentId);
            if(accepted==null||!technicianId.equals(accepted.getTechnicianId())||accepted.getAcceptedAt()==null)
                throw BusinessException.stateConflict("接单时间与分派不一致，请后台协调");
            acceptedAt=accepted.getAcceptedAt();
        }
        int n=orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",order.getId()).eq("version",order.getVersion())
                .set("status",next).set("current_assignment_id",assignmentId).set("technician_id",technicianId)
                .set("assigned_at",acceptedAt).set("takeover_by",null).set("takeover_at",null).set("takeover_reason",null)
                .set("version",order.getVersion()+1).set("updated_at",clock.now()));
        if(n!=1)throw BusinessException.versionConflict("工单已变化");
        if(!before.equals(next)) {
            var log=new WorkOrderStatusLogEntity();log.setWorkOrderId(order.getId());log.setFromStatus(before);log.setToStatus(next);
            log.setOperatorType("STAFF");log.setOperatorId(actor);log.setReason(reason);log.setCreatedAt(clock.now());statusLogs.insert(log);
        }
        order.setStatus(next);order.setCurrentAssignmentId(assignmentId);order.setTechnicianId(technicianId);order.setVersion(order.getVersion()+1);
        order.setTakeoverBy(null);order.setTakeoverAt(null);order.setTakeoverReason(null);
    }
    private void event(Long actor,WorkOrderAssignmentEntity a,String type,String reason,EmployeeWechatAccessService.EmployeeIdentity employee) {
        var data=new LinkedHashMap<String,Object>();data.put("allocationSource",a.getAllocationSource()==null?"DIRECT":a.getAllocationSource());data.put("eventType",type);data.put("reason",reason);data.put("version",a.getVersion());data.put("workOrderId",a.getWorkOrderId());
        if(employee!=null){data.put("customerId",employee.customerId());data.put("bindingId",employee.bindingId());data.put("bindingVersion",employee.bindingVersion());}
        audit.record("STAFF",actor,"ASSIGNMENT_"+type,"WORK_ASSIGNMENT",a.getId(),null,data);
    }
    private String reason(WorkOrderEntity order,String input) {
        String text=StaffIdentityGuard.text(input,2,1000,"分派处理依据");var repair=repairs.selectById(order.getRepairRequestId());
        return repair==null?text.replaceAll("(?<![0-9])([0-9]{3})[0-9]{4}([0-9]{4})(?![0-9])","$1****$2"):contacts.redactNote(text,repair);
    }
    private Long parseId(String value){try{long id=Long.parseLong(value);if(id<=0)throw new NumberFormatException();return id;}catch(RuntimeException ex){throw BusinessException.validation("须指定替换维修人员");}}
    private AssignmentVO view(WorkOrderAssignmentEntity a){return new AssignmentVO(a.getAllocationSource()==null?"DIRECT":a.getAllocationSource(),a.getId(),a.getWorkOrderId(),a.getTechnicianId(),a.getStatus(),offset(a.getAssignedAt()),offset(a.getAcceptedAt()),offset(a.getEndedAt()),a.getVersion(),access.actor(a.getAssignedBy()),access.actor(a.getEndedBy()),a.getEndReason());}
    @Override public WorkOrderAssignmentEntity currentAssignment(WorkOrderEntity order,boolean lock){return current(order,lock);}
    private void retained(WorkOrderEntity order,WorkOrderAssignmentEntity a){
        if(!order.getId().equals(a.getWorkOrderId())||!Objects.equals(order.getTechnicianId(),a.getTechnicianId())||a.getAcceptedAt()==null||a.getEndedAt()==null||a.getEndedBy()==null||!Set.of("WAITING_ACCEPTANCE","WAITING_MANUAL_REVIEW","COMPLETED","CANCELLED","CLOSED_UNRESOLVED").contains(order.getStatus())||assignments.selectCount(new LambdaQueryWrapper<WorkOrderAssignmentEntity>().eq(WorkOrderAssignmentEntity::getWorkOrderId,order.getId()).in(WorkOrderAssignmentEntity::getStatus,LIVE))>0)throw BusinessException.stateConflict("结束分派指针或人员镜像异常");
    }
    @Override public WorkOrderAssignmentEntity displayAssignment(WorkOrderEntity order,boolean lock){
        if(order.getCurrentAssignmentId()!=null){var a=assignment(order.getCurrentAssignmentId(),lock);if("FINISHED".equals(a.getStatus())){retained(order,a);return a;}}
        return current(order,lock);
    }
    @Override public boolean canAssignManual(WorkOrderEntity order){
        var cause=followUp.manualCause(order);
        return cause!=null&&WorkOrderFollowUp.ACCEPTANCE_ISSUE.equals(cause.type())
                &&current(order,false)==null&&visits.hasReworkPreference(order)
                &&followUp.originalTechnician(order.getId())!=null
                &&acceptedScope.valid(quotes.selectList(new LambdaQueryWrapper<QuoteEntity>().eq(QuoteEntity::getWorkOrderId,order.getId())));
    }
    @Override public void finishForClosure(WorkOrderEntity order,Long actor,String reason){
        var a=displayAssignment(order,true);if(a==null||"FINISHED".equals(a.getStatus()))return;
        if(a.getAcceptedAt()!=null){end(a,actor,"FINISHED",reason);}
        else{end(a,actor,"REVOKED",reason);orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",order.getId()).eq("version",order.getVersion()).set("current_assignment_id",null).set("technician_id",null).set("assigned_at",null));order.setCurrentAssignmentId(null);order.setTechnicianId(null);order.setAssignedAt(null);}
    }
    @Override public void releaseForCustomerCancellation(WorkOrderEntity order,Long customerId,String reason){
        var a=current(order,true);
        if(a!=null){
            markEnded(a,null,"REVOKED",reason);
            audit.recordCustomer(customerId,"ASSIGNMENT_REVOKED","WORK_ASSIGNMENT",a.getId(),null,
                    Map.of("workOrderId",order.getId(),"eventType","REVOKED","reason",reason,"version",a.getVersion()));
        }
        if(order.getCurrentAssignmentId()==null&&order.getTechnicianId()==null&&order.getAssignedAt()==null
                &&order.getTakeoverBy()==null&&order.getTakeoverAt()==null&&order.getTakeoverReason()==null)return;
        int n=orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",order.getId()).eq("version",order.getVersion())
                .set("current_assignment_id",null).set("technician_id",null).set("assigned_at",null)
                .set("takeover_by",null).set("takeover_at",null).set("takeover_reason",null));
        if(n!=1)throw BusinessException.versionConflict("工单已变化");
    }
    @Override public AssignmentVO assignmentView(WorkOrderAssignmentEntity a){return a==null?null:view(a);}
    @Override public void endForCoordination(WorkOrderEntity order,Long actor,String reason){
        var a=current(order,true);if(a!=null)end(a,actor,"REVOKED",reason);
        prepareReassignment(order,actor,reason);
        order.setCurrentAssignmentId(null);order.setTechnicianId(null);order.setAssignedAt(null);
    }
    private OffsetDateTime offset(LocalDateTime value){return value==null?null:value.atOffset(ZoneOffset.UTC);}
    @Override @Transactional public void revokeDisabledStaff(StaffPrincipal actor,Long staffId,String input) {
        // 由持有整个员工目录锁的停用事务调用；订单按ID锁，然后当前分派，含PENDING无technician_id镜像的任务。
        var ids=new TreeSet<Long>();
        for(var a:assignments.selectList(new LambdaQueryWrapper<WorkOrderAssignmentEntity>().eq(WorkOrderAssignmentEntity::getTechnicianId,staffId).in(WorkOrderAssignmentEntity::getStatus,LIVE)))ids.add(a.getWorkOrderId());
        for(var w:orders.selectList(new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getTechnicianId,staffId)
                .notIn(WorkOrderEntity::getStatus,List.of("COMPLETED","CANCELLED","CLOSED_UNRESOLVED"))))ids.add(w.getId());
        for(Long id:ids) {
            var w=order(id,true);if(terminal(w)||"WAITING_ACCEPTANCE".equals(w.getStatus()))continue;var retained=displayAssignment(w,true);if(retained!=null&&"FINISHED".equals(retained.getStatus()))continue;
            var a=current(w,true);if(a!=null&&!staffId.equals(a.getTechnicianId()))throw BusinessException.stateConflict("人员与分派异常，请先协调");
            String reason=reason(w,input);if(a!=null)end(a,actor.staffId(),"REVOKED",reason);String next=prepareReassignment(w,actor.staffId(),reason);move(w,next,null,null,actor.staffId(),reason);
            var todo=new HumanHandoffRecordEntity();todo.setWorkOrderId(id);todo.setUserId(w.getCustomerId());todo.setReason("员工停用，需调度改派");todo.setStatus("PENDING");todo.setCreatedAt(clock.now());todo.setUpdatedAt(clock.now());todos.insert(todo);
            audit.record("STAFF",actor.staffId(),"STAFF_DISABLED_ORDER_COORDINATION","WORK_ORDER",id,null,Map.of("staffId",staffId,"handoffId",todo.getId()));
        }
    }
}
