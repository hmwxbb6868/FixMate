package com.fixmate.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class HumanHandoffCreateDTO {

    @NotNull
    @Min(0)
    private Integer expectedWorkOrderVersion;

    @NotBlank
    @Size(min = 2, max = 500)
    private String reason;

    public Integer getExpectedWorkOrderVersion() {
        return expectedWorkOrderVersion;
    }

    public void setExpectedWorkOrderVersion(Integer expectedWorkOrderVersion) {
        this.expectedWorkOrderVersion = expectedWorkOrderVersion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
