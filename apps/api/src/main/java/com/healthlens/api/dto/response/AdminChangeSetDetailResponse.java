package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AdminChangeSetDetailResponse(
        UUID id,
        UUID adminId,
        String adminEmail,
        String adminName,
        String entityType,
        UUID entityId,
        String operation,
        String changesJson,
        String currentSnapshotJson,
        String status,
        Instant createdAt,
        Instant approvedAt,
        UUID reviewerId,
        String reviewerEmail,
        String reviewerName,
        String rejectionReason
) {
}
