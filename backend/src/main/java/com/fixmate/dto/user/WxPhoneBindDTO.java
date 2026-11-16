package com.fixmate.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WxPhoneBindDTO(
        @NotBlank @Size(max = 256) String phoneCode) {
}
