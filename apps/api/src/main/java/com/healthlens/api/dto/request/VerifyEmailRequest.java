package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "Token không được để trống")
        String token
) {
}
