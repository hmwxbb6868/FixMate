package com.fixmate.dto.user;

import java.time.LocalDateTime;
import java.util.List;

public class RepairRequestVO {

    private Long id;
    private String repairNo;
    private String status;
    private String applianceType;
    private Long brandId;
    private String brandCode;
    private Long modelId;
    private String modelCode;
    private String modelText;
    private String serialNumber;
    private String symptomDescription;
    private String errorCodeText;
    private LocalDateTime occurredAt;
    private List<Long> mediaFileIds;
    private List<FileAssetVO> mediaFiles;
    private SafetyAnswersDTO safetyAnswers;
    private Long addressId;
    private String coverageStatus;
    private Long workOrderId;
    private List<String> allowedActions;
    private Integer version;
    private LocalDateTime createdAt;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getBrandCode() {
        return brandCode;
    }

    public void setBrandCode(String brandCode) {
        this.brandCode = brandCode;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
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

    public List<Long> getMediaFileIds() {
        return mediaFileIds;
    }

    public void setMediaFileIds(List<Long> mediaFileIds) {
        this.mediaFileIds = mediaFileIds;
    }

    public List<FileAssetVO> getMediaFiles() {
        return mediaFiles;
    }

    public void setMediaFiles(List<FileAssetVO> mediaFiles) {
        this.mediaFiles = mediaFiles;
    }

    public SafetyAnswersDTO getSafetyAnswers() {
        return safetyAnswers;
    }

    public void setSafetyAnswers(SafetyAnswersDTO safetyAnswers) {
        this.safetyAnswers = safetyAnswers;
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getCoverageStatus() {
        return coverageStatus;
    }

    public void setCoverageStatus(String coverageStatus) {
        this.coverageStatus = coverageStatus;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public List<String> getAllowedActions() {
        return allowedActions;
    }

    public void setAllowedActions(List<String> allowedActions) {
        this.allowedActions = allowedActions;
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
}
