package com.fixmate.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WxLoginDTO(
        @NotBlank @Size(max = 128) String code) {
}
