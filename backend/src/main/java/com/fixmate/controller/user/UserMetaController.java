package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.dto.user.RepairFormMetaVO;
import com.fixmate.service.user.UserMetaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserMetaController {

    private final UserMetaService metaService;

    public UserMetaController(UserMetaService metaService) {
        this.metaService = metaService;
    }

    @GetMapping("/repair-form-meta")
    public ApiResponse<RepairFormMetaVO> repairFormMeta() {
        return ApiResponse.ok(metaService.getRepairFormMeta());
    }
}
