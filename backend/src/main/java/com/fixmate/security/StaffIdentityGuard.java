package com.fixmate.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fixmate.entity.StaffUserEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.StaffUserMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** 低频员工身份管理按ID顺序锁员工行，串行化末位管理员、停用和绑定决策。调用方必须在事务中。 */
@Service
public class StaffIdentityGuard {
    private final StaffUserMapper staff;
    private final TokenFamilyStore families;
    public StaffIdentityGuard(StaffUserMapper staff, TokenFamilyStore families) { this.staff=staff; this.families=families; }
    public List<StaffUserEntity> lockDirectory() {
        return staff.selectList(new LambdaQueryWrapper<StaffUserEntity>().orderByAsc(StaffUserEntity::getId).last("FOR UPDATE"));
    }
    public List<StaffUserEntity> lockIds(java.util.Collection<Long> ids){return staff.selectList(new LambdaQueryWrapper<StaffUserEntity>().in(StaffUserEntity::getId,new java.util.TreeSet<>(ids)).orderByAsc(StaffUserEntity::getId).last("FOR UPDATE"));}
    public StaffUserEntity requireAdmin(StaffPrincipal p, List<StaffUserEntity> locked) {
        return requireRoles(p,locked,"ADMIN");
    }
    public StaffUserEntity requireRoles(StaffPrincipal p,List<StaffUserEntity> locked,String... roles) {
        if(p==null)throw BusinessException.unauthorized("登录已失效");
        StaffUserEntity actor=target(locked,p.staffId());
        if(!"ENABLED".equals(actor.getStatus()) || !Objects.equals(actor.getCredentialVersion(),p.credentialVersion())
                || !families.isStaffFamilyActive(p.familyId(),p.staffId(),p.credentialVersion()))
            throw BusinessException.unauthorized("登录已失效");
        if(!java.util.Arrays.asList(roles).contains(actor.getRole()) || Boolean.TRUE.equals(actor.getPasswordChangeRequired()))
            throw BusinessException.forbidden("无权执行该操作");
        return actor;
    }
    public StaffUserEntity target(List<StaffUserEntity> locked, Long id) {
        return locked.stream().filter(s->s.getId().equals(id)).findFirst().orElseThrow(()->BusinessException.notFound("员工不存在"));
    }
    public static void version(Integer actual, Integer expected) {
        if(!Objects.equals(actual,expected))throw BusinessException.versionConflict("资料已更新，请刷新后重试");
    }
    public static String text(String value,int minimum,int maximum,String label) {
        String clean=value==null?"":value.strip();
        if(clean.length()<minimum || clean.length()>maximum || clean.codePoints().anyMatch(Character::isISOControl))
            throw BusinessException.validation(label+"格式不正确");
        return clean;
    }
}
