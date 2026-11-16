package com.fixmate.common.enums;

import java.util.Map;
import java.util.Set;

public enum WorkOrderStatus {
    WAITING_MANUAL_REVIEW,
    WAITING_CUSTOMER_VISIT,
    WAITING_ASSIGNMENT,
    WAITING_APPOINTMENT,
    APPOINTED,
    IN_INSPECTION,
    WAITING_QUOTE,
    WAITING_QUOTE_CONFIRMATION,
    IN_REPAIR,
    PAUSED_FOR_REQUOTE,
    WAITING_ACCEPTANCE,
    COMPLETED,
    CANCELLED,
    CLOSED_UNRESOLVED;

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> TRANSITIONS = Map.ofEntries(
            // 报价被拒后的人工处理：可重报（回 WAITING_QUOTE）、放弃追加恢复维修、或关闭
            Map.entry(WAITING_MANUAL_REVIEW,
                    Set.of(WAITING_CUSTOMER_VISIT, WAITING_ASSIGNMENT, WAITING_APPOINTMENT, WAITING_QUOTE, IN_REPAIR, CANCELLED, CLOSED_UNRESOLVED)),
            Map.entry(WAITING_CUSTOMER_VISIT, Set.of(WAITING_ASSIGNMENT, CANCELLED)),
            Map.entry(WAITING_ASSIGNMENT, Set.of(WAITING_APPOINTMENT, CANCELLED)),
            Map.entry(WAITING_APPOINTMENT, Set.of(APPOINTED, WAITING_ASSIGNMENT, WAITING_MANUAL_REVIEW, CANCELLED)),
            Map.entry(APPOINTED, Set.of(IN_INSPECTION, WAITING_APPOINTMENT, WAITING_ASSIGNMENT, CANCELLED)),
            Map.entry(IN_INSPECTION, Set.of(WAITING_QUOTE, WAITING_APPOINTMENT, IN_REPAIR, CLOSED_UNRESOLVED, CANCELLED)),
            Map.entry(WAITING_QUOTE, Set.of(WAITING_QUOTE_CONFIRMATION, PAUSED_FOR_REQUOTE, IN_REPAIR, CLOSED_UNRESOLVED, CANCELLED)),
            Map.entry(WAITING_QUOTE_CONFIRMATION, Set.of(IN_REPAIR, WAITING_MANUAL_REVIEW, WAITING_QUOTE, CANCELLED)),
            Map.entry(IN_REPAIR, Set.of(WAITING_ACCEPTANCE, PAUSED_FOR_REQUOTE, WAITING_MANUAL_REVIEW)),
            Map.entry(PAUSED_FOR_REQUOTE, Set.of(IN_REPAIR, WAITING_MANUAL_REVIEW, WAITING_QUOTE)),
            Map.entry(WAITING_ACCEPTANCE, Set.of(COMPLETED, WAITING_MANUAL_REVIEW)),
            Map.entry(COMPLETED, Set.of()),
            Map.entry(CANCELLED, Set.of()),
            Map.entry(CLOSED_UNRESOLVED, Set.of()));

    public boolean canTransitionTo(WorkOrderStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public Set<WorkOrderStatus> allowedTargets() {
        return TRANSITIONS.getOrDefault(this, Set.of());
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == CLOSED_UNRESOLVED;
    }
}
