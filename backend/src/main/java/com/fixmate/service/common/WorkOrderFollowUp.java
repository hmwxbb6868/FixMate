package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fixmate.entity.QuoteEntity;
import com.fixmate.entity.WorkOrderAssignmentEntity;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.entity.WorkOrderStatusLogEntity;
import com.fixmate.mapper.WorkOrderAssignmentMapper;
import com.fixmate.mapper.WorkOrderStatusLogMapper;
import com.fixmate.mapper.QuoteMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WorkOrderFollowUp {
    private static final String OUTSTANDING_REJECTION = """
            quote.status='REJECTED' AND quote.user_decided_at IS NOT NULL
            AND quote.version_no>COALESCE((SELECT MAX(a.version_no) FROM quote a
                WHERE a.work_order_id=quote.work_order_id AND a.status='ACCEPTED'),0)
            """;
    public static final String QUOTE_REJECTED_QUEUE = """
            EXISTS (SELECT 1 FROM quote WHERE quote.work_order_id=work_order.id AND
            """ + OUTSTANDING_REJECTION + ")";
    public static final String INITIAL_QUOTE_REJECTED = "INITIAL_QUOTE_REJECTED";
    public static final String ADDITIONAL_QUOTE_REJECTED = "ADDITIONAL_QUOTE_REJECTED";
    public static final String ACCEPTANCE_ISSUE = "ACCEPTANCE_ISSUE";

    private final WorkOrderStatusLogMapper logs;
    private final WorkOrderAssignmentMapper assignments;
    private final QuoteMapper quotes;

    public WorkOrderFollowUp(WorkOrderStatusLogMapper logs, WorkOrderAssignmentMapper assignments, QuoteMapper quotes) {
        this.logs = logs;
        this.assignments = assignments;
        this.quotes = quotes;
    }

    public record Cause(String type, String reason) {}

    public Cause manualCause(WorkOrderEntity order) {
        if (!"WAITING_MANUAL_REVIEW".equals(order.getStatus())) return null;
        var rejected = quoteRejection(order.getId());
        if (rejected != null) return rejected;
        var entry = lastManualEntry(order.getId());
        if (entry != null && "WAITING_APPOINTMENT".equals(entry.getFromStatus()) && isRework(order)) {
            entry = logs.selectOne(new LambdaQueryWrapper<WorkOrderStatusLogEntity>()
                    .eq(WorkOrderStatusLogEntity::getWorkOrderId, order.getId())
                    .eq(WorkOrderStatusLogEntity::getFromStatus, "WAITING_ACCEPTANCE")
                    .eq(WorkOrderStatusLogEntity::getToStatus, "WAITING_MANUAL_REVIEW")
                    .orderByDesc(WorkOrderStatusLogEntity::getId).last("LIMIT 1"));
        }
        return cause(entry);
    }

    public boolean isRework(WorkOrderEntity order) {
        return reworkIssue(order)!=null;
    }

    public WorkOrderStatusLogEntity reworkIssue(WorkOrderEntity order) {
        if (!List.of("WAITING_MANUAL_REVIEW", "WAITING_APPOINTMENT", "APPOINTED", "IN_INSPECTION",
                "IN_REPAIR", "WAITING_QUOTE", "PAUSED_FOR_REQUOTE").contains(order.getStatus())) return null;
        var entry = logs.selectOne(new LambdaQueryWrapper<WorkOrderStatusLogEntity>()
                .eq(WorkOrderStatusLogEntity::getWorkOrderId, order.getId())
                .and(q -> q.eq(WorkOrderStatusLogEntity::getFromStatus, "WAITING_ACCEPTANCE")
                        .or().eq(WorkOrderStatusLogEntity::getToStatus, "WAITING_ACCEPTANCE"))
                .orderByDesc(WorkOrderStatusLogEntity::getId).last("LIMIT 1"));
        return entry != null && "WAITING_ACCEPTANCE".equals(entry.getFromStatus())
                && "WAITING_MANUAL_REVIEW".equals(entry.getToStatus()) && "CUSTOMER".equals(entry.getOperatorType()) ? entry : null;
    }

    public Long originalTechnician(Long workOrderId) {
        var assignment = lastCompletedAssignment(workOrderId);
        return assignment == null ? null : assignment.getTechnicianId();
    }

    public WorkOrderAssignmentEntity lastCompletedAssignment(Long workOrderId) {
        return assignments.selectOne(new LambdaQueryWrapper<WorkOrderAssignmentEntity>()
                .eq(WorkOrderAssignmentEntity::getWorkOrderId, workOrderId)
                .eq(WorkOrderAssignmentEntity::getStatus, "FINISHED")
                .isNotNull(WorkOrderAssignmentEntity::getAcceptedAt)
                .isNotNull(WorkOrderAssignmentEntity::getEndedAt)
                .orderByDesc(WorkOrderAssignmentEntity::getId).last("LIMIT 1"));
    }

    public boolean requoteRequested(WorkOrderEntity order) {
        return "WAITING_QUOTE".equals(order.getStatus()) && quoteRejection(order.getId()) != null;
    }

    public Cause quoteRejection(Long id) {
        var rejected = quotes.selectOne(new QueryWrapper<QuoteEntity>().eq("work_order_id",id)
                .apply(OUTSTANDING_REJECTION).orderByDesc("version_no","id").last("LIMIT 1"));
        if (rejected == null) return null;
        return new Cause("ADDITIONAL".equals(rejected.getQuoteType())?ADDITIONAL_QUOTE_REJECTED:INITIAL_QUOTE_REJECTED,
                rejected.getDecisionReason());
    }

    public Cause coordinationCause(WorkOrderEntity order) {
        if ("WAITING_QUOTE".equals(order.getStatus())) return quoteRejection(order.getId());
        return manualCause(order);
    }

    private WorkOrderStatusLogEntity lastManualEntry(Long id) {
        return logs.selectOne(new LambdaQueryWrapper<WorkOrderStatusLogEntity>()
                .eq(WorkOrderStatusLogEntity::getWorkOrderId, id)
                .eq(WorkOrderStatusLogEntity::getToStatus, "WAITING_MANUAL_REVIEW")
                .orderByDesc(WorkOrderStatusLogEntity::getId).last("LIMIT 1"));
    }

    private Cause cause(WorkOrderStatusLogEntity entry) {
        if (entry == null) return new Cause("MANUAL", null);
        if (entry.getFromStatus() == null && "SYSTEM".equals(entry.getOperatorType())) {
            return new Cause("SAFETY_REVIEW", entry.getReason());
        }
        if ("CUSTOMER".equals(entry.getOperatorType())) {
            String type = switch (java.util.Objects.toString(entry.getFromStatus(), "")) {
                case "WAITING_QUOTE_CONFIRMATION" -> INITIAL_QUOTE_REJECTED;
                case "PAUSED_FOR_REQUOTE" -> ADDITIONAL_QUOTE_REJECTED;
                case "WAITING_ACCEPTANCE" -> ACCEPTANCE_ISSUE;
                default -> "MANUAL";
            };
            return new Cause(type, entry.getReason());
        }
        return new Cause("MANUAL", entry.getReason());
    }
}
