package com.fixmate.dto.user;

import java.time.LocalDateTime;
import java.util.List;

/** 新记录用例只读投影，不含客户页面的 allowedActions。 */
public record RepairRequestSnapshotVO(Long id, String repairNo, String status,
        String applianceType, Long brandId, String brandCode, Long modelId, String modelCode,
        String modelText, String serialNumber, String symptomDescription, String errorCodeText,
        LocalDateTime occurredAt, List<Long> mediaFileIds, List<FileAssetVO> mediaFiles,
        SafetyAnswersDTO safetyAnswers, Long addressId, String coverageStatus, Long workOrderId,
        Integer version, LocalDateTime createdAt) {}
