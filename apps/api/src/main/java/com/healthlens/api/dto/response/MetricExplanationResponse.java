package com.healthlens.api.dto.response;

public record MetricExplanationResponse(
        String explanation,
        String source,
        String promptVersion,
        String modelVersion
) {
    public MetricExplanationResponse(String explanation, String source) {
        this(explanation, source, "unknown", "unknown");
    }
}
