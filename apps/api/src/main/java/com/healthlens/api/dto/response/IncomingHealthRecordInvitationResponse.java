package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record IncomingHealthRecordInvitationResponse(
        UUID id,
        UUID healthRecordId,
        UUID profileId,
        String inviterName,
        Instant expiresAt,
        Instant createdAt,
        /** Relative path for the SPA, e.g. /health-record-invitations/accept?token=... */
        String acceptPath
) {
}
