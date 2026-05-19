package com.healthlens.api.dto.response;

public record RetrievalTraceResponse(
        String source,
        boolean hit,
        double topScore,
        String fallbackPath
) {
}
