package com.fixmate.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class QuoteRejectDTO {

    @NotNull
    @Min(1)
    private Integer expectedQuoteVersion;

    @NotBlank
    @Size(min = 2, max = 200)
    private String reason;

    public Integer getExpectedQuoteVersion() {
        return expectedQuoteVersion;
    }

    public void setExpectedQuoteVersion(Integer expectedQuoteVersion) {
        this.expectedQuoteVersion = expectedQuoteVersion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
