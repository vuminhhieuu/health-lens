package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "Token khong duoc de trong")
        String token
) {
}
