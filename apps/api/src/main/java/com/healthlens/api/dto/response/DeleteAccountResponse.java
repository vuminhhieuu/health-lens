package com.healthlens.api.dto.response;

import java.time.Instant;

/**
 * Response confirming deletion request has been submitted.
 */
public record DeleteAccountResponse(
        String message,
        String requestId,
        Instant scheduledDeletionAt,
        String cancellationLink
) {
}
