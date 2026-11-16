package com.fixmate.dto.user;

import java.time.LocalDateTime;

public record ServiceRecordVO(Long id, LocalDateTime inspectedAt, String technicianName, String summary) {
}
