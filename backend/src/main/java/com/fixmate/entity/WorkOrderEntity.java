package com.fixmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("work_order")
public class WorkOrderEntity {
    private java.math.BigDecimal visitFeeAmount;
    private java.math.BigDecimal cancellationFeeAmount;
    private Long visitFeeTechnicianId;

    public java.math.BigDecimal getVisitFeeAmount() { return visitFeeAmount; }
    public void setVisitFeeAmount(java.math.BigDecimal amount) { visitFeeAmount = amount; }
    public java.math.BigDecimal getCancellationFeeAmount() { return cancellationFeeAmount; }
    public void setCancellationFeeAmount(java.math.BigDecimal amount) { cancellationFeeAmount = amount; }
    public Long getVisitFeeTechnicianId() { return visitFeeTechnicianId; }
    public void setVisitFeeTechnicianId(Long id) { visitFeeTechnicianId = id; }

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String workOrderNo;
    private Long repairRequestId;
    private Long customerId;
    private String status;
    private String applianceType;
    private String brandCode;
    private String modelCode;
    private String deviceSnapshot;
    private String contactSnapshot;
    private String addressSnapshot;
    private String symptomDescription;
    private String acceptanceNote;
    private LocalDateTime acceptedAt;
    private Long technicianId;
    private LocalDateTime assignedAt;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWorkOrderNo() {
        return workOrderNo;
    }

    public void setWorkOrderNo(String workOrderNo) {
        this.workOrderNo = workOrderNo;
    }

    public Long getRepairRequestId() {
        return repairRequestId;
    }

    public void setRepairRequestId(Long repairRequestId) {
        this.repairRequestId = repairRequestId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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

    public String getBrandCode() {
        return brandCode;
    }

    public void setBrandCode(String brandCode) {
        this.brandCode = brandCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getDeviceSnapshot() {
        return deviceSnapshot;
    }

    public void setDeviceSnapshot(String deviceSnapshot) {
        this.deviceSnapshot = deviceSnapshot;
    }

    public String getContactSnapshot() {
        return contactSnapshot;
    }

    public void setContactSnapshot(String contactSnapshot) {
        this.contactSnapshot = contactSnapshot;
    }

    public String getAddressSnapshot() {
        return addressSnapshot;
    }

    public void setAddressSnapshot(String addressSnapshot) {
        this.addressSnapshot = addressSnapshot;
    }

    public String getSymptomDescription() {
        return symptomDescription;
    }

    public void setSymptomDescription(String symptomDescription) {
        this.symptomDescription = symptomDescription;
    }

    public String getAcceptanceNote() {
        return acceptanceNote;
    }

    public void setAcceptanceNote(String acceptanceNote) {
        this.acceptanceNote = acceptanceNote;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
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
    private Long currentAssignmentId;
    public Long getCurrentAssignmentId(){return currentAssignmentId;}
    public void setCurrentAssignmentId(Long value){currentAssignmentId=value;}
    private Long takeoverBy;
    public Long getTakeoverBy(){return takeoverBy;}
    public void setTakeoverBy(Long value){takeoverBy=value;}
    private LocalDateTime takeoverAt;
    public LocalDateTime getTakeoverAt(){return takeoverAt;}
    public void setTakeoverAt(LocalDateTime value){takeoverAt=value;}
    private String takeoverReason;
    public String getTakeoverReason(){return takeoverReason;}
    public void setTakeoverReason(String value){takeoverReason=value;}
    private String visitPolicy;
    private LocalDateTime preferredVisitStartAt;
    private LocalDateTime preferredVisitEndAt;
    public String getVisitPolicy(){return visitPolicy==null?"LEGACY_PLATFORM":visitPolicy;}
    public void setVisitPolicy(String v){visitPolicy=v;}
    public LocalDateTime getPreferredVisitStartAt(){return preferredVisitStartAt;}
    public void setPreferredVisitStartAt(LocalDateTime v){preferredVisitStartAt=v;}
    public LocalDateTime getPreferredVisitEndAt(){return preferredVisitEndAt;}
    public void setPreferredVisitEndAt(LocalDateTime v){preferredVisitEndAt=v;}

}
