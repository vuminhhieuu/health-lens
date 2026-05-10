package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProfileInvitationResponse(
        UUID id,
        UUID viewerId,
        String email,
        String status,
        Instant expiresAt,
        Instant createdAt,
        String accessLevel
) {
}
