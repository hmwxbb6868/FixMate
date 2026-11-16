package com.fixmate.controller.technician;
import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.controller.admin.AdminCommandExecutor;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.security.*;
import com.fixmate.service.admin.JobPoolService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Validated @RestController @RequestMapping("/api/v1/technician/job-pool")
public class TechnicianJobPoolController {
 private final JobPoolService service;private final IdempotentExecutor executor;private final TokenFamilyStore families;
 public TechnicianJobPoolController(JobPoolService service,IdempotentExecutor executor,TokenFamilyStore families){this.service=service;this.executor=executor;this.families=families;}
 @GetMapping public ResponseEntity<Object> list(@AuthenticationPrincipal JwtPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return AdminCommandExecutor.ok(service.list(actor.userId(),page,size));}
 @GetMapping("/{id}") public ResponseEntity<Object> detail(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.detail(actor.userId(),id));}
 @PostMapping("/{id}/claim") public ResponseEntity<Object> claim(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody JobPoolClaimDTO input){
  var ctx=new IdempotencyContext(IdempotencyService.scopeForUser(actor.userId(),"technicianClaimJob"),key,"technicianClaimJob",executor.hashCommand("POST","/api/v1/technician/job-pool/"+id+"/claim",input),actor.familyId(),false,86400,true);
  return AdminCommandExecutor.noStore(executor.executeCurrentRead(ctx,()->ResponseEntity.ok(ApiResponse.ok(service.claim(actor.userId(),id,input))),()->{if(!families.isFamilyActive(actor.familyId()))throw BusinessException.unauthorized("登录已失效");service.requireClaimAccess(actor.userId(),id);}));
 }
}
