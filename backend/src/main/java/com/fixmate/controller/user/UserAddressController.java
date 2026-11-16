package com.fixmate.controller.user;

import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.idempotency.IdempotencyContext;
import com.fixmate.common.idempotency.IdempotentExecutor;
import com.fixmate.dto.user.AddressCreateDTO;
import com.fixmate.dto.user.AddressUpdateDTO;
import com.fixmate.dto.user.AddressVO;
import com.fixmate.dto.user.DisableRequestDTO;
import com.fixmate.security.JwtPrincipal;
import com.fixmate.service.user.UserAddressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/addresses")
public class UserAddressController {

    private static final long TTL = 86400;

    private final UserAddressService addressService;
    private final IdempotentExecutor executor;

    public UserAddressController(UserAddressService addressService, IdempotentExecutor executor) {
        this.addressService = addressService;
        this.executor = executor;
    }

    @GetMapping
    public ApiResponse<List<AddressVO>> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return ApiResponse.ok(addressService.list(principal.userId()));
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @Valid @RequestBody AddressCreateDTO dto,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "createAddress", key,
                executor.hash(dto), TTL);
        return executor.execute(ctx,
                () -> ResponseEntity.ok(ApiResponse.ok(addressService.create(principal.userId(), dto))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable @Positive Long id,
                                         @RequestHeader("X-Request-Id") String requestId,
                                         @RequestHeader("Idempotency-Key") String key,
                                         @Valid @RequestBody AddressUpdateDTO dto,
                                         @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "updateAddress", key,
                executor.hashWithPath(dto, id), TTL);
        return executor.execute(ctx,
                () -> ResponseEntity.ok(ApiResponse.ok(addressService.update(principal.userId(), id, dto))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> disable(@PathVariable @Positive Long id,
                                          @RequestHeader("X-Request-Id") String requestId,
                                          @RequestHeader("Idempotency-Key") String key,
                                          @Valid @RequestBody DisableRequestDTO dto,
                                          @AuthenticationPrincipal JwtPrincipal principal) {
        IdempotencyContext ctx = IdempotencyContext.forUser(principal.userId(), "disableAddress", key,
                executor.hashWithPath(dto, id), TTL);
        return executor.execute(ctx, () -> {
            addressService.disable(principal.userId(), id, dto);
            return ResponseEntity.ok(ApiResponse.ok());
        });
    }
}
