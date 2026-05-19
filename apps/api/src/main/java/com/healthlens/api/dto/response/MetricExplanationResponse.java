package com.healthlens.api.dto.response;

public record MetricExplanationResponse(
        String explanation,
        String source,
        String promptVersion,
        String modelVersion,
        RetrievalTraceResponse retrievalTrace
) {
    public MetricExplanationResponse(String explanation, String source) {
        this(explanation, source, "unknown", "unknown");
    }

    public MetricExplanationResponse(String explanation, String source, String promptVersion, String modelVersion) {
        this(explanation, source, promptVersion, modelVersion, null);
    }
}
