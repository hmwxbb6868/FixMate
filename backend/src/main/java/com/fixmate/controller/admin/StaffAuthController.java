package com.fixmate.controller.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.service.admin.StaffAuthService;
import com.fixmate.security.*;
import com.fixmate.config.JwtProperties;
import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;

@org.springframework.validation.annotation.Validated
@RestController @RequestMapping("/api/v1/admin/auth")
public class StaffAuthController {
    private final StaffAuthService auth;private final StaffAccessService access;private final IdempotentExecutor executor;private final JwtProperties properties;
    public StaffAuthController(StaffAuthService auth,StaffAccessService access,IdempotentExecutor executor,JwtProperties properties){this.auth=auth;this.access=access;this.executor=executor;this.properties=properties;}
    @PostMapping("/login") public ResponseEntity<Object> login(@Valid @RequestBody StaffLoginDTO dto,@RequestHeader("Idempotency-Key") String key,HttpServletRequest request) {
        var p=auth.prepareLogin(dto,key,request.getRemoteAddr());
        var ctx=new IdempotencyContext(IdempotencyService.scopeForAnonymous("staffLogin"),key,"staffLogin",auth.secretHash(IdempotentExecutor.commandPayload("POST","/api/v1/admin/auth/login",dto)),p.familyId(),true,properties.getAccessTtlSeconds(),true);
        return executor.execute(ctx,()->ResponseEntity.ok(ApiResponse.ok(auth.login(p))),()->access.active(p));
    }
    @PostMapping("/refresh") public ResponseEntity<Object> refresh(@Valid @RequestBody com.fixmate.dto.user.RefreshTokenDTO dto,@RequestHeader("Idempotency-Key") String key) {
        var p=auth.refreshPrincipal(dto.refreshToken(),true);
        var ctx=IdempotencyContext.forStaffFamily(p.familyId(),"staffRefresh",key,executor.hashCommand("POST","/api/v1/admin/auth/refresh",dto),true,true,properties.getAccessTtlSeconds());
        return executor.execute(ctx,()->ResponseEntity.ok(ApiResponse.ok(auth.refresh(p,key))),()->access.active(p));
    }
    @PostMapping("/logout") public ResponseEntity<Object> logout(@Valid @RequestBody com.fixmate.dto.user.RefreshTokenDTO dto,@RequestHeader("Idempotency-Key") String key) {
        var p=auth.refreshPrincipal(dto.refreshToken(),false);
        var ctx=IdempotencyContext.forStaffFamily(p.familyId(),"staffLogout",key,executor.hashCommand("POST","/api/v1/admin/auth/logout",dto),false,false,86400);
        return executor.execute(ctx,()->{auth.logout(p,key);return ResponseEntity.ok(ApiResponse.ok());},()->access.credential(p));
    }
    @PostMapping("/password/change") public ResponseEntity<Object> change(@Valid @RequestBody StaffPasswordChangeDTO dto,@RequestHeader("Idempotency-Key") String key,@AuthenticationPrincipal StaffPrincipal p) {
        return executor.execute(IdempotencyContext.forStaff(p.staffId(),"changeStaffPassword",key,auth.secretHash(IdempotentExecutor.commandPayload("POST","/api/v1/admin/auth/password/change",dto)),86400),()->{auth.changePassword(p,dto);return ResponseEntity.ok(ApiResponse.ok());},()->access.active(p));
    }
}
