package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UpdateUserRequest(
                @NotBlank(message = "Họ và tên không được để trống") String fullName,

                @Past(message = "Ngày sinh phải là ngày trong quá khứ") LocalDate birthDate,

                @Pattern(regexp = "^(male|female|other)$", message = "Giới tính phải là male, female hoặc other") String gender) {
}
