package com.fixmate.controller.admin;
import com.fixmate.controller.admin.*;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.StaffQuoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Validated @RestController @RequestMapping("/api/v1/admin")
public class AdminQuoteController {
    private final StaffQuoteService service;private final QuoteCommandExecutor commands;
    public AdminQuoteController(StaffQuoteService service,QuoteCommandExecutor commands){this.service=service;this.commands=commands;}
    @GetMapping("/work-orders/{id}/quotes") public ResponseEntity<Object> list(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.list(actor,null,id));}
    @PostMapping("/work-orders/{id}/quotes") public ResponseEntity<Object> create(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffQuoteCreateDTO input){return commands.run(actor,null,id,"adminCreateQuote","POST","/api/v1/admin/work-orders/"+id+"/quotes",key,input,false,()->service.create(actor,null,id,input));}
    @PutMapping("/quotes/{id}") public ResponseEntity<Object> update(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffQuoteUpdateDTO input){return commands.run(actor,null,service.quoteOrder(id),"adminUpdateQuote","PUT","/api/v1/admin/quotes/"+id+"",key,input,false,()->service.update(actor,null,id,input));}
    @PostMapping("/quotes/{id}/submit") public ResponseEntity<Object> submit(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QuoteSubmitCommandDTO input){return commands.run(actor,null,service.quoteOrder(id),"adminSubmitQuote","POST","/api/v1/admin/quotes/"+id+"/submit",key,input,false,()->service.submit(actor,null,id,input));}
    @PostMapping("/quotes/{id}/withdraw") public ResponseEntity<Object> withdraw(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QuoteWithdrawCommandDTO input){return commands.run(actor,null,service.quoteOrder(id),"adminWithdrawQuote","POST","/api/v1/admin/quotes/"+id+"/withdraw",key,input,false,()->service.withdraw(actor,null,id,input));}
    @PostMapping("/quotes/{id}/override-approvals") public ResponseEntity<Object> requestOverride(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QuoteOverrideRequestDTO input){return commands.run(actor,null,service.quoteOrder(id),"adminRequestQuoteOverride","POST","/api/v1/admin/quotes/"+id+"/override-approvals",key,input,false,()->service.requestOverride(actor,null,id,input));}
    @PostMapping("/quote-override-approvals/{id}/approve") public ResponseEntity<Object> approve(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QuoteOverrideDecisionDTO input){return commands.run(actor,null,service.approvalOrder(id),"approveQuoteOverride","POST","/api/v1/admin/quote-override-approvals/"+id+"/approve",key,input,true,()->service.decide(actor,id,input,true));}
    @PostMapping("/quote-override-approvals/{id}/reject") public ResponseEntity<Object> reject(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QuoteOverrideDecisionDTO input){return commands.run(actor,null,service.approvalOrder(id),"rejectQuoteOverride","POST","/api/v1/admin/quote-override-approvals/"+id+"/reject",key,input,true,()->service.decide(actor,id,input,false));}
    @GetMapping("/quote-override-approvals") public ResponseEntity<Object> approvals(@AuthenticationPrincipal StaffPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(defaultValue="PENDING") String status){return AdminCommandExecutor.ok(service.approvalList(actor,page,size,status));}
}
