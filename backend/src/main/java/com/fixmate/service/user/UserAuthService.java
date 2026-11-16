package com.fixmate.service.user;

import com.fixmate.dto.user.CurrentUserVO;
import com.fixmate.dto.user.TokenVO;

public interface UserAuthService {

    TokenVO login(String code);

    CurrentUserVO bindPhone(Long userId, String phoneCode);

    TokenVO refresh(String refreshToken, String idempotencyKey);

    void logout(String refreshToken, String idempotencyKey);
}
