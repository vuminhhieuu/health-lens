package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminPendingChangeSetSummary(
        UUID changeSetId,
        String operation,
        Instant createdAt,
        String proposedName,
        String proposedDisplayNameVi,
        String proposedUnit,
        int proposedRangesCount
) {
}
