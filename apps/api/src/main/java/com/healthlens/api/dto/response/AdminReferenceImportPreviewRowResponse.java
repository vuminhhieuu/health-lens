package com.healthlens.api.dto.response;

import java.math.BigDecimal;

public record AdminReferenceImportPreviewRowResponse(
        int line,
        String metricName,
        String displayNameVi,
        String unit,
        BigDecimal minValue,
        BigDecimal maxValue,
        BigDecimal attentionMin,
        BigDecimal attentionMax,
        boolean attentionMinDefaulted,
        boolean attentionMaxDefaulted,
        String gender,
        Integer minAge,
        Integer maxAge,
        String aliases,
        String sourceUrl,
        String sourceTitle,
        String sourcePublisher,
        String accessedDate,
        String rangeType,
        String reviewerNote,
        String conversionNote,
        String methodSpecimenNote
) {
}
