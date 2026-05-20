package com.healthlens.api.dto.event;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public record EmailEvent(
        String eventType,
        UUID userId,
        String email,
        Map<String, Object> data
) {
    public enum Type {
        VERIFICATION("verification"),
        PASSWORD_RESET("password_reset"),
        DELETION_CONFIRMATION("deletion_confirmation"),
        DELETION_CANCELLATION("deletion_cancellation"),
        DELETION_COMPLETION("deletion_completion"),
        PROFILE_INVITATION("profile_invitation"),
        HEALTH_RECORD_INVITATION("health_record_invitation"),
        FOLLOW_UP_REMINDER("follow_up_reminder");

        private final String streamValue;

        Type(String streamValue) {
            this.streamValue = streamValue;
        }

        public String streamValue() {
            return streamValue;
        }
    }

    public static EmailEvent of(Type type, UUID userId, String email, Map<String, Object> data) {
        return new EmailEvent(type.streamValue(), userId, email, data == null ? Map.of() : data);
    }

    public Map<String, String> toStreamMap() {
        Map<String, Object> payload = data == null ? Map.of() : data;
        Map<String, String> streamMap = payload.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.valueOf(entry.getValue())
                ));
        streamMap.put("eventType", eventType);
        streamMap.put("userId", userId != null ? userId.toString() : "");
        streamMap.put("email", email != null ? email : "");
        return streamMap;
    }

    public EmailEvent {
        Objects.requireNonNull(eventType, "eventType is required");
        data = data == null ? Map.of() : data;
    }
}
