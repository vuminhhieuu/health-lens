package com.healthlens.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricDto {
    private String name;
    private String value;
    private String unit;
    private Float confidence;
    private String source;
    private String confidenceLevel; // "high", "medium", "low"
}
