package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record OcrExtractRequest(
        @NotBlank(message = "imageUrl is required")
        @URL(message = "imageUrl must be a valid URL")
        String imageUrl
) {
}
