package com.healthlens.api.dto.response;

import java.time.Instant;

public record CancelDeletionResponse(
        String message,
        String email,
        Instant cancelledAt
) {
}
