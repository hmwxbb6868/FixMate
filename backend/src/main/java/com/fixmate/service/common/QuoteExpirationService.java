package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.entity.QuoteEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.mapper.DbTimeMapper;
import com.fixmate.mapper.QuoteMapper;
import com.fixmate.mapper.WorkOrderMapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报价过期：报价置 EXPIRED 与工单回退必须在同一事务内完成。
 * 过期任务不得自动接受、恢复维修或关闭工单。
 *
 * 工单不处于待确认状态时不得把报价单独标成 EXPIRED（那会留下"已过期但工单还在等确认"的死状态），
 * 这种情况只写异常审计，留待人工处理。
 */
@Service
public class QuoteExpirationService {

    private static final Set<String> EXPIRABLE_ORDER_STATUS =
            Set.of("WAITING_QUOTE_CONFIRMATION", "PAUSED_FOR_REQUOTE");

    private final QuoteMapper quoteMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderStateMachine stateMachine;
    private final AuditLogService auditLogService;
    private final DbTimeMapper dbTimeMapper;

    public QuoteExpirationService(QuoteMapper quoteMapper,
                                  WorkOrderMapper workOrderMapper,
                                  WorkOrderStateMachine stateMachine,
                                  AuditLogService auditLogService,
                                  DbTimeMapper dbTimeMapper) {
        this.quoteMapper = quoteMapper;
        this.workOrderMapper = workOrderMapper;
        this.stateMachine = stateMachine;
        this.auditLogService = auditLogService;
        this.dbTimeMapper = dbTimeMapper;
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void expireOne(Long quoteId) {
        LocalDateTime now = dbTimeMapper.utcNow();
        QuoteEntity quote = quoteMapper.selectById(quoteId);
        if (quote == null || !"SUBMITTED".equals(quote.getStatus()) || quote.getExpiresAt() == null
                || quote.getExpiresAt().isAfter(now)) {
            return;
        }

        WorkOrderEntity order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,quote.getWorkOrderId()).last("FOR UPDATE"));
        quote=quoteMapper.selectOne(new LambdaQueryWrapper<QuoteEntity>().eq(QuoteEntity::getId,quoteId).last("FOR UPDATE"));
        now=dbTimeMapper.utcNow();
        if(quote==null||!"SUBMITTED".equals(quote.getStatus())||quote.getExpiresAt()==null||quote.getExpiresAt().isAfter(now))return;
        if (order == null || !EXPIRABLE_ORDER_STATUS.contains(order.getStatus())) {
            auditLogService.recordSystem("QUOTE_EXPIRATION_SKIPPED", "QUOTE", quote.getId(),
                    Map.of("quoteStatus", quote.getStatus(),
                            "expiresAt", String.valueOf(quote.getExpiresAt())),
                    Map.of("orderStatus", order == null ? "MISSING" : order.getStatus(),
                            "reason", "工单不在待确认状态，未标记过期"));
            return;
        }

        int rows = quoteMapper.update(null, new UpdateWrapper<QuoteEntity>()
                .eq("id", quote.getId())
                .eq("status", "SUBMITTED")
                .eq("version", quote.getVersion())
                .set("status", "EXPIRED")
                .set("version", quote.getVersion() + 1)
                .set("updated_at", now));
        if (rows == 0) {
            return;
        }

        stateMachine.transition(order.getId(), WorkOrderStatus.WAITING_QUOTE, order.getVersion(),
                "报价 " + quote.getQuoteNo() + " 已过期，等待重报", "SYSTEM", null);

        auditLogService.recordSystem("QUOTE_EXPIRED", "QUOTE", quote.getId(),
                Map.of("status", "SUBMITTED"), Map.of("status", "EXPIRED"));
    }

    public java.util.List<QuoteEntity> findExpiredCandidates() {
        LocalDateTime now = dbTimeMapper.utcNow();
        return quoteMapper.selectList(new LambdaQueryWrapper<QuoteEntity>()
                .eq(QuoteEntity::getStatus, "SUBMITTED")
                .isNotNull(QuoteEntity::getExpiresAt)
                .le(QuoteEntity::getExpiresAt, now));
    }
}
