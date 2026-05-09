package com.healthlens.api.dto.response;

public record AdminLoginResponse(
        String accessToken,
        boolean totpRequired,
        boolean totpSetupRequired,
        String email
) {}
