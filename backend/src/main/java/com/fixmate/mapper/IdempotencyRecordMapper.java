package com.fixmate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fixmate.entity.IdempotencyRecordEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecordEntity> {

    /**
     * 原子认领已过期记录：只有 expires_at 仍早于当前时间的那一次 UPDATE 能成功（条件更新 + 行锁），
     * 避免并发下"先删后插"造成的重复执行窗口。
     */
    @Update("""
            UPDATE idempotency_record
               SET status = 'PENDING',
                   family_id = #{familyId},
                   operation = #{operation},
                   request_hash = #{requestHash},
                   response_status = NULL,
                   response_body = NULL,
                   sensitive_flag = #{sensitiveFlag},
                   expires_at = #{expiresAt},
                   created_at = #{createdAt}
             WHERE scope_key = #{scopeKey}
               AND idempotency_key = #{idempotencyKey}
               AND expires_at < #{now}
            """)
    int reclaimExpired(@Param("scopeKey") String scopeKey,
                       @Param("idempotencyKey") String idempotencyKey,
                       @Param("familyId") String familyId,
                       @Param("operation") String operation,
                       @Param("requestHash") String requestHash,
                       @Param("sensitiveFlag") boolean sensitiveFlag,
                       @Param("expiresAt") LocalDateTime expiresAt,
                       @Param("createdAt") LocalDateTime createdAt,
                       @Param("now") LocalDateTime now);
}
