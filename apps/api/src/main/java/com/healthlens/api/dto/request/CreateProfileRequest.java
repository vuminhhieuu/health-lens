package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateProfileRequest(
        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 50, message = "Tên hiển thị quá dài")
        String displayName,

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        LocalDate birthDate,

        @Size(max = 10, message = "Giới tính quá dài")
        String gender,

        @Size(max = 1000, message = "Ghi chú quá dài")
        String notes
) {
}
