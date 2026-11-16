package com.fixmate.dto.user;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class QuoteAcceptDTO {

    @NotNull
    @Min(1)
    private Integer expectedQuoteVersion;

    public Integer getExpectedQuoteVersion() {
        return expectedQuoteVersion;
    }

    public void setExpectedQuoteVersion(Integer expectedQuoteVersion) {
        this.expectedQuoteVersion = expectedQuoteVersion;
    }
}
