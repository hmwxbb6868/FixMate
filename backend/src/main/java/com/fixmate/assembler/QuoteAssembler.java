package com.fixmate.assembler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fixmate.dto.user.QuoteItemVO;
import com.fixmate.dto.user.QuoteVO;
import com.fixmate.entity.QuoteEntity;
import com.fixmate.entity.QuoteItemEntity;
import com.fixmate.mapper.QuoteItemMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuoteAssembler {

    private final QuoteItemMapper quoteItemMapper;

    public QuoteAssembler(QuoteItemMapper quoteItemMapper) {
        this.quoteItemMapper = quoteItemMapper;
    }

    public QuoteVO toVO(QuoteEntity quote) {
        QuoteVO vo = new QuoteVO();
        vo.setId(quote.getId());
        vo.setQuoteNo(quote.getQuoteNo());
        vo.setQuoteType(quote.getQuoteType());
        vo.setVersionNo(quote.getVersionNo());
        vo.setVersion(quote.getVersion());
        vo.setPreviousQuoteId(quote.getPreviousQuoteId());
        vo.setBaseQuoteId(quote.getBaseQuoteId());
        vo.setStatus(quote.getStatus());
        vo.setCurrency(quote.getCurrency());
        vo.setSubtotalAmount(quote.getSubtotalAmount());
        vo.setConfirmedAmountBefore(quote.getConfirmedAmountBefore());
        vo.setCurrentAdditionalAmount(quote.getSubtotalAmount());
        vo.setPayableTotalAfterAcceptance(quote.getTotalAmount());
        vo.setChangeReason(quote.getChangeReason());
        vo.setExpiresAt(quote.getExpiresAt());
        vo.setSubmittedAt(quote.getSubmittedAt());
        vo.setItems(items(quote.getId(), quote.getItemRevision()));
        return vo;
    }

    private List<QuoteItemVO> items(Long quoteId, Integer revision) {
        return quoteItemMapper.selectList(new LambdaQueryWrapper<QuoteItemEntity>()
                        .eq(QuoteItemEntity::getQuoteId, quoteId)
                        .eq(QuoteItemEntity::getContentRevision, revision == null ? 0 : revision)
                        .orderByAsc(QuoteItemEntity::getSortOrder))
                .stream()
                .map(this::toItemVO)
                .toList();
    }

    private QuoteItemVO toItemVO(QuoteItemEntity item) {
        QuoteItemVO vo = new QuoteItemVO();
        vo.setId(item.getId());
        vo.setFeeType(item.getFeeType());
        vo.setItemName(item.getItemName());
        vo.setSpecification(item.getSpecification());
        vo.setUnit(item.getUnit());
        vo.setUnitPrice(item.getUnitPrice());
        vo.setQuantity(item.getQuantity());
        vo.setAmount(item.getAmount());
        vo.setPricingClauseNo(item.getPricingClauseNo());
        vo.setPricingClauseTitle(item.getPricingClauseTitle());
        vo.setPricingRuleVersion(item.getPricingRuleVersion());
        vo.setPricingBasisSnapshot(item.getPricingBasisSnapshot());
        vo.setPartSourceNote(item.getPartSourceNote());
        vo.setAdditionReason(item.getAdditionReason());
        return vo;
    }
}
