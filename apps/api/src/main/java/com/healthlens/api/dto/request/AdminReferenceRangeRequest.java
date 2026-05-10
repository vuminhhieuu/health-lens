package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdminReferenceRangeRequest(
        @NotNull BigDecimal minValue,
        @NotNull BigDecimal maxValue,
        @NotNull BigDecimal attentionMin,
        @NotNull BigDecimal attentionMax,
        String gender,
        Integer minAge,
        Integer maxAge
) {
}
