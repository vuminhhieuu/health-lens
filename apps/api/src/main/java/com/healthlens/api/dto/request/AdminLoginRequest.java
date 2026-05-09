package com.healthlens.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        String totpCode  // nullable — required only if TOTP is already set up
) {}
