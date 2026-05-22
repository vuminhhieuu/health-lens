package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SharedProfileResponse(
        UUID profileId,
        String displayName,
        String accessLevel,
        String latestStatus,
        Instant lastUpdated,
        Instant lastRecordAt,
        java.time.LocalDate birthDate,
        String gender,
        String notes,
        String chronicConditions,
        String currentMedications,
        String allergies
) {
}
