package com.fixmate.controller.admin;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@org.springframework.validation.annotation.Validated
@RestController @RequestMapping("/api/v1/admin/staff")
public class StaffDirectoryController {
    private final StaffDirectoryService directory;
    private final StaffBindingService bindings;
    private final StaffAccessService access;
    private final StaffAuthService auth;
    private final IdempotentExecutor executor;
    private final com.fixmate.config.JwtProperties jwt;
    public StaffDirectoryController(StaffDirectoryService directory,StaffBindingService bindings,StaffAccessService access,
            StaffAuthService auth,IdempotentExecutor executor,com.fixmate.config.JwtProperties jwt) {
        this.directory=directory;this.bindings=bindings;this.access=access;this.auth=auth;this.executor=executor;this.jwt=jwt;
    }
    @GetMapping public ResponseEntity<Object> list(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return AdminCommandExecutor.ok(directory.list(actor,page,size));
    }
    @GetMapping("/options") public ResponseEntity<Object> options(@AuthenticationPrincipal StaffPrincipal actor) {
        return AdminCommandExecutor.ok(directory.options(actor));
    }
    @PostMapping public ResponseEntity<Object> create(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffCreateDTO input) {
        return AdminCommandExecutor.noStore(executor.execute(IdempotencyContext.forSensitiveStaff(actor.staffId(),"createStaff",key,auth.secretHash(IdempotentExecutor.commandPayload("POST","/api/v1/admin/staff",input)),jwt.getAccessTtlSeconds()),
                ()->ResponseEntity.ok(ApiResponse.ok(directory.create(actor,input))),()->access.require(actor,"ADMIN")));
    }
    @PostMapping("/{id}/status") public ResponseEntity<Object> status(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffStatusDTO input) {
        return AdminCommandExecutor.noStore(executor.execute(IdempotencyContext.forSensitiveStaff(actor.staffId(),"setStaffStatus",key,executor.hashCommand("POST","/api/v1/admin/staff/"+id+"/status",input),jwt.getAccessTtlSeconds()),
                ()->ResponseEntity.ok(ApiResponse.ok(directory.status(actor,id,input))),()->access.require(actor,"ADMIN")));
    }
    @PostMapping("/{id}/password-reset") public ResponseEntity<Object> reset(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffPasswordResetDTO input) {
        String hash=auth.secretHash(IdempotentExecutor.commandPayload("POST","/api/v1/admin/staff/"+id+"/password-reset",input));
        return executor.execute(IdempotencyContext.forStaff(actor.staffId(),"resetStaffPassword",key,hash,86400),
                ()->{directory.resetPassword(actor,id,input);return ResponseEntity.ok(ApiResponse.ok());},()->access.require(actor,"ADMIN"));
    }
    @PostMapping("/{id}/invitations") public ResponseEntity<Object> issue(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffInvitationCreateDTO input) {
        var ctx=new IdempotencyContext("STAFF:"+actor.staffId()+":issueStaffInvitation",key,"issueStaffInvitation",executor.hashCommand("POST","/api/v1/admin/staff/"+id+"/invitations",input),
                actor.familyId(),true,Math.min(900,jwt.getAccessTtlSeconds()),true);
        return AdminCommandExecutor.noStore(executor.execute(ctx,()->ResponseEntity.ok(ApiResponse.ok(bindings.issue(actor,id,input))),()->access.require(actor,"ADMIN")));
    }
}
