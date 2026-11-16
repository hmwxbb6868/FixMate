package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.api.ErrorCode;
import com.fixmate.common.idempotency.IdempotencyContext;
import com.fixmate.common.idempotency.IdempotentExecutor;
import com.fixmate.dto.user.CancelRequestDTO;
import com.fixmate.dto.user.RepairDraftUpsertDTO;
import com.fixmate.dto.user.RepairRequestVO;
import com.fixmate.dto.user.RepairSubmitDTO;
import com.fixmate.dto.user.RepairSubmitResult;
import com.fixmate.dto.user.SafetyBlockVO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.user.UserRepairConvertService;
import com.fixmate.service.user.UserRepairRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/repair-requests")
public class UserRepairRequestController {

    private static final long TTL = 86400;

    private final UserRepairRequestService repairRequestService;
    private final UserRepairConvertService convertService;
    private final IdempotentExecutor executor;

    public UserRepairRequestController(UserRepairRequestService repairRequestService,
                                       UserRepairConvertService convertService,
                                       IdempotentExecutor executor) {
        this.repairRequestService = repairRequestService;
        this.convertService = convertService;
        this.executor = executor;
    }

    @PostMapping("/drafts")
    public ResponseEntity<Object> createDraft(@RequestHeader("X-Request-Id") String requestId,
                                              @RequestHeader("Idempotency-Key") String key,
                                              @Valid @RequestBody RepairDraftUpsertDTO dto,
                                              @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "createRepairDraft", key,
                executor.hash(dto), TTL);
        return executor.execute(ctx, () ->
                ResponseEntity.ok(ApiResponse.ok(repairRequestService.createDraft(principal.userId(), dto))));
    }

    @PutMapping("/drafts/{id}")
    public ResponseEntity<Object> updateDraft(@PathVariable @Positive Long id,
                                              @RequestHeader("X-Request-Id") String requestId,
                                              @RequestHeader("Idempotency-Key") String key,
                                              @Valid @RequestBody RepairDraftUpsertDTO dto,
                                              @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "updateRepairDraft", key,
                executor.hashWithPath(dto, id), TTL);
        return executor.execute(ctx, () -> ResponseEntity.ok(
                ApiResponse.ok(repairRequestService.updateDraft(principal.userId(), id, dto))));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<Object> submit(@PathVariable @Positive Long id,
                                         @RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @Valid @RequestBody RepairSubmitDTO dto,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "submitRepairRequest",
                key, executor.hashWithPath(dto, id), TTL);
        return executor.execute(ctx, () -> {
            RepairSubmitResult result = repairRequestService.submit(principal.userId(), id, dto);
            if (result.isBlocked()) {
                SafetyBlockVO block = result.getBlock();
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.of(ErrorCode.Codes.HARD_RULE_BLOCKED,
                                "已检测到高风险情况，已转人工处理", block));
            }
            return ResponseEntity.ok(ApiResponse.ok(result.getRequest()));
        });
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Object> cancel(@PathVariable @Positive Long id,
                                         @RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @Valid @RequestBody CancelRequestDTO dto,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "cancelRepairRequest",
                key, executor.hashWithPath(dto, id), TTL);
        return executor.execute(ctx, () ->
                ResponseEntity.ok(ApiResponse.ok(repairRequestService.cancel(principal.userId(), id, dto))));
    }

    @GetMapping("/{id}")
    public ApiResponse<RepairRequestVO> get(@PathVariable @Positive Long id,
                                            @AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(repairRequestService.get(principal.userId(), id));
    }

    @PostMapping("/{id}/convert-to-work-order")
    public ResponseEntity<Object> convert(@PathVariable @Positive Long id,
                                          @RequestHeader("X-Request-Id") String requestId,
                                          @RequestHeader("Idempotency-Key") String key,
                                          @Valid @RequestBody RepairSubmitDTO dto,
                                          @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(),
                "convertRepairRequestToWorkOrder", key, executor.hashWithPath(dto, id), TTL);
        return executor.execute(ctx, () -> ResponseEntity.ok(ApiResponse.ok(
                convertService.convertToWorkOrder(principal.userId(), id, dto.getExpectedVersion()))));
    }
}
