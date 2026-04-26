package com.healthlens.api.dto;

import java.math.BigDecimal;

public record ReferenceRangeDto(
        BigDecimal min,
        BigDecimal max,
        BigDecimal attentionMin,
        BigDecimal attentionMax,
        String unit
) {
}
