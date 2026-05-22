package com.healthlens.api.dto.response;

public record UserTotpStatusResponse(boolean enabled, boolean pendingVerification) {
}
