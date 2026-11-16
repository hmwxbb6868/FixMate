package com.fixmate.dto.user;

import java.time.LocalDateTime;

public record TimelineEventVO(String eventType, String title, String detail, LocalDateTime occurredAt) {
}
