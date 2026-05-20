package com.healthlens.api.dto.response;

import java.util.List;

public record ActivityAnalyticsResponse(
        ActivitySummary summary,
        List<WauBucket> wauBuckets,
        List<WauBucket> wauBucketsPrevious,
        List<UploadBucket> uploadBuckets,
        List<UploadBucket> uploadBucketsPrevious
) {
    public record ActivitySummary(
            long wauCurrentWeek,
            long wauPreviousWeek,
            Double wauChangePercent,
            long uploadsInRange,
            long uploadsPreviousRange,
            Double uploadChangePercent,
            long uploadsCurrentWeek,
            long uploadsPreviousWeek,
            Double uploadsWeekChangePercent
    ) {}

    public record WauBucket(
            String periodStart,
            long wau
    ) {}

    public record UploadBucket(
            String periodStart,
            long count,
            long retryCount
    ) {}
}
