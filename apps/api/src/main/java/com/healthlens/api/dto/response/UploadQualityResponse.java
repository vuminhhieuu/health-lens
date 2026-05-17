package com.healthlens.api.dto.response;

import java.util.List;
import java.util.Map;

public record UploadQualityResponse(
        UploadQualitySummary summary,
        List<UploadQualityBucketResponse> buckets
) {
    public record UploadQualitySummary(
            long totalUploads,
            long successCount,
            long failedCount,
            double successRate,
            double failureRate
    ) {}

    public record UploadQualityBucketResponse(
            String date,
            long success,
            long failed,
            double successRate,
            Map<String, Long> failureBreakdown
    ) {}
}
