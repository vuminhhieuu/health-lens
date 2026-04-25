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
    private String rawName;
    private String rawValue;
    private String rawUnit;
    private String normalizedName;
    private String normalizedValue;
    private String normalizedUnit;
    private Float confidence;
    private String source;
    private String confidenceLevel; // "high", "medium", "low"
    private String displayNameVi;
    private String status; // "normal", "attention", "abnormal", "no_data"
    private String statusSource; // "document", "system", "none"
    private String interpretation; // "high", "low", "normal", "critical", "unknown"
    private String interpretationSource; // "document", "computed", "system"
    private boolean critical;
    private ReferenceRangeDto referenceRange;
    private String referenceRangeSource; // "document", "system", "none"
    private String explanation;
}
