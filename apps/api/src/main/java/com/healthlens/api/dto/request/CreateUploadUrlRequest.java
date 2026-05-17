package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUploadUrlRequest(
        @NotNull(message = "Mã hồ sơ không được để trống")
        UUID profileId,
        @NotBlank(message = "Loại tệp không được để trống")
        String fileType,
        UUID retryRecordId
) {
}
