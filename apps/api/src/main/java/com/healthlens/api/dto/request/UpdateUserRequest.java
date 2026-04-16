package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UpdateUserRequest(
                @NotBlank(message = "Ho va ten khong duoc de trong") String fullName,

                @Past(message = "Ngay sinh phai la ngay trong qua khu") LocalDate birthDate,

                @Pattern(regexp = "^(male|female|other)$", message = "Gioi tinh phai la male, female hoac other") String gender) {
}
