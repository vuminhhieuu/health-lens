package com.healthlens.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String displayName,
        LocalDate birthDate,
        String gender,
        String notes,
        boolean isDefault,
        Instant lastRecordAt,
        Instant createdAt,
        Instant updatedAt
) {
}
