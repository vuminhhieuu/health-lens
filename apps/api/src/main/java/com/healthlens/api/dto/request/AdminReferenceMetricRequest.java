package com.healthlens.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminReferenceMetricRequest(
        @NotBlank String name,
        @NotBlank String displayNameVi,
        @NotBlank String unit,
        @Valid @NotEmpty List<AdminReferenceRangeRequest> ranges
) {
}
