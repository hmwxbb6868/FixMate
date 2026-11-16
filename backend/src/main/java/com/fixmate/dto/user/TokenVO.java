package com.fixmate.dto.user;

public record TokenVO(
        String accessToken,
        String refreshToken,
        long expiresIn,
        CurrentUserVO user) {
}
