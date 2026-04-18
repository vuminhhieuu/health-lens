package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "Token khong duoc de trong")
    String token,

    @NotBlank(message = "Mat khau moi khong duoc de trong")
    @Size(min = 8, message = "Mat khau phai co it nhat 8 ky tu")
    @Pattern(regexp = ".*[A-Z].*", message = "Mat khau phai co it nhat 1 chu hoa")
    @Pattern(regexp = ".*\\d.*", message = "Mat khau phai co it nhat 1 chu so")
    String newPassword
) {}
