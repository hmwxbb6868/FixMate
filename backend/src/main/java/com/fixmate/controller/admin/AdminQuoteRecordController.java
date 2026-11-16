package com.fixmate.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fixmate.common.api.ApiResponse;
import com.fixmate.dto.admin.StaffIdentityContracts.AdminQuoteRecordVOPageVO;
import com.fixmate.mapper.AdminQuoteRecordMapper;
import com.fixmate.security.StaffAccessService;
import com.fixmate.security.StaffPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class AdminQuoteRecordController {
    private final AdminQuoteRecordMapper records;
    private final StaffAccessService access;

    public AdminQuoteRecordController(AdminQuoteRecordMapper records,StaffAccessService access) {
        this.records=records;
        this.access=access;
    }

    @GetMapping("/api/v1/admin/quote-records")
    public ResponseEntity<Object> list(@AuthenticationPrincipal StaffPrincipal actor,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
            @RequestParam(required=false) @Pattern(regexp="WAITING_QUOTE_CONFIRMATION|COMPLETED|REJECTED|WITHDRAWN|EXPIRED|VISIT_FEE_RECORDED") String status,
            @RequestParam(required=false) @Size(max=64) String keyword) {
        access.require(actor,"ADMIN","DISPATCHER","AUDITOR");
        var result=records.list(new Page<>(page,size),status,keyword);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok(new AdminQuoteRecordVOPageVO(
                        result.getRecords().stream().map(AdminQuoteRecordMapper.Row::toView).toList(),page,size,result.getTotal())));
    }
}
