package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UploadHistoryItemResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String userFullName,
        UUID profileId,
        String profileDisplayName,
        String status,
        String failureReason,
        Instant createdAt,
        String hospitalName,
        String recordType
) {
}
