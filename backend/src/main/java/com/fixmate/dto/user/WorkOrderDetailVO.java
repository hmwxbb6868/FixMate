package com.fixmate.dto.user;

import java.time.LocalDateTime;
import java.util.List;

public record WorkOrderDetailVO(
        Long id,
        String workOrderNo,
        String status,
        String applianceType,
        String brandCode,
        String modelCode,
        String symptomDescription,
        String errorCodeText,
        LocalDateTime occurredAt,
        List<FileAssetVO> mediaFiles,
        String contactName,
        String contactPhone,
        String addressText,
        AppointmentVO appointment,
        String technicianName,
        QuoteVO currentQuote,
        List<QuoteVO> quoteHistory,
        List<ServiceRecordVO> serviceRecords,
        AcceptanceVO acceptance,
        List<TimelineEventVO> timeline,
        List<String> allowedActions,
        Integer version,
        String cancellationFeeAmount) {
}
