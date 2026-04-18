package com.healthlens.api.dto.response;

/**
 * Response DTO for the active consent version endpoint.
 * Allows frontend to dynamically fetch the current consent version
 * without requiring redeployment.
 */
public record ConsentVersionResponse(
        String version
) {
}
