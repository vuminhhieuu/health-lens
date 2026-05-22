package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserRequest(
                @NotBlank(message = "Họ và tên không được để trống") String fullName,

                @Past(message = "Ngày sinh phải là ngày trong quá khứ") LocalDate birthDate,

                @Pattern(regexp = "^(male|female|other)$", message = "Giới tính phải là male, female hoặc other") String gender,

                @Size(max = 500, message = "Mô tả cá nhân tối đa 500 ký tự")
                String personalDescription,

                @Size(max = 500, message = "Ghi chú cá nhân tối đa 500 ký tự")
                String personalNotes) {
}
