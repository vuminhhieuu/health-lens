package com.healthlens.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SharedHealthRecordResponse(
        UUID recordId,
        UUID profileId,
        String profileDisplayName,
        String recordType,
        LocalDate examDate,
        String hospitalName,
        String status,
        String overallStatus,
        Instant lastUpdated,
        Instant sharedAt
) {
}
