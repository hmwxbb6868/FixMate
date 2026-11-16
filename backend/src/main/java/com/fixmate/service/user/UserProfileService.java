package com.fixmate.service.user;

import com.fixmate.dto.user.CurrentUserVO;

public interface UserProfileService {
    CurrentUserVO current(Long userId);
    CurrentUserVO update(Long userId, com.fixmate.dto.user.UpdateCurrentUserDTO dto);
}
