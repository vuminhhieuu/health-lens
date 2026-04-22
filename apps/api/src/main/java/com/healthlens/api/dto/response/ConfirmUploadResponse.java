package com.healthlens.api.dto.response;

import java.util.UUID;

public record ConfirmUploadResponse(
        UUID recordId,
        String status
) {
}
