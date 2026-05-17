package com.healthlens.api.service;

import com.healthlens.api.dto.response.UploadHistoryItemResponse;
import com.healthlens.api.dto.response.UploadHistoryPageResponse;
import com.healthlens.api.dto.response.UploadQualityResponse;
import com.healthlens.api.repository.AnalyticsRepository;
import com.healthlens.api.repository.projection.UploadFailureBreakdownProjection;
import com.healthlens.api.repository.projection.UploadHistoryProjection;
import com.healthlens.api.repository.projection.UploadQualityBucketProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AnalyticsService {

    private static final Set<String> ALLOWED_GRANULARITIES = Set.of("day", "week");
    private static final Set<String> KNOWN_FAILURE_REASONS = Set.of(
            "timeout", "low_confidence", "api_error", "invalid_file");
    private static final Set<String> ALLOWED_TERMINAL_STATUSES = Set.of("done", "ocr_failed");
    private static final int MAX_HISTORY_PAGE_SIZE = 50;

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional(readOnly = true)
    public UploadQualityResponse getUploadQuality(Instant from, Instant toExclusive, String granularity) {
        String normalizedGranularity = normalizeGranularity(granularity);
        List<UploadQualityBucketProjection> bucketRows = analyticsRepository.findUploadQualityBuckets(
                from, toExclusive, normalizedGranularity);
        List<UploadFailureBreakdownProjection> breakdownRows = analyticsRepository.findUploadFailureBreakdown(
                from, toExclusive, normalizedGranularity);

        Map<LocalDate, Map<String, Long>> breakdownByDate = new HashMap<>();
        for (UploadFailureBreakdownProjection row : breakdownRows) {
            breakdownByDate
                    .computeIfAbsent(row.getBucketDate(), ignored -> new LinkedHashMap<>())
                    .merge(normalizeFailureReason(row.getFailureReason()), row.getFailureCount(), Long::sum);
        }

        long totalSuccess = 0;
        long totalFailed = 0;
        List<UploadQualityResponse.UploadQualityBucketResponse> buckets = new ArrayList<>();

        for (UploadQualityBucketProjection row : bucketRows) {
            long success = row.getSuccessCount();
            long failed = row.getFailedCount();
            totalSuccess += success;
            totalFailed += failed;
            long terminal = success + failed;
            double successRate = terminal == 0 ? 0.0 : (double) success / terminal;

            buckets.add(new UploadQualityResponse.UploadQualityBucketResponse(
                    formatBucketDate(row.getBucketDate(), normalizedGranularity),
                    success,
                    failed,
                    successRate,
                    Map.copyOf(breakdownByDate.getOrDefault(row.getBucketDate(), Map.of()))
            ));
        }

        long totalUploads = totalSuccess + totalFailed;
        double aggregateSuccessRate = totalUploads == 0 ? 0.0 : (double) totalSuccess / totalUploads;

        UploadQualityResponse.UploadQualitySummary summary = new UploadQualityResponse.UploadQualitySummary(
                totalUploads,
                totalSuccess,
                totalFailed,
                aggregateSuccessRate,
                totalUploads == 0 ? 0.0 : 1.0 - aggregateSuccessRate
        );

        return new UploadQualityResponse(summary, buckets);
    }

    @Transactional(readOnly = true)
    public UploadHistoryPageResponse getUploadHistory(
            Instant from,
            Instant toExclusive,
            String status,
            String failureReason,
            int page,
            int limit) {
        String normalizedStatus = normalizeTerminalStatus(status);
        String normalizedFailureReason = failureReason == null || failureReason.isBlank()
                ? null
                : normalizeFailureReason(failureReason);

        int safeLimit = Math.min(Math.max(limit, 1), MAX_HISTORY_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        Page<UploadHistoryProjection> result = analyticsRepository.findUploadHistory(
                from,
                toExclusive,
                normalizedStatus,
                normalizedFailureReason,
                PageRequest.of(safePage, safeLimit));

        List<UploadHistoryItemResponse> items = result.getContent().stream()
                .map(row -> new UploadHistoryItemResponse(
                        row.getRecordId(),
                        row.getUserId(),
                        row.getUserEmail(),
                        row.getUserFullName(),
                        row.getProfileId(),
                        row.getProfileDisplayName(),
                        row.getStatus(),
                        row.getStatus().equals("ocr_failed")
                                ? normalizeFailureReason(row.getFailureReason())
                                : null,
                        row.getCreatedAt(),
                        row.getHospitalName(),
                        row.getRecordType()
                ))
                .toList();

        return new UploadHistoryPageResponse(
                items,
                safePage,
                safeLimit,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    static String normalizeGranularity(String granularity) {
        if (granularity == null || granularity.isBlank()) {
            return "day";
        }
        String normalized = granularity.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_GRANULARITIES.contains(normalized)) {
            throw new IllegalArgumentException("granularity phai la day hoac week");
        }
        return normalized;
    }

    static String normalizeTerminalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TERMINAL_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("status phai la done hoac ocr_failed");
        }
        return normalized;
    }

    static String normalizeFailureReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "api_error";
        }
        String normalized = reason.trim().toLowerCase(Locale.ROOT);
        if ("processing_error".equals(normalized)) {
            return "api_error";
        }
        if (KNOWN_FAILURE_REASONS.contains(normalized)) {
            return normalized;
        }
        return "api_error";
    }

    private static String formatBucketDate(LocalDate date, String granularity) {
        if ("week".equals(granularity)) {
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " (tuan)";
        }
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static Instant defaultFrom() {
        return LocalDate.now(ZoneOffset.UTC).minusDays(29).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    public static Instant defaultToExclusive() {
        return LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
