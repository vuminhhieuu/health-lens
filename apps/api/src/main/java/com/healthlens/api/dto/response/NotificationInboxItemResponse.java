package com.healthlens.api.dto.response;

import java.time.Instant;

public record NotificationInboxItemResponse(
        String id,
        NotificationInboxItemType type,
        String title,
        String body,
        Instant createdAt,
        String actionUrl,
        boolean read
) {
}
