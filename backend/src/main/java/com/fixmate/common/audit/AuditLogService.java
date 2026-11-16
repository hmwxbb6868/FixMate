package com.fixmate.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.entity.AuditLogEntity;
import com.fixmate.mapper.AuditLogMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

/**
 * 审计日志只追加，不提供删除接口。核心写操作必须调用。
 */
@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper auditLogMapper, ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = objectMapper;
    }

    public void recordCustomer(Long userId, String action, String targetType, Object targetId,
                               Object beforeState, Object afterState) {
        record("CUSTOMER", userId, action, targetType, targetId, beforeState, afterState);
    }

    public void recordSystem(String action, String targetType, Object targetId,
                             Object beforeState, Object afterState) {
        record("SYSTEM", null, action, targetType, targetId, beforeState, afterState);
    }

    public void record(String actorType, Long actorId, String action, String targetType, Object targetId,
                       Object beforeState, Object afterState) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setActorType(actorType);
        entity.setActorId(actorId);
        entity.setAction(action);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId == null ? null : String.valueOf(targetId));
        entity.setBeforeState(toJson(beforeState));
        entity.setAfterState(toJson(afterState));
        entity.setTraceId(com.fixmate.common.trace.TraceContext.current());
        entity.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        auditLogMapper.insert(entity);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("审计元数据序列化失败", ex);
        }
    }
}
