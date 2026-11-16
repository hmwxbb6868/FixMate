package com.fixmate.service.admin.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.entity.StaffUserEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.StaffUserMapper;
import com.fixmate.security.*;
import com.fixmate.config.JwtProperties;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.service.admin.StaffAuthService;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAuthServiceImpl implements StaffAuthService {
    private final com.fixmate.mapper.DatabaseClockMapper clock;
    private final StaffUserMapper users;private final PasswordEncoder passwords;private final StaffJwtService jwt;
    private final TokenFamilyStore families;private final StaffAccessService access;private final JwtProperties properties;
    private final StaffAttemptLimiter attempts;private final AuditLogService audit;
    private final ObjectMapper json;private final String dummy;
    public StaffAuthServiceImpl(StaffUserMapper users,PasswordEncoder passwords,StaffJwtService jwt,TokenFamilyStore families,
        StaffAccessService access,JwtProperties properties,StaffAttemptLimiter attempts,AuditLogService audit,ObjectMapper json,com.fixmate.mapper.DatabaseClockMapper clock) {
        this.clock=clock;this.users=users;this.passwords=passwords;this.jwt=jwt;this.families=families;this.access=access;this.properties=properties;
        this.attempts=attempts;this.audit=audit;this.json=json;dummy=passwords.encode(UUID.randomUUID().toString());
    }
    public static String normalized(String name){return java.text.Normalizer.normalize(name.strip(),java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);}
    @Override public StaffPrincipal prepareLogin(StaffLoginDTO dto,String key,String ip) {
        attempts.hit("login-ip",ip,30,60);
        String name=normalized(dto.username());
        var user=users.selectOne(new LambdaQueryWrapper<StaffUserEntity>().eq(StaffUserEntity::getUsername,name));
        String subject=user==null?"unknown:"+name:"staff:"+user.getId();String pair=subject+"|"+ip;
        attempts.check("login-pair",pair,5);attempts.check("login-account",subject,20);
        boolean matched=passwords.matches(dto.password(),user==null?dummy:user.getPasswordHash());
        if(user==null||!matched||!"ENABLED".equals(user.getStatus())) {
            attempts.failed("login-pair",pair,900,5);attempts.failed("login-account",subject,900,20);
            audit.recordSystem("STAFF_LOGIN_REJECTED","STAFF_AUTH",null,null,Map.of("reason","INVALID_CREDENTIALS"));
            throw BusinessException.unauthorized("账号或凭据无效");
        }
        String family=UUID.nameUUIDFromBytes(attempts.digest("LOGIN:"+user.getId()+":"+user.getCredentialVersion()+":"+key).getBytes(StandardCharsets.UTF_8)).toString();
        families.createStaffFamily(family,user.getId(),user.getCredentialVersion(),Duration.ofSeconds(properties.getRefreshTtlSeconds()));
        StaffPrincipal permit=new StaffPrincipal(user.getId(),family,"login",user.getCredentialVersion());access.active(permit);return permit;
    }
    @Override @Transactional public StaffTokenVO login(StaffPrincipal permit) {
        var user=lock(permit.staffId());validate(permit,user);
        users.update(null,new UpdateWrapper<StaffUserEntity>().eq("id",user.getId()).set("last_login_at",clock.now()));
        audit.record("STAFF",user.getId(),"STAFF_LOGIN","STAFF_USER",user.getId(),null,null);
        return tokens(user,permit.familyId());
    }
    @Override public StaffPrincipal refreshPrincipal(String token,boolean activeFamily) {
        StaffPrincipal p;
        try {p=jwt.parse(token,"REFRESH");}catch(io.jsonwebtoken.JwtException e){throw BusinessException.unauthorized("登录已失效");}
        if(activeFamily)access.active(p);else access.credential(p);return p;
    }
    @Override @Transactional public StaffTokenVO refresh(StaffPrincipal p,String key) {
        var user=lock(p.staffId());validate(p,user);
        var use=families.markRefreshUsed(p.tokenId(),key,Duration.ofSeconds(properties.getRefreshTtlSeconds()));
        if(use==TokenFamilyStore.RefreshUse.REPLAY){families.revokeFamily(p.familyId(),key,Duration.ofSeconds(properties.getRefreshTtlSeconds()));throw BusinessException.unauthorized("登录已失效");}
        return tokens(user,p.familyId());
    }
    @Override public void logout(StaffPrincipal p,String key) {
        access.credential(p);
        if(!families.revokeFamily(p.familyId(),key,Duration.ofSeconds(properties.getRefreshTtlSeconds())))throw BusinessException.unauthorized("登录已失效");
        audit.record("STAFF",p.staffId(),"STAFF_LOGOUT","STAFF_USER",p.staffId(),null,null);
    }
    @Override @Transactional public void changePassword(StaffPrincipal p,StaffPasswordChangeDTO dto) {
        attempts.check("password-change",p.staffId().toString(),5);
        var user=lock(p.staffId());validate(p,user);
        if(!passwords.matches(dto.currentPassword(),user.getPasswordHash())) {
            attempts.failed("password-change",p.staffId().toString(),900,5);
            throw BusinessException.unauthorized("账号或凭据无效");
        }
        checkPassword(dto.newPassword());
        if(passwords.matches(dto.newPassword(),user.getPasswordHash()))throw BusinessException.validation("新密码不能与当前密码相同");
        int changed=users.update(null,new UpdateWrapper<StaffUserEntity>().eq("id",user.getId()).eq("version",user.getVersion())
            .set("password_hash",passwords.encode(dto.newPassword())).set("credential_version",user.getCredentialVersion()+1)
            .set("password_change_required",false).set("version",user.getVersion()+1).set("updated_at",clock.now()));
        if(changed!=1)throw BusinessException.versionConflict("员工资料已更新，请刷新后重试");
        // DB credentialVersion is authoritative: all old STAFF families fail immediately, even if Redis revocation is unavailable.
        audit.record("STAFF",p.staffId(),"STAFF_PASSWORD_CHANGED","STAFF_USER",p.staffId(),null,Map.of("credentialVersion",user.getCredentialVersion()+1));
    }
    public static void checkPassword(String password) {
        if(password==null||!password.matches("[0-9]{6}"))throw BusinessException.validation("密码须为6位数字");
    }
    private StaffUserEntity lock(Long id){return users.selectOne(new LambdaQueryWrapper<StaffUserEntity>().eq(StaffUserEntity::getId,id).last("FOR UPDATE"));}
    private void validate(StaffPrincipal p,StaffUserEntity user){if(user==null||!"ENABLED".equals(user.getStatus())||!Objects.equals(p.credentialVersion(),user.getCredentialVersion())||!families.isStaffFamilyActive(p.familyId(),p.staffId(),p.credentialVersion()))throw BusinessException.unauthorized("登录已失效");}
    private StaffTokenVO tokens(StaffUserEntity s,String family){return new StaffTokenVO(jwt.issue(s.getId(),family,s.getCredentialVersion(),"ACCESS"),jwt.issue(s.getId(),family,s.getCredentialVersion(),"REFRESH"),(int)properties.getAccessTtlSeconds(),access.profile(s),"STAFF","fixmate-admin",Boolean.TRUE.equals(s.getPasswordChangeRequired()));}
    @Override public String secretHash(Object value){try{return attempts.digest(json.writeValueAsString(value));}catch(Exception e){throw new IllegalStateException(e);}}
}
