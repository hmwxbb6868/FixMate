package com.fixmate.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("safety_event")
public class SafetyEventEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long repairRequestId;
    private String ruleCodes;
    private String matchedRules;
    private String matchedText;
    private String status;
    private Long workOrderId;
    private Long acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepairRequestId() {
        return repairRequestId;
    }

    public void setRepairRequestId(Long repairRequestId) {
        this.repairRequestId = repairRequestId;
    }

    public String getRuleCodes() {
        return ruleCodes;
    }

    public void setRuleCodes(String ruleCodes) {
        this.ruleCodes = ruleCodes;
    }

    public String getMatchedRules() {
        return matchedRules;
    }

    public void setMatchedRules(String matchedRules) {
        this.matchedRules = matchedRules;
    }

    public String getMatchedText() {
        return matchedText;
    }

    public void setMatchedText(String matchedText) {
        this.matchedText = matchedText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(Long workOrderId) {
        this.workOrderId = workOrderId;
    }

    public Long getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(Long acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
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
    private Integer version;
    public Integer getVersion(){return version;}
    public void setVersion(Integer value){version=value;}
    private String acknowledgementNote;
    public String getAcknowledgementNote(){return acknowledgementNote;}
    public void setAcknowledgementNote(String value){acknowledgementNote=value;}
    private String resolutionBasis;
    public String getResolutionBasis(){return resolutionBasis;}
    public void setResolutionBasis(String value){resolutionBasis=value;}
}
