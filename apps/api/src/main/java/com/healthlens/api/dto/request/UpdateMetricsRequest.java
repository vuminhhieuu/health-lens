package com.healthlens.api.dto.request;

import com.healthlens.api.dto.MetricDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMetricsRequest {
    @NotNull
    private List<@Valid @NotNull MetricDto> metrics;
}
