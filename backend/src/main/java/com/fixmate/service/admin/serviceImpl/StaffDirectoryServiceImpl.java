package com.fixmate.service.admin.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.StaffDirectoryService;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffDirectoryServiceImpl implements StaffDirectoryService {
    private final DatabaseClockMapper clock;
    private final StaffUserMapper staff;
    private final StaffInvitationMapper invitations;
    private final StaffWechatBindingMapper bindings;
    private final CustomerUserMapper customers;
    private final WorkOrderMapper orders;
    private final com.fixmate.service.admin.WorkAssignmentService workAssignments;
    private final WorkAssignmentQueryMapper workloads;
    private final PasswordEncoder passwords;
    private final StaffIdentityGuard guard;
    private final StaffAccessService access;
    private final AuditLogService audit;
    private final StaffAttemptLimiter attempts;
    public StaffDirectoryServiceImpl(StaffUserMapper staff,StaffInvitationMapper invitations,StaffWechatBindingMapper bindings,
            WorkOrderMapper orders,com.fixmate.service.admin.WorkAssignmentService workAssignments,WorkAssignmentQueryMapper workloads,PasswordEncoder passwords,StaffIdentityGuard guard,
            StaffAccessService access,AuditLogService audit,StaffAttemptLimiter attempts,DatabaseClockMapper clock,
            CustomerUserMapper customers) {
        this.clock=clock;this.staff=staff;this.invitations=invitations;this.bindings=bindings;this.orders=orders;this.workAssignments=workAssignments;this.workloads=workloads;
        this.passwords=passwords;this.guard=guard;this.access=access;this.audit=audit;this.attempts=attempts;
        this.customers=customers;
    }
    public static void page(int page,int size) {
        if(page<1||size<1||size>100)throw BusinessException.validation("分页参数不正确");
    }
    @Override public StaffProfileVOPageVO list(StaffPrincipal actor,int page,int size) {
        access.require(actor,"ADMIN");page(page,size);
        var result=staff.selectPage(new Page<>(page,size),new LambdaQueryWrapper<StaffUserEntity>().orderByAsc(StaffUserEntity::getId));
        var ids=result.getRecords().stream().filter(s->"TECHNICIAN".equals(s.getRole())).map(StaffUserEntity::getId).toList();
        Map<Long,StaffWechatBindingEntity> liveBindings=new HashMap<>();
        if(!ids.isEmpty()) {
            for(var binding:bindings.selectList(new LambdaQueryWrapper<StaffWechatBindingEntity>()
                    .select(StaffWechatBindingEntity::getStaffId,StaffWechatBindingEntity::getStatus,
                            StaffWechatBindingEntity::getApplicantContactPhone)
                    .in(StaffWechatBindingEntity::getStaffId,ids)
                    .in(StaffWechatBindingEntity::getStatus,List.of("PENDING","ACTIVE")))) {
                liveBindings.put(binding.getStaffId(),binding);
            }
        }
        var items=result.getRecords().stream().map(s->{
            var binding=liveBindings.get(s.getId());
            String bindingStatus=binding==null?null:binding.getStatus();
            String applicationPhone="ACTIVE".equals(bindingStatus)?binding.getApplicantContactPhone():null;
            return new StaffDirectoryItemVO(s.getId(),s.getUsername(),s.getName(),s.getPhone(),s.getRole(),s.getStatus(),
                    s.getVersion(),bindingStatus,applicationPhone);
        }).toList();
        return new StaffProfileVOPageVO(items,page,size,result.getTotal());
    }
    @Override public AdminCustomerVOPageVO customers(StaffPrincipal actor,int page,int size,String keyword,String status) {
        access.require(actor,"ADMIN");
        page(page,size);
        var query=new LambdaQueryWrapper<CustomerUserEntity>()
                .select(CustomerUserEntity::getId,CustomerUserEntity::getNickname,CustomerUserEntity::getPhone,
                        CustomerUserEntity::getPhoneVerified,CustomerUserEntity::getStatus,
                        CustomerUserEntity::getCreatedAt,CustomerUserEntity::getLastLoginAt)
                .eq(status!=null,CustomerUserEntity::getStatus,status)
                .orderByDesc(CustomerUserEntity::getId);
        if(keyword!=null&&!keyword.isBlank()) {
            String search=keyword.strip();
            query.and(q->{
                q.like(CustomerUserEntity::getNickname,search);
                if(search.matches("[1-9][0-9]{0,18}"))q.or().apply("id = CAST({0} AS UNSIGNED)",search);
            });
        }
        var result=customers.selectPage(new Page<>(page,size),query);
        return new AdminCustomerVOPageVO(result.getRecords().stream().map(this::customerProfile).toList(),
                page,size,result.getTotal());
    }
    private AdminCustomerVO customerProfile(CustomerUserEntity row) {
        String phone=row.getPhone();
        String masked=phone==null||phone.isBlank()?null:phone.length()>7
                ?phone.substring(0,3)+"****"+phone.substring(phone.length()-4):"****";
        return new AdminCustomerVO(row.getId(),row.getNickname(),masked,Boolean.TRUE.equals(row.getPhoneVerified()),
                row.getStatus(),row.getCreatedAt().atOffset(ZoneOffset.UTC),
                row.getLastLoginAt()==null?null:row.getLastLoginAt().atOffset(ZoneOffset.UTC));
    }
    @Override public List<StaffOptionVO> options(StaffPrincipal actor) {
        access.require(actor,"ADMIN","DISPATCHER");
        return staff.selectList(new LambdaQueryWrapper<StaffUserEntity>().eq(StaffUserEntity::getStatus,"ENABLED")
                .eq(StaffUserEntity::getRole,"TECHNICIAN")
                .inSql(StaffUserEntity::getId,"SELECT b.staff_id FROM staff_wechat_binding b JOIN customer_user c ON c.id=b.customer_id WHERE b.status='ACTIVE' AND c.status='ENABLED'").orderByAsc(StaffUserEntity::getId)).stream()
                .map(s->new StaffOptionVO(s.getId(),s.getName(),Math.toIntExact(workloads.activeTaskCount(s.getId())))).toList();
    }
    @Override @Transactional public StaffProfileVO create(StaffPrincipal actor,StaffCreateDTO input) {
        var locked=guard.lockDirectory();guard.requireAdmin(actor,locked);
        String username=StaffAuthServiceImpl.normalized(input.username());
        StaffIdentityGuard.text(username,3,64,"用户名");
        String name=StaffIdentityGuard.text(input.name(),1,64,"员工姓名");
        if(!Set.of("ADMIN","DISPATCHER","TECHNICIAN","KNOWLEDGE_MANAGER","AUDITOR").contains(input.role()))
            throw BusinessException.validation("员工角色不正确");
        boolean supplied=input.initialPassword()!=null;
        if(!supplied&&!"TECHNICIAN".equals(input.role()))throw BusinessException.validation("后台账号须设置初始密码");
        if(supplied)StaffAuthServiceImpl.checkPassword(input.initialPassword());
        StaffUserEntity user=new StaffUserEntity();
        user.setUsername(username);user.setName(name);user.setPhone(input.phone()==null?null:StaffIdentityGuard.text(input.phone(),0,20,"联系方式"));
        user.setRole(input.role());user.setStatus("ENABLED");user.setVersion(0);user.setCredentialVersion(0);
        // 未提供密码的维修人员使用不可分发的随机凭据；微信身份仍须独立审核。
        user.setPasswordHash(passwords.encode(supplied?input.initialPassword():UUID.randomUUID()+UUID.randomUUID().toString()));
        user.setPasswordChangeRequired(true);user.setCreatedAt(now());user.setUpdatedAt(now());
        try {staff.insert(user);}catch(DuplicateKeyException ex){throw BusinessException.stateConflict("用户名已存在");}
        audit.record("STAFF",actor.staffId(),"STAFF_CREATED","STAFF_USER",user.getId(),null,access.profile(user));
        return access.profile(user);
    }
    @Override @Transactional public StaffProfileVO status(StaffPrincipal actor,Long id,StaffStatusDTO input) {
        var locked=guard.lockDirectory();guard.requireAdmin(actor,locked);var user=guard.target(locked,id);
        StaffIdentityGuard.version(user.getVersion(),input.expectedVersion());
        String reason=StaffIdentityGuard.text(input.reason(),2,1000,"操作原因");
        if(!Set.of("ENABLED","DISABLED").contains(input.status()))throw BusinessException.validation("员工状态不正确");
        if(input.status().equals(user.getStatus()))throw BusinessException.stateConflict("员工已处于目标状态");
        if("DISABLED".equals(input.status())&&"ADMIN".equals(user.getRole())&&locked.stream()
                .filter(s->"ADMIN".equals(s.getRole())&&"ENABLED".equals(s.getStatus())).count()<=1)
            throw BusinessException.stateConflict("不能停用最后一个有效管理员");
        var before=access.profile(user);
        if("DISABLED".equals(input.status())) revokeBindingsAndInvitations(actor,id,reason);
        int updated=staff.update(null,new UpdateWrapper<StaffUserEntity>().eq("id",id).eq("version",input.expectedVersion())
                .set("status",input.status()).set("version",user.getVersion()+1).set("credential_version",user.getCredentialVersion()+1).set("updated_at",now()));
        if(updated!=1)throw BusinessException.versionConflict("员工资料已更新");
        user.setStatus(input.status());user.setVersion(user.getVersion()+1);user.setCredentialVersion(user.getCredentialVersion()+1);
        audit.record("STAFF",actor.staffId(),"STAFF_STATUS_CHANGED","STAFF_USER",id,before,Map.of("staff",access.profile(user),"reason",reason));
        return access.profile(user);
    }
    private void revokeBindingsAndInvitations(StaffPrincipal actor,Long id,String reason) {
        for(var b:bindings.selectList(new LambdaQueryWrapper<StaffWechatBindingEntity>().eq(StaffWechatBindingEntity::getStaffId,id)
                .in(StaffWechatBindingEntity::getStatus,List.of("PENDING","ACTIVE")).orderByAsc(StaffWechatBindingEntity::getId).last("FOR UPDATE"))) {
            int changed=bindings.update(null,new UpdateWrapper<StaffWechatBindingEntity>().eq("id",b.getId()).eq("version",b.getVersion())
                    .set("status","REVOKED").set("revoked_by",actor.staffId()).set("revoked_at",now()).set("decision_reason",reason)
                    .set("version",b.getVersion()+1).set("updated_at",now()));
            if(changed!=1)throw BusinessException.versionConflict("绑定已变化，停用操作回滚");
            audit.record("STAFF",actor.staffId(),"STAFF_BINDING_REVOKED","STAFF_BINDING",b.getId(),Map.of("status",b.getStatus()),Map.of("reason",reason));
        }
        for(var i:invitations.selectList(new LambdaQueryWrapper<StaffInvitationEntity>().eq(StaffInvitationEntity::getStaffId,id)
                .eq(StaffInvitationEntity::getStatus,"ISSUED").orderByAsc(StaffInvitationEntity::getId).last("FOR UPDATE"))) {
            int changed=invitations.update(null,new UpdateWrapper<StaffInvitationEntity>().eq("id",i.getId()).eq("version",i.getVersion())
                    .set("status","REVOKED").set("revoked_by",actor.staffId()).set("revoked_at",now()).set("revocation_reason",reason)
                    .set("version",i.getVersion()+1).set("updated_at",now()));
            if(changed!=1)throw BusinessException.versionConflict("邀请已变化，停用操作回滚");
            audit.record("STAFF",actor.staffId(),"STAFF_INVITATION_REVOKED","STAFF_INVITATION",i.getId(),null,Map.of("reason",reason));
        }
        workAssignments.revokeDisabledStaff(actor,id,reason);
    }
    @Override @Transactional public void resetPassword(StaffPrincipal actor,Long id,StaffPasswordResetDTO input) {
        var locked=guard.lockDirectory();guard.requireAdmin(actor,locked);var user=guard.target(locked,id);
        if(id.equals(actor.staffId()))throw BusinessException.forbidden("须由另一管理员核验重置");
        StaffIdentityGuard.version(user.getVersion(),input.expectedVersion());
        String reason=StaffIdentityGuard.text(input.reason(),2,1000,"重置原因");
        attempts.hit("password-reset",actor.staffId().toString(),10,3600);
        StaffAuthServiceImpl.checkPassword(input.temporaryPassword());
        if(passwords.matches(input.temporaryPassword(),user.getPasswordHash()))throw BusinessException.validation("新密码不能与当前密码相同");
        int changed=staff.update(null,new UpdateWrapper<StaffUserEntity>().eq("id",id).eq("version",input.expectedVersion())
                .set("password_hash",passwords.encode(input.temporaryPassword())).set("password_change_required",true)
                .set("credential_version",user.getCredentialVersion()+1).set("version",user.getVersion()+1).set("updated_at",now()));
        if(changed!=1)throw BusinessException.versionConflict("员工资料已更新");
        audit.record("STAFF",actor.staffId(),"STAFF_PASSWORD_RESET","STAFF_USER",id,null,Map.of("reason",reason,"credentialVersion",user.getCredentialVersion()+1));
    }
    private LambdaQueryWrapper<WorkOrderEntity> activeOrders(Long id) {
        return new LambdaQueryWrapper<WorkOrderEntity>().eq(WorkOrderEntity::getTechnicianId,id)
                .notIn(WorkOrderEntity::getStatus,List.of("COMPLETED","CANCELLED","CLOSED_UNRESOLVED"));
    }
    private LocalDateTime now(){return clock.now();}
}
