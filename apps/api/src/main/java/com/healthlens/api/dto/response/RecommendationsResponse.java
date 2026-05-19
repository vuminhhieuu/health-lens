package com.healthlens.api.dto.response;

import java.util.List;

public record RecommendationsResponse(
        List<String> recommendations,
        String disclaimer,
        boolean allNormal,
        String promptVersion,
        String modelVersion
) {
    public RecommendationsResponse(List<String> recommendations, String disclaimer, boolean allNormal) {
        this(recommendations, disclaimer, allNormal, "unknown", "unknown");
    }
}
