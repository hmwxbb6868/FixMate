package com.fixmate.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressCreateDTO(
        @NotBlank @Size(max = 32) String contactName,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String contactPhone,
        @NotBlank @Size(max = 32) String province,
        @NotBlank @Size(max = 32) String city,
        @NotBlank @Size(max = 32) String district,
        @NotBlank @Size(max = 200) String detail,
        Boolean isDefault) {
}
