package com.fixmate.controller.admin;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.PricingConfigurationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.*;
import com.fixmate.config.JwtProperties;
import java.util.function.Supplier;
@Validated @RestController @RequestMapping("/api/v1/admin")
public class PricingConfigurationController {
    private final PricingConfigurationService service;
    private final IdempotentExecutor executor;
    private final JwtProperties properties;
    public PricingConfigurationController(PricingConfigurationService service,IdempotentExecutor executor,JwtProperties properties){this.service=service;this.executor=executor;this.properties=properties;}
    private ResponseEntity<Object> run(StaffPrincipal actor,String op,String method,String path,String key,Object input,Supplier<?> action){
        var ctx=new IdempotencyContext("STAFF:"+actor.staffId()+":"+op,key,op,executor.hashCommand(method,path,input),null,true,Math.min(900,properties.getAccessTtlSeconds()),false);
        return AdminCommandExecutor.noStore(executor.executeCurrentRead(ctx,()->ResponseEntity.ok(ApiResponse.ok(action.get())),()->service.guard(actor)));
    }
    @GetMapping("/service-items") public ResponseEntity<Object> listItem(@AuthenticationPrincipal StaffPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return AdminCommandExecutor.ok(service.listItems(actor,null,page,size,null));}
    @GetMapping("/service-items/{id}") public ResponseEntity<Object> getItem(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.getItem(actor,id));}
    @PostMapping("/service-items") public ResponseEntity<Object> createItem(@AuthenticationPrincipal StaffPrincipal actor,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ServiceItemInputDTO input){return run(actor,"createServiceItems","POST","/api/v1/admin/service-items",key,input,()->service.createItem(actor,input));}
    @PutMapping("/service-items/{id}") public ResponseEntity<Object> updateItem(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ServiceItemUpdateDTO input){return run(actor,"updateServiceItems","PUT","/api/v1/admin/service-items/"+id,key,input,()->service.updateItem(actor,id,input));}
    @PostMapping("/service-items/{id}/status") public ResponseEntity<Object> statusItem(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ConfigStatusDTO input){return run(actor,"statusServiceItems","POST","/api/v1/admin/service-items/"+id + "/status",key,input,()->service.statusItem(actor,id,input));}
    @GetMapping("/pricing-rules") public ResponseEntity<Object> listRule(@AuthenticationPrincipal StaffPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return AdminCommandExecutor.ok(service.listRules(actor,null,page,size,null,null));}
    @GetMapping("/pricing-rules/{id}") public ResponseEntity<Object> getRule(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.getRule(actor,id));}
    @PostMapping("/pricing-rules") public ResponseEntity<Object> createRule(@AuthenticationPrincipal StaffPrincipal actor,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody PricingRuleInputDTO input){return run(actor,"createPricingRules","POST","/api/v1/admin/pricing-rules",key,input,()->service.createRule(actor,input));}
    @PutMapping("/pricing-rules/{id}") public ResponseEntity<Object> updateRule(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody PricingRuleUpdateDTO input){return run(actor,"updatePricingRules","PUT","/api/v1/admin/pricing-rules/"+id,key,input,()->service.updateRule(actor,id,input));}
    @PostMapping("/pricing-rules/{id}/status") public ResponseEntity<Object> statusRule(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ConfigStatusDTO input){return run(actor,"statusPricingRules","POST","/api/v1/admin/pricing-rules/"+id + "/status",key,input,()->service.statusRule(actor,id,input));}
}
