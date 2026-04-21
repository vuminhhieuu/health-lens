package com.healthlens.api.dto.response;

import java.util.UUID;

public record HealthRecordStatusResponse(
        UUID recordId,
        String status
) {
}
