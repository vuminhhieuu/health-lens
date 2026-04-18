package com.healthlens.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    String email
) {}
