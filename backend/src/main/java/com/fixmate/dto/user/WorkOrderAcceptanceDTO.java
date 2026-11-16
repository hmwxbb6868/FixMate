package com.fixmate.dto.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkOrderAcceptanceDTO(
        @NotNull @Min(0) Integer expectedWorkOrderVersion,
        @NotNull Boolean accepted,
        @Size(max = 500) String note) {

    /** V1 只允许确认验收；false 视为参数非法（400）。 */
    @AssertTrue(message = "V1 仅支持确认验收")
    public boolean isAcceptedTrue() {
        return Boolean.TRUE.equals(accepted);
    }
}
