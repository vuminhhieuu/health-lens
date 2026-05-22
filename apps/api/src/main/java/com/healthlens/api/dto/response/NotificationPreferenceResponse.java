package com.healthlens.api.dto.response;

public record NotificationPreferenceResponse(
        boolean shareInvite,
        boolean shareAccepted,
        boolean followUpReminder,
        boolean security
) {
}
