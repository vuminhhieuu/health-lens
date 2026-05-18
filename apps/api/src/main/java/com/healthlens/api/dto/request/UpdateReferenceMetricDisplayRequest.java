package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReferenceMetricDisplayRequest(
        @NotBlank @Size(max = 255) String displayNameVi
) {}
