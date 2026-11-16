package com.fixmate.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public class AppointmentRescheduleDTO {

    @NotNull
    @Min(0)
    private Integer expectedWorkOrderVersion;

    @NotNull
    private OffsetDateTime startAt;

    @NotNull
    private OffsetDateTime endAt;

    @NotBlank
    @Size(min = 2, max = 200)
    private String reason;

    public Integer getExpectedWorkOrderVersion() {
        return expectedWorkOrderVersion;
    }

    public void setExpectedWorkOrderVersion(Integer expectedWorkOrderVersion) {
        this.expectedWorkOrderVersion = expectedWorkOrderVersion;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(OffsetDateTime endAt) {
        this.endAt = endAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
