package com.fixmate.controller.technician;
import com.fixmate.dto.admin.StaffIdentityContracts.*;
import com.fixmate.security.*;
import com.fixmate.service.admin.PricingConfigurationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.fixmate.controller.admin.AdminCommandExecutor;
@Validated @RestController @RequestMapping("/api/v1/technician")
public class TechnicianPricingConfigurationController {
    private final PricingConfigurationService service;
    public TechnicianPricingConfigurationController(PricingConfigurationService service){this.service=service;}
    @GetMapping("/service-items") public ResponseEntity<Object> items(@AuthenticationPrincipal JwtPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(required=false) String applianceType){return AdminCommandExecutor.ok(service.listItems(null,actor.userId(),page,size,applianceType));}
    @GetMapping("/pricing-rules") public ResponseEntity<Object> rules(@AuthenticationPrincipal JwtPrincipal actor,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(required=false) @Positive Long serviceItemId,@RequestParam(required=false) String applianceType){return AdminCommandExecutor.ok(service.listRules(null,actor.userId(),page,size,serviceItemId,applianceType));}
}
