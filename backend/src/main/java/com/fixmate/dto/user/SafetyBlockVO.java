package com.fixmate.dto.user;

import java.time.LocalDateTime;
import java.util.List;

public class SafetyBlockVO {

    private Long safetyEventId;
    private List<MatchedRuleVO> matchedRules;
    private Long emergencyWorkOrderId;
    private String emergencyWorkOrderNo;
    private LocalDateTime createdAt;

    public record MatchedRuleVO(String ruleCode, String title, String templateText) {
    }

    public Long getSafetyEventId() {
        return safetyEventId;
    }

    public void setSafetyEventId(Long safetyEventId) {
        this.safetyEventId = safetyEventId;
    }

    public List<MatchedRuleVO> getMatchedRules() {
        return matchedRules;
    }

    public void setMatchedRules(List<MatchedRuleVO> matchedRules) {
        this.matchedRules = matchedRules;
    }

    public Long getEmergencyWorkOrderId() {
        return emergencyWorkOrderId;
    }

    public void setEmergencyWorkOrderId(Long emergencyWorkOrderId) {
        this.emergencyWorkOrderId = emergencyWorkOrderId;
    }

    public String getEmergencyWorkOrderNo() {
        return emergencyWorkOrderNo;
    }

    public void setEmergencyWorkOrderNo(String emergencyWorkOrderNo) {
        this.emergencyWorkOrderNo = emergencyWorkOrderNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
