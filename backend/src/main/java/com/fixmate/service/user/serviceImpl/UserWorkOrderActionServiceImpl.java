package com.fixmate.service.user.serviceImpl;

import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.user.HumanHandoffCreateDTO;
import com.fixmate.dto.user.UrgeCreateDTO;
import com.fixmate.entity.HumanHandoffRecordEntity;
import com.fixmate.entity.UrgeRecordEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.HumanHandoffRecordMapper;
import com.fixmate.mapper.UrgeRecordMapper;
import com.fixmate.mapper.WorkOrderMapper;
import com.fixmate.service.common.WorkOrderUrges;
import com.fixmate.service.common.CustomerCancellationPolicy;
import com.fixmate.service.common.WorkOrderStateMachine;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.entity.AppointmentEntity;
import com.fixmate.mapper.AppointmentMapper;
import com.fixmate.mapper.DatabaseClockMapper;
import com.fixmate.service.admin.WorkAssignmentService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.service.user.UserWorkOrderActionService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWorkOrderActionServiceImpl implements UserWorkOrderActionService {

    private static final Set<String> CLOSED_STATUSES = Set.of("COMPLETED", "CANCELLED", "CLOSED_UNRESOLVED");

    private final WorkOrderMapper workOrderMapper;
    private final UrgeRecordMapper urgeRecordMapper;
    private final HumanHandoffRecordMapper handoffRecordMapper;
    private final AuditLogService auditLogService;
    private final WorkOrderUrges urges;
    private final CustomerCancellationPolicy cancellations;
    private final WorkOrderStateMachine stateMachine;
    private final WorkAssignmentService assignments;
    private final AppointmentMapper appointments;
    private final DatabaseClockMapper clock;
    private final com.fixmate.service.admin.StaffQuoteService quoteWork;

    public UserWorkOrderActionServiceImpl(WorkOrderMapper workOrderMapper,
                                          UrgeRecordMapper urgeRecordMapper,
                                          HumanHandoffRecordMapper handoffRecordMapper,
                                          AuditLogService auditLogService,WorkOrderUrges urges,
                                          CustomerCancellationPolicy cancellations,WorkOrderStateMachine stateMachine,
                                          WorkAssignmentService assignments,AppointmentMapper appointments,DatabaseClockMapper clock,
                                          com.fixmate.service.admin.StaffQuoteService quoteWork) {
        this.workOrderMapper = workOrderMapper;
        this.urgeRecordMapper = urgeRecordMapper;
        this.handoffRecordMapper = handoffRecordMapper;
        this.auditLogService = auditLogService;
        this.urges = urges;
        this.cancellations=cancellations;this.stateMachine=stateMachine;this.assignments=assignments;
        this.appointments=appointments;this.clock=clock;this.quoteWork=quoteWork;
    }

    @Override
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void cancel(Long userId,Long workOrderId,Integer expectedVersion,String expectedVisitFee) {
        var order=requireOwnedOpenWorkOrder(userId,workOrderId,expectedVersion);
        String action=cancellations.action(order);
        if(action==null)throw BusinessException.stateConflict("当前工单无法取消，请刷新后查看");
        String reason="客户取消报修";
        if("CANCEL_WITH_VISIT_FEE".equals(action)) {
            var fee=cancellations.visitFee(order);
            if(expectedVisitFee==null||!expectedVisitFee.matches("^[0-9]{1,10}(\\.[0-9]{1,2})?$")
                    ||fee.compareTo(new java.math.BigDecimal(expectedVisitFee))!=0)
                throw BusinessException.stateConflict("请刷新并确认当前上门费后取消");
            if(workOrderMapper.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",workOrderId)
                    .eq("version",expectedVersion).set("cancellation_fee_amount",fee)
                    .set(order.getVisitFeeTechnicianId()==null,"visit_fee_technician_id",order.getTechnicianId()))!=1)
                throw BusinessException.versionConflict("工单已变化");
            reason="客户取消报修，仅登记应收上门费";
            auditLogService.recordCustomer(userId,"VISIT_CANCELLATION_FEE_RECORDED","WORK_ORDER",workOrderId,null,
                    java.util.Map.of("amount",fee.toPlainString()));
        }
        quoteWork.withdrawForLifecycle(workOrderId,"CUSTOMER",userId,reason);
        assignments.releaseForCustomerCancellation(order,userId,reason);
        appointments.update(null,new UpdateWrapper<AppointmentEntity>().eq("work_order_id",workOrderId)
                .in("status",java.util.List.of("PENDING","CONFIRMED","RESCHEDULE_REQUESTED"))
                .set("status","CANCELLED").set("previous_confirmed_start_at",null)
                .set("previous_confirmed_end_at",null).setSql("version=version+1").set("updated_at",clock.now()));
        stateMachine.transition(workOrderId,WorkOrderStatus.CANCELLED,expectedVersion,reason,"CUSTOMER",userId);
    }

    @Override
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void urge(Long userId, Long workOrderId, UrgeCreateDTO dto) {
        WorkOrderEntity order = requireOwnedOpenWorkOrder(userId, workOrderId, dto.getExpectedWorkOrderVersion());
        if (urges.awaitingRedispatchAfterWithdrawal(order)) {
            throw BusinessException.stateConflict("工单已从接单大厅撤回，重新派出后可催单");
        }

        UrgeRecordEntity record = new UrgeRecordEntity();
        record.setWorkOrderId(order.getId());
        record.setUserId(userId);
        record.setReason(dto.getReason());
        record.setStatus("PENDING");
        record.setCreatedAt(now());
        record.setUpdatedAt(now());
        urgeRecordMapper.insert(record);

        auditLogService.recordCustomer(userId, "WORK_ORDER_URGED", "WORK_ORDER", order.getId(),
                null, java.util.Map.of("urgeRecordId", record.getId(), "reasonRecorded", dto.getReason()!=null&&!dto.getReason().isBlank()));
    }

    @Override
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void requestHumanHandoff(Long userId, Long workOrderId, HumanHandoffCreateDTO dto) {
        WorkOrderEntity order = requireOwnedOpenWorkOrder(userId, workOrderId, dto.getExpectedWorkOrderVersion());

        HumanHandoffRecordEntity record = new HumanHandoffRecordEntity();
        record.setWorkOrderId(order.getId());
        record.setUserId(userId);
        record.setReason(dto.getReason());
        record.setStatus("PENDING");
        record.setCreatedAt(now());
        record.setUpdatedAt(now());
        handoffRecordMapper.insert(record);

        auditLogService.recordCustomer(userId, "HUMAN_HANDOFF_REQUESTED", "WORK_ORDER", order.getId(),
                null, java.util.Map.of("handoffRecordId", record.getId()));
    }

    private WorkOrderEntity requireOwnedOpenWorkOrder(Long userId, Long workOrderId, Integer expectedVersion) {
        WorkOrderEntity order = workOrderMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,workOrderId).last("FOR UPDATE"));
        if (order == null) {
            throw BusinessException.notFound("工单不存在");
        }
        if (!order.getCustomerId().equals(userId)) {
            throw BusinessException.forbidden("无权操作该工单");
        }
        if (CLOSED_STATUSES.contains(order.getStatus())) {
            throw BusinessException.stateConflict("工单已结束，无法执行该操作");
        }
        if (expectedVersion != null && !expectedVersion.equals(order.getVersion())) {
            throw BusinessException.versionConflict("工单已被更新，请刷新后重试");
        }
        return order;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
