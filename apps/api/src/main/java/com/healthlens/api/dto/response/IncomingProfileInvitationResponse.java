package com.healthlens.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record IncomingProfileInvitationResponse(
        UUID id,
        UUID profileId,
        String profileDisplayName,
        String inviterName,
        Instant expiresAt,
        Instant createdAt,
        String accessLevel,
        /** Relative path for the SPA, e.g. /invitations/accept?token=... */
        String acceptPath
) {
}
