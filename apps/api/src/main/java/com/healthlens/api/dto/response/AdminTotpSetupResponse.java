package com.healthlens.api.dto.response;

import java.util.List;

public record AdminTotpSetupResponse(
        String secret,
        String qrCodeUrl,
        List<String> backupCodes
) {}
