package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsentRequest {
    @NotBlank(message = "Version is required")
    private String version;

    @NotNull(message = "Accepted field is required")
    private Boolean accepted;
}
