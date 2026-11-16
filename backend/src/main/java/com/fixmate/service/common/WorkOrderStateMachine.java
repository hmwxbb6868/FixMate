package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.entity.WorkOrderEntity;
import com.fixmate.entity.HumanHandoffRecordEntity;
import com.fixmate.entity.WorkOrderStatusLogEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.WorkOrderMapper;
import com.fixmate.mapper.HumanHandoffRecordMapper;
import com.fixmate.mapper.WorkOrderStatusLogMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工单状态迁移的唯一入口。Mapper 不得直接更新 status。
 */
@Service
public class WorkOrderStateMachine {

    private final com.fixmate.mapper.DatabaseClockMapper clock;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderStatusLogMapper statusLogMapper;
    private final AuditLogService auditLogService;
    private final WorkOrderUrges urges;
    private final HumanHandoffRecordMapper handoffs;

    public WorkOrderStateMachine(WorkOrderMapper workOrderMapper,
                                 WorkOrderStatusLogMapper statusLogMapper,
                                 AuditLogService auditLogService,com.fixmate.mapper.DatabaseClockMapper clock,
                                 WorkOrderUrges urges, HumanHandoffRecordMapper handoffs) {
        this.clock=clock;
        this.workOrderMapper = workOrderMapper;
        this.statusLogMapper = statusLogMapper;
        this.auditLogService = auditLogService;
        this.urges = urges;
        this.handoffs = handoffs;
    }

    @Transactional
    public WorkOrderEntity transition(Long workOrderId,
                                      WorkOrderStatus target,
                                      Integer expectedVersion,
                                      String reason,
                                      String operatorType,
                                      Long operatorId) {
        WorkOrderEntity order = workOrderMapper.selectById(workOrderId);
        if (order == null) {
            throw BusinessException.notFound("工单不存在");
        }
        WorkOrderStatus current = WorkOrderStatus.valueOf(order.getStatus());
        if (!current.canTransitionTo(target)) {
            throw BusinessException.stateConflict(
                    "不允许从 " + current + " 迁移到 " + target);
        }
        var now=clock.now();
        int expected = expectedVersion == null ? order.getVersion() : expectedVersion;
        int rows = workOrderMapper.update(null, new UpdateWrapper<WorkOrderEntity>()
                .eq("id", workOrderId)
                .eq("version", expected)
                .set("status", target.name())
                .set("version", expected + 1)
                .set("updated_at", now));
        if (rows == 0) {
            throw BusinessException.versionConflict("工单已被更新，请刷新后重试");
        }
        if (target.isTerminal()) {
            urges.invalidatePending(workOrderId,now,"关联工单已结束，催办自动失效");
            handoffs.update(null, new UpdateWrapper<HumanHandoffRecordEntity>()
                    .eq("work_order_id",workOrderId).eq("status","PENDING")
                    .set("status","HANDLED").set("handled_by",null).set("handled_at",now)
                    .set("handling_conclusion","关联工单已结束，系统结束待跟进请求")
                    .setSql("version=version+1").set("updated_at",now));
        }

        WorkOrderStatusLogEntity log = new WorkOrderStatusLogEntity();
        log.setWorkOrderId(workOrderId);
        log.setFromStatus(current.name());
        log.setToStatus(target.name());
        log.setReason(reason);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setTraceId(com.fixmate.common.trace.TraceContext.current());
        log.setCreatedAt(now);
        statusLogMapper.insert(log);

        auditLogService.record(operatorType, operatorId, "WORK_ORDER_STATUS_CHANGED", "WORK_ORDER", workOrderId,
                Map.of("status", current.name()), Map.of("status", target.name(), "reason", String.valueOf(reason)));

        order.setStatus(target.name());
        order.setVersion(expected + 1);
        order.setUpdatedAt(now);
        return order;
    }
}
