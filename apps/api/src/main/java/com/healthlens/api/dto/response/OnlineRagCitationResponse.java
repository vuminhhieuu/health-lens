package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record OnlineRagCitationResponse(
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
        boolean stale
) {
}
