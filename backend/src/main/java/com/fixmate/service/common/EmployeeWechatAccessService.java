package com.fixmate.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import org.springframework.stereotype.Service;

/** 微信维修请求仍携带CUSTOMER令牌；每次以真实customerId重查有效员工绑定。 */
@Service
public class EmployeeWechatAccessService {
    public record EmployeeIdentity(Long customerId,Long staffId,Long bindingId,Integer bindingVersion) {}
    private final CustomerUserMapper customers;
    private final StaffUserMapper staff;
    private final StaffWechatBindingMapper bindings;
    public EmployeeWechatAccessService(CustomerUserMapper customers,StaffUserMapper staff,StaffWechatBindingMapper bindings) {
        this.customers=customers;this.staff=staff;this.bindings=bindings;
    }
    public EmployeeIdentity require(Long customerId) {
        var b=bindings.selectOne(new LambdaQueryWrapper<StaffWechatBindingEntity>()
                .eq(StaffWechatBindingEntity::getCustomerId,customerId).eq(StaffWechatBindingEntity::getStatus,"ACTIVE"));
        return validate(customerId,customers.selectById(customerId),b,b==null?null:staff.selectById(b.getStaffId()));
    }
    /** 在业务事务中、锁工单之前调用；与员工停用/解绑共享staff→binding→customer锁序。 */
    public EmployeeIdentity requireLocked(Long customerId) {
        var candidate=bindings.selectOne(new LambdaQueryWrapper<StaffWechatBindingEntity>()
                .eq(StaffWechatBindingEntity::getCustomerId,customerId).eq(StaffWechatBindingEntity::getStatus,"ACTIVE"));
        if(candidate==null)throw denied();
        var employee=staff.selectOne(new LambdaQueryWrapper<StaffUserEntity>().eq(StaffUserEntity::getId,candidate.getStaffId()).last("FOR UPDATE"));
        var b=bindings.selectOne(new LambdaQueryWrapper<StaffWechatBindingEntity>().eq(StaffWechatBindingEntity::getId,candidate.getId()).last("FOR UPDATE"));
        var customer=customers.selectOne(new LambdaQueryWrapper<CustomerUserEntity>().eq(CustomerUserEntity::getId,customerId).last("FOR UPDATE"));
        return validate(customerId,customer,b,employee);
    }
    private EmployeeIdentity validate(Long customerId,CustomerUserEntity customer,StaffWechatBindingEntity b,StaffUserEntity employee) {
        if(customer==null||!"ENABLED".equals(customer.getStatus())||b==null||!"ACTIVE".equals(b.getStatus())
                ||!customerId.equals(b.getCustomerId())||employee==null||!employee.getId().equals(b.getStaffId())
                ||!"ENABLED".equals(employee.getStatus())||!"TECHNICIAN".equals(employee.getRole()))throw denied();
        // Web口令重置不会撤掉已核实的微信绑定；两种凭据生命周期独立。
        return new EmployeeIdentity(customerId,employee.getId(),b.getId(),b.getVersion());
    }
    private BusinessException denied(){return BusinessException.forbidden("当前账号没有有效维修人员授权");}
}
