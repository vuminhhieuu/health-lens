package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateUploadUrlRequest(
        @NotNull(message = "profileId khong duoc de trong")
        UUID profileId,
        @NotBlank(message = "fileType khong duoc de trong")
        String fileType
) {
}
