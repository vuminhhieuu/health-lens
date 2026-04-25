package com.healthlens.api.dto;

public record MetricClassificationDto(
        String status,
        ReferenceRangeDto referenceRange,
        String displayNameVi
) {
}
