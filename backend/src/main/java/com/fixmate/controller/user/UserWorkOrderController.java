package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.api.PageVO;
import com.fixmate.common.enums.WorkOrderStatus;
import com.fixmate.common.idempotency.IdempotencyContext;
import com.fixmate.common.idempotency.IdempotentExecutor;
import com.fixmate.dto.user.AppointmentRescheduleDTO;
import com.fixmate.dto.user.CancelWorkOrderDTO;
import com.fixmate.dto.user.HumanHandoffCreateDTO;
import com.fixmate.dto.user.UrgeCreateDTO;
import com.fixmate.dto.user.WorkOrderAcceptanceDTO;
import com.fixmate.dto.user.WorkOrderDetailVO;
import com.fixmate.dto.user.WorkOrderListItemVO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.user.UserAppointmentService;
import com.fixmate.service.user.UserWorkOrderActionService;
import com.fixmate.service.user.UserWorkOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/work-orders")
public class UserWorkOrderController {

    private static final long TTL = 86400;

    private final UserWorkOrderService workOrderService;
    private final UserAppointmentService appointmentService;
    private final UserWorkOrderActionService actionService;
    private final IdempotentExecutor executor;

    public UserWorkOrderController(UserWorkOrderService workOrderService,
                                   UserAppointmentService appointmentService,
                                   UserWorkOrderActionService actionService,
                                   IdempotentExecutor executor) {
        this.workOrderService = workOrderService;
        this.appointmentService = appointmentService;
        this.actionService = actionService;
        this.executor = executor;
    }

    @GetMapping
    public ApiResponse<PageVO<WorkOrderListItemVO>> list(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                         @RequestParam(required = false) WorkOrderStatus status,
                                                         @AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(workOrderService.list(principal.userId(), page, size,
                status == null ? null : status.name()));
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrderDetailVO> detail(@PathVariable @Positive Long id,
                                                 @AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(workOrderService.detail(principal.userId(), id));
    }

    @PostMapping("/{id}/appointments/reschedule")
    public ResponseEntity<Object> reschedule(@PathVariable @Positive Long id,
                                             @RequestHeader("X-Request-Id") String requestId,
                                             @RequestHeader("Idempotency-Key") String key,
                                             @Valid @RequestBody AppointmentRescheduleDTO dto,
                                             @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(),
                "rescheduleAppointment", key, executor.hashWithPath(dto, id), TTL);
        return executor.execute(ctx, () -> ResponseEntity.ok(
                ApiResponse.ok(appointmentService.reschedule(principal.userId(), id, dto))));
    }

    @PostMapping("/{id}/urges")
    public ResponseEntity<Object> urge(@PathVariable @Positive Long id,
                                        @RequestHeader("X-Request-Id") String requestId,
                                        @RequestHeader("Idempotency-Key") String key,
                                        @Valid @RequestBody UrgeCreateDTO dto,
                                        @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "createUrge", key,
                executor.hashWithPath(dto, id), TTL);
        return executor.executeCurrentRead(ctx, () -> {
            actionService.urge(principal.userId(), id, dto);
            return ResponseEntity.ok(ApiResponse.ok());
        }, () -> {});
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Object> cancel(@PathVariable @Positive Long id,
                                         @RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @Valid @RequestBody CancelWorkOrderDTO dto,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        var ctx=IdempotencyContext.forUser(principal.userId(),"cancelUserWorkOrder",key,executor.hashWithPath(dto,id),TTL);
        return executor.executeCurrentRead(ctx,()->{
            actionService.cancel(principal.userId(),id,dto.expectedVersion(),dto.expectedVisitFee());
            return ResponseEntity.ok(ApiResponse.ok());
        },()->{});
    }

    @PostMapping("/{id}/human-handoff")
    public ResponseEntity<Object> handoff(@PathVariable @Positive Long id,
                                          @RequestHeader("X-Request-Id") String requestId,
                                          @RequestHeader("Idempotency-Key") String key,
                                          @Valid @RequestBody HumanHandoffCreateDTO dto,
                                          @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(),
                "requestHumanHandoff", key, executor.hashWithPath(dto, id), TTL);
        return executor.executeCurrentRead(ctx, () -> {
            actionService.requestHumanHandoff(principal.userId(), id, dto);
            return ResponseEntity.ok(ApiResponse.ok());
        }, () -> {});
    }

    @PostMapping("/{id}/acceptance")
    public ResponseEntity<Object> acceptance(@PathVariable @Positive Long id,
                                             @RequestHeader("X-Request-Id") String requestId,
                                             @RequestHeader("Idempotency-Key") String key,
                                             @Valid @RequestBody WorkOrderAcceptanceDTO dto,
                                             @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "submitAcceptance",
                key, executor.hashWithPath(dto, id), TTL);
        return executor.executeCurrentRead(ctx, () -> ResponseEntity.ok(
                ApiResponse.ok(workOrderService.accept(principal.userId(), id, dto))), () -> {});
    }
}
