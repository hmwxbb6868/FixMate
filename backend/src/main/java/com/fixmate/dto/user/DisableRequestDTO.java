package com.fixmate.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DisableRequestDTO(
        @NotNull @Min(0) Integer expectedVersion) {
}
