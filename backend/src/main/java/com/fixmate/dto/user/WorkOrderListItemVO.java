package com.fixmate.dto.user;

import java.time.LocalDateTime;

public record WorkOrderListItemVO(
        Long id,
        String workOrderNo,
        String status,
        String applianceType,
        String brandCode,
        String modelCode,
        LocalDateTime appointmentAt,
        LocalDateTime createdAt) {
}
