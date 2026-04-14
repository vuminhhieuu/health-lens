package com.healthlens.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong hop le")
        String email,

        @NotBlank(message = "Mat khau khong duoc de trong")
        String password
) {
}
