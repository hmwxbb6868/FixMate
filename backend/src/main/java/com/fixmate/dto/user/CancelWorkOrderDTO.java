package com.fixmate.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CancelWorkOrderDTO(
        @NotNull @Min(0) Integer expectedVersion,
        @Pattern(regexp="^[0-9]{1,10}(\\.[0-9]{1,2})?$") String expectedVisitFee) {}
