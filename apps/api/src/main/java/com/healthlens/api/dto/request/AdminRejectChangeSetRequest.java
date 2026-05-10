package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRejectChangeSetRequest(
        @NotBlank(message = "Vui lòng cung cấp lý do từ chối.")
        String reason
) {
}
