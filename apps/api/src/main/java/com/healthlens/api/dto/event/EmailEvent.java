package com.healthlens.api.dto.event;

import java.util.Map;
import java.util.UUID;

public record EmailEvent(
        String eventType,
        UUID userId,
        String email,
        Map<String, Object> data
) {
    public Map<String, String> toStreamMap() {
        return Map.of(
                "eventType", eventType,
                "userId", userId.toString(),
                "email", email,
                "token", String.valueOf(data.getOrDefault("token", ""))
        );
    }
}
