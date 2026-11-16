package com.fixmate.dto.user;

import java.math.BigDecimal;

public class QuoteItemVO {

    private Long id;
    private String feeType;
    private String itemName;
    private String specification;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String pricingClauseNo;
    private String pricingClauseTitle;
    private String pricingRuleVersion;
    private String pricingBasisSnapshot;
    private String partSourceNote;
    private String additionReason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
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
}
