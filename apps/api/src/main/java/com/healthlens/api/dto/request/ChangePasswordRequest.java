package com.healthlens.api.dto.request;

// Password rules must stay aligned with packages/shared/schemas/auth.ts (passwordSchema).
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    String currentPassword,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(regexp = ".*[A-Z].*", message = "Mật khẩu phải có ít nhất 1 chữ hoa")
    @Pattern(regexp = ".*\\d.*", message = "Mật khẩu phải có ít nhất 1 chữ số")
    String newPassword
) {}
