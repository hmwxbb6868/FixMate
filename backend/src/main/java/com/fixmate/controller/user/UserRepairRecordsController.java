package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.api.PageVO;
import com.fixmate.common.enums.RepairRecordGroup;
import com.fixmate.dto.user.RepairRecordDetailVO;
import com.fixmate.dto.user.RepairRecordListItemVO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.user.UserRepairRecordsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserRepairRecordsController {
    private final UserRepairRecordsService service;
    public UserRepairRecordsController(UserRepairRecordsService service) { this.service=service; }
    @GetMapping("/api/v1/user/repair-requests")
    public ApiResponse<PageVO<RepairRecordListItemVO>> list(
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue="ALL") RepairRecordGroup group,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(service.list(principal.userId(),page,size,group));
    }
    @GetMapping("/api/v1/user/repair-records/{id}")
    public ApiResponse<RepairRecordDetailVO> detail(@PathVariable @Positive Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(service.detail(principal.userId(),id));
    }
}
