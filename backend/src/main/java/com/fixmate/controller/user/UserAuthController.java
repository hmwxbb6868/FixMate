package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.IdempotencyContext;
import com.fixmate.common.idempotency.IdempotentExecutor;
import com.fixmate.config.JwtProperties;
import com.fixmate.dto.user.RefreshTokenDTO;
import com.fixmate.dto.user.TokenVO;
import com.fixmate.dto.user.WxLoginDTO;
import com.fixmate.dto.user.WxPhoneBindDTO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.security.JwtService;
import com.fixmate.service.user.UserAuthService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/auth")
public class UserAuthController {

    private final UserAuthService authService;
    private final IdempotentExecutor executor;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final com.fixmate.common.ratelimit.RateLimitService rateLimitService;

    public UserAuthController(UserAuthService authService, IdempotentExecutor executor,
                              JwtService jwtService, JwtProperties jwtProperties,
                              com.fixmate.common.ratelimit.RateLimitService rateLimitService) {
        this.authService = authService;
        this.executor = executor;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/wx-login")
    public ResponseEntity<Object> wxLogin(@RequestHeader("X-Request-Id") String requestId,
                                          @RequestHeader("Idempotency-Key") String key,
                                          @Valid @RequestBody WxLoginDTO dto,
                                          jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        rateLimitService.check("wx-login", ip, 10, java.time.Duration.ofMinutes(1));
        IdempotencyContext ctx = IdempotencyContext.forAnonymous("userWxLogin", key,
                executor.hash(dto), true, jwtProperties.getAccessTtlSeconds());
        return executor.execute(ctx, () -> {
            TokenVO token = authService.login(dto.code());
            return ResponseEntity.ok(ApiResponse.ok(token));
        });
    }

    @PostMapping("/bind-phone")
    public ResponseEntity<Object> bindPhone(@RequestHeader("X-Request-Id") String requestId,
                                            @RequestHeader("Idempotency-Key") String key,
                                            @Valid @RequestBody WxPhoneBindDTO dto,
                                            @org.springframework.security.core.annotation.AuthenticationPrincipal
                                            JwtPrincipal principal) {
        // 已登录前提下，当前主体仍不支持该能力：返回 422 PHONE_BIND_UNAVAILABLE
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "userBindPhone", key,
                executor.hash(dto), 86400);
        return executor.execute(ctx,
                () -> ResponseEntity.ok(ApiResponse.ok(authService.bindPhone(principal.userId(), dto.phoneCode()))));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(@RequestHeader("X-Request-Id") String requestId,
                                          @RequestHeader("Idempotency-Key") String key,
                                          @Valid @RequestBody RefreshTokenDTO dto) {
        JwtPrincipal principal = parseRefresh(dto.refreshToken());
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "登录已失效"));
        }
        IdempotencyContext ctx = IdempotencyContext.forFamily(principal.familyId(), "userRefreshToken", key,
                executor.hash(dto), true, true, jwtProperties.getAccessTtlSeconds());
        return executor.execute(ctx, () -> {
            TokenVO token = authService.refresh(dto.refreshToken(), key);
            return ResponseEntity.ok(ApiResponse.ok(token));
        });
    }

    @PostMapping("/logout")
    public ResponseEntity<Object> logout(@RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @Valid @RequestBody RefreshTokenDTO dto) {
        JwtPrincipal principal = parseRefresh(dto.refreshToken());
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("UNAUTHORIZED", "登录已失效"));
        }
        // family 已撤销时，同幂等键重试返回 200；不同幂等键由 Service 抛 401
        IdempotencyContext ctx = IdempotencyContext.forFamily(principal.familyId(), "userLogout", key,
                executor.hash(dto), false, false, jwtProperties.getAccessTtlSeconds());
        return executor.execute(ctx, () -> {
            authService.logout(dto.refreshToken(), key);
            return ResponseEntity.ok(ApiResponse.ok());
        });
    }

    private JwtPrincipal parseRefresh(String refreshToken) {
        try {
            return jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException ex) {
            return null;
        }
    }
}
