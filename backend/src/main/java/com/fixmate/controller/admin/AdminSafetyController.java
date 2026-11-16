package com.fixmate.controller.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;
import com.fixmate.service.admin.AdminSafetyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1/admin/safety-events")
public class AdminSafetyController {
    private final AdminSafetyService service;
    private final AdminCommandExecutor commands;
    public AdminSafetyController(AdminSafetyService service,AdminCommandExecutor commands){this.service=service;this.commands=commands;}
    @GetMapping public ResponseEntity<Object> list(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue="OPEN") String status){return AdminCommandExecutor.ok(service.list(actor,page,size,status));}
    @PostMapping("/{id}/acknowledge") public ResponseEntity<Object> acknowledge(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonCommandDTO input) {
        return commands.run(actor,"acknowledgeSafetyEvent",key,"/api/v1/admin/safety-events/"+id+"/acknowledge",input,()->service.acknowledge(actor,id,input));
    }
    @PostMapping("/{id}/resolve") public ResponseEntity<Object> resolve(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ReasonCommandDTO input) {
        return commands.run(actor,"resolveSafetyEvent",key,"/api/v1/admin/safety-events/"+id+"/resolve",input,()->service.resolve(actor,id,input));
    }
}
