package com.healthlens.api.dto.admin;

import java.util.UUID;

public record ReferenceMetricAdminDto(
        UUID id,
        String name,
        String displayNameVi,
        String unit
) {}
