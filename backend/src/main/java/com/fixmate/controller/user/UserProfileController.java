package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.dto.user.CurrentUserVO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.user.UserProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserProfileController {
    private final UserProfileService service;
    private final com.fixmate.common.idempotency.IdempotentExecutor executor;
    public UserProfileController(UserProfileService service, com.fixmate.common.idempotency.IdempotentExecutor executor) { this.service=service; this.executor=executor; }
    @GetMapping("/api/v1/user/me")
    public ApiResponse<CurrentUserVO> current(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(service.current(principal.userId()));
    }
    @org.springframework.web.bind.annotation.PutMapping("/api/v1/user/me")
    public org.springframework.http.ResponseEntity<Object> update(
            @org.springframework.web.bind.annotation.RequestHeader("X-Request-Id") String requestId,
            @org.springframework.web.bind.annotation.RequestHeader("Idempotency-Key") String key,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.fixmate.dto.user.UpdateCurrentUserDTO dto,
            @AuthenticationPrincipal JwtPrincipal principal) {
        var ctx=com.fixmate.common.idempotency.IdempotencyContext.forUser(principal.userId(),"updateCurrentCustomer",key,executor.hash(dto),86400);
        return executor.execute(ctx,()->org.springframework.http.ResponseEntity.ok(ApiResponse.ok(service.update(principal.userId(),dto))));
    }

}
