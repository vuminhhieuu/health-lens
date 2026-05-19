package com.healthlens.api.dto.response;

import java.util.List;

public record MetricExplanationResponse(
        String explanation,
        String source,
        String promptVersion,
        String modelVersion,
        RetrievalTraceResponse retrievalTrace,
        List<OnlineRagCitationResponse> onlineRagCitations
) {
    public MetricExplanationResponse {
        onlineRagCitations = onlineRagCitations == null ? List.of() : List.copyOf(onlineRagCitations);
    }

    public MetricExplanationResponse(String explanation, String source) {
        this(explanation, source, "unknown", "unknown");
    }

    public MetricExplanationResponse(String explanation, String source, String promptVersion, String modelVersion) {
        this(explanation, source, promptVersion, modelVersion, null, List.of());
    }

    public MetricExplanationResponse(
            String explanation,
            String source,
            String promptVersion,
            String modelVersion,
            RetrievalTraceResponse retrievalTrace
    ) {
        this(explanation, source, promptVersion, modelVersion, retrievalTrace, List.of());
    }
}
