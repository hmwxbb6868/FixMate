package com.fixmate.controller.admin;

import com.fixmate.security.StaffPrincipal;
import com.fixmate.service.admin.StaffDirectoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/admin/customers")
public class AdminCustomerDirectoryController {
    private final StaffDirectoryService directory;

    public AdminCustomerDirectoryController(StaffDirectoryService directory) {
        this.directory=directory;
    }

    @GetMapping
    public ResponseEntity<Object> list(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) @Size(max=64) String keyword,
            @RequestParam(required=false) @Pattern(regexp="ENABLED|DISABLED") String status) {
        return AdminCommandExecutor.ok(directory.customers(actor,page,size,keyword,status));
    }
}
