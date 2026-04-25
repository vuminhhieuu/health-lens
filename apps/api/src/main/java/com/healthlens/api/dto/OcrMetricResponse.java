package com.healthlens.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class OcrMetricResponse {
    private List<MetricDto> metrics;
}
