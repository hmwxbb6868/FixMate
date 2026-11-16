package com.fixmate.security;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.StaffUserEntity;
import com.fixmate.mapper.StaffUserMapper;
import com.fixmate.exception.BusinessException;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class StaffAccessService {
    private final StaffUserMapper staff; private final TokenFamilyStore families;
    public StaffAccessService(StaffUserMapper staff,TokenFamilyStore families) {this.staff=staff;this.families=families;}
    public StaffUserEntity credential(StaffPrincipal p) {
        StaffUserEntity user=p==null?null:staff.selectById(p.staffId());
        if(user==null||!"ENABLED".equals(user.getStatus())||!Objects.equals(user.getCredentialVersion(),p.credentialVersion()))
            throw BusinessException.unauthorized("登录已失效");
        return user;
    }
    public StaffUserEntity active(StaffPrincipal p) {
        StaffUserEntity user=credential(p);
        if(!families.isStaffFamilyActive(p.familyId(),p.staffId(),p.credentialVersion()))throw BusinessException.unauthorized("登录已失效");
        return user;
    }
    public StaffUserEntity require(StaffPrincipal p,String... roles) {
        StaffUserEntity user=active(p);
        if(!Arrays.asList(roles).contains(user.getRole()))throw BusinessException.forbidden("无权执行该操作");
        if(Boolean.TRUE.equals(user.getPasswordChangeRequired()))throw BusinessException.forbidden("请先修改初始密码");
        return user;
    }
    /** 业务写入先锁实际操作者，和账号停用/重置互斥；随后再锁业务聚合。 */
    public StaffUserEntity requireLocked(StaffPrincipal p,String... roles) {
        if(p==null)throw BusinessException.unauthorized("登录已失效");
        var user=staff.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StaffUserEntity>()
                .eq(StaffUserEntity::getId,p.staffId()).last("FOR UPDATE"));
        if(user==null||!"ENABLED".equals(user.getStatus())||!Objects.equals(user.getCredentialVersion(),p.credentialVersion())
                ||!families.isStaffFamilyActive(p.familyId(),p.staffId(),p.credentialVersion()))throw BusinessException.unauthorized("登录已失效");
        if(!Arrays.asList(roles).contains(user.getRole())||Boolean.TRUE.equals(user.getPasswordChangeRequired()))
            throw BusinessException.forbidden("无权执行该操作");
        return user;
    }
    public StaffProfileVO profile(StaffUserEntity s) {
        return new StaffProfileVO(s.getId(),s.getUsername(),s.getName(),s.getPhone(),s.getRole(),s.getStatus(),s.getVersion());
    }
    public StaffActorVO actor(Long id) {if(id==null)return null;var s=staff.selectById(id);return s==null?null:new StaffActorVO(id,s.getName());}
}
