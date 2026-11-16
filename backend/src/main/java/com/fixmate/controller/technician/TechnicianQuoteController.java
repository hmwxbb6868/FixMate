package com.fixmate.controller.technician;
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
@Validated @RestController @RequestMapping("/api/v1/technician")
public class TechnicianQuoteController {
    private final StaffQuoteService service;private final QuoteCommandExecutor commands;
    public TechnicianQuoteController(StaffQuoteService service,QuoteCommandExecutor commands){this.service=service;this.commands=commands;}
    @GetMapping("/work-orders/{id}/quotes") public ResponseEntity<Object> list(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id){return AdminCommandExecutor.ok(service.list(null,actor.userId(),id));}
    @PostMapping("/work-orders/{id}/quotes") public ResponseEntity<Object> create(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffQuoteCreateDTO input){return commands.run(null,actor,id,"technicianCreateQuote","POST","/api/v1/technician/work-orders/"+id+"/quotes",key,input,false,()->service.create(null,actor.userId(),id,input));}
    @PutMapping("/quotes/{id}") public ResponseEntity<Object> update(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody StaffQuoteUpdateDTO input){return commands.run(null,actor,service.quoteOrder(id),"technicianUpdateQuote","PUT","/api/v1/technician/quotes/"+id+"",key,input,false,()->service.update(null,actor.userId(),id,input));}
    @PostMapping("/quotes/{id}/submit") public ResponseEntity<Object> submit(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QuoteSubmitCommandDTO input){return commands.run(null,actor,service.quoteOrder(id),"technicianSubmitQuote","POST","/api/v1/technician/quotes/"+id+"/submit",key,input,false,()->service.submit(null,actor.userId(),id,input));}
    @PostMapping("/quotes/{id}/withdraw") public ResponseEntity<Object> withdraw(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QuoteWithdrawCommandDTO input){return commands.run(null,actor,service.quoteOrder(id),"technicianWithdrawQuote","POST","/api/v1/technician/quotes/"+id+"/withdraw",key,input,false,()->service.withdraw(null,actor.userId(),id,input));}
    @PostMapping("/quotes/{id}/override-approvals") public ResponseEntity<Object> requestOverride(@AuthenticationPrincipal JwtPrincipal actor,@PathVariable @Positive Long id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody QuoteOverrideRequestDTO input){return commands.run(null,actor,service.quoteOrder(id),"technicianRequestQuoteOverride","POST","/api/v1/technician/quotes/"+id+"/override-approvals",key,input,false,()->service.requestOverride(null,actor.userId(),id,input));}
}
