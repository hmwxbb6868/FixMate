package com.fixmate.controller.admin;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;
import com.fixmate.service.admin.JobPoolService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Validated @RestController @RequestMapping("/api/v1/admin")
public class AdminJobPoolController {
 private final JobPoolService service;private final AdminCommandExecutor commands;
 public AdminJobPoolController(JobPoolService service,AdminCommandExecutor commands){this.service=service;this.commands=commands;}
 @GetMapping("/work-orders/{id}/job-pool") public ResponseEntity<Object> latest(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.latest(actor,id));}
 @PostMapping("/work-orders/{id}/job-pool") public ResponseEntity<Object> publish(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody JobPoolPublishDTO input){return commands.run(actor,"adminPublishJobPool",key,"/api/v1/admin/work-orders/"+id+"/job-pool",input,()->service.publish(actor,id,input));}
 @PostMapping("/job-pool/{id}/withdraw") public ResponseEntity<Object> withdraw(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody JobPoolWithdrawDTO input){return commands.run(actor,"adminWithdrawJobPool",key,"/api/v1/admin/job-pool/"+id+"/withdraw",input,()->service.withdraw(actor,id,input));}
}
