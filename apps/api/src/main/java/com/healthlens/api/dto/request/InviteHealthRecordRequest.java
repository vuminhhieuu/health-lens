package com.healthlens.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record InviteHealthRecordRequest(
        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong hop le")
        String email,
        @Pattern(regexp = "^(view|edit)?$", message = "accessLevel phai la view hoac edit")
        String accessLevel
) {
}
