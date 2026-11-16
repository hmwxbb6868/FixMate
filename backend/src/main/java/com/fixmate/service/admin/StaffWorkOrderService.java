package com.fixmate.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.*;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.dto.user.AppointmentVO;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.common.*;
import com.fixmate.service.user.serviceImpl.UserWorkOrderServiceImpl;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** B2-C：所有聚合子资源写入先锁身份、工单；读投影在根锁内组装。 */
@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class StaffWorkOrderService {
    private static final List<String> CURRENT_APPOINTMENTS=List.of("PENDING","CONFIRMED","RESCHEDULE_REQUESTED");
    private static final String LATEST_APPOINTMENT="ap.id=(SELECT MAX(ap2.id) FROM appointment ap2 WHERE ap2.work_order_id=work_order.id)";
    private static final String LATEST_PUBLICATION="p.id=(SELECT MAX(p2.id) FROM job_pool_entry p2 WHERE p2.work_order_id=work_order.id)";
    private static final String EFFECTIVE_ASSIGNMENT="COALESCE(work_order.current_assignment_id,(SELECT MAX(a2.id) FROM work_order_assignment a2 WHERE a2.work_order_id=work_order.id))";
    private static final String POOL_ELIGIBLE="EXISTS (SELECT 1 FROM work_order w JOIN repair_request r ON r.id=w.repair_request_id WHERE w.id=work_order.id AND "
            + JobPoolQueryMapper.ORDER_ELIGIBLE + ")";
    private final WorkOrderIdentityLocker locker;
    private final VisitSchedulingService visits;
    private final UrgeRecordMapper urgeTodos;
    private final StaffQuoteService quoteWork;
    private final WorkOrderMapper orders;
    private final RepairRequestMapper repairs;
    private final AppointmentMapper appointments;
    private final ServiceRecordMapper records;
    private final QuoteMapper quotes;
    private final StaffWechatBindingMapper bindings;
    private final StaffUserMapper staff;
    private final WorkOrderAssignmentMapper assignmentRows;
    private final JobPoolEntryMapper poolEntries;
    private final HumanHandoffRecordMapper todos;
    private final StaffAccessService access;
    private final StaffIdentityGuard identities;
    private final EmployeeWechatAccessService employees;
    private final WorkAssignmentService assignments;
    private final WorkOrderSafetyGate safety;
    private final ServiceContactProjection contacts;
    private final TaskSummaryProjection summaries;
    private final AcceptedQuoteScope acceptedScope;
    private final WorkOrderStateMachine machine;
    private final UserWorkOrderServiceImpl userProjection;
    private final DatabaseClockMapper clock;
    private final AuditLogService audit;
    private final ObjectMapper json;
    private final CustomerCancellationPolicy cancellations;
    private final WorkOrderFollowUp followUp;
    public StaffWorkOrderService(WorkOrderMapper orders,RepairRequestMapper repairs,AppointmentMapper appointments,
            ServiceRecordMapper records,QuoteMapper quotes,StaffWechatBindingMapper bindings,StaffUserMapper staff,
            WorkOrderAssignmentMapper assignmentRows,HumanHandoffRecordMapper todos,StaffAccessService access,StaffIdentityGuard identities,
            EmployeeWechatAccessService employees,WorkAssignmentService assignments,WorkOrderSafetyGate safety,
            ServiceContactProjection contacts,TaskSummaryProjection summaries,AcceptedQuoteScope acceptedScope,WorkOrderStateMachine machine,
            UserWorkOrderServiceImpl userProjection,DatabaseClockMapper clock,AuditLogService audit,ObjectMapper json,StaffQuoteService quoteWork,UrgeRecordMapper urgeTodos,WorkOrderIdentityLocker locker,VisitSchedulingService visits,JobPoolEntryMapper poolEntries,CustomerCancellationPolicy cancellations,WorkOrderFollowUp followUp) {
        this.visits=visits;this.locker=locker;
        this.poolEntries=poolEntries;
        this.cancellations=cancellations;
        this.followUp=followUp;
        this.urgeTodos=urgeTodos;
        this.quoteWork=quoteWork;
        this.orders=orders;this.repairs=repairs;this.appointments=appointments;this.records=records;this.quotes=quotes;this.bindings=bindings;
        this.staff=staff;this.assignmentRows=assignmentRows;this.todos=todos;this.access=access;this.identities=identities;this.employees=employees;
        this.assignments=assignments;this.safety=safety;this.contacts=contacts;this.summaries=summaries;this.acceptedScope=acceptedScope;
        this.machine=machine;this.userProjection=userProjection;this.clock=clock;this.audit=audit;this.json=json;
    }

    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public AdminWorkOrderListItemVOPageVO list(StaffPrincipal actor,int page,int size,String status,String keyword,Long technicianId,
            String applianceType,String brandCode,Boolean riskFlag,LocalDate appointmentDate,String coverageStatus,String queue) {
        access.require(actor,"ADMIN","DISPATCHER","AUDITOR");paging(page,size);
        var q=new QueryWrapper<WorkOrderEntity>();
        if(status!=null){parseStatus(status);q.eq("status",status);}
        if(technicianId!=null)q.and(t->t.apply("EXISTS (SELECT 1 FROM work_order_assignment ca WHERE ca.id=work_order.current_assignment_id AND ca.work_order_id=work_order.id AND ca.technician_id={0})",technicianId).or(n->n.isNull("current_assignment_id").eq("technician_id",technicianId)));
        if(applianceType!=null){if(!Set.of("AIR_CONDITIONER","REFRIGERATOR","WASHING_MACHINE","AIR_SOURCE_HEAT_PUMP").contains(applianceType))throw BusinessException.validation("设备类型不正确");q.eq("appliance_type",applianceType);}
        if(brandCode!=null)q.eq("brand_code",StaffIdentityGuard.text(brandCode,1,32,"品牌"));
        if(keyword!=null&&!keyword.isBlank()){
            String word=StaffIdentityGuard.text(keyword,1,100,"关键词");
            if(word.matches("(?i)RR[0-9a-z]{1,30}")){
                q.apply("""
                    EXISTS (SELECT 1 FROM repair_request r WHERE r.id=work_order.repair_request_id
                    AND r.work_order_id=work_order.id AND r.user_id=work_order.customer_id AND r.repair_no={0})
                    """,word);
            }else q.and(t->t.like("work_order_no",word).or().like("symptom_description",word));
        }
        if(riskFlag!=null)q.apply((riskFlag?"EXISTS":"NOT EXISTS")+" (SELECT 1 FROM safety_event se WHERE se.work_order_id=work_order.id AND se.status='OPEN')");
        if(coverageStatus!=null){if(!Set.of("REGISTERED","PENDING_SOURCE","COVERED","CONFLICTED","DISABLED").contains(coverageStatus))throw BusinessException.validation("覆盖状态不正确");
            q.apply("EXISTS (SELECT 1 FROM repair_request r WHERE r.id=work_order.repair_request_id AND r.coverage_status={0})",coverageStatus);}
        if(appointmentDate!=null){
            var begin=appointmentDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            q.apply("EXISTS (SELECT 1 FROM appointment ap WHERE "+LATEST_APPOINTMENT+" AND ap.scheduled_start_at>={0} AND ap.scheduled_start_at<{1})",begin,begin.plusDays(1));
        }
        if(queue!=null)filterQueue(q,queue);
        boolean directProgress="DIRECT_PROGRESS".equals(queue);
        boolean progress=directProgress||"SELF_CLAIM_PROGRESS".equals(queue);
        if(progress)q.orderByDesc(EFFECTIVE_ASSIGNMENT);
        q.orderByDesc("id");var result=orders.selectPage(new Page<>(page,size),q);
        var publishedOrders=directProgress&&!result.getRecords().isEmpty()?poolEntries.selectList(new LambdaQueryWrapper<JobPoolEntryEntity>()
                .select(JobPoolEntryEntity::getWorkOrderId).eq(JobPoolEntryEntity::getStatus,"PUBLISHED")
                .in(JobPoolEntryEntity::getWorkOrderId,result.getRecords().stream().map(WorkOrderEntity::getId).toList()))
                .stream().map(JobPoolEntryEntity::getWorkOrderId).collect(java.util.stream.Collectors.toSet()):Set.<Long>of();
        var items=result.getRecords().stream().map(w->{var ap=latestAppointment(w.getId(),false);var assigned=assignments.displayAssignment(w,false);var assignee=assigned==null?null:staff.selectById(assigned.getTechnicianId());
            var linkedRepair=repairIfLinked(w);
            String summary=linkedRepair==null?"原报修工单关联异常，请核对":summaries.summary(w,linkedRepair);
            var latest=progress&&assigned==null?assignmentRows.selectOne(new LambdaQueryWrapper<WorkOrderAssignmentEntity>()
                    .eq(WorkOrderAssignmentEntity::getWorkOrderId,w.getId()).orderByDesc(WorkOrderAssignmentEntity::getId).last("LIMIT 1")):assigned;
            var latestAssignee=latest==assigned?assignee:staff.selectById(latest.getTechnicianId());
            var cause=followUp.coordinationCause(w);
            return new AdminWorkOrderListItemVO(w.getId(),w.getWorkOrderNo(),w.getStatus(),w.getApplianceType(),summary,
                    !safety.clear(w.getId()),w.getTechnicianId()==null?null:w.getTechnicianId().toString(),ap==null?null:offset(ap.getScheduledStartAt()),offset(w.getCreatedAt()),assigned==null?null:assigned.getTechnicianId().toString(),assignee==null?null:assignee.getName(),assigned==null?null:assigned.getStatus(),
                    VisitSchedulingService.preferred(w),ap==null?null:ap.getStatus(),
                    progress&&latestAssignee!=null?latestAssignee.getName():null,progress&&latest!=null?latest.getStatus():null,
                    directProgress?publishedOrders.contains(w.getId()):null,linkedRepair==null?null:linkedRepair.getRepairNo(),
                    cause==null?null:cause.type(),cause==null||linkedRepair==null?null:contacts.redactPersonalText(cause.reason(),linkedRepair));}).toList();
        return new AdminWorkOrderListItemVOPageVO(items,page,size,result.getTotal());
    }

    private void filterQueue(QueryWrapper<WorkOrderEntity> q,String queue) {
        if(queue.startsWith("POOL_")||Set.of("DIRECT_PROGRESS","SELF_CLAIM_PROGRESS").contains(queue))q.ne("status","CANCELLED");
        String latestPublication="EXISTS (SELECT 1 FROM job_pool_entry p WHERE "+LATEST_PUBLICATION+" AND p.status={0})";
        String clear="NOT EXISTS (SELECT 1 FROM safety_event se WHERE se.work_order_id=work_order.id AND se.status<>'RESOLVED')";
        switch(queue){
            case "DISPATCH" -> q.eq("status","WAITING_ASSIGNMENT").isNull("current_assignment_id").isNull("technician_id")
                    .isNull("takeover_by").apply(clear).apply("NOT "+latestPublication,"PUBLISHED");
            case "APPOINTMENT" -> q.apply(clear)
                    .apply("EXISTS (SELECT 1 FROM work_order_assignment a WHERE a.id=work_order.current_assignment_id AND a.work_order_id=work_order.id AND a.status='ACTIVE')")
                    .and(t->t.eq("status","WAITING_APPOINTMENT").or(n->n.eq("status","APPOINTED")
                            .apply("EXISTS (SELECT 1 FROM appointment ap WHERE "+LATEST_APPOINTMENT+" AND ap.status='RESCHEDULE_REQUESTED')")));
            case "COORDINATION" -> q.eq("status","WAITING_MANUAL_REVIEW").apply(clear).apply("NOT "+WorkOrderFollowUp.QUOTE_REJECTED_QUEUE);
            case "QUOTE_REJECTED" -> q.in("status",List.of("WAITING_MANUAL_REVIEW","WAITING_QUOTE")).apply(clear).apply(WorkOrderFollowUp.QUOTE_REJECTED_QUEUE);
            case "CUSTOMER_CANCELLED" -> q.eq("status","CANCELLED").apply("""
                    EXISTS (SELECT 1 FROM work_order_status_log s WHERE s.work_order_id=work_order.id
                        AND s.to_status='CANCELLED' AND s.operator_type='CUSTOMER')
                    """);
            case "RETURN" -> q.notIn("status",List.of("COMPLETED","CANCELLED","CLOSED_UNRESOLVED"))
                    .apply("EXISTS (SELECT 1 FROM work_order_assignment a WHERE a.id=work_order.current_assignment_id AND a.work_order_id=work_order.id AND a.status='RETURN_REQUESTED')");
            case "POOL_READY" -> q.apply(POOL_ELIGIBLE).apply("NOT "+latestPublication,"PUBLISHED");
            case "POOL_LIVE" -> q.apply(latestPublication,"PUBLISHED").apply(POOL_ELIGIBLE);
            case "POOL_BLOCKED" -> q.apply(latestPublication,"PUBLISHED").apply("NOT "+POOL_ELIGIBLE);
            case "POOL_CLAIMED" -> q.apply(latestPublication,"CLAIMED");
            case "POOL_WITHDRAWN" -> q.apply(latestPublication,"WITHDRAWN");
            case "DIRECT_PROGRESS", "SELF_CLAIM_PROGRESS" -> q.apply("EXISTS (SELECT 1 FROM work_order_assignment a WHERE a.id="
                    +EFFECTIVE_ASSIGNMENT+" AND a.work_order_id=work_order.id AND COALESCE(a.allocation_source,'DIRECT')={0})",
                    "DIRECT_PROGRESS".equals(queue)?"DIRECT":"SELF_CLAIM");
            default -> throw BusinessException.validation("工单队列不正确");
        }
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public StaffWorkOrderDetailVO adminDetail(StaffPrincipal actor,Long id){
        var operator=access.require(actor,"ADMIN","DISPATCHER","AUDITOR");return view(order(id,false),operator.getId(),operator.getRole(),false);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public StaffWorkOrderDetailVO technicianDetail(Long customerId,Long id){
        var employee=employees.require(customerId);var w=order(id,false);requireTechnician(w,employee.staffId(),false);
        return view(w,employee.staffId(),"TECHNICIAN",true);
    }
    /** 缓存重放也校验实时资源权限，但不以旧版本/已完成动作状态拒绝正常幂等重试。 */
    public void guardAdmin(StaffPrincipal actor,Long id,String operation,String resolution){
        var s=access.require(actor,"ADMIN","DISPATCHER");
        if((Set.of("adminTakeover","adminStartInspection","adminCreateServiceRecord","adminCompletion","adminCloseUnresolved").contains(operation)
                ||("adminCoordination".equals(operation)&&Set.of("ESCALATE_TO_MANUAL","RESUME_REPAIR").contains(resolution==null?"":resolution)))
                &&!"ADMIN".equals(s.getRole()))throw BusinessException.forbidden("仅管理员可操作");
        var w=order(id,false);
        if("adminCompletion".equals(operation)&&terminal(w))throw BusinessException.forbidden("已结束工单请读取历史详情");
        requireCoordinator(w,s);
        if(Set.of("adminStartInspection","adminCreateServiceRecord","adminCompletion").contains(operation)&&!Objects.equals(w.getTakeoverBy(),actor.staffId()))throw BusinessException.forbidden("须由当前接管管理员操作");
    }
    public void guardTechnician(Long customerId,Long id){var e=employees.require(customerId);requireTechnician(order(id,false),e.staffId(),true);}

    public StaffWorkOrderDetailVO takeover(StaffPrincipal actor,Long id,WorkOrderCommandDTO input){
        var w=locker.lock(actor,id,List.of(),"ADMIN").order();
        StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());nonterminal(w);activeAssignment(w);
        if(Objects.equals(w.getTakeoverBy(),actor.staffId()))throw BusinessException.stateConflict("已由本人接管");
        String reason=reason(w,input.reason());var before=new LinkedHashMap<String,Object>();before.put("takeoverBy",w.getTakeoverBy());
        orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",id).eq("version",w.getVersion())
                .set("takeover_by",actor.staffId()).set("takeover_at",clock.now()).set("takeover_reason",reason));
        w.setTakeoverBy(actor.staffId());bump(w);
        var todo=new HumanHandoffRecordEntity();todo.setWorkOrderId(id);todo.setUserId(w.getCustomerId());todo.setStatus("PENDING");
        todo.setReason("管理员已接管，需协调履约安排");todo.setCreatedAt(clock.now());todo.setUpdatedAt(clock.now());todos.insert(todo);
        audit.record("STAFF",actor.staffId(),"WORK_ORDER_TAKEOVER","WORK_ORDER",id,before,Map.of("takeoverBy",actor.staffId(),"reason",reason,"technicianId",w.getTechnicianId(),"handoffId",todo.getId()));
        return view(w,actor.staffId(),"ADMIN",false);
    }
    public StaffAppointmentVO createAppointment(StaffPrincipal actor,Long id,AppointmentCreateDTO input){
        var locked=locker.lock(actor,id,List.of(),"ADMIN","DISPATCHER");var operator=locked.actor();var w=locked.order();
        requireCoordinator(w,operator);StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());
        if(!Set.of("WAITING_APPOINTMENT","APPOINTED","IN_INSPECTION").contains(w.getStatus()))throw BusinessException.stateConflict("当前状态不能预约");
        var assignment=activeAssignment(w);safety.requireClear(id);
        var start=utc(input.startAt());var end=utc(input.endAt());validTime(start,end);if("CUSTOMER_SLOT".equals(w.getVisitPolicy())&&(!Objects.equals(start,w.getPreferredVisitStartAt())||!Objects.equals(end,w.getPreferredVisitEndAt())))throw BusinessException.stateConflict("须沿用户选择的时段确认，调整时间请由用户申请改约");checkOverlap(w,assignment.getTechnicianId(),start,end);
        var before=appointmentAudit(latestAppointment(id,true));
        for(var ap:appointments.selectList(new LambdaQueryWrapper<AppointmentEntity>().eq(AppointmentEntity::getWorkOrderId,id)
                .in(AppointmentEntity::getStatus,CURRENT_APPOINTMENTS).orderByAsc(AppointmentEntity::getId).last("FOR UPDATE")))setAppointment(ap,"CANCELLED");
        String note=input.note()==null?null:reasonOptional(w,input.note());
        var ap=new AppointmentEntity();ap.setWorkOrderId(id);ap.setTechnicianId(assignment.getTechnicianId());ap.setStatus(Boolean.TRUE.equals(input.confirmed())?"CONFIRMED":"PENDING");
        ap.setScheduledStartAt(start);ap.setScheduledEndAt(end);ap.setNote(note);ap.setVersion(0);ap.setCreatedAt(clock.now());ap.setUpdatedAt(clock.now());appointments.insert(ap);
        String r="后台核实并安排预约";
        if(Set.of("APPOINTED","IN_INSPECTION").contains(w.getStatus()))w=move(w,"WAITING_APPOINTMENT",actor.staffId(),r);
        if(Boolean.TRUE.equals(input.confirmed()))w=move(w,"APPOINTED",actor.staffId(),r);else bump(w);
        audit.record("STAFF",actor.staffId(),"APPOINTMENT_CREATED","WORK_ORDER",id,before,appointmentAudit(ap));return appointmentView(ap);
    }
    public StaffAppointmentVO confirmAppointment(StaffPrincipal actor,Long id,AppointmentConfirmDTO input){
        var candidate=appointments.selectById(id);if(candidate==null)throw BusinessException.notFound("预约不存在");
        var locked=locker.lock(actor,candidate.getWorkOrderId(),List.of(),"ADMIN","DISPATCHER");var operator=locked.actor();var w=locked.order();
        requireCoordinator(w,operator);StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());var ap=latestAppointment(w.getId(),true);
        if(ap==null||!id.equals(ap.getId())||!Set.of("PENDING","RESCHEDULE_REQUESTED").contains(ap.getStatus())
                ||!Set.of("WAITING_APPOINTMENT","APPOINTED").contains(w.getStatus()))throw BusinessException.stateConflict("不是当前待确认预约");
        StaffIdentityGuard.version(ap.getVersion(),input.expectedAppointmentVersion());var assignment=activeAssignment(w);safety.requireClear(w.getId());
        if(ap.getTechnicianId()!=null&&!assignment.getTechnicianId().equals(ap.getTechnicianId()))throw BusinessException.stateConflict("预约人员已变化，请重新安排");
        validTime(ap.getScheduledStartAt(),ap.getScheduledEndAt());checkOverlap(w,assignment.getTechnicianId(),ap.getScheduledStartAt(),ap.getScheduledEndAt());
        String reason=reason(w,input.reason());var before=appointmentAudit(ap);ap.setTechnicianId(assignment.getTechnicianId());setAppointment(ap,"CONFIRMED");
        if("WAITING_APPOINTMENT".equals(w.getStatus()))move(w,"APPOINTED",actor.staffId(),reason);else bump(w);
        var after=appointmentAudit(ap);after.put("reason",reason);audit.record("STAFF",actor.staffId(),"APPOINTMENT_CONFIRMED","WORK_ORDER",w.getId(),before,after);return appointmentView(ap);
    }
    public Long appointmentOrderId(Long id){var ap=appointments.selectById(id);if(ap==null)throw BusinessException.notFound("预约不存在");return ap.getWorkOrderId();}
    public StaffWorkOrderDetailVO adminStart(StaffPrincipal actor,Long id,WorkOrderCommandDTO input){
        var w=locker.lock(actor,id,List.of(),"ADMIN").order();requireTakeover(w,actor.staffId());
        return start(w,actor.staffId(),"ADMIN",input,false);
    }
    public StaffWorkOrderDetailVO technicianStart(Long customerId,Long id,WorkOrderCommandDTO input){
        var e=employees.requireLocked(customerId);var w=order(id,true);requireTechnician(w,e.staffId(),true);return start(w,e.staffId(),"TECHNICIAN",input,true);
    }
    private StaffWorkOrderDetailVO start(WorkOrderEntity w,Long actor,String role,WorkOrderCommandDTO input,boolean technician){
        StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());
        activeAssignment(w);safety.requireClear(w.getId());
        if(!canStart(w))throw BusinessException.stateConflict("检测须当前已接单、预约确认、安全解除且服务联系方式已核实");
        if(w.getVisitFeeAmount()==null&&!followUp.isRework(w)){
            if(orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",w.getId()).eq("version",w.getVersion())
                    .set("visit_fee_amount",CustomerCancellationPolicy.STANDARD_VISIT_FEE)
                    .set("visit_fee_technician_id",w.getTechnicianId()))!=1)
                throw BusinessException.versionConflict("工单已变化");
        }
        var ap=latestAppointment(w.getId(),true);var before=appointmentAudit(ap);setAppointment(ap,"COMPLETED");w=move(w,"IN_INSPECTION",actor,reason(w,input.reason()));
        var after=appointmentAudit(ap);after.put("actualTechnicianId",w.getTechnicianId());after.put("submittedBy",actor);
        audit.record("STAFF",actor,"INSPECTION_STARTED","WORK_ORDER",w.getId(),before,after);
        return view(w,actor,role,technician);
    }
    public StaffServiceRecordVO adminRecord(StaffPrincipal actor,Long id,ServiceRecordCreateDTO input){
        var w=locker.lock(actor,id,List.of(),"ADMIN").order();requireTakeover(w,actor.staffId());return record(w,actor.staffId(),input);
    }
    public StaffServiceRecordVO technicianRecord(Long customerId,Long id,ServiceRecordCreateDTO input){
        var e=employees.requireLocked(customerId);var w=order(id,true);requireTechnician(w,e.staffId(),true);return record(w,e.staffId(),input);
    }
    private StaffServiceRecordVO record(WorkOrderEntity w,Long actor,ServiceRecordCreateDTO input){
        StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());var assignment=activeAssignment(w);safety.requireClear(w.getId());
        if(!assignment.getTechnicianId().equals(input.technicianId()))throw BusinessException.validation("实际维修人员必须与当前有效分派一致");
        boolean inspection="INSPECTION".equals(input.kind());
        if(inspection){
            if(!"IN_INSPECTION".equals(w.getStatus())&&!("WAITING_QUOTE".equals(w.getStatus())&&input.correctsRecordId()!=null))throw BusinessException.stateConflict("当前不能提交检测记录");
        }else if(!"REPAIR".equals(input.kind())||!canWork(w))throw BusinessException.stateConflict("维修记录须在报价争议已解决且当前服务范围已确认后提交");
        Long corrects=parseNullableId(input.correctsRecordId());
        if(corrects!=null){var old=records.selectById(corrects);
            if(old==null||!w.getId().equals(old.getWorkOrderId())||!input.kind().equals(old.getRecordKind())||old.getSubmittedAt()==null
                    ||records.selectCount(new LambdaQueryWrapper<ServiceRecordEntity>().eq(ServiceRecordEntity::getCorrectsRecordId,corrects))>0)throw BusinessException.stateConflict("只能新增修正同一工单同类记录的末版");}
        String facts=multiline(input.facts(),10,4000,"检测/服务事实");String summary=contacts.redactNote(multiline(input.summary(),2,1000,"记录摘要"),repair(w));
        var row=new ServiceRecordEntity();row.setWorkOrderId(w.getId());row.setTechnicianId(assignment.getTechnicianId());row.setSubmittedBy(actor);
        row.setRecordKind(input.kind());row.setCorrectsRecordId(corrects);row.setSummary(summary);
        try{row.setInspectionFacts(json.writeValueAsString(Map.of("facts",facts)));}catch(Exception ex){throw new IllegalStateException("事实序列化失败",ex);}
        row.setSubmittedAt(clock.now());row.setCreatedAt(row.getSubmittedAt());row.setUpdatedAt(row.getSubmittedAt());records.insert(row);
        if(inspection&&"IN_INSPECTION".equals(w.getStatus())){
            boolean rework=followUp.isRework(w);
            move(w,rework?"IN_REPAIR":"WAITING_QUOTE",actor,rework?"已提交免费返修检测记录":"已提交检测事实");
        }else bump(w);
        var data=new LinkedHashMap<String,Object>();data.put("recordId",row.getId());data.put("kind",input.kind());data.put("technicianId",row.getTechnicianId());data.put("submittedBy",actor);data.put("correctsRecordId",corrects);
        audit.record("STAFF",actor,"SERVICE_RECORD_SUBMITTED","WORK_ORDER",w.getId(),null,data);return recordView(row);
    }
    public StaffWorkOrderDetailVO coordinate(StaffPrincipal actor,Long id,WorkOrderCoordinationDTO input){
        var locked=locker.lock(actor,id,List.of(),"ADMIN","DISPATCHER");var operator=locked.actor();var w=locked.order();
        requireCoordinator(w,operator);StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());
        boolean continueAfterSafety="CONTINUE_AFTER_SAFETY".equals(input.resolution());
        String reason=input.reason()==null?(continueAfterSafety?"安全事件已解除，等待客户确认继续维修并选择时段":"管理端确认协调处理"):reason(w,input.reason());
        if(!coordinationActions(w,operator.getRole()).contains(input.resolution()))throw BusinessException.stateConflict("当前状态或业务前置不允许该协调结论");
        if(!"ESCALATE_TO_MANUAL".equals(input.resolution()))safety.requireClear(w.getId());
        if("CANCEL_REJECTED_QUOTE".equals(input.resolution())){
            String confirmedReason=reason(w,input.reason());
            var fee=cancellations.visitFee(w);
            if("CANCEL_WITH_VISIT_FEE".equals(cancellations.action(w))){
                if(orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",id).eq("version",w.getVersion())
                        .set("cancellation_fee_amount",fee)
                        .set(w.getVisitFeeTechnicianId()==null,"visit_fee_technician_id",w.getTechnicianId()))!=1)
                    throw BusinessException.versionConflict("工单已变化");
                audit.record("STAFF",actor.staffId(),"VISIT_CANCELLATION_FEE_RECORDED","WORK_ORDER",id,null,
                        Map.of("amount",fee.toPlainString()));
            }
            return closeLocked(w,actor.staffId(),operator.getRole(),confirmedReason,false);
        }
        String next=switch(input.resolution()){
            case "ESCALATE_TO_MANUAL"->"WAITING_MANUAL_REVIEW";
            case "RETURN_TO_ASSIGNMENT","CONTINUE_AFTER_SAFETY"->{
                assignments.endForCoordination(w,actor.staffId(),reason);
                yield continueAfterSafety?"WAITING_CUSTOMER_VISIT":"WAITING_ASSIGNMENT";
            }
            case "REQUOTE"->{withdrawPending(w,actor.staffId(),reason);yield "WAITING_QUOTE";}
            case "SCHEDULE_REWORK"->"WAITING_APPOINTMENT";
            case "RESUME_REPAIR"->"IN_REPAIR";
            case "ABANDON_ADDITIONAL"->{withdrawPending(w,actor.staffId(),reason);yield "IN_REPAIR";}
            default->throw BusinessException.validation("协调结论不正确");};
        if(continueAfterSafety||"RETURN_TO_ASSIGNMENT".equals(input.resolution())){
            orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",id).eq("version",w.getVersion()).set("current_assignment_id",null)
                    .set("technician_id",null).set("assigned_at",null).set("takeover_by",null).set("takeover_at",null).set("takeover_reason",null)
                    .set(continueAfterSafety,"preferred_visit_start_at",null).set(continueAfterSafety,"preferred_visit_end_at",null));w.setTakeoverBy(null);
        }
        w=move(w,next,actor.staffId(),reason);audit.record("STAFF",actor.staffId(),"WORK_ORDER_COORDINATION","WORK_ORDER",id,null,Map.of("resolution",input.resolution(),"reason",reason));
        return view(w,actor.staffId(),operator.getRole(),false);
    }

    public StaffWorkOrderDetailVO adminComplete(StaffPrincipal actor,Long id,WorkOrderCommandDTO input){
        var w=locker.lock(actor,id,List.of(),"ADMIN").order();requireTakeover(w,actor.staffId());complete(w,actor.staffId(),input);return view(w,actor.staffId(),"ADMIN",false);
    }
    public StaffParticipationVO technicianComplete(Long customer,Long id,WorkOrderCommandDTO input){
        var e=employees.requireLocked(customer);var w=order(id,true);requireTechnician(w,e.staffId(),true);complete(w,e.staffId(),input);return participationView(w,e.staffId());
    }
    public void guardCompletion(Long customer,Long id){
        var e=employees.require(customer);var w=order(id,false);
        if(assignmentRows.selectCount(new LambdaQueryWrapper<WorkOrderAssignmentEntity>().eq(WorkOrderAssignmentEntity::getWorkOrderId,id).eq(WorkOrderAssignmentEntity::getTechnicianId,e.staffId()).eq(WorkOrderAssignmentEntity::getStatus,"FINISHED"))>0)return;
        requireTechnician(w,e.staffId(),true);
    }
    private void complete(WorkOrderEntity w,Long actor,WorkOrderCommandDTO input){
        StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());activeAssignment(w);safety.requireClear(w.getId());
        if(!canComplete(w))throw BusinessException.stateConflict("完工须当前报价已确认、报价争议已解决并有本次维修记录");
        String reason=reason(w,input.reason());cancelAppointments(w);withdrawPending(w,actor,"完工撤回未提交新增范围");
        assignments.finishForClosure(w,actor,reason);
        var moved=move(w,"WAITING_ACCEPTANCE",actor,reason);w.setStatus(moved.getStatus());w.setVersion(moved.getVersion());
        audit.record("STAFF",actor,"WORK_ORDER_COMPLETION_SUBMITTED","WORK_ORDER",w.getId(),null,Map.of("technicianId",w.getTechnicianId(),"assignmentId",w.getCurrentAssignmentId(),"reason",reason));
    }
    public StaffWorkOrderDetailVO close(StaffPrincipal actor,Long id,WorkOrderCommandDTO input,boolean unresolved){
        var locked=locker.lock(actor,id,List.of(),unresolved?new String[]{"ADMIN"}:new String[]{"ADMIN","DISPATCHER"});var operator=locked.actor();
        var w=locked.order();requireCoordinator(w,operator);StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());
        if(unresolved?!canClose(w):!canCancel(w))throw BusinessException.stateConflict(unresolved?"当前状态须先转人工，不能直接异常关闭":"已开始现场服务或已有已接受范围，不能取消");
        return closeLocked(w,actor.staffId(),operator.getRole(),reason(w,input.reason()),unresolved);
    }
    private StaffWorkOrderDetailVO closeLocked(WorkOrderEntity w,Long actor,String role,String reason,boolean unresolved){
        Long id=w.getId();
        assignments.displayAssignment(w,true);cancelAppointments(w);withdrawPending(w,actor,reason);
        assignments.finishForClosure(w,actor,reason);var old=w.getStatus();w=move(w,unresolved?"CLOSED_UNRESOLVED":"CANCELLED",actor,reason);
        audit.record("STAFF",actor,unresolved?"WORK_ORDER_CLOSED_UNRESOLVED":"WORK_ORDER_CANCELLED","WORK_ORDER",id,Map.of("status",old),Map.of("reason",reason,"acceptedQuoteIds",quoteRows(id).stream().filter(q->"ACCEPTED".equals(q.getStatus())).map(QuoteEntity::getId).toList()));return view(w,actor,role,false);
    }
    private void cancelAppointments(WorkOrderEntity w){for(var ap:appointments.selectList(new LambdaQueryWrapper<AppointmentEntity>().eq(AppointmentEntity::getWorkOrderId,w.getId()).in(AppointmentEntity::getStatus,CURRENT_APPOINTMENTS).orderByAsc(AppointmentEntity::getId).last("FOR UPDATE")))setAppointment(ap,"CANCELLED");}
    private boolean canComplete(WorkOrderEntity w){
        return canWork(w)&&hasCurrentRepairRecord(w);
    }
    private boolean hasCurrentRepairRecord(WorkOrderEntity w){
        var a=assignments.currentAssignment(w,false);return a!=null&&"ACTIVE".equals(a.getStatus())&&records.selectCount(new LambdaQueryWrapper<ServiceRecordEntity>().eq(ServiceRecordEntity::getWorkOrderId,w.getId()).eq(ServiceRecordEntity::getRecordKind,"REPAIR").eq(ServiceRecordEntity::getTechnicianId,a.getTechnicianId()).isNotNull(ServiceRecordEntity::getSubmittedAt).ge(ServiceRecordEntity::getSubmittedAt,a.getAcceptedAt()))>0;
    }
    private boolean canWork(WorkOrderEntity w){
        return "IN_REPAIR".equals(w.getStatus())&&followUp.quoteRejection(w.getId())==null
                &&acceptedScope.valid(quoteRows(w.getId()))&&!hasSubmitted(w.getId());
    }
    private boolean canCancel(WorkOrderEntity w){return cancellations.canCancelNow(w);}
    private boolean hasBusinessTodo(Long id){return todos.selectCount(new QueryWrapper<HumanHandoffRecordEntity>().eq("work_order_id",id).eq("status","PENDING"))>0||urgeTodos.selectCount(new QueryWrapper<UrgeRecordEntity>().eq("work_order_id",id).eq("status","PENDING"))>0;}
    private boolean canClose(WorkOrderEntity w){return Set.of("IN_INSPECTION","WAITING_QUOTE","WAITING_MANUAL_REVIEW").contains(w.getStatus());}

    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public StaffParticipationVOPageVO participations(Long customerId,int page,int size){
        var e=employees.require(customerId);paging(page,size);
        var q=new QueryWrapper<WorkOrderEntity>().apply(participationSql(),e.staffId(),e.staffId(),e.staffId()).orderByDesc("id");
        var result=orders.selectPage(new Page<>(page,size),q);
        return new StaffParticipationVOPageVO(result.getRecords().stream().map(w->participationView(w,e.staffId())).toList(),page,size,result.getTotal());
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public StaffParticipationVO participation(Long customerId,Long id){
        var e=employees.require(customerId);var w=orders.selectOne(new QueryWrapper<WorkOrderEntity>().eq("id",id).apply(participationSql(),e.staffId(),e.staffId(),e.staffId()));
        if(w==null)throw BusinessException.notFound("未找到本人参与记录");return participationView(w,e.staffId());
    }
    private String participationSql(){return "(EXISTS (SELECT 1 FROM work_order_assignment a WHERE a.work_order_id=work_order.id AND a.technician_id={0}) OR EXISTS (SELECT 1 FROM service_record sr WHERE sr.work_order_id=work_order.id AND (sr.technician_id={1} OR sr.submitted_by={2})))";}
    private StaffParticipationVO participationView(WorkOrderEntity w,Long staffId){
        var own=assignmentRows.selectList(new LambdaQueryWrapper<WorkOrderAssignmentEntity>().eq(WorkOrderAssignmentEntity::getWorkOrderId,w.getId()).eq(WorkOrderAssignmentEntity::getTechnicianId,staffId).orderByAsc(WorkOrderAssignmentEntity::getId));
        var facts=records.selectList(new LambdaQueryWrapper<ServiceRecordEntity>().eq(ServiceRecordEntity::getWorkOrderId,w.getId()).and(q->q.eq(ServiceRecordEntity::getTechnicianId,staffId).or().eq(ServiceRecordEntity::getSubmittedBy,staffId)));
        LocalDateTime began=own.stream().map(WorkOrderAssignmentEntity::getAssignedAt).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        var factTime=facts.stream().map(ServiceRecordEntity::getSubmittedAt).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        if(began==null||(factTime!=null&&factTime.isBefore(began)))began=factTime;
        var last=own.isEmpty()?null:own.get(own.size()-1);
        String result=null;
        if(last!=null){
            if(VisitSchedulingService.wasRescheduleDeclined(last))result="RESCHEDULE_DECLINED";
            else if("DECLINED".equals(last.getStatus()))result="DECLINED";
            else if(last.getAcceptedAt()!=null)result="ACCEPTED";
        }
        return new StaffParticipationVO(w.getId(),w.getWorkOrderNo(),w.getApplianceType(),w.getStatus(),offset(began),last==null?null:offset(last.getEndedAt()),
                "分派记录 "+own.size()+" 条；本人相关服务记录 "+facts.size()+" 条"+(last==null?"":"；最近分派："+assignmentText(last.getStatus())),result,
                staffId.equals(w.getVisitFeeTechnicianId())?cancellations.cancellationAmount(w,null):null);
    }
    private String assignmentText(String s){return switch(s){case "PENDING"->"待接单";case "ACTIVE"->"已接单";case "RETURN_REQUESTED"->"申请退回";case "DECLINED"->"已拒接";case "REASSIGNED"->"已改派";case "REVOKED"->"已撤回";case "FINISHED"->"已结束";default->"待后台核对";};}

    private StaffWorkOrderDetailVO view(WorkOrderEntity w,Long actor,String role,boolean technician){
        var r=repair(w);boolean disclose=!"AUDITOR".equals(role)&&!terminal(w)&&(!technician||(r.getServiceContactSnapshot()!=null&&contacts.complete(r)));
        var original=userProjection.buildDetail(w,disclose);
        if(!disclose){if(original.currentQuote()!=null)redactQuote(original.currentQuote(),r);for(var quote:original.quoteHistory())redactQuote(quote,r);if(original.appointment()!=null)original.appointment().setNote(contacts.redactPersonalText(original.appointment().getNote(),r));}
        ServiceContactVO contact;
        if(technician&&r.getServiceContactSnapshot()==null)contact=new ServiceContactVO(null,null,null,"UNAVAILABLE",true,null);
        else contact=contacts.view(r,disclose);
        var projection=new StaffWorkOrderProjectionVO(original.id(),original.workOrderNo(),original.status(),original.applianceType(),original.brandCode(),original.modelCode(),disclose?original.symptomDescription():contacts.redactPersonalText(original.symptomDescription(),r),disclose?original.errorCodeText():contacts.redactPersonalText(original.errorCodeText(),r),offset(original.occurredAt()),original.mediaFiles(),
                empty(contact.contactName()),empty(contact.contactPhone()),empty(contact.addressText()),original.appointment(),original.technicianName(),original.currentQuote(),original.quoteHistory(),List.of(),original.acceptance()==null?null:disclose?original.acceptance():new com.fixmate.dto.user.AcceptanceVO(original.acceptance().accepted(),contacts.redactPersonalText(original.acceptance().note(),r),original.acceptance().acceptedAt()),original.timeline(),original.version());
        var a=assignments.displayAssignment(w,false);var ap=latestAppointment(w.getId(),false);
        var assignee=technician||a==null?null:staff.selectById(a.getTechnicianId());
        var serviceRecords=records.selectList(new LambdaQueryWrapper<ServiceRecordEntity>().eq(ServiceRecordEntity::getWorkOrderId,w.getId()).orderByAsc(ServiceRecordEntity::getId)).stream().map(row->{var vo=recordView(row);return disclose?vo:new StaffServiceRecordVO(vo.id(),vo.workOrderId(),vo.kind(),vo.technicianId(),vo.submittedBy(),vo.submittedAt(),contacts.redactPersonalText(vo.facts(),r),contacts.redactPersonalText(vo.summary(),r),vo.correctsRecordId());}).toList();
        var resolutions=coordinationActions(w,role);
        var cause=followUp.coordinationCause(w);
        if(technician&&cause!=null&&!Set.of(WorkOrderFollowUp.INITIAL_QUOTE_REJECTED,WorkOrderFollowUp.ADDITIONAL_QUOTE_REJECTED).contains(cause.type()))cause=null;
        boolean rework=followUp.isRework(w);
        Long originalTechnician=rework&&!technician?followUp.originalTechnician(w.getId()):null;
        return new StaffWorkOrderDetailVO(VisitSchedulingService.preferred(w),VisitSchedulingService.policy(w),projection,
                assignments.assignmentView(a),serviceRecords,allowed(w,actor,role,resolutions),resolutions,ap==null?null:ap.getVersion(),
                w.getRepairRequestId(),technician?null:r.getRepairNo(),technician?null:!safety.clear(w.getId()),
                technician||ap==null?null:ap.getStatus(),assignee==null?null:assignee.getName(),contact,
                cause==null?null:cause.type(),cause==null||technician?null:contacts.redactPersonalText(cause.reason(),r),
                originalTechnician==null?null:originalTechnician.toString(),rework);
    }
    private void redactQuote(com.fixmate.dto.user.QuoteVO quote,RepairRequestEntity repair){
        quote.setChangeReason(contacts.redactPersonalText(quote.getChangeReason(),repair));for(var line:quote.getItems()){
            line.setItemName(contacts.redactPersonalText(line.getItemName(),repair));line.setSpecification(contacts.redactPersonalText(line.getSpecification(),repair));line.setPricingBasisSnapshot(contacts.redactPersonalText(line.getPricingBasisSnapshot(),repair));line.setPartSourceNote(contacts.redactPersonalText(line.getPartSourceNote(),repair));line.setAdditionReason(contacts.redactPersonalText(line.getAdditionReason(),repair));
        }
    }
    private List<String> allowed(WorkOrderEntity w,Long actor,String role,List<String> resolutions){
        if("AUDITOR".equals(role)||terminal(w))return List.of();var actions=new ArrayList<String>();var a=assignments.displayAssignment(w,false);
        if(a!=null&&"FINISHED".equals(a.getStatus())){
            if("TECHNICIAN".equals(role))return actions;
            if(hasBusinessTodo(w.getId()))actions.add("RESOLVE_TODO");
            if("ADMIN".equals(role)&&safety.clear(w.getId())&&assignments.canAssignManual(w)&&validEmployee(a.getTechnicianId()))actions.add("ASSIGN");
            if("ADMIN".equals(role)&&canClose(w))actions.add("CLOSE_UNRESOLVED");return actions;
        }
        boolean active=a!=null&&"ACTIVE".equals(a.getStatus())&&validEmployee(a.getTechnicianId());
        boolean technician="TECHNICIAN".equals(role);boolean owner=technician?(active&&actor.equals(a.getTechnicianId())&&w.getTakeoverBy()==null):("ADMIN".equals(role)&&actor.equals(w.getTakeoverBy())&&active);
        boolean clear=safety.clear(w.getId());
        if(owner&&clear){
            if(canStart(w))actions.add("START_INSPECTION");
            if(Set.of("IN_INSPECTION","WAITING_QUOTE").contains(w.getStatus())&&( "IN_INSPECTION".equals(w.getStatus())||hasInspection(w.getId())))actions.add("SUBMIT_INSPECTION");
            if(canWork(w)){
                actions.add("RECORD_SERVICE");
                if(hasCurrentRepairRecord(w))actions.add("SUBMIT_COMPLETION");
            }
        }
        var quoteAction=quoteWork.creationAction(w,actor,role);if(quoteAction!=null)actions.add(quoteAction);
        if(technician&&owner&&visits.canConfirm(w,actor))actions.add("CONFIRM_VISIT");
        if(technician&&owner&&visits.canDeclineReschedule(w,actor))actions.add("DECLINE_RESCHEDULE");
        if(technician){if(active&&w.getTakeoverBy()==null&&!"WAITING_ACCEPTANCE".equals(w.getStatus()))actions.add("REQUEST_RETURN");return actions;}
        if(w.getTakeoverBy()!=null&&!actor.equals(w.getTakeoverBy())&&!"ADMIN".equals(role))return actions;
        if(a!=null&&"RETURN_REQUESTED".equals(a.getStatus())){
            actions.add("DECIDE_RETURN");
            if(clear&&assignments.canReleaseReturnForPublication(w)&&"CUSTOMER_SLOT".equals(w.getVisitPolicy())
                    &&visits.hasValidPreference(w)&&contacts.confirmedForOrder(repair(w),w))
                actions.add("APPROVE_RETURN_TO_POOL");
        }
        if(clear&&!followUp.isRework(w)&&Set.of("WAITING_ASSIGNMENT","WAITING_APPOINTMENT","APPOINTED","IN_INSPECTION","WAITING_QUOTE","WAITING_QUOTE_CONFIRMATION","PAUSED_FOR_REQUOTE","IN_REPAIR").contains(w.getStatus())
                &&(!"IN_REPAIR".equals(w.getStatus())||("ADMIN".equals(role)&&acceptedScope.valid(quoteRows(w.getId())))))actions.add("ASSIGN");
        if(active&&clear&&Set.of("WAITING_APPOINTMENT","APPOINTED","IN_INSPECTION").contains(w.getStatus())){
            actions.add("APPOINT");var ap=latestAppointment(w.getId(),false);
            if(ap!=null&&Set.of("PENDING","RESCHEDULE_REQUESTED").contains(ap.getStatus())&&!"IN_INSPECTION".equals(w.getStatus())
                    &&(ap.getTechnicianId()==null||a.getTechnicianId().equals(ap.getTechnicianId()))
                    &&ap.getScheduledStartAt()!=null&&ap.getScheduledEndAt()!=null&&ap.getScheduledStartAt().isBefore(ap.getScheduledEndAt())
                    &&!ap.getScheduledStartAt().isBefore(clock.now())&&!overlaps(w,a.getTechnicianId(),ap.getScheduledStartAt(),ap.getScheduledEndAt()))actions.add("CONFIRM_APPOINTMENT");}
        if(active&&"ADMIN".equals(role)&&!actor.equals(w.getTakeoverBy()))actions.add("TAKEOVER");
        if(!resolutions.isEmpty())actions.add("COORDINATE");
        if(clear&&"ADMIN".equals(role)&&assignments.canAssignManual(w)&&validEmployee(followUp.originalTechnician(w.getId())))actions.add("ASSIGN");
        if(hasBusinessTodo(w.getId()))actions.add("RESOLVE_TODO");
        if(canCancel(w))actions.add("CANCEL");
        if("ADMIN".equals(role)&&canClose(w))actions.add("CLOSE_UNRESOLVED");
        // 报价子资源的编辑/提交/撤回动作只由报价工作VO下发。
        return actions;
    }
    private List<String> coordinationActions(WorkOrderEntity w,String role){
        if(!Set.of("ADMIN","DISPATCHER").contains(role)||terminal(w))return List.of();var result=new ArrayList<String>();
        if("ADMIN".equals(role)&&"IN_REPAIR".equals(w.getStatus()))result.add("ESCALATE_TO_MANUAL");
        if(!safety.clear(w.getId()))return result;var qs=quoteRows(w.getId());boolean accepted=qs.stream().anyMatch(q->"ACCEPTED".equals(q.getStatus()));
        var cause=followUp.manualCause(w);
        var a=assignments.currentAssignment(w,false);boolean active=a!=null&&"ACTIVE".equals(a.getStatus())&&validEmployee(a.getTechnicianId());
        if(cause!=null){
            if(Set.of(WorkOrderFollowUp.INITIAL_QUOTE_REJECTED,WorkOrderFollowUp.ADDITIONAL_QUOTE_REJECTED).contains(cause.type())){
                if(active&&hasInspection(w.getId()))result.add("REQUOTE");
                if(WorkOrderFollowUp.INITIAL_QUOTE_REJECTED.equals(cause.type())&&!accepted&&cancellations.action(w)!=null)result.add("CANCEL_REJECTED_QUOTE");
                return result;
            }
            if(WorkOrderFollowUp.ACCEPTANCE_ISSUE.equals(cause.type())){
                if(active&&"ADMIN".equals(role)&&Objects.equals(a.getTechnicianId(),followUp.originalTechnician(w.getId()))
                        &&acceptedScope.valid(qs)&&visits.hasReworkPreference(w))result.add("SCHEDULE_REWORK");
                return result;
            }
        }
        if("WAITING_MANUAL_REVIEW".equals(w.getStatus())&&!accepted){
            if("HIGH_RISK_BLOCKED".equals(repair(w).getStatus())&&canCancel(w))
                return safety.hasResolvedEvent(w.getId())?List.of("CONTINUE_AFTER_SAFETY"):List.of();
            result.add("RETURN_TO_ASSIGNMENT");
        }
        if("WAITING_MANUAL_REVIEW".equals(w.getStatus())&&active&&hasInspection(w.getId()))result.add("REQUOTE");
        if(active&&acceptedScope.valid(qs)){
            if("WAITING_MANUAL_REVIEW".equals(w.getStatus())&&"ADMIN".equals(role)&&!hasSubmitted(w.getId()))result.add("RESUME_REPAIR");
            if(Set.of("WAITING_MANUAL_REVIEW","PAUSED_FOR_REQUOTE","WAITING_QUOTE").contains(w.getStatus())
                    &&followUp.quoteRejection(w.getId())==null&&acceptedScope.canAbandonLatestAdditional(qs))result.add("ABANDON_ADDITIONAL");
        }
        return result;
    }
    private boolean validEmployee(Long id){var s=staff.selectById(id);if(s==null||!"ENABLED".equals(s.getStatus())||!"TECHNICIAN".equals(s.getRole()))return false;
        var b=bindings.selectOne(new LambdaQueryWrapper<StaffWechatBindingEntity>().eq(StaffWechatBindingEntity::getStaffId,id).eq(StaffWechatBindingEntity::getStatus,"ACTIVE"));
        if(b==null)return false;try{return id.equals(employees.require(b.getCustomerId()).staffId());}catch(BusinessException ex){return false;}}
    private WorkOrderAssignmentEntity activeAssignment(WorkOrderEntity w){var a=assignments.currentAssignment(w,true);if(a==null||!"ACTIVE".equals(a.getStatus())||!validEmployee(a.getTechnicianId()))throw BusinessException.stateConflict("需要当前有效已接单维修人员");return a;}
    private void requireTechnician(WorkOrderEntity w,Long actor,boolean write){nonterminal(w);var a=assignments.currentAssignment(w,false);
        if(a==null||!actor.equals(a.getTechnicianId())||!"ACTIVE".equals(a.getStatus())||w.getTakeoverBy()!=null||(write&&!"ACTIVE".equals(a.getStatus())))throw BusinessException.forbidden("无权访问当前完整工单，请查看本人参与摘要");}
    private void requireCoordinator(WorkOrderEntity w,StaffUserEntity actor){if(w.getTakeoverBy()!=null&&!"ADMIN".equals(actor.getRole()))throw BusinessException.forbidden("接管中的工单由管理员协调");}
    private void requireTakeover(WorkOrderEntity w,Long actor){if(!actor.equals(w.getTakeoverBy()))throw BusinessException.forbidden("须先由本人审计接管");nonterminal(w);}
    private boolean canStart(WorkOrderEntity w){if(!"APPOINTED".equals(w.getStatus())||!safety.clear(w.getId()))return false;
        var r=repair(w);var a=assignments.displayAssignment(w,false);var ap=latestAppointment(w.getId(),false);
        return r.getServiceContactSnapshot()!=null&&contacts.complete(r)&&a!=null&&"ACTIVE".equals(a.getStatus())&&validEmployee(a.getTechnicianId())
                &&ap!=null&&"CONFIRMED".equals(ap.getStatus())&&Objects.equals(ap.getTechnicianId(),a.getTechnicianId());}
    private void checkOverlap(WorkOrderEntity w,Long technician,LocalDateTime start,LocalDateTime end){
        if(overlaps(w,technician,start,end))throw BusinessException.stateConflict("维修人员在该时段已有安排，请协调时间");}
    private boolean overlaps(WorkOrderEntity w,Long technician,LocalDateTime start,LocalDateTime end){return appointments.selectCount(new LambdaQueryWrapper<AppointmentEntity>().eq(AppointmentEntity::getTechnicianId,technician).ne(AppointmentEntity::getWorkOrderId,w.getId())
                .in(AppointmentEntity::getStatus,CURRENT_APPOINTMENTS).and(q->q.nested(t->t.lt(AppointmentEntity::getScheduledStartAt,end).gt(AppointmentEntity::getScheduledEndAt,start))
                    .or(t->t.lt(AppointmentEntity::getPreviousConfirmedStartAt,end).gt(AppointmentEntity::getPreviousConfirmedEndAt,start))))>0;}
    private void validTime(LocalDateTime start,LocalDateTime end){if(start==null||end==null||!start.isBefore(end)||start.isBefore(clock.now()))throw BusinessException.validation("预约须为未来且开始早于结束");}
    private void setAppointment(AppointmentEntity ap,String status){if("CONFIRMED".equals(status)){ap.setPreviousConfirmedStartAt(null);ap.setPreviousConfirmedEndAt(null);}if(appointments.update(null,new UpdateWrapper<AppointmentEntity>().eq("id",ap.getId()).eq("version",ap.getVersion()).set("status",status)
                .set("technician_id",ap.getTechnicianId()).set("previous_confirmed_start_at",ap.getPreviousConfirmedStartAt()).set("previous_confirmed_end_at",ap.getPreviousConfirmedEndAt()).set("version",ap.getVersion()+1).set("updated_at",clock.now()))!=1)throw BusinessException.versionConflict("预约已变化");ap.setStatus(status);ap.setVersion(ap.getVersion()+1);}
    private Map<String,Object> appointmentAudit(AppointmentEntity ap){var data=new LinkedHashMap<String,Object>();data.put("appointmentId",ap==null?null:ap.getId());data.put("status",ap==null?null:ap.getStatus());data.put("startAt",ap==null?null:ap.getScheduledStartAt());data.put("endAt",ap==null?null:ap.getScheduledEndAt());data.put("technicianId",ap==null?null:ap.getTechnicianId());data.put("version",ap==null?null:ap.getVersion());return data;}
    private StaffAppointmentVO appointmentView(AppointmentEntity ap){var vo=new AppointmentVO();vo.setId(ap.getId());vo.setStatus(ap.getStatus());vo.setScheduledStartAt(ap.getScheduledStartAt());vo.setScheduledEndAt(ap.getScheduledEndAt());vo.setNote(ap.getNote());var s=ap.getTechnicianId()==null?null:staff.selectById(ap.getTechnicianId());vo.setTechnicianName(s==null?null:s.getName());return new StaffAppointmentVO(vo,ap.getVersion());}
    private AppointmentEntity latestAppointment(Long id,boolean lock){var q=new LambdaQueryWrapper<AppointmentEntity>().eq(AppointmentEntity::getWorkOrderId,id).orderByDesc(AppointmentEntity::getId).last(lock?"LIMIT 1 FOR UPDATE":"LIMIT 1");return appointments.selectOne(q);}
    private void withdrawPending(WorkOrderEntity w,Long actor,String reason){quoteWork.withdrawForLifecycle(w.getId(),"STAFF",actor,reason);}
    private List<QuoteEntity> quoteRows(Long id){return quotes.selectList(new LambdaQueryWrapper<QuoteEntity>().eq(QuoteEntity::getWorkOrderId,id).orderByAsc(QuoteEntity::getVersionNo));}
    private boolean hasSubmitted(Long id){return quotes.selectCount(new LambdaQueryWrapper<QuoteEntity>().eq(QuoteEntity::getWorkOrderId,id).eq(QuoteEntity::getStatus,"SUBMITTED"))>0;}
    private boolean hasInspection(Long id){return records.selectCount(new LambdaQueryWrapper<ServiceRecordEntity>().eq(ServiceRecordEntity::getWorkOrderId,id).eq(ServiceRecordEntity::getRecordKind,"INSPECTION").isNotNull(ServiceRecordEntity::getSubmittedAt))>0;}
    private StaffServiceRecordVO recordView(ServiceRecordEntity r){String facts=r.getInspectionFacts();try{var node=json.readTree(facts);if(node!=null&&node.path("facts").isTextual())facts=node.path("facts").asText();}catch(Exception ignored){}
        if(facts==null||facts.length()>4000)throw BusinessException.stateConflict("存量服务事实超出当前契约范围，请先核对并按契约变更处理");
        return new StaffServiceRecordVO(r.getId(),r.getWorkOrderId(),r.getRecordKind(),r.getTechnicianId(),r.getSubmittedBy(),offset(r.getSubmittedAt()),facts,empty(r.getSummary()),r.getCorrectsRecordId()==null?null:r.getCorrectsRecordId().toString());}
    private WorkOrderEntity order(Long id,boolean lock){var q=new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,id);if(lock)q.last("FOR UPDATE");var w=orders.selectOne(q);if(w==null)throw BusinessException.notFound("工单不存在");return w;}
    private RepairRequestEntity repair(WorkOrderEntity w){
        var r=repairIfLinked(w);
        if(r==null)throw BusinessException.stateConflict("原报修工单关联异常，请核对");
        return r;
    }
    private RepairRequestEntity repairIfLinked(WorkOrderEntity w){var r=repairs.selectById(w.getRepairRequestId());
        if(r==null||!Objects.equals(r.getUserId(),w.getCustomerId())||!Objects.equals(r.getWorkOrderId(),w.getId()))return null;
        // 兼容旧紧急单：原地址快照可能只保存在工单；仅投影回退，不改写报修历史。
        if(r.getAddressSnapshot()==null)r.setAddressSnapshot(w.getAddressSnapshot());return r;}
    private WorkOrderEntity move(WorkOrderEntity w,String next,Long actor,String reason){return machine.transition(w.getId(),WorkOrderStatus.valueOf(next),w.getVersion(),reason,"STAFF",actor);}
    private void bump(WorkOrderEntity w){if(orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",w.getId()).eq("version",w.getVersion()).set("version",w.getVersion()+1).set("updated_at",clock.now()))!=1)throw BusinessException.versionConflict("工单已变化");w.setVersion(w.getVersion()+1);}
    private boolean terminal(WorkOrderEntity w){return WorkOrderStatus.valueOf(w.getStatus()).isTerminal();}
    private void nonterminal(WorkOrderEntity w){if(terminal(w))throw BusinessException.forbidden("工单已结束，仅可查看历史摘要");}
    private void parseStatus(String value){try{WorkOrderStatus.valueOf(value);}catch(RuntimeException ex){throw BusinessException.validation("状态不正确");}}
    private static void paging(int page,int size){if(page<1||size<1||size>100)throw BusinessException.validation("分页参数不正确");}
    private String reason(WorkOrderEntity w,String value){return contacts.redactNote(StaffIdentityGuard.text(value,2,1000,"处理依据"),repair(w));}
    private String reasonOptional(WorkOrderEntity w,String value){return contacts.redactNote(multiline(value,0,1000,"预约备注"),repair(w));}
    private static String multiline(String value,int min,int max,String label){String clean=value==null?"":value.strip();if(clean.length()<min||clean.length()>max||clean.codePoints().anyMatch(c->Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t'))throw BusinessException.validation(label+"格式不正确");return clean;}
    private Long parseNullableId(String value){if(value==null)return null;try{long id=Long.parseLong(value);if(id<=0)throw new NumberFormatException();return id;}catch(RuntimeException ex){throw BusinessException.validation("修正记录ID不正确");}}
    private static OffsetDateTime offset(LocalDateTime v){return v==null?null:v.atOffset(ZoneOffset.UTC);}
    private static LocalDateTime utc(OffsetDateTime v){if(v==null)throw BusinessException.validation("缺少预约时间");return v.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();}
    private static String empty(String s){return s==null?"":s;}
}

