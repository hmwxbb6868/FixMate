package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.admin.StaffBindingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/user")
public class UserStaffBindingController {
    private final StaffBindingService bindings;
    private final IdempotentExecutor executor;
    public UserStaffBindingController(StaffBindingService bindings,IdempotentExecutor executor) {this.bindings=bindings;this.executor=executor;}
    @GetMapping("/workspace-access") public ApiResponse<WorkspaceAccessVO> workspace(@AuthenticationPrincipal JwtPrincipal user) {
        return ApiResponse.ok(bindings.workspace(user.userId()));
    }
    @GetMapping("/staff-bindings/current") public ApiResponse<StaffBindingVO> current(@AuthenticationPrincipal JwtPrincipal user) {
        return ApiResponse.ok(bindings.current(user.userId()));
    }
    @PostMapping("/staff-bindings") public ResponseEntity<Object> apply(@AuthenticationPrincipal JwtPrincipal user,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffBindingApplyDTO input,HttpServletRequest request) {
        return executor.execute(IdempotencyContext.forUser(user.userId(),"applyStaffBinding",key,executor.hashCommand("POST","/api/v1/user/staff-bindings",input),86400),
                ()->ResponseEntity.ok(ApiResponse.ok(bindings.apply(user.userId(),input,request.getRemoteAddr()))),()->bindings.requireCustomer(user.userId()));
    }
}
