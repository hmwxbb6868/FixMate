package com.fixmate.service.common;

import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.*;
import org.springframework.stereotype.Component;

/** 只锁本命令涉及的员工；身份→绑定/客户→工单，根锁内复核锁前观察到的分派。 */
@Component
public class WorkOrderIdentityLocker {
    public record Locked(StaffUserEntity actor,WorkOrderEntity order,List<StaffUserEntity> staff) {}
    private final WorkOrderMapper orders;private final WorkOrderAssignmentMapper assignments;private final StaffWechatBindingMapper bindings;
    private final StaffIdentityGuard guard;private final EmployeeWechatAccessService employees;
    public WorkOrderIdentityLocker(WorkOrderMapper orders,WorkOrderAssignmentMapper assignments,StaffWechatBindingMapper bindings,StaffIdentityGuard guard,EmployeeWechatAccessService employees){this.orders=orders;this.assignments=assignments;this.bindings=bindings;this.guard=guard;this.employees=employees;}
    public Locked lock(StaffPrincipal actor,Long id,Collection<Long> additional,String... roles){
        var candidate=orders.selectById(id);if(candidate==null)throw BusinessException.notFound("工单不存在");
        var prior=candidate.getCurrentAssignmentId()==null?null:assignments.selectById(candidate.getCurrentAssignmentId());
        var ids=new TreeSet<Long>();ids.add(actor.staffId());if(candidate.getTechnicianId()!=null)ids.add(candidate.getTechnicianId());if(prior!=null)ids.add(prior.getTechnicianId());for(var staffId:additional)if(staffId!=null)ids.add(staffId);
        var locked=guard.lockIds(ids);var operator=guard.requireRoles(actor,locked,roles);
        for(var person:locked)if("TECHNICIAN".equals(person.getRole())){
            var b=bindings.selectOne(new QueryWrapper<StaffWechatBindingEntity>().eq("staff_id",person.getId()).eq("status","ACTIVE"));
            if(b!=null)try{employees.requireLocked(b.getCustomerId());}catch(BusinessException denied){if(denied.getStatus().value()!=403)throw denied;}
        }
        var current=orders.selectOne(new QueryWrapper<WorkOrderEntity>().eq("id",id).last("FOR UPDATE"));
        if(current==null)throw BusinessException.notFound("工单不存在");
        if(!Objects.equals(candidate.getCurrentAssignmentId(),current.getCurrentAssignmentId())||!Objects.equals(candidate.getTechnicianId(),current.getTechnicianId()))throw BusinessException.versionConflict("分派已变化，请刷新后重试");
        if(prior!=null){var row=assignments.selectById(prior.getId());if(row==null||!Objects.equals(prior.getTechnicianId(),row.getTechnicianId())||!Objects.equals(prior.getVersion(),row.getVersion()))throw BusinessException.versionConflict("分派已变化，请刷新后重试");}
        return new Locked(operator,current,locked);
    }
}
