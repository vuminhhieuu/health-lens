package com.healthlens.api.dto.response;

import java.util.UUID;

/**
 * Response DTO for token refresh endpoint.
 * Includes consent information to allow frontend to update consent modal
 * based on latest consent status.
 */
public record RefreshResponse(
        String accessToken,
        UserInfo user,
        boolean consentGiven,
        String consentVersion
) {
    public record UserInfo(UUID id, String email, String role, String fullName) {
    }
}
