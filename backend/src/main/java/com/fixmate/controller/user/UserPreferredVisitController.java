package com.fixmate.controller.user;
import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.controller.admin.AdminCommandExecutor;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.exception.BusinessException;
import com.fixmate.security.*;
import com.fixmate.service.common.VisitSchedulingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Validated @RestController @RequestMapping("/api/v1/user/work-orders/{id}/preferred-visit")
public class UserPreferredVisitController {
 private final VisitSchedulingService visits;private final IdempotentExecutor executor;private final TokenFamilyStore families;
 public UserPreferredVisitController(VisitSchedulingService visits,IdempotentExecutor executor,TokenFamilyStore families){this.visits=visits;this.executor=executor;this.families=families;}
 @GetMapping public ResponseEntity<Object> get(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(visits.get(actor.userId(),id));}
 @PostMapping public ResponseEntity<Object> select(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody PreferredVisitDTO input){
  var ctx=new IdempotencyContext(IdempotencyService.scopeForUser(actor.userId(),"selectPreferredVisit"),key,"selectPreferredVisit",executor.hashCommand("POST","/api/v1/user/work-orders/"+id+"/preferred-visit",input),actor.familyId(),false,86400,true);
  return AdminCommandExecutor.noStore(executor.executeCurrentRead(ctx,()->ResponseEntity.ok(ApiResponse.ok(visits.select(actor.userId(),id,input))),()->{if(!families.isFamilyActive(actor.familyId()))throw BusinessException.unauthorized("登录已失效");visits.get(actor.userId(),id);}));
 }
}
