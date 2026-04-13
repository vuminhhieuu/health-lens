package com.healthlens.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Ho va ten khong duoc de trong")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotNull(message = "Ngay sinh khong duoc de trong")
        @Past(message = "Ngay sinh phai la ngay trong qua khu")
        LocalDate birthDate,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Mật khẩu phải có ít nhất 8 ký tự, gồm 1 chữ hoa và 1 chữ số"
        )
        String password
) {
}
