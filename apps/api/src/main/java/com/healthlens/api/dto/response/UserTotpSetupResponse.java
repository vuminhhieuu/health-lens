package com.healthlens.api.dto.response;

import java.util.List;

public record UserTotpSetupResponse(
        String secret,
        String otpauthUri,
        String qrDataUrl,
        List<String> backupCodes
) {
}
