package com.fixmate.controller.admin;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.config.JwtProperties;
import com.fixmate.dto.admin.StaffIdentityContracts.ReasonCommandDTO;
import com.fixmate.security.StaffPrincipal;
import com.fixmate.service.admin.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1/admin")
public class AdminBusinessController {
    private final BusinessTodoService todos;private final AuditReadService audits;private final AdminDashboardService dashboard;
    private final IdempotentExecutor executor;private final JwtProperties properties;
    public AdminBusinessController(BusinessTodoService todos,AuditReadService audits,AdminDashboardService dashboard,IdempotentExecutor executor,JwtProperties properties){this.todos=todos;this.audits=audits;this.dashboard=dashboard;this.executor=executor;this.properties=properties;}
    @GetMapping("/dashboard") public ResponseEntity<Object> dashboard(@AuthenticationPrincipal StaffPrincipal actor){return AdminCommandExecutor.ok(dashboard.get(actor));}
    @GetMapping("/urges") public ResponseEntity<Object> urges(@AuthenticationPrincipal StaffPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(defaultValue="PENDING") @Pattern(regexp="PENDING|HANDLED|INVALIDATED") String status){return AdminCommandExecutor.ok(todos.list(actor,false,page,size,status));}
    @GetMapping("/human-handoffs") public ResponseEntity<Object> handoffs(@AuthenticationPrincipal StaffPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(defaultValue="PENDING") @Pattern(regexp="ALL|PENDING|HANDLED") String status){return AdminCommandExecutor.ok(todos.list(actor,true,page,size,status));}
    @PostMapping("/urges/{id}/acknowledge") public ResponseEntity<Object> handleUrge(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonCommandDTO input){return handle(actor,false,id,key,input);}
    @PostMapping("/human-handoffs/{id}/resolve") public ResponseEntity<Object> handleHandoff(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonCommandDTO input){return handle(actor,true,id,key,input);}
    private ResponseEntity<Object> handle(StaffPrincipal actor,boolean handoff,Long id,String key,ReasonCommandDTO input){String operation=handoff?"resolveHumanHandoffs":"resolveUrges",path="/api/v1/admin/"+(handoff?"human-handoffs/":"urges/")+id+(handoff?"/resolve":"/acknowledge");
        var ctx=new IdempotencyContext("STAFF:"+actor.staffId()+":"+operation,key,operation,executor.hashCommand("POST",path,input),null,true,Math.min(900,properties.getAccessTtlSeconds()),false);
        return AdminCommandExecutor.noStore(executor.executeCurrentRead(ctx,()->ResponseEntity.ok(ApiResponse.ok(todos.handle(actor,handoff,id,input))),()->todos.guard(actor)));
    }
    @GetMapping("/audit-logs") public ResponseEntity<Object> auditList(@AuthenticationPrincipal StaffPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(required=false) @Positive Long workOrderId){return AdminCommandExecutor.ok(audits.list(actor,page,size,workOrderId));}
    @GetMapping("/audit-logs/{id}") public ResponseEntity<Object> auditDetail(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(audits.detail(actor,id));}
}
