package com.fixmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("quote_item")
public class QuoteItemEntity {

    private String pricingReason;
    public String getPricingReason() { return pricingReason; }
    public void setPricingReason(String value) { this.pricingReason = value; }
    private String pricingRuleFingerprint;
    public String getPricingRuleFingerprint() { return pricingRuleFingerprint; }
    public void setPricingRuleFingerprint(String value) { this.pricingRuleFingerprint = value; }
    private Integer contentRevision;
    public Integer getContentRevision() { return contentRevision; }
    public void setContentRevision(Integer value) { this.contentRevision = value; }
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long quoteId;
    private String feeType;
    private Long serviceItemId;
    private String itemName;
    private String specification;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal amount;
    private Long pricingRuleId;
    private String pricingClauseNo;
    private String pricingClauseTitle;
    private String pricingRuleVersion;
    private String pricingBasisSnapshot;
    private String partSourceNote;
    private String additionReason;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public Long getServiceItemId() {
        return serviceItemId;
    }

    public void setServiceItemId(Long serviceItemId) {
        this.serviceItemId = serviceItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getPricingRuleId() {
        return pricingRuleId;
    }

    public void setPricingRuleId(Long pricingRuleId) {
        this.pricingRuleId = pricingRuleId;
    }

    public String getPricingClauseNo() {
        return pricingClauseNo;
    }

    public void setPricingClauseNo(String pricingClauseNo) {
        this.pricingClauseNo = pricingClauseNo;
    }

    public String getPricingClauseTitle() {
        return pricingClauseTitle;
    }

    public void setPricingClauseTitle(String pricingClauseTitle) {
        this.pricingClauseTitle = pricingClauseTitle;
    }

    public String getPricingRuleVersion() {
        return pricingRuleVersion;
    }

    public void setPricingRuleVersion(String pricingRuleVersion) {
        this.pricingRuleVersion = pricingRuleVersion;
    }

    public String getPricingBasisSnapshot() {
        return pricingBasisSnapshot;
    }

    public void setPricingBasisSnapshot(String pricingBasisSnapshot) {
        this.pricingBasisSnapshot = pricingBasisSnapshot;
    }

    public String getPartSourceNote() {
        return partSourceNote;
    }

    public void setPartSourceNote(String partSourceNote) {
        this.partSourceNote = partSourceNote;
    }

    public String getAdditionReason() {
        return additionReason;
    }

    public void setAdditionReason(String additionReason) {
        this.additionReason = additionReason;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
