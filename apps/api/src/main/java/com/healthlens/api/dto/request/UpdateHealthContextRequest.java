package com.healthlens.api.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateHealthContextRequest(
        @Size(max = 1000, message = "Bệnh nền tối đa 1000 ký tự")
        String chronicConditions,

        @Size(max = 1000, message = "Thuốc đang dùng tối đa 1000 ký tự")
        String currentMedications,

        @Size(max = 1000, message = "Dị ứng tối đa 1000 ký tự")
        String allergies
) {
}
