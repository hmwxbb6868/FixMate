package com.fixmate.dto.user;

import java.time.LocalDateTime;

public record RepairRecordListItemVO(Long id, String repairNo, String repairStatus,
        String applianceType, String brandCode, String modelText, String symptomSummary,
        LocalDateTime createdAt, Long workOrderId, String workOrderStatus, Integer version) {}
