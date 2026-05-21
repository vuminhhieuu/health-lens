package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarkNotificationInboxReadRequest(
        @NotBlank @Size(max = 120) String itemId
) {
}
