package com.healthlens.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HealthRecordHistoryItemResponse(
        UUID id,
        LocalDate examDate,
        String testType,
        String overallStatus,
        int abnormalCount,
        String hospitalName,
        String sourceType,
        Instant createdAt
) {
}
