package com.fixmate.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.dto.admin.StaffIdentityContracts.AcceptanceIssueDTO;
import com.fixmate.dto.user.RepairRecordDetailVO;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.StaffIdentityGuard;
import com.fixmate.service.common.*;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(isolation=Isolation.READ_COMMITTED)
public class UserAcceptanceIssueService {
    private final WorkOrderMapper orders;private final RepairRequestMapper repairs;private final HumanHandoffRecordMapper todos;
    private final UserProfileService profiles;private final UserRepairRecordsService records;private final WorkOrderStateMachine machine;
    private final DatabaseClockMapper clock;private final ServiceContactProjection contacts;private final AuditLogService audit;
    public UserAcceptanceIssueService(WorkOrderMapper orders,RepairRequestMapper repairs,HumanHandoffRecordMapper todos,UserProfileService profiles,
            UserRepairRecordsService records,WorkOrderStateMachine machine,DatabaseClockMapper clock,ServiceContactProjection contacts,AuditLogService audit){
        this.orders=orders;this.repairs=repairs;this.todos=todos;this.profiles=profiles;this.records=records;this.machine=machine;this.clock=clock;this.contacts=contacts;this.audit=audit;
    }
    public void guard(Long user,Long id){profiles.current(user);var w=orders.selectById(id);if(w==null)throw BusinessException.notFound("工单不存在");if(!user.equals(w.getCustomerId()))throw BusinessException.forbidden("无权操作此工单");}
    public RepairRecordDetailVO raise(Long user,Long id,AcceptanceIssueDTO input){
        profiles.current(user);var w=orders.selectOne(new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getId,id).last("FOR UPDATE"));
        if(w==null)throw BusinessException.notFound("工单不存在");if(!user.equals(w.getCustomerId()))throw BusinessException.forbidden("无权操作此工单");
        StaffIdentityGuard.version(w.getVersion(),input.expectedWorkOrderVersion());if(!"WAITING_ACCEPTANCE".equals(w.getStatus()))throw BusinessException.stateConflict("当前状态不支持验收异议");
        var repair=repairs.selectById(w.getRepairRequestId());if(repair==null||!user.equals(repair.getUserId())||!w.getId().equals(repair.getWorkOrderId()))throw BusinessException.stateConflict("原报修关联异常");
        String reason=contacts.redactNote(StaffIdentityGuard.text(input.reason(),2,1000,"异议原因"),repair);
        if(orders.update(null,new UpdateWrapper<WorkOrderEntity>().eq("id",id).eq("version",w.getVersion())
                .set("preferred_visit_start_at",null).set("preferred_visit_end_at",null)
                .set("takeover_by",null).set("takeover_at",null).set("takeover_reason",null))!=1)
            throw BusinessException.versionConflict("工单已变化");
        machine.transition(id,WorkOrderStatus.WAITING_MANUAL_REVIEW,w.getVersion(),reason,"CUSTOMER",user);
        var todo=new HumanHandoffRecordEntity();todo.setWorkOrderId(id);todo.setUserId(user);todo.setReason(reason);todo.setStatus("PENDING");todo.setCreatedAt(clock.now());todo.setUpdatedAt(todo.getCreatedAt());todos.insert(todo);
        audit.recordCustomer(user,"WORK_ORDER_ACCEPTANCE_ISSUE","WORK_ORDER",id,Map.of("status","WAITING_ACCEPTANCE"),Map.of("source","USER_ACCEPTANCE_ISSUE","todoId",todo.getId(),"reason",reason));
        return records.detail(user,repair.getId());
    }
}
