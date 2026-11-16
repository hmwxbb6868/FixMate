package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.IdempotencyContext;
import com.fixmate.common.idempotency.IdempotentExecutor;
import com.fixmate.dto.user.QuoteAcceptDTO;
import com.fixmate.dto.user.QuoteRejectDTO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.user.UserQuoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/quotes")
public class UserQuoteController {

    private static final long TTL = 86400;

    private final UserQuoteService quoteService;
    private final IdempotentExecutor executor;

    public UserQuoteController(UserQuoteService quoteService, IdempotentExecutor executor) {
        this.quoteService = quoteService;
        this.executor = executor;
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Object> accept(@PathVariable @Positive Long id,
                                         @RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @Valid @RequestBody QuoteAcceptDTO dto,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "acceptQuote", key,
                executor.hashWithPath(dto, id), TTL);
        return executor.executeCurrentRead(ctx, () -> ResponseEntity.ok(
                ApiResponse.ok(quoteService.accept(principal.userId(), id, dto))), () -> {});
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Object> reject(@PathVariable @Positive Long id,
                                         @RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @Valid @RequestBody QuoteRejectDTO dto,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "rejectQuote", key,
                executor.hashWithPath(dto, id), TTL);
        return executor.executeCurrentRead(ctx, () -> ResponseEntity.ok(
                ApiResponse.ok(quoteService.reject(principal.userId(), id, dto))), () -> {});
    }
}
