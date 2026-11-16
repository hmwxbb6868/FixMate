package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fixmate.entity.SafetyEventEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.SafetyEventMapper;
import org.springframework.stereotype.Service;

/** 调用者先持有工单根锁；确认不等于解除，不以matchedRules或前端按钮代替事件状态。 */
@Service
public class WorkOrderSafetyGate {
    private final SafetyEventMapper events;
    public WorkOrderSafetyGate(SafetyEventMapper events){this.events=events;}
    public boolean clear(Long orderId) {
        return events.selectCount(new LambdaQueryWrapper<SafetyEventEntity>().eq(SafetyEventEntity::getWorkOrderId,orderId)
                .ne(SafetyEventEntity::getStatus,"RESOLVED"))==0;
    }
    public boolean clearLocked(Long orderId) {
        // 锁定读避免REPEATABLE READ中较早的一致性快照漏掉已提交的安全事件。
        return events.selectList(new LambdaQueryWrapper<SafetyEventEntity>().select(SafetyEventEntity::getId)
                .eq(SafetyEventEntity::getWorkOrderId,orderId).ne(SafetyEventEntity::getStatus,"RESOLVED").last("FOR UPDATE")).isEmpty();
    }
    public boolean hasResolvedEvent(Long orderId) {
        return events.selectCount(new LambdaQueryWrapper<SafetyEventEntity>().eq(SafetyEventEntity::getWorkOrderId,orderId)
                .eq(SafetyEventEntity::getStatus,"RESOLVED"))>0;
    }
    public void requireClear(Long orderId){if(!clearLocked(orderId))throw BusinessException.stateConflict("安全事件尚未全部解除");}
}
