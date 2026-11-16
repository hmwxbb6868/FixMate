package com.fixmate.dto.admin;

// Generated from unique OpenAPI by scripts/phase2/generate_staff_identity_dtos.py. Do not edit fields manually.
public final class StaffIdentityContracts {
    private StaffIdentityContracts() {}
    public record PreferredVisitVO(java.time.OffsetDateTime startAt,
            java.time.OffsetDateTime endAt) {}
    public record PreferredVisitDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull java.time.OffsetDateTime startAt,
            @jakarta.validation.constraints.NotNull java.time.OffsetDateTime endAt) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record PreferredVisitResultVO(Long workOrderId,
            Integer workOrderVersion,
            String visitPolicy,
            PreferredVisitVO preferredVisit,
            java.util.List<String> allowedActions) {}
    public record JobPoolPublishDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.Size(min=2,max=1000) String reason,
            @jakarta.validation.constraints.Min(0) Integer expectedAssignmentVersion) {
        public JobPoolPublishDTO(Integer expectedWorkOrderVersion,String reason) {
            this(expectedWorkOrderVersion,reason,null);
        }
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record JobPoolWithdrawDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedPublicationVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record JobPoolClaimDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedPublicationVersion) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record JobPoolEntryVO(PreferredVisitVO preferredVisit,
            String visitPolicy,
            java.util.List<String> allowedActions,
            Long id,
            Long workOrderId,
            String workOrderNo,
            String applianceType,
            String symptomSummary,
            String serviceAreaText,
            java.time.OffsetDateTime publishedAt,
            Integer version,
            Integer workOrderVersion) {}
    public record JobPoolEntryVOPageVO(java.util.List<JobPoolEntryVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record JobPoolPublicationVO(Long id,
            Long workOrderId,
            String status,
            Integer version,
            Integer workOrderVersion,
            java.time.OffsetDateTime publishedAt) {}
    public record BusinessTodoVO(Long id,
            Long workOrderId,
            String kind,
            String status,
            String reason,
            java.time.OffsetDateTime createdAt,
            Integer version,
            StaffActorVO handledBy,
            java.time.OffsetDateTime handledAt,
            String handlingConclusion) {}
    public record BusinessTodoVOPageVO(java.util.List<BusinessTodoVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record AdminFollowUpVO(String kind,
            Long targetId,
            String referenceNo,
            String waitingFor,
            java.time.OffsetDateTime waitingSince,
            long categoryTotal) {}
    public record DashboardVO(Integer manualReviewCount,
            Integer unresolvedSafetyCount,
            Integer humanHandoffCount,
            Integer urgeCount,
            Integer assignmentCount,
            Integer quoteConfirmationCount,
            Integer overrideApprovalCount,
            Integer upcomingAppointmentCount,
            Integer missedAppointmentCount,
            Integer returnRequestCount,
            Integer requoteCount,
            java.util.Map<String,Integer> statusDistribution,
            java.time.OffsetDateTime generatedAt,
            Boolean aiEnabled,
            java.util.List<AdminFollowUpVO> followUps) {}
    public record AuditSummaryVO(Long id,
            String operatorType,
            String operatorId,
            String action,
            String resourceType,
            String resourceId,
            String reason,
            String traceId,
            java.time.OffsetDateTime occurredAt) {}
    public record AuditSummaryVOPageVO(java.util.List<AuditSummaryVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record AuditDetailVO(AuditSummaryVO summary,
            String beforeSummary,
            String afterSummary,
            String proxyTechnicianId) {}
    public record AcceptanceIssueDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffQuoteCreateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:INITIAL|ADDITIONAL)") String quoteType,
            @jakarta.validation.constraints.Pattern(regexp="^[1-9][0-9]{0,18}$") String previousQuoteId,
            @jakarta.validation.constraints.Pattern(regexp="^[1-9][0-9]{0,18}$") String baseQuoteId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long serviceRecordId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:TECHNICIAN_SELF|DISPATCHER_PROXY|ADMIN_TAKEOVER)") String submissionSource,
            @jakarta.validation.constraints.Size(min=0,max=1000) String changeReason,
            @jakarta.validation.constraints.NotNull java.time.OffsetDateTime expiresAt,
            @jakarta.validation.constraints.NotNull @jakarta.validation.Valid @jakarta.validation.constraints.Size(min=1,max=100) java.util.List<QuoteItemInputDTO> items,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long technicianId) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffQuoteUpdateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.Size(min=0,max=1000) String changeReason,
            @jakarta.validation.constraints.NotNull java.time.OffsetDateTime expiresAt,
            @jakarta.validation.constraints.NotNull @jakarta.validation.Valid @jakarta.validation.constraints.Size(min=1,max=100) java.util.List<QuoteItemInputDTO> items) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record QuoteItemInputDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:VISIT|INSPECTION|LABOR|PART|MATERIAL|OTHER)") String feeType,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long serviceItemId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long pricingRuleId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="^(?:0\\.(?!000$)[0-9]{3}|[1-9][0-9]{0,6}\\.[0-9]{3})$") String quantity,
            @jakarta.validation.constraints.Pattern(regexp="^[0-9]{1,10}\\.[0-9]{2}$") String selectedUnitPrice,
            @jakarta.validation.constraints.Size(min=0,max=1000) String pricingReason,
            @jakarta.validation.constraints.Size(min=0,max=200) String itemName,
            @jakarta.validation.constraints.Size(min=0,max=200) String specification,
            @jakarta.validation.constraints.Size(min=0,max=1000) String partSourceNote,
            @jakarta.validation.constraints.Size(min=0,max=1000) String additionReason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record QuoteSubmitCommandDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="^[a-f0-9]{64}$") String expectedContentHash) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record QuoteWithdrawCommandDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedQuoteVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record QuoteOverrideRequestDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="^[a-f0-9]{64}$") String expectedContentHash,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String overrideReason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record QuoteOverrideDecisionDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedApprovalVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reviewComment) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record QuoteOverrideApprovalVO(Long id,
            Long quoteId,
            Long requestedBy,
            String reviewedBy,
            String quoteContentHash,
            String status,
            Integer version,
            String overrideReason,
            String reviewComment) {}
    public record StaffQuoteVO(com.fixmate.dto.user.QuoteVO quote,
            String contentHash,
            String submissionSource,
            Long createdBy,
            String submittedBy,
            String serviceRecordId,
            QuoteOverrideApprovalVO approval,
            java.util.List<String> allowedActions,
            String technicianId) {}
    public record QuoteSubmitResultVO(String submissionOutcome,
            StaffQuoteVO quote) {}
    public record ServiceItemInputDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String code,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:AIR_CONDITIONER|REFRIGERATOR|WASHING_MACHINE|AIR_SOURCE_HEAT_PUMP)") String applianceType,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=200) String name,
            @jakarta.validation.constraints.Size(min=0,max=1000) String description) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record ServiceItemUpdateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String code,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:AIR_CONDITIONER|REFRIGERATOR|WASHING_MACHINE|AIR_SOURCE_HEAT_PUMP)") String applianceType,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=200) String name,
            @jakarta.validation.constraints.Size(min=0,max=1000) String description,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record ServiceItemStaffVO(Long id,
            String code,
            String applianceType,
            String name,
            String description,
            String status,
            Integer version) {}
    public record ServiceItemStaffVOPageVO(java.util.List<ServiceItemStaffVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record PricingRuleInputDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long serviceItemId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:VISIT|INSPECTION|LABOR|PART|MATERIAL|OTHER)") String feeType,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String clauseNo,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String ruleVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=200) String title,
            @jakarta.validation.constraints.Size(min=0,max=2000) String description,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=32) String unit,
            @jakarta.validation.constraints.Pattern(regexp="^[0-9]{1,10}\\.[0-9]{2}$") String fixedPrice,
            @jakarta.validation.constraints.Pattern(regexp="^[0-9]{1,10}\\.[0-9]{2}$") String minPrice,
            @jakarta.validation.constraints.Pattern(regexp="^[0-9]{1,10}\\.[0-9]{2}$") String maxPrice,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=0,max=1000) String refundPolicy,
            @jakarta.validation.constraints.NotNull java.time.OffsetDateTime effectiveFrom,
            java.time.OffsetDateTime effectiveTo) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record PricingRuleUpdateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long serviceItemId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:VISIT|INSPECTION|LABOR|PART|MATERIAL|OTHER)") String feeType,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String clauseNo,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String ruleVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=200) String title,
            @jakarta.validation.constraints.Size(min=0,max=2000) String description,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=32) String unit,
            @jakarta.validation.constraints.Pattern(regexp="^[0-9]{1,10}\\.[0-9]{2}$") String fixedPrice,
            @jakarta.validation.constraints.Pattern(regexp="^[0-9]{1,10}\\.[0-9]{2}$") String minPrice,
            @jakarta.validation.constraints.Pattern(regexp="^[0-9]{1,10}\\.[0-9]{2}$") String maxPrice,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=0,max=1000) String refundPolicy,
            @jakarta.validation.constraints.NotNull java.time.OffsetDateTime effectiveFrom,
            java.time.OffsetDateTime effectiveTo,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record PricingRuleStaffVO(Long id,
            Long serviceItemId,
            String feeType,
            String clauseNo,
            String ruleVersion,
            String title,
            String description,
            String unit,
            String fixedPrice,
            String minPrice,
            String maxPrice,
            String refundPolicy,
            java.time.OffsetDateTime effectiveFrom,
            java.time.OffsetDateTime effectiveTo,
            String status,
            Integer version) {}
    public record PricingRuleStaffVOPageVO(java.util.List<PricingRuleStaffVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record ConfigStatusDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:ENABLED|DISABLED)") String status,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record WorkOrderCoordinationDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:REQUOTE|ABANDON_ADDITIONAL|RESUME_REPAIR|RETURN_TO_ASSIGNMENT|CONTINUE_AFTER_SAFETY|ESCALATE_TO_MANUAL|CANCEL_REJECTED_QUOTE|SCHEDULE_REWORK)") String resolution,
            @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record WorkOrderCommandDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record AppointmentCreateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull java.time.OffsetDateTime startAt,
            @jakarta.validation.constraints.NotNull java.time.OffsetDateTime endAt,
            @jakarta.validation.constraints.Size(min=0,max=1000) String note,
            @jakarta.validation.constraints.NotNull Boolean confirmed) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record AppointmentConfirmDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedAppointmentVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record ServiceRecordCreateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:INSPECTION|REPAIR)") String kind,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long technicianId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=10,max=4000) String facts,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String summary,
            @jakarta.validation.constraints.Pattern(regexp="^[1-9][0-9]{0,18}$") String correctsRecordId) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffServiceRecordVO(Long id,
            Long workOrderId,
            String kind,
            Long technicianId,
            Long submittedBy,
            java.time.OffsetDateTime submittedAt,
            String facts,
            String summary,
            String correctsRecordId) {}
    public record StaffWorkOrderProjectionVO(Long id,
            String workOrderNo,
            String status,
            String applianceType,
            String brandCode,
            String modelCode,
            String symptomDescription,
            String errorCodeText,
            java.time.OffsetDateTime occurredAt,
            java.util.List<com.fixmate.dto.user.FileAssetVO> mediaFiles,
            String contactName,
            String contactPhone,
            String addressText,
            com.fixmate.dto.user.AppointmentVO appointment,
            String technicianName,
            com.fixmate.dto.user.QuoteVO currentQuote,
            java.util.List<com.fixmate.dto.user.QuoteVO> quoteHistory,
            java.util.List<com.fixmate.dto.user.ServiceRecordVO> serviceRecords,
            com.fixmate.dto.user.AcceptanceVO acceptance,
            java.util.List<com.fixmate.dto.user.TimelineEventVO> timeline,
            Integer version) {}
    public record StaffWorkOrderDetailVO(PreferredVisitVO preferredVisit,
            String visitPolicy,
            StaffWorkOrderProjectionVO workOrder,
            AssignmentVO assignment,
            java.util.List<StaffServiceRecordVO> serviceRecords,
            java.util.List<String> allowedActions,
            java.util.List<String> allowedCoordinationResolutions,
            Integer appointmentVersion,
            Long repairRequestId,
            String repairNo,
            Boolean safetyBlocked,
            String appointmentStatus,
            String assigneeName,
            ServiceContactVO serviceContact,
            String coordinationCause,
            String coordinationReason,
            String reworkTechnicianId,
            Boolean reworkVisit) {}
    public record StaffAppointmentVO(com.fixmate.dto.user.AppointmentVO appointment,
            Integer version) {}
    public record AdminWorkOrderListItemVO(Long id,
            String workOrderNo,
            String status,
            String applianceType,
            String symptomSummary,
            Boolean riskFlag,
            String technicianId,
            java.time.OffsetDateTime appointmentStartAt,
            java.time.OffsetDateTime createdAt,
            String currentAssigneeId,
            String currentAssigneeName,
            String assignmentStatus,
            PreferredVisitVO preferredVisit,
            String appointmentStatus,
            String latestAssigneeName,
            String latestAssignmentStatus,
            Boolean poolPublished,
            String repairNo,
            String coordinationCause,
            String coordinationReason) {}
    public record AdminWorkOrderListItemVOPageVO(java.util.List<AdminWorkOrderListItemVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record StaffParticipationVO(Long workOrderId,
            String workOrderNo,
            String applianceType,
            String status,
            java.time.OffsetDateTime participatedAt,
            java.time.OffsetDateTime endedAt,
            String participationSummary,
            String latestAssignmentResult,
            String cancellationFeeAmount) {}
    public record StaffParticipationVOPageVO(java.util.List<StaffParticipationVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record SafetyRuleDetail(String ruleCode,
            String title,
            String templateText) {}
    public record SafetyEventStaffVO(Long id,
            String workOrderId,
            String status,
            java.util.List<SafetyRuleDetail> matchedRules,
            Integer version,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime acknowledgedAt,
            StaffActorVO acknowledgedBy,
            String acknowledgementNote,
            StaffActorVO resolvedBy,
            java.time.OffsetDateTime resolvedAt,
            String resolutionBasis) {}
    public record SafetyEventStaffVOPageVO(java.util.List<SafetyEventStaffVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record AssignmentVO(String allocationSource,
            Long id,
            Long workOrderId,
            Long technicianId,
            String status,
            java.time.OffsetDateTime assignedAt,
            java.time.OffsetDateTime acceptedAt,
            java.time.OffsetDateTime endedAt,
            Integer version,
            StaffActorVO assignedBy,
            StaffActorVO endedBy,
            String endReason) {}
    public record AssignmentVOPageVO(java.util.List<AssignmentVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record AssignmentCreateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long technicianId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record AssignmentActionDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedAssignmentVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record AssignmentReturnDecisionDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedAssignmentVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedWorkOrderVersion,
            @jakarta.validation.constraints.NotNull Boolean approved,
            @jakarta.validation.constraints.Pattern(regexp="^[1-9][0-9]{0,18}$") String replacementTechnicianId,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffTaskListItemVO(PreferredVisitVO preferredVisit,
            String visitPolicy,
            Long id,
            String workOrderNo,
            String status,
            String applianceType,
            String symptomSummary,
            String serviceArea,
            Boolean riskFlag,
            java.time.OffsetDateTime appointmentStartAt,
            AssignmentVO assignment,
            java.time.OffsetDateTime createdAt,
            Integer workOrderVersion,
            java.util.List<String> allowedActions,
            PreferredVisitVO rescheduleVisit,
            Boolean requoteRequested,
            Boolean reworkVisit,
            ReworkSummaryVO reworkSummary) {}
    public record ReworkSummaryVO(String issueReason, java.time.OffsetDateTime issueAt, String originalFault,
            PreferredVisitVO previousVisit, java.time.OffsetDateTime previousCompletedAt, String previousRepairSummary) {}
    public record StaffTaskListItemVOPageVO(java.util.List<StaffTaskListItemVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record AssignmentEventVO(Long id,
            Long assignmentId,
            String eventType,
            StaffActorVO operator,
            String reason,
            java.time.OffsetDateTime occurredAt) {}
    public record AssignmentHistoryDetailVO(AssignmentVO assignment,
            java.util.List<AssignmentEventVO> events) {}
    public record ServiceContactConfirmationDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedAddressVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reviewNote) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record ServiceContactVO(String contactName,
            String contactPhone,
            String addressText,
            String contactSource,
            Boolean phoneMasked,
            Integer addressVersion) {}
    public record AdminRepairRequestDetailVO(com.fixmate.dto.user.RepairRequestSnapshotVO repairRequest,
            StaffActorVO manualReviewedBy,
            java.time.OffsetDateTime manualReviewedAt,
            String manualReviewNote,
            java.util.List<String> allowedActions,
            ServiceContactVO serviceContact) {}
    public record ManualTriageDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.Size(min=2,max=1000) String reviewNote,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.AssertTrue Boolean informationConfirmed,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.AssertTrue Boolean safetyReviewed,
            @jakarta.validation.constraints.Min(0) Integer expectedAddressVersion) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record RepairRecordListItemVOPageVO(java.util.List<com.fixmate.dto.user.RepairRecordListItemVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record StaffOptionVO(Long id,
            String name,
            Integer activeTaskCount) {}
    public record StaffProfileVO(Long id,
            String username,
            String name,
            String phone,
            String role,
            String status,
            Integer version) {}
    public record StaffDirectoryItemVO(Long id,
            String username,
            String name,
            String phone,
            String role,
            String status,
            Integer version,
            String bindingStatus,
            String bindingApplicationPhone) {}
    public record AdminCustomerVO(Long id,
            String nickname,
            String maskedPhone,
            Boolean phoneVerified,
            String status,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime lastLoginAt) {}
    public record AdminCustomerVOPageVO(java.util.List<AdminCustomerVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record AdminQuoteRecordVO(String id,
            Long workOrderId,
            String workOrderNo,
            String quoteNo,
            String quoteType,
            String status,
            String amount,
            String visitFeeAmount,
            java.time.OffsetDateTime recordedAt) {}
    public record AdminQuoteRecordVOPageVO(java.util.List<AdminQuoteRecordVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record StaffTokenVO(String accessToken,
            String refreshToken,
            Integer expiresIn,
            StaffProfileVO staff,
            String principalType,
            String audience,
            Boolean passwordChangeRequired) {}
    public record StaffLoginDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String username,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=128) String password) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffCreateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=3,max=64) String username,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String name,
            @jakarta.validation.constraints.Size(min=0,max=20) String phone,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:ADMIN|DISPATCHER|TECHNICIAN|KNOWLEDGE_MANAGER|AUDITOR)") String role,
            @jakarta.validation.constraints.Size(min=6,max=6) @jakarta.validation.constraints.Pattern(regexp="^[0-9]{6}$") String initialPassword) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffStatusDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="(?:ENABLED|DISABLED)") String status,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffInvitationCreateDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedStaffVersion) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffInvitationVO(Long id,
            Long staffId,
            String status,
            java.time.OffsetDateTime expiresAt,
            Integer version,
            StaffActorVO createdBy,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime consumedAt,
            String consumedBindingId,
            StaffActorVO revokedBy,
            java.time.OffsetDateTime revokedAt,
            String revocationReason) {}
    public record StaffInvitationIssuedVO(StaffInvitationVO invitation,
            String invitationCode,
            String entryPage) {}
    public record NewTechnicianInvitationIssuedVO(StaffInvitationVO invitation,
            String invitationUrl,
            String invitationCode) {}
    public record StaffBindingApplyDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=16,max=64) String invitationCode,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=64) String applicantName,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="^[0-9+() -]{6,20}$") String applicantContactPhone) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffBindingVO(Long id,
            Long staffId,
            Long customerId,
            String staffName,
            String status,
            java.time.OffsetDateTime appliedAt,
            java.time.OffsetDateTime confirmedAt,
            Integer version,
            Long invitationId,
            String applicantName,
            String applicantContactPhone,
            Boolean contactPhoneVerified,
            String decisionReason,
            StaffActorVO confirmedBy,
            StaffActorVO rejectedBy,
            java.time.OffsetDateTime rejectedAt,
            StaffActorVO revokedBy,
            java.time.OffsetDateTime revokedAt) {}
    public record StaffBindingDecisionDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record WorkspaceAccessVO(java.util.List<String> availableWorkspaces,
            String staffId,
            String staffName,
            String staffPhone,
            String staffRole,
            String bindingId,
            Integer bindingVersion) {}
    public record StaffActorVO(Long id,
            String name) {}
    public record StaffPasswordChangeDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=1,max=128) String currentPassword,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=6,max=6) @jakarta.validation.constraints.Pattern(regexp="^[0-9]{6}$") String newPassword) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffPasswordResetDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=6,max=6) @jakarta.validation.constraints.Pattern(regexp="^[0-9]{6}$") String temporaryPassword,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record VersionCommandDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record ReasonCommandDTO(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Integer expectedVersion,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=2,max=1000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("Unexpected request field"); }
    }
    public record StaffProfileVOPageVO(java.util.List<StaffDirectoryItemVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record StaffInvitationVOPageVO(java.util.List<StaffInvitationVO> items,
            Integer page,
            Integer size,
            long total) {}
    public record StaffBindingVOPageVO(java.util.List<StaffBindingVO> items,
            Integer page,
            Integer size,
            long total) {}
}
