package com.healthlens.api.dto.response;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        UserInfo user
) {
    public record UserInfo(UUID id, String email, String role) {
    }
}
