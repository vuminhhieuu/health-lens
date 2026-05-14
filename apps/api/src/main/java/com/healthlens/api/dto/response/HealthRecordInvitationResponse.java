package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HealthRecordInvitationResponse(
        UUID id,
        UUID viewerId,
        String email,
        String status,
        String shareScope,
        String accessLevel,
        Instant expiresAt,
        Instant createdAt,
        Instant acceptedAt
) {
}
