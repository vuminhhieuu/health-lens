package com.healthlens.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FollowUpReminderResponse(
        UUID id,
        UUID profileId,
        LocalDate reminderDate,
        String reminderType,
        String note,
        Instant emailSentAt,
        Instant emailSkippedOptOutAt,
        Instant createdAt,
        Instant updatedAt
) {
}
