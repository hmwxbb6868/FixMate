package com.fixmate.controller.admin;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.StaffBindingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@org.springframework.validation.annotation.Validated
@RestController @RequestMapping("/api/v1/admin")
public class StaffBindingController {
    private final com.fixmate.config.JwtProperties jwt;
    private final StaffBindingService bindings;
    private final StaffAccessService access;
    private final IdempotentExecutor executor;
    public StaffBindingController(StaffBindingService bindings,StaffAccessService access,IdempotentExecutor executor,com.fixmate.config.JwtProperties jwt) {this.jwt=jwt;this.bindings=bindings;this.access=access;this.executor=executor;}
    @PostMapping("/staff-invitations")
    public ResponseEntity<Object> issueNewTechnician(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestHeader("Idempotency-Key") String key) {
        String operation="issueNewTechnicianInvitation";
        var context=IdempotencyContext.forStaffFamily(actor.familyId(),operation,key,
                executor.hashCommand("POST","/api/v1/admin/staff-invitations",null),true,true,Math.min(900,jwt.getAccessTtlSeconds()));
        return AdminCommandExecutor.noStore(executor.execute(context,
                ()->ResponseEntity.ok(ApiResponse.ok(bindings.issueNewTechnician(actor))),()->access.require(actor,"ADMIN")));
    }
    @GetMapping("/staff-invitations") public ResponseEntity<Object> invitations(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) Long staffId,@RequestParam(required=false) String status) {
        return AdminCommandExecutor.ok(bindings.invitations(actor,page,size,staffId,status));
    }
    @GetMapping("/staff-invitations/{id}") public ResponseEntity<Object> invitation(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id) {
        return AdminCommandExecutor.ok(bindings.invitation(actor,id));
    }
    @PostMapping("/staff-invitations/{id}/revoke") public ResponseEntity<Object> revokeInvitation(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonCommandDTO input) {
        return AdminCommandExecutor.noStore(executor.execute(IdempotencyContext.forSensitiveStaff(actor.staffId(),"revokeStaffInvitation",key,executor.hashCommand("POST","/api/v1/admin/staff-invitations/"+id+"/revoke",input),jwt.getAccessTtlSeconds()),
                ()->ResponseEntity.ok(ApiResponse.ok(bindings.revokeInvitation(actor,id,input))),()->access.require(actor,"ADMIN")));
    }
    @GetMapping("/staff-bindings") public ResponseEntity<Object> bindings(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="PENDING") String status) {
        return AdminCommandExecutor.ok(bindings.bindings(actor,page,size,status));
    }
    @GetMapping("/staff-bindings/{id}") public ResponseEntity<Object> binding(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id) {
        return AdminCommandExecutor.ok(bindings.binding(actor,id));
    }
    @PostMapping("/staff-bindings/{id}/confirm") public ResponseEntity<Object> confirm(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffBindingDecisionDTO input) {return decision(actor,id,key,input,"ACTIVE","confirmStaffBinding");}
    @PostMapping("/staff-bindings/{id}/reject") public ResponseEntity<Object> reject(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffBindingDecisionDTO input) {return decision(actor,id,key,input,"REJECTED","rejectStaffBinding");}
    @PostMapping("/staff-bindings/{id}/revoke") public ResponseEntity<Object> revoke(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @jakarta.validation.constraints.Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffBindingDecisionDTO input) {return decision(actor,id,key,input,"REVOKED","revokeStaffBinding");}
    private ResponseEntity<Object> decision(StaffPrincipal actor,Long id,String key,StaffBindingDecisionDTO input,String next,String operation) {
        return AdminCommandExecutor.noStore(executor.execute(IdempotencyContext.forSensitiveStaff(actor.staffId(),operation,key,executor.hashCommand("POST","/api/v1/admin/staff-bindings/"+id+"/"+java.util.Map.of("ACTIVE","confirm","REJECTED","reject","REVOKED","revoke").get(next),input),jwt.getAccessTtlSeconds()),
                ()->ResponseEntity.ok(ApiResponse.ok(bindings.decide(actor,id,input,next))),()->access.require(actor,"ADMIN")));
    }
}
