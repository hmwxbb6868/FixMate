package com.fixmate.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenDTO(
        @NotBlank @Size(min = 16, max = 2048) String refreshToken) {
}
