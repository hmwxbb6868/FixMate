package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.entity.AppointmentEntity;
import com.fixmate.entity.ServiceRecordEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.mapper.AppointmentMapper;
import com.fixmate.mapper.ServiceRecordMapper;
import com.fixmate.mapper.WorkOrderMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 爽约判定与报价过期：预约与工单的状态变化必须在同一事务内完成，
 * 避免出现"预约 MISSED、工单仍 APPOINTED"的不一致。
 */
@Service
public class TimeoutTransitionService {

    private final AppointmentMapper appointmentMapper;
    private final WorkOrderMapper workOrderMapper;
    private final ServiceRecordMapper serviceRecordMapper;
    private final WorkOrderStateMachine stateMachine;
    private final AuditLogService auditLogService;
    private final com.fixmate.mapper.DatabaseClockMapper clock;

    public TimeoutTransitionService(AppointmentMapper appointmentMapper,
                                    WorkOrderMapper workOrderMapper,
                                    ServiceRecordMapper serviceRecordMapper,
                                    WorkOrderStateMachine stateMachine,
                                    AuditLogService auditLogService,com.fixmate.mapper.DatabaseClockMapper clock) {
        this.appointmentMapper = appointmentMapper;
        this.workOrderMapper = workOrderMapper;
        this.serviceRecordMapper = serviceRecordMapper;
        this.stateMachine = stateMachine;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /**
     * 工单→预约统一锁序，并在锁内重查超时条件，避免与检测/改约交错。
     * 任一失败则整体回滚。
     */
    @Transactional
    public void markMissed(Long appointmentId) {
        AppointmentEntity appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !"CONFIRMED".equals(appointment.getStatus())) {
            return;
        }
        WorkOrderEntity order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrderEntity>()
                .eq(WorkOrderEntity::getId,appointment.getWorkOrderId()).last("FOR UPDATE"));
        if (order == null || !WorkOrderStatus.APPOINTED.name().equals(order.getStatus())) {
            return;
        }
        appointment=appointmentMapper.selectOne(new LambdaQueryWrapper<AppointmentEntity>()
                .eq(AppointmentEntity::getId,appointmentId).last("FOR UPDATE"));
        if(appointment==null||!order.getId().equals(appointment.getWorkOrderId())||!"CONFIRMED".equals(appointment.getStatus())
                ||appointment.getScheduledEndAt()==null||appointment.getScheduledEndAt().isAfter(clock.now().minusHours(2)))return;
        Long started = serviceRecordMapper.selectCount(new LambdaQueryWrapper<ServiceRecordEntity>()
                .eq(ServiceRecordEntity::getWorkOrderId, order.getId())
                .isNotNull(ServiceRecordEntity::getSubmittedAt)
                .ge(appointment.getCreatedAt()!=null,ServiceRecordEntity::getSubmittedAt,appointment.getCreatedAt()));
        if (started != null && started > 0) {
            return;
        }

        int rows = appointmentMapper.update(null, new UpdateWrapper<AppointmentEntity>()
                .eq("id", appointment.getId())
                .eq("status", "CONFIRMED")
                .eq("version", appointment.getVersion())
                .set("status", "MISSED")
                .set("version", appointment.getVersion() + 1)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
        if (rows == 0) {
            return;
        }

        stateMachine.transition(order.getId(), WorkOrderStatus.WAITING_APPOINTMENT,
                order.getVersion(), "预约超时未上门，系统判定爽约", "SYSTEM", null);

        auditLogService.recordSystem("APPOINTMENT_MISSED", "WORK_ORDER", order.getId(),
                Map.of("appointmentId", appointment.getId(),
                        "scheduledEndAt", appointment.getScheduledEndAt().toString()),
                Map.of("appointmentStatus", "MISSED", "workOrderStatus", "WAITING_APPOINTMENT"));
    }
}
