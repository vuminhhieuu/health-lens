package com.healthlens.api.dto.response;

import java.math.BigDecimal;

public record AdminReferenceImportPreviewRowResponse(
        int line,
        String metricName,
        String displayNameVi,
        String unit,
        BigDecimal minValue,
        BigDecimal maxValue,
        String gender,
        Integer minAge,
        Integer maxAge
) {
}
