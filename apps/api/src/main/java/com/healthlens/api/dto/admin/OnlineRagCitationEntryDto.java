package com.healthlens.api.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record OnlineRagCitationEntryDto(
        UUID id,
        UUID healthRecordId,
        String metricName,
        String answerHash,
        UUID sourceSnapshotId,
        String sourceUrl,
        String publisher,
        Instant retrievedAt,
        String snapshotHash,
        String reviewStatus,
        boolean excluded,
        boolean cacheHit,
        boolean usableForAi,
        boolean reviewRequired,
        boolean rejected,
        boolean stale,
        String retrievalSource,
        String promptVersion,
        String modelVersion,
        Instant createdAt
) {
}
