package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferencesRequest(
        @NotNull Boolean shareInvite,
        @NotNull Boolean shareAccepted,
        @NotNull Boolean followUpReminder,
        Boolean security
) {
}
