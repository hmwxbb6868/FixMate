package com.fixmate.service.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fixmate.common.api.ErrorCode;
import com.fixmate.common.audit.AuditLogService;
import com.fixmate.config.JwtProperties;
import com.fixmate.dto.user.CurrentUserVO;
import com.fixmate.dto.user.TokenVO;
import com.fixmate.entity.CustomerUserEntity;
import com.fixmate.exception.BusinessException;
import com.fixmate.mapper.CustomerUserMapper;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.security.JwtService;
import com.fixmate.security.TokenFamilyStore;
import com.fixmate.security.wechat.WechatClient;
import com.fixmate.service.user.UserAuthService;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    private final WechatClient wechatClient;
    private final CustomerUserMapper customerUserMapper;
    private final JwtService jwtService;
    private final TokenFamilyStore tokenFamilyStore;
    private final JwtProperties jwtProperties;
    private final AuditLogService auditLogService;
    private final com.fixmate.service.user.UserProfileService profiles;

    public UserAuthServiceImpl(WechatClient wechatClient,
                               CustomerUserMapper customerUserMapper,
                               JwtService jwtService,
                               TokenFamilyStore tokenFamilyStore,
                               JwtProperties jwtProperties,
                               AuditLogService auditLogService, com.fixmate.service.user.UserProfileService profiles) {
        this.wechatClient = wechatClient;
        this.customerUserMapper = customerUserMapper;
        this.jwtService = jwtService;
        this.tokenFamilyStore = tokenFamilyStore;
        this.jwtProperties = jwtProperties;
        this.auditLogService = auditLogService;
        this.profiles = profiles;
    }

    @Override
    @Transactional
    public TokenVO login(String code) {
        WechatClient.WechatSession session = wechatClient.code2Session(code);
        CustomerUserEntity user = customerUserMapper.selectOne(new LambdaQueryWrapper<CustomerUserEntity>()
                .eq(CustomerUserEntity::getOpenid, session.openid()));
        if (user == null) {
            user = new CustomerUserEntity();
            user.setOpenid(session.openid());
            user.setUnionid(session.unionid());
            user.setStatus("ENABLED");
            user.setPhoneVerified(false);
            user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
            user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            customerUserMapper.insert(user);
        } else {
            if (!"ENABLED".equals(user.getStatus())) {
                throw BusinessException.forbidden("账号不可用");
            }
            customerUserMapper.update(null, new UpdateWrapper<CustomerUserEntity>()
                    .eq("id", user.getId())
                    .set("last_login_at", LocalDateTime.now(ZoneOffset.UTC))
                    .set("updated_at", LocalDateTime.now(ZoneOffset.UTC)));
        }

        String familyId = tokenFamilyStore.createFamily(user.getId(), refreshTtl());
        auditLogService.recordCustomer(user.getId(), "CUSTOMER_LOGIN", "CUSTOMER_USER", user.getId(), null, null);
        return issueTokens(user, familyId);
    }

    @Override
    public CurrentUserVO bindPhone(Long userId, String phoneCode) {
        throw new BusinessException(ErrorCode.Codes.PHONE_BIND_UNAVAILABLE, HttpStatus.UNPROCESSABLE_ENTITY,
                "当前小程序主体不支持微信手机号快速验证");
    }

    @Override
    public TokenVO refresh(String refreshToken, String idempotencyKey) {
        JwtPrincipal principal = parseRefresh(refreshToken);
        if (!tokenFamilyStore.isFamilyActive(principal.familyId())) {
            throw BusinessException.unauthorized("登录已失效");
        }
        TokenFamilyStore.RefreshUse use = tokenFamilyStore.markRefreshUsed(
                principal.tokenId(), idempotencyKey, refreshTtl());
        if (use == TokenFamilyStore.RefreshUse.REPLAY) {
            // 不同幂等键复用旧 Refresh Token：判定为重放，撤销整条家族
            tokenFamilyStore.revokeFamily(principal.familyId(), idempotencyKey, refreshTtl());
            throw BusinessException.unauthorized("检测到令牌重放，已退出登录");
        }
        // SAME_KEY_RETRY 是同一逻辑动作重试：允许重新签发，避免响应丢失后误判重放
        CustomerUserEntity user = customerUserMapper.selectById(principal.userId());
        if (user == null || !"ENABLED".equals(user.getStatus())) {
            throw BusinessException.unauthorized("账号不可用");
        }
        auditLogService.recordCustomer(user.getId(), "CUSTOMER_TOKEN_REFRESHED", "CUSTOMER_USER", user.getId(),
                null, java.util.Map.of("familyId", String.valueOf(principal.familyId())));
        return issueTokens(user, principal.familyId());
    }

    @Override
    public void logout(String refreshToken, String idempotencyKey) {
        JwtPrincipal principal = parseRefresh(refreshToken);
        if (!tokenFamilyStore.revokeFamily(principal.familyId(), idempotencyKey, refreshTtl())) {
            throw BusinessException.unauthorized("登录已失效");
        }
        // 审计失败会回滚数据库；Redis 保留首个操作键，同键恢复时再补记审计。
        auditLogService.recordCustomer(principal.userId(), "CUSTOMER_LOGOUT", "CUSTOMER_USER",
                principal.userId(), null, null);
    }

    private TokenVO issueTokens(CustomerUserEntity user, String familyId) {
        String access = jwtService.issueAccessToken(user.getId(), familyId);
        String refresh = jwtService.issueRefreshToken(user.getId(), familyId);
        return new TokenVO(access, refresh, jwtService.getAccessTtlSeconds(), toVO(user));
    }

    private JwtPrincipal parseRefresh(String refreshToken) {
        try {
            return jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException ex) {
            throw BusinessException.unauthorized("登录已失效");
        }
    }

    private CurrentUserVO toVO(CustomerUserEntity user) {
        var current=profiles.current(user.getId());
        return new CurrentUserVO(current.id(),current.nickname(),current.avatarUrl(),
                maskPhone(current.phone()),current.phoneVerified());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private Duration refreshTtl() {
        return Duration.of(jwtProperties.getRefreshTtlSeconds(), ChronoUnit.SECONDS);
    }
}
