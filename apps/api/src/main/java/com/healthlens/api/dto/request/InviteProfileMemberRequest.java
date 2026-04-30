package com.healthlens.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Pattern;

public record InviteProfileMemberRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")

        String email,
        @Pattern(regexp = "view|edit", message = "Quyền truy cập phải là view hoặc edit")
        String accessLevel
) {
}
