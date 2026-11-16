package com.fixmate.controller.admin;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.config.JwtProperties;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.AdminRepairIntakeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1/admin/repair-requests")
public class AdminRepairIntakeController {
    private final AdminRepairIntakeService service;
    private final StaffAccessService access;
    private final IdempotentExecutor executor;
    private final JwtProperties jwt;
    public AdminRepairIntakeController(AdminRepairIntakeService service,StaffAccessService access,IdempotentExecutor executor,JwtProperties jwt) {
        this.service=service;this.access=access;this.executor=executor;this.jwt=jwt;
    }
    @GetMapping public ResponseEntity<Object> list(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) String status,
            @RequestParam(defaultValue="false") boolean pendingReview,
            @RequestParam(required=false) @jakarta.validation.constraints.Size(max=32) String repairNo,
            @RequestParam(defaultValue="false") boolean pendingCustomerConfirmation) {
        return noStore(ResponseEntity.ok(ApiResponse.ok(service.list(actor,page,size,status,pendingReview,repairNo,pendingCustomerConfirmation))));
    }
    @GetMapping("/{id}") public ResponseEntity<Object> detail(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id) {
        return noStore(ResponseEntity.ok(ApiResponse.ok(service.detail(actor,id))));
    }
    @PostMapping("/{id}/manual-triage") public ResponseEntity<Object> triage(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ManualTriageDTO input) {
        return execute(actor,id,key,"manualTriageRepairRequest","manual-triage",input,()->service.triage(actor,id,input));
    }
    @PostMapping("/{id}/service-contact/confirm") public ResponseEntity<Object> contact(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ServiceContactConfirmationDTO input) {
        return execute(actor,id,key,"confirmRepairServiceContact","service-contact/confirm",input,()->service.confirmContact(actor,id,input));
    }
    private ResponseEntity<Object> execute(StaffPrincipal actor,Long id,String key,String operation,String suffix,Object input,
            java.util.function.Supplier<AdminRepairRequestDetailVO> action) {
        var context=new IdempotencyContext("STAFF:"+actor.staffId()+":"+operation,key,operation,
                executor.hashCommand("POST","/api/v1/admin/repair-requests/"+id+"/"+suffix,input),actor.familyId(),true,jwt.getAccessTtlSeconds(),true);
        return noStore(executor.execute(context,()->ResponseEntity.ok(ApiResponse.ok(action.get())),()->access.require(actor,"ADMIN","DISPATCHER")));
    }
    private ResponseEntity<Object> noStore(ResponseEntity<?> response) {
        return ResponseEntity.status(response.getStatusCode()).cacheControl(CacheControl.noStore()).body(response.getBody());
    }
}
