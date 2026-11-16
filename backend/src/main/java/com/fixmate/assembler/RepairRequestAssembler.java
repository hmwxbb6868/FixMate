package com.fixmate.assembler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.dto.user.FileAssetVO;
import com.fixmate.dto.user.RepairRequestVO;
import com.fixmate.dto.user.SafetyAnswersDTO;
import com.fixmate.dto.user.SafetyBlockVO;
import com.fixmate.entity.ApplianceBrandEntity;
import com.fixmate.entity.ApplianceModelEntity;
import com.fixmate.entity.RepairRequestEntity;
import com.fixmate.entity.SafetyEventEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.service.common.SafetyRuleEngine;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RepairRequestAssembler {

    private final ObjectMapper objectMapper;

    public RepairRequestAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RepairRequestVO toVO(RepairRequestEntity entity, ApplianceBrandEntity brand, ApplianceModelEntity model,
                                List<Long> mediaFileIds, List<FileAssetVO> mediaFiles) {
        RepairRequestVO vo = new RepairRequestVO();
        vo.setId(entity.getId());
        vo.setRepairNo(entity.getRepairNo());
        vo.setStatus(entity.getStatus());
        vo.setApplianceType(entity.getApplianceType());
        vo.setBrandId(entity.getBrandId());
        vo.setBrandCode(brand == null ? null : brand.getCode());
        vo.setModelId(entity.getModelId());
        vo.setModelCode(model == null ? null : model.getModelCode());
        vo.setModelText(entity.getModelText());
        vo.setSerialNumber(entity.getSerialNumber());
        vo.setSymptomDescription(entity.getSymptomDescription());
        vo.setErrorCodeText(entity.getErrorCodeText());
        vo.setOccurredAt(entity.getOccurredAt());
        vo.setMediaFileIds(mediaFileIds);
        vo.setMediaFiles(mediaFiles);
        vo.setSafetyAnswers(parseSafety(entity.getSafetyAnswers()));
        vo.setAddressId(entity.getAddressId());
        vo.setCoverageStatus(entity.getCoverageStatus());
        vo.setWorkOrderId(entity.getWorkOrderId());
        vo.setAllowedActions(allowedActions(entity.getStatus()));
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    /** 阶段 1 不返回 CONVERT_TO_WORK_ORDER，页面不会出现该入口。 */
    private List<String> allowedActions(String status) {
        List<String> actions = new ArrayList<>();
        if (status == null) {
            return actions;
        }
        switch (status) {
            case "DRAFT" -> {
                actions.add("EDIT_DRAFT");
                actions.add("SUBMIT");
                actions.add("CANCEL");
            }
            case "SUBMITTED", "TRIAGED", "HUMAN_HANDOFF", "COLLECTING" -> {
                actions.add("CANCEL");
                actions.add("CONTACT_HUMAN");
            }
            case "HIGH_RISK_BLOCKED" -> {
                actions.add("VIEW_WORK_ORDER");
                actions.add("CONTACT_HUMAN");
            }
            default -> {
            }
        }
        return actions;
    }

    public SafetyBlockVO toBlock(SafetyEventEntity event, WorkOrderEntity order,
                                 List<SafetyRuleEngine.MatchedRule> rules) {
        SafetyBlockVO vo = new SafetyBlockVO();
        vo.setSafetyEventId(event.getId());
        vo.setMatchedRules(rules.stream()
                .map(r -> new SafetyBlockVO.MatchedRuleVO(r.ruleCode(), r.title(), r.templateText()))
                .toList());
        vo.setEmergencyWorkOrderId(order.getId());
        vo.setEmergencyWorkOrderNo(order.getWorkOrderNo());
        vo.setCreatedAt(event.getCreatedAt());
        return vo;
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("json serialize failed", ex);
        }
    }

    private SafetyAnswersDTO parseSafety(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<SafetyAnswersDTO>() {
            });
        } catch (Exception ex) {
            return null;
        }
    }
}
