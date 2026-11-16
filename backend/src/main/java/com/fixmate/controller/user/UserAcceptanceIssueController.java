package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.config.JwtProperties;
import com.fixmate.controller.admin.AdminCommandExecutor;
import com.fixmate.dto.admin.StaffIdentityContracts.AcceptanceIssueDTO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.user.UserAcceptanceIssueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController
public class UserAcceptanceIssueController {
    private final UserAcceptanceIssueService service;private final IdempotentExecutor executor;private final JwtProperties properties;
    public UserAcceptanceIssueController(UserAcceptanceIssueService service,IdempotentExecutor executor,JwtProperties properties){this.service=service;this.executor=executor;this.properties=properties;}
    @PostMapping("/api/v1/user/work-orders/{id}/acceptance-issues")
    public ResponseEntity<Object> raise(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AcceptanceIssueDTO input){
        String operation="raiseAcceptanceIssue";var ctx=new IdempotencyContext(IdempotencyService.scopeForUser(actor.userId(),operation),key,operation,executor.hashCommand("POST","/api/v1/user/work-orders/"+id+"/acceptance-issues",input),actor.familyId(),true,Math.min(900,properties.getAccessTtlSeconds()),true);
        return AdminCommandExecutor.noStore(executor.executeCurrentRead(ctx,()->ResponseEntity.ok(ApiResponse.ok(service.raise(actor.userId(),id,input))),()->service.guard(actor.userId(),id)));
    }
}
