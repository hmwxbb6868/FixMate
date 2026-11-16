package com.fixmate.security;

import com.fixmate.common.audit.AuditLogService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 认证限流失败也保留审计，不能随被拒绝的业务事务一起回滚。 */
@Service
public class StaffSecurityAudit {
    private final AuditLogService audit;
    public StaffSecurityAudit(AuditLogService audit){this.audit=audit;}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void rateLimited(String purpose) {
        audit.recordSystem("STAFF_RATE_LIMITED","STAFF_AUTH",null,null,Map.of("purpose",purpose));
    }
}
