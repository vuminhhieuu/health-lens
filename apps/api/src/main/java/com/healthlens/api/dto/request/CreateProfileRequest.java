package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateProfileRequest(
        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự")
        String displayName,

        @Past(message = "Ngày sinh phải là ngày trong quá khứ")
        LocalDate birthDate,

        @Size(max = 10, message = "Giới tính quá dài")
        String gender,

        @Size(max = 1000, message = "Ghi chú quá dài")
        String notes,

        @Size(max = 1000, message = "Bệnh nền tối đa 1000 ký tự")
        String chronicConditions,

        @Size(max = 1000, message = "Thuốc đang dùng tối đa 1000 ký tự")
        String currentMedications,

        @Size(max = 1000, message = "Dị ứng tối đa 1000 ký tự")
        String allergies
) {
}
