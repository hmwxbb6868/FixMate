package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.assembler.QuoteAssembler;
import com.fixmate.common.api.ErrorCode;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.dto.user.QuoteAcceptDTO;
import com.fixmate.dto.user.QuoteRejectDTO;
import com.fixmate.dto.user.QuoteVO;
import com.fixmate.entity.QuoteEntity;
import com.fixmate.entity.QuoteItemEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.QuoteItemMapper;
import com.fixmate.mapper.QuoteMapper;
import com.fixmate.mapper.WorkOrderMapper;
import com.fixmate.service.common.PricingCalculationService;
import com.fixmate.service.common.WorkOrderStateMachine;
import com.fixmate.service.user.UserQuoteService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserQuoteServiceImpl implements UserQuoteService {

    private static final Set<String> CONFIRMABLE_ORDER_STATUS =
            Set.of("WAITING_QUOTE_CONFIRMATION", "PAUSED_FOR_REQUOTE");

    private final com.fixmate.mapper.DbTimeMapper clock;
    private final QuoteMapper quoteMapper;
    private final QuoteItemMapper quoteItemMapper;
    private final WorkOrderMapper workOrderMapper;
    private final QuoteAssembler quoteAssembler;
    private final PricingCalculationService pricingCalculationService;
    private final WorkOrderStateMachine stateMachine;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public UserQuoteServiceImpl(QuoteMapper quoteMapper,
                                QuoteItemMapper quoteItemMapper,
                                WorkOrderMapper workOrderMapper,
                                QuoteAssembler quoteAssembler,
                                PricingCalculationService pricingCalculationService,
                                WorkOrderStateMachine stateMachine,
                                AuditLogService auditLogService,
                                ObjectMapper objectMapper, com.fixmate.mapper.DbTimeMapper clock) {
        this.clock = clock;
        this.quoteMapper = quoteMapper;
        this.quoteItemMapper = quoteItemMapper;
        this.workOrderMapper = workOrderMapper;
        this.quoteAssembler = quoteAssembler;
        this.pricingCalculationService = pricingCalculationService;
        this.stateMachine = stateMachine;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public QuoteVO accept(Long userId, Long quoteId, QuoteAcceptDTO dto) {
        QuoteEntity quote = requireConfirmable(userId, quoteId, dto.getExpectedQuoteVersion());
        QuoteVO snapshot = quoteAssembler.toVO(quote);

        int rows = quoteMapper.update(null, new UpdateWrapper<QuoteEntity>()
                .eq("id", quote.getId())
                .eq("version", quote.getVersion())
                .eq("status", "SUBMITTED")
                .set("status", "ACCEPTED")
                .set("user_decided_at", LocalDateTime.now(ZoneOffset.UTC))
                .set("accepted_view_snapshot", toJson(snapshot))
                .set("version", quote.getVersion() + 1)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
        if (rows == 0) {
            throw BusinessException.versionConflict("报价已被更新，请刷新后重试");
        }

        stateMachine.transition(quote.getWorkOrderId(), WorkOrderStatus.IN_REPAIR,
                null, "用户接受报价 " + quote.getQuoteNo(), "CUSTOMER", userId);

        auditLogService.recordCustomer(userId, "QUOTE_ACCEPTED", "QUOTE", quote.getId(),
                Map.of("status", "SUBMITTED", "versionNo", quote.getVersionNo()),
                Map.of("status", "ACCEPTED", "payableTotalAfterAcceptance",
                        quote.getTotalAmount().toPlainString()));

        return quoteAssembler.toVO(quoteMapper.selectById(quote.getId()));
    }

    @Override
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public QuoteVO reject(Long userId, Long quoteId, QuoteRejectDTO dto) {
        QuoteEntity quote = requireConfirmable(userId, quoteId, dto.getExpectedQuoteVersion());

        int rows = quoteMapper.update(null, new UpdateWrapper<QuoteEntity>()
                .eq("id", quote.getId())
                .eq("version", quote.getVersion())
                .eq("status", "SUBMITTED")
                .set("status", "REJECTED")
                .set("user_decided_at", LocalDateTime.now(ZoneOffset.UTC))
                .set("decision_reason", dto.getReason())
                .set("version", quote.getVersion() + 1)
                .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
        if (rows == 0) {
            throw BusinessException.versionConflict("报价已被更新，请刷新后重试");
        }

        stateMachine.transition(quote.getWorkOrderId(), WorkOrderStatus.WAITING_MANUAL_REVIEW,
                null, "用户拒绝报价：" + dto.getReason(), "CUSTOMER", userId);

        auditLogService.recordCustomer(userId, "QUOTE_REJECTED", "QUOTE", quote.getId(),
                Map.of("status", "SUBMITTED", "versionNo", quote.getVersionNo()),
                Map.of("status", "REJECTED", "reason", dto.getReason()));

        return quoteAssembler.toVO(quoteMapper.selectById(quote.getId()));
    }

    private QuoteEntity requireConfirmable(Long userId, Long quoteId, Integer expectedVersion) {
        QuoteEntity quote = quoteMapper.selectById(quoteId);
        if (quote == null) {
            throw BusinessException.notFound("报价不存在");
        }
        WorkOrderEntity order = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,quote.getWorkOrderId()).last("FOR UPDATE"));
        quote = quoteMapper.selectOne(new LambdaQueryWrapper<QuoteEntity>().eq(QuoteEntity::getId,quoteId).last("FOR UPDATE"));
        if(quote==null)throw BusinessException.notFound("报价不存在");
        if (order == null) {
            throw BusinessException.notFound("工单不存在");
        }
        if (!order.getCustomerId().equals(userId)) {
            throw BusinessException.forbidden("无权操作该报价");
        }
        if (!"SUBMITTED".equals(quote.getStatus())) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                    "该报价不是当前有效版本");
        }
        if (!quote.getVersionNo().equals(expectedVersion)) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                    "报价版本已变化，请刷新后重试");
        }
        if (quote.getExpiresAt() != null && !quote.getExpiresAt().isAfter(clock.utcNow())) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT, "报价已过期");
        }
        if (!CONFIRMABLE_ORDER_STATUS.contains(order.getStatus())) {
            throw BusinessException.stateConflict("当前工单状态不支持确认报价");
        }
        validateIntegrity(quote);
        return quote;
    }

    /**
     * 接受/拒绝前重新核算金额并校验版本链。
     * 历史链、金额或当前唯一性与数据库记录不一致时一律拒绝，不信任客户端与既有数据。
     */
    private void validateIntegrity(QuoteEntity quote) {
        Long others = quoteMapper.selectCount(new LambdaQueryWrapper<QuoteEntity>()
                .eq(QuoteEntity::getWorkOrderId, quote.getWorkOrderId())
                .eq(QuoteEntity::getStatus, "SUBMITTED")
                .ne(QuoteEntity::getId, quote.getId()));
        if (others != null && others > 0) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                    "该工单存在其它待确认报价，请联系客服");
        }

        List<QuoteItemEntity> items = quoteItemMapper.selectList(new LambdaQueryWrapper<QuoteItemEntity>()
                .eq(QuoteItemEntity::getQuoteId, quote.getId())
                .eq(QuoteItemEntity::getContentRevision, quote.getItemRevision() == null ? 0 : quote.getItemRevision()));
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT, "报价缺少行项目");
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (QuoteItemEntity item : items) {
            if (item.getUnitPrice() == null || item.getQuantity() == null || item.getAmount() == null) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "报价行项目金额缺失");
            }
            BigDecimal expected;
            try {
                expected = pricingCalculationService.lineAmount(item.getUnitPrice(), item.getQuantity());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "报价行项目数量非法");
            }
            if (expected.compareTo(item.getAmount()) != 0) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "报价行项目金额与单价数量不符");
            }
            // 冻结规则 10.3：配件费必须记录来源说明，不能作为无说明兜底
            if ("PART".equals(item.getFeeType())
                    && (item.getPartSourceNote() == null || item.getPartSourceNote().isBlank())) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "配件费用必须填写来源说明");
            }
            sum = sum.add(item.getAmount());
        }
        BigDecimal subtotal = pricingCalculationService.subtotal(List.of(sum));
        if (quote.getSubtotalAmount() == null || subtotal.compareTo(quote.getSubtotalAmount()) != 0) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                    "报价合计与行项目不一致");
        }

        BigDecimal confirmedBefore;
        if ("INITIAL".equals(quote.getQuoteType())) {
            if (quote.getBaseQuoteId() != null) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "初始报价不应有基准报价");
            }
            confirmedBefore = BigDecimal.ZERO;
        } else {
            if (quote.getBaseQuoteId() == null) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "追加报价缺少基准报价");
            }
            QuoteEntity base = quoteMapper.selectById(quote.getBaseQuoteId());
            if (base == null || !"ACCEPTED".equals(base.getStatus())
                    || !base.getWorkOrderId().equals(quote.getWorkOrderId())) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "基准报价无效");
            }
            QuoteEntity latestAccepted = latestAccepted(quote.getWorkOrderId());
            if (latestAccepted == null || !latestAccepted.getId().equals(base.getId())) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "基准报价不是该工单最近一份已接受的报价");
            }
            confirmedBefore = base.getTotalAmount();
        }

        if (quote.getConfirmedAmountBefore() == null
                || quote.getConfirmedAmountBefore().compareTo(confirmedBefore) != 0) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                    "此前已确认金额不一致");
        }
        BigDecimal expectedTotal = pricingCalculationService.total(confirmedBefore, subtotal);
        if (quote.getTotalAmount() == null || quote.getTotalAmount().compareTo(expectedTotal) != 0) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                    "累计应付金额不一致");
        }

        validateVersionChain(quote);
    }

    /** 冻结规则 10.5：previousQuoteId 必须逐版本连续、同属一个工单，且不能成环。 */
    private void validateVersionChain(QuoteEntity quote) {
        if (quote.getVersionNo() == null) {
            throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT, "报价版本号缺失");
        }
        if (quote.getVersionNo() == 1) {
            if (quote.getPreviousQuoteId() != null) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "首个版本不应存在上一版报价");
            }
            return;
        }
        java.util.Set<Long> visited = new java.util.HashSet<>();
        QuoteEntity cursor = quote;
        int expectedVersionNo = quote.getVersionNo();
        while (expectedVersionNo > 1) {
            if (cursor.getPreviousQuoteId() == null || !visited.add(cursor.getPreviousQuoteId())) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "版本链缺失或成环");
            }
            QuoteEntity previous = quoteMapper.selectById(cursor.getPreviousQuoteId());
            if (previous == null || !previous.getWorkOrderId().equals(quote.getWorkOrderId())) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "版本链不连续");
            }
            if (previous.getVersionNo() == null || previous.getVersionNo() != expectedVersionNo - 1) {
                throw new BusinessException(ErrorCode.Codes.QUOTE_NOT_CURRENT, HttpStatus.CONFLICT,
                        "版本链不连续");
            }
            cursor = previous;
            expectedVersionNo--;
        }
    }

    private QuoteEntity latestAccepted(Long workOrderId) {
        return quoteMapper.selectList(new LambdaQueryWrapper<QuoteEntity>()
                        .eq(QuoteEntity::getWorkOrderId, workOrderId)
                        .eq(QuoteEntity::getStatus, "ACCEPTED")
                        .orderByDesc(QuoteEntity::getVersionNo)
                        .last("limit 1"))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("snapshot serialize failed", ex);
        }
    }
}
