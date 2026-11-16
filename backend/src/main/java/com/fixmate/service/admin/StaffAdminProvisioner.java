package com.fixmate.service.admin;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.entity.StaffUserEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.DatabaseClockMapper;
import com.fixmate.mapper.StaffUserMapper;
import com.fixmate.security.StaffIdentityGuard;
import com.fixmate.service.admin.serviceImpl.StaffAuthServiceImpl;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 仅由本地交互式初始化Runner调用，没有HTTP映射或默认凭据。 */
@Service
public class StaffAdminProvisioner {
    private final StaffUserMapper staff;
    private final StaffIdentityGuard guard;
    private final PasswordEncoder passwords;
    private final AuditLogService audit;
    private final DatabaseClockMapper clock;
    public StaffAdminProvisioner(StaffUserMapper staff,StaffIdentityGuard guard,PasswordEncoder passwords,AuditLogService audit,DatabaseClockMapper clock) {
        this.staff=staff;this.guard=guard;this.passwords=passwords;this.audit=audit;this.clock=clock;
    }
    @Transactional public void provision(String mode,String username,String name,String password) {
        String normalized=StaffIdentityGuard.text(StaffAuthServiceImpl.normalized(username),3,64,"用户名");
        StaffAuthServiceImpl.checkPassword(password);var directory=guard.lockDirectory();
        if("create".equals(mode)) {
            if(directory.stream().anyMatch(s->"ADMIN".equals(s.getRole())))throw BusinessException.stateConflict("已有管理员档案，拒绝再次初始化");
            StaffUserEntity user=new StaffUserEntity();user.setUsername(normalized);user.setName(StaffIdentityGuard.text(name,1,64,"姓名"));
            user.setRole("ADMIN");user.setStatus("ENABLED");user.setPasswordHash(passwords.encode(password));user.setVersion(0);user.setCredentialVersion(0);
            user.setPasswordChangeRequired(true);user.setCreatedAt(clock.now());user.setUpdatedAt(user.getCreatedAt());staff.insert(user);
            audit.recordSystem("STAFF_ADMIN_BOOTSTRAPPED","STAFF_USER",user.getId(),null,Map.of("role","ADMIN","passwordChangeRequired",true));
        } else if("recover".equals(mode)) {
            var target=directory.stream().filter(s->s.getUsername().equals(normalized)&&"ADMIN".equals(s.getRole())&&"ENABLED".equals(s.getStatus()))
                    .findFirst().orElseThrow(()->BusinessException.stateConflict("只允许恢复现有有效管理员，不会启用已停用账号"));
            if(directory.stream().anyMatch(s->!s.getId().equals(target.getId())&&"ADMIN".equals(s.getRole())&&"ENABLED".equals(s.getStatus())))
                throw BusinessException.stateConflict("存在另一有效管理员，请走后台核验重置流程");
            if(passwords.matches(password,target.getPasswordHash()))throw BusinessException.validation("新密码不能与当前密码相同");
            int changed=staff.update(null,new UpdateWrapper<StaffUserEntity>().eq("id",target.getId()).eq("version",target.getVersion())
                    .set("password_hash",passwords.encode(password)).set("password_change_required",true)
                    .set("credential_version",target.getCredentialVersion()+1).set("version",target.getVersion()+1).set("updated_at",clock.now()));
            if(changed!=1)throw BusinessException.versionConflict("管理员资料已更新");
            audit.recordSystem("STAFF_ADMIN_OFFLINE_RECOVERED","STAFF_USER",target.getId(),null,
                    Map.of("credentialVersion",target.getCredentialVersion()+1,"basis","OWNER_INTERACTIVE_CONSOLE"));
        } else throw BusinessException.validation("初始化模式不正确");
    }
}
