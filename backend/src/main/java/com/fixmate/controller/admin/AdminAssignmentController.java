package com.fixmate.controller.admin;

import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.StaffPrincipal;
import com.fixmate.service.admin.WorkAssignmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1/admin")
public class AdminAssignmentController {
    private final WorkAssignmentService service;
    private final AdminCommandExecutor commands;
    public AdminAssignmentController(WorkAssignmentService service,AdminCommandExecutor commands){this.service=service;this.commands=commands;}
    @PostMapping("/work-orders/{id}/assignments") public ResponseEntity<Object> assign(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AssignmentCreateDTO input) {
        return commands.run(actor,"assignWorkOrder",key,"/api/v1/admin/work-orders/"+id+"/assignments",input,()->service.assign(actor,id,input));
    }
    @GetMapping("/work-orders/{id}/assignments") public ResponseEntity<Object> history(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,
            @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return AdminCommandExecutor.ok(service.history(actor,id,page,size));
    }
    @GetMapping("/assignments/{id}") public ResponseEntity<Object> historyDetail(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id) {
        return AdminCommandExecutor.ok(service.historyDetail(actor,id));
    }
    @PostMapping("/assignments/{id}/return-decision") public ResponseEntity<Object> decide(@AuthenticationPrincipal StaffPrincipal actor,@PathVariable @Positive Long id,
            @RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AssignmentReturnDecisionDTO input) {
        return commands.run(actor,"decideAssignmentReturn",key,"/api/v1/admin/assignments/"+id+"/return-decision",input,()->service.decideReturn(actor,id,input));
    }
}
