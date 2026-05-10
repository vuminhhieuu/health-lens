package com.healthlens.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminReferenceRangeResponse(
        UUID id,
        BigDecimal minValue,
        BigDecimal maxValue,
        BigDecimal attentionMin,
        BigDecimal attentionMax,
        String gender,
        Integer minAge,
        Integer maxAge,
        String status
) {
}
