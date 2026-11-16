package com.fixmate.controller.technician;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.controller.admin.AdminCommandExecutor;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.security.*;
import com.fixmate.service.admin.WorkAssignmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1/technician")
public class TechnicianTaskController {
    private final WorkAssignmentService service;
    private final IdempotentExecutor executor;
    private final TokenFamilyStore families;
    public TechnicianTaskController(WorkAssignmentService service,IdempotentExecutor executor,TokenFamilyStore families) {
        this.service=service;this.executor=executor;this.families=families;
    }
    @GetMapping("/tasks") public ResponseEntity<Object> tasks(@AuthenticationPrincipal JwtPrincipal actor,
            @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return AdminCommandExecutor.ok(service.tasks(actor.userId(),page,size));
    }
    @GetMapping("/tasks/{id}") public ResponseEntity<Object> task(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id) {
        return AdminCommandExecutor.ok(service.task(actor.userId(),id));
    }
    @PostMapping("/assignments/{id}/accept") public ResponseEntity<Object> accept(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AssignmentActionDTO input) {
        return act(actor,id,key,input,"ACCEPT","accept","technicianAccept");
    }
    @PostMapping("/assignments/{id}/decline") public ResponseEntity<Object> decline(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AssignmentActionDTO input) {
        return act(actor,id,key,input,"DECLINE","decline","technicianDecline");
    }
    @PostMapping("/assignments/{id}/return-request") public ResponseEntity<Object> requestReturn(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AssignmentActionDTO input) {
        return act(actor,id,key,input,"RETURN","return-request","technicianReturnRequest");
    }
    private ResponseEntity<Object> act(JwtPrincipal actor,Long id,String key,AssignmentActionDTO input,String action,String suffix,String operation) {
        var context=new IdempotencyContext(IdempotencyService.scopeForUser(actor.userId(),operation),key,operation,
                executor.hashCommand("POST","/api/v1/technician/assignments/"+id+"/"+suffix,input),actor.familyId(),false,86400,true);
        return AdminCommandExecutor.noStore(executor.executeCurrentRead(context,()->ResponseEntity.ok(ApiResponse.ok(service.act(actor.userId(),id,input,action))),()->{
            if(!families.isFamilyActive(actor.familyId()))throw BusinessException.unauthorized("登录已失效");
            service.requireOwn(actor.userId(),id,action);
        }));
    }
}
