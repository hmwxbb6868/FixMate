package com.fixmate.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fixmate.entity.AppointmentEntity;
import com.fixmate.mapper.AppointmentMapper;
import com.fixmate.service.common.TimeoutTransitionService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 爽约扫描：每 10 分钟一次，逐条交给事务服务处理。
 */
@Component
public class AppointmentMissedTask {

    private static final Logger log = LoggerFactory.getLogger(AppointmentMissedTask.class);
    private static final long MISSED_GRACE_HOURS = 2;

    private final AppointmentMapper appointmentMapper;
    private final TimeoutTransitionService timeoutTransitionService;

    public AppointmentMissedTask(AppointmentMapper appointmentMapper,
                                 TimeoutTransitionService timeoutTransitionService) {
        this.appointmentMapper = appointmentMapper;
        this.timeoutTransitionService = timeoutTransitionService;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT10M")
    public void markMissedAppointments() {
        LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC).minusHours(MISSED_GRACE_HOURS);
        List<AppointmentEntity> candidates = appointmentMapper.selectList(
                new LambdaQueryWrapper<AppointmentEntity>()
                        .eq(AppointmentEntity::getStatus, "CONFIRMED")
                        .le(AppointmentEntity::getScheduledEndAt, threshold));
        for (AppointmentEntity appointment : candidates) {
            try {
                timeoutTransitionService.markMissed(appointment.getId());
            } catch (RuntimeException ex) {
                log.warn("mark missed failed for appointment {}", appointment.getId(), ex);
            }
        }
    }
}
