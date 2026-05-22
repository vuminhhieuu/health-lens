package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserTotpDisableRequest(
        @NotBlank(message = "Mật khẩu không được để trống")
        String password,
        @NotBlank(message = "Mã xác thực không được để trống")
        @Size(min = 6, max = 16, message = "Mã xác thực không hợp lệ")
        String code
) {
}
