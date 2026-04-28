package com.healthlens.api.dto.response;

import java.util.List;

public record RecommendationsResponse(
        List<String> recommendations,
        String disclaimer,
        boolean allNormal
) {
}
