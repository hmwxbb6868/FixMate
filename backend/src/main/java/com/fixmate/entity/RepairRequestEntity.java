package com.fixmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("repair_request")
public class RepairRequestEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String serviceContactSnapshot;
    private Long manualReviewedBy;
    private LocalDateTime manualReviewedAt;
    private String manualReviewNote;
    private String repairNo;
    private Long userId;
    private String applianceType;
    private Long brandId;
    private Long modelId;
    private String modelText;
    private String serialNumber;
    private String symptomDescription;
    private String errorCodeText;
    private LocalDateTime occurredAt;
    private Long addressId;
    private String addressSnapshot;
    private String contactSnapshot;
    private String safetyAnswers;
    private String status;
    private String riskFlags;
    private String coverageStatus;
    private Long triageSessionId;
    private Long workOrderId;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepairNo() {
        return repairNo;
    }

    public void setRepairNo(String repairNo) {
        this.repairNo = repairNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getApplianceType() {
        return applianceType;
    }

    public void setApplianceType(String applianceType) {
        this.applianceType = applianceType;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getModelText() {
        return modelText;
    }

    public void setModelText(String modelText) {
        this.modelText = modelText;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSymptomDescription() {
        return symptomDescription;
    }

    public void setSymptomDescription(String symptomDescription) {
        this.symptomDescription = symptomDescription;
    }

    public String getErrorCodeText() {
        return errorCodeText;
    }

    public void setErrorCodeText(String errorCodeText) {
        this.errorCodeText = errorCodeText;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getAddressSnapshot() {
        return addressSnapshot;
    }

    public void setAddressSnapshot(String addressSnapshot) {
        this.addressSnapshot = addressSnapshot;
    }

    public String getContactSnapshot() {
        return contactSnapshot;
    }

    public void setContactSnapshot(String contactSnapshot) {
        this.contactSnapshot = contactSnapshot;
    }

    public String getSafetyAnswers() {
        return safetyAnswers;
    }

    public void setSafetyAnswers(String safetyAnswers) {
        this.safetyAnswers = safetyAnswers;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRiskFlags() {
        return riskFlags;
    }

    public void setRiskFlags(String riskFlags) {
        this.riskFlags = riskFlags;
    }

    public String getCoverageStatus() {
        return coverageStatus;
    }

    public void setCoverageStatus(String coverageStatus) {
        this.coverageStatus = coverageStatus;
    }

    public Long getTriageSessionId() {
        return triageSessionId;
    }

    public void setTriageSessionId(Long triageSessionId) {
        this.triageSessionId = triageSessionId;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
    public String getServiceContactSnapshot(){return serviceContactSnapshot;}
    public void setServiceContactSnapshot(String value){serviceContactSnapshot=value;}
    public Long getManualReviewedBy(){return manualReviewedBy;}
    public void setManualReviewedBy(Long value){manualReviewedBy=value;}
    public LocalDateTime getManualReviewedAt(){return manualReviewedAt;}
    public void setManualReviewedAt(LocalDateTime value){manualReviewedAt=value;}
    public String getManualReviewNote(){return manualReviewNote;}
    public void setManualReviewNote(String value){manualReviewNote=value;}
}
