package com.fixmate.config;

import com.fixmate.common.idempotency.IdempotencyService;
import com.fixmate.mapper.DatabaseClockMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 随用户启动应用执行；无需读取/输出密钥或SQL手工处理已完成回执。 */
@Component
public class StaffSensitiveResponseUpgrade {
    private final IdempotencyService service;private final DatabaseClockMapper clock;private final JwtProperties jwt;
    public StaffSensitiveResponseUpgrade(IdempotencyService service,DatabaseClockMapper clock,JwtProperties jwt){this.service=service;this.clock=clock;this.jwt=jwt;}
    @EventListener(ApplicationReadyEvent.class)
    public void upgrade(){while(service.upgradeLegacyStaffResponses(clock.now(),jwt.getAccessTtlSeconds())>0){/* 每批独立事务，可恢复。 */}}
}
