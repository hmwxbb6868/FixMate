package com.fixmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("quote")
public class QuoteEntity {

    private Long technicianId;
    public Long getTechnicianId() { return technicianId; }
    public void setTechnicianId(Long value) { this.technicianId = value; }
    private Integer itemRevision;
    public Integer getItemRevision() { return itemRevision; }
    public void setItemRevision(Integer value) { this.itemRevision = value; }
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String quoteNo;
    private Long workOrderId;
    private String quoteType;
    private Integer versionNo;
    private Long previousQuoteId;
    private Long baseQuoteId;
    private String status;
    private String currency;
    private BigDecimal subtotalAmount;
    private BigDecimal confirmedAmountBefore;
    private BigDecimal totalAmount;
    private String changeReason;
    private String submissionSource;
    private Long serviceRecordId;
    private String acceptedViewSnapshot;
    private Long createdBy;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private LocalDateTime userDecidedAt;
    private String decisionReason;
    private LocalDateTime expiresAt;
    private Integer version;
    // V3生成列：保留SELECT映射，任何实体INSERT/UPDATE均由数据库计算。
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Long submittedWorkOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getSubmissionSource() {
        return submissionSource;
    }

    public void setSubmissionSource(String submissionSource) {
        this.submissionSource = submissionSource;
    }

    public Long getServiceRecordId() {
        return serviceRecordId;
    }

    public void setServiceRecordId(Long serviceRecordId) {
        this.serviceRecordId = serviceRecordId;
    }

    public String getAcceptedViewSnapshot() {
        return acceptedViewSnapshot;
    }

    public void setAcceptedViewSnapshot(String acceptedViewSnapshot) {
        this.acceptedViewSnapshot = acceptedViewSnapshot;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Long submittedBy) {
        this.submittedBy = submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getUserDecidedAt() {
        return userDecidedAt;
    }

    public void setUserDecidedAt(LocalDateTime userDecidedAt) {
        this.userDecidedAt = userDecidedAt;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getSubmittedWorkOrderId() {
        return submittedWorkOrderId;
    }

    public void setSubmittedWorkOrderId(Long submittedWorkOrderId) {
        this.submittedWorkOrderId = submittedWorkOrderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
