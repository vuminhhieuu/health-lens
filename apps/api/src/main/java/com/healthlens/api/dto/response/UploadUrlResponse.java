package com.healthlens.api.dto.response;

import java.util.UUID;

public record UploadUrlResponse(
        String uploadUrl,
        UUID recordId,
        String fileKey
) {
}
