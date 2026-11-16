package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.user.AppointmentRescheduleDTO;
import com.fixmate.dto.user.AppointmentVO;
import com.fixmate.entity.AppointmentEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.AppointmentMapper;
import com.fixmate.mapper.WorkOrderMapper;
import com.fixmate.service.common.WorkOrderFollowUp;
import com.fixmate.service.user.UserAppointmentService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAppointmentServiceImpl implements UserAppointmentService {

    private static final Set<String> RESCHEDULABLE_STATUSES = Set.of("WAITING_APPOINTMENT", "APPOINTED");

    private final WorkOrderMapper workOrderMapper;
    private final com.fixmate.mapper.DatabaseClockMapper clock;
    private final AppointmentMapper appointmentMapper;
    private final com.fixmate.mapper.StaffUserMapper staffUserMapper;
    private final AuditLogService auditLogService;
    private final WorkOrderFollowUp followUp;

    public UserAppointmentServiceImpl(WorkOrderMapper workOrderMapper,
                                      AppointmentMapper appointmentMapper,
                                      com.fixmate.mapper.StaffUserMapper staffUserMapper,
                                      AuditLogService auditLogService,com.fixmate.mapper.DatabaseClockMapper clock,WorkOrderFollowUp followUp) {
        this.clock=clock;
        this.workOrderMapper = workOrderMapper;
        this.appointmentMapper = appointmentMapper;
        this.staffUserMapper = staffUserMapper;
        this.auditLogService = auditLogService;
        this.followUp = followUp;
    }

    @Override
    @Transactional
    public AppointmentVO reschedule(Long userId, Long workOrderId, AppointmentRescheduleDTO dto) {
        WorkOrderEntity order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,workOrderId).last("FOR UPDATE"));
        if (order == null) {
            throw BusinessException.notFound("工单不存在");
        }
        if (!order.getCustomerId().equals(userId)) {
            throw BusinessException.forbidden("无权操作该工单");
        }
        if (!RESCHEDULABLE_STATUSES.contains(order.getStatus())) {
            throw BusinessException.stateConflict("当前工单状态不支持改约");
        }
        if ("WAITING_APPOINTMENT".equals(order.getStatus()) && followUp.isRework(order)) {
            throw BusinessException.stateConflict("请通过返修时段入口调整期望时间，师傅确认后预约成立");
        }
        LocalDateTime now = clock.now();
        LocalDateTime startAt = dto.getStartAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        LocalDateTime endAt = dto.getEndAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        if (!startAt.isBefore(endAt)) {
            throw BusinessException.validation("开始时间必须早于结束时间");
        }
        if (startAt.isBefore(now)) {
            throw BusinessException.validation("不能改约到过去的时间");
        }

        if("CUSTOMER_SLOT".equals(order.getVisitPolicy()))com.fixmate.service.common.VisitSchedulingService.validate(startAt,endAt,now);
        // 改约是工单聚合上的写操作：先原子推进工单版本，再写预约，
        // 这样并发改约只有第一个能通过版本校验，不会插入多条预约。
        int expectedVersion = dto.getExpectedWorkOrderVersion();
        var orderUpdate = new UpdateWrapper<WorkOrderEntity>()
                .eq("id", workOrderId)
                .eq("version", expectedVersion)
                .set("version", expectedVersion + 1)
                .set("updated_at", now);
        if("CUSTOMER_SLOT".equals(order.getVisitPolicy()))orderUpdate.set("preferred_visit_start_at",startAt).set("preferred_visit_end_at",endAt);
        int orderRows=workOrderMapper.update(null,orderUpdate);
        if (orderRows == 0) {
            throw BusinessException.versionConflict("工单已被更新，请刷新后重试");
        }
        AppointmentEntity appointment = appointmentMapper.selectOne(
                new LambdaQueryWrapper<AppointmentEntity>()
                        .eq(AppointmentEntity::getWorkOrderId, workOrderId)
                        .orderByDesc(AppointmentEntity::getId)
                        .last("limit 1 FOR UPDATE"));

        if (appointment == null) {
            AppointmentEntity created = new AppointmentEntity();
            created.setWorkOrderId(workOrderId);
            created.setStatus("RESCHEDULE_REQUESTED");
            created.setScheduledStartAt(startAt);
            created.setScheduledEndAt(endAt);
            created.setNote(dto.getReason());
            created.setVersion(0);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            try {
                appointmentMapper.insert(created);
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                throw BusinessException.stateConflict("该工单已有当前预约，请刷新后重试");
            }
            appointment = created;
        } else {
            // 仅已成立的预约产生原时段保留；连续改约请求保留第一次的原预约。
            if ("CONFIRMED".equals(appointment.getStatus())) {
                appointment.setPreviousConfirmedStartAt(appointment.getScheduledStartAt());
                appointment.setPreviousConfirmedEndAt(appointment.getScheduledEndAt());
            }
            int rows;
            try {
                rows = appointmentMapper.update(null, new UpdateWrapper<AppointmentEntity>()
                        .eq("id", appointment.getId())
                        .eq("version", appointment.getVersion())
                        .set("status", "RESCHEDULE_REQUESTED")
                        .set("previous_confirmed_start_at", appointment.getPreviousConfirmedStartAt())
                        .set("previous_confirmed_end_at", appointment.getPreviousConfirmedEndAt())
                        .set("scheduled_start_at", startAt)
                        .set("scheduled_end_at", endAt)
                        .set("note", dto.getReason())
                        .set("version", appointment.getVersion() + 1)
                        .set("updated_at", now));
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                throw BusinessException.stateConflict("该工单已有当前预约，请刷新后重试");
            }
            if (rows == 0) {
                throw BusinessException.versionConflict("预约已被更新，请刷新后重试");
            }
            appointment.setStatus("RESCHEDULE_REQUESTED");
            appointment.setScheduledStartAt(startAt);
            appointment.setScheduledEndAt(endAt);
            appointment.setNote(dto.getReason());
            appointment.setVersion(appointment.getVersion() + 1);
        }

        auditLogService.recordCustomer(userId, "APPOINTMENT_RESCHEDULE_REQUESTED", "WORK_ORDER", workOrderId,
                null, Map.of("appointmentId", appointment.getId(),
                        "startAt", startAt.toString(),
                        "endAt", endAt.toString(),
                        "reason", dto.getReason()));
        return toVO(appointment);
    }

    private AppointmentVO toVO(AppointmentEntity appointment) {
        AppointmentVO vo = new AppointmentVO();
        vo.setId(appointment.getId());
        vo.setStatus(appointment.getStatus());
        vo.setScheduledStartAt(appointment.getScheduledStartAt());
        vo.setScheduledEndAt(appointment.getScheduledEndAt());
        vo.setNote(appointment.getNote());
        if (appointment.getTechnicianId() != null) {
            com.fixmate.entity.StaffUserEntity staff = staffUserMapper.selectById(appointment.getTechnicianId());
            vo.setTechnicianName(staff == null ? null : staff.getName());
        }
        return vo;
    }
}
