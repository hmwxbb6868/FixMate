package com.fixmate.dto.user;

import java.time.LocalDateTime;

public record AcceptanceVO(boolean accepted, String note, LocalDateTime acceptedAt) {
}
