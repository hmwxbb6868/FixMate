package com.fixmate.dto.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class QuoteVO {

    private Long id;
    private String quoteNo;
    private String quoteType;
    private Integer versionNo;
    private Integer version;
    private Long previousQuoteId;
    private Long baseQuoteId;
    private String status;
    private String currency;
    private BigDecimal subtotalAmount;
    private BigDecimal confirmedAmountBefore;
    private BigDecimal currentAdditionalAmount;
    private BigDecimal payableTotalAfterAcceptance;
    private String changeReason;
    private LocalDateTime expiresAt;
    private LocalDateTime submittedAt;
    private List<QuoteItemVO> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuoteNo() {
        return quoteNo;
    }

    public void setQuoteNo(String quoteNo) {
        this.quoteNo = quoteNo;
    }

    public String getQuoteType() {
        return quoteType;
    }

    public void setQuoteType(String quoteType) {
        this.quoteType = quoteType;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getPreviousQuoteId() {
        return previousQuoteId;
    }

    public void setPreviousQuoteId(Long previousQuoteId) {
        this.previousQuoteId = previousQuoteId;
    }

    public Long getBaseQuoteId() {
        return baseQuoteId;
    }

    public void setBaseQuoteId(Long baseQuoteId) {
        this.baseQuoteId = baseQuoteId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getConfirmedAmountBefore() {
        return confirmedAmountBefore;
    }

    public void setConfirmedAmountBefore(BigDecimal confirmedAmountBefore) {
        this.confirmedAmountBefore = confirmedAmountBefore;
    }

    public BigDecimal getCurrentAdditionalAmount() {
        return currentAdditionalAmount;
    }

    public void setCurrentAdditionalAmount(BigDecimal currentAdditionalAmount) {
        this.currentAdditionalAmount = currentAdditionalAmount;
    }

    public BigDecimal getPayableTotalAfterAcceptance() {
        return payableTotalAfterAcceptance;
    }

    public void setPayableTotalAfterAcceptance(BigDecimal payableTotalAfterAcceptance) {
        this.payableTotalAfterAcceptance = payableTotalAfterAcceptance;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<QuoteItemVO> getItems() {
        return items;
    }

    public void setItems(List<QuoteItemVO> items) {
        this.items = items;
    }
}
