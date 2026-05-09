package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminTotpVerifyRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{6,10}$", message = "Ma xac thuc khong hop le") String code
) {}
