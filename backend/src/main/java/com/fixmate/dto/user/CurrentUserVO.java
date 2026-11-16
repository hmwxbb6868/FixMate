package com.fixmate.dto.user;

public record CurrentUserVO(
        Long id,
        String nickname,
        String avatarUrl,
        String phone,
        boolean phoneVerified) {
}
