package com.healthlens.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsentRequest {
    @NotBlank(message = "Phiên bản đồng thuận là bắt buộc")
    private String version;

    @NotNull(message = "Trạng thái đồng thuận là bắt buộc")
    private Boolean accepted;
}
