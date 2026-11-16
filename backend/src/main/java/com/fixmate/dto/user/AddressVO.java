package com.fixmate.dto.user;

public record AddressVO(
        Long id,
        String contactName,
        String contactPhone,
        boolean contactPhoneVerified,
        String province,
        String city,
        String district,
        String detail,
        boolean isDefault,
        int version) {
}
