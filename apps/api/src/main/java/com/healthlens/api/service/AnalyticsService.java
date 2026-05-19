package com.healthlens.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.response.UploadHistoryItemResponse;
import com.healthlens.api.dto.response.UploadHistoryPageResponse;
import com.healthlens.api.dto.response.UploadQualityResponse;
import com.healthlens.api.dto.response.UserAnalyticsResponse;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.AnalyticsRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.repository.projection.MonthlyUserGrowthProjection;
import com.healthlens.api.repository.projection.UploadFailureBreakdownProjection;
import com.healthlens.api.repository.projection.UploadHistoryProjection;
import com.healthlens.api.repository.projection.UploadQualityBucketProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    public static final int MAX_USER_ANALYTICS_MONTHS = 24;

    private static final Duration USER_ANALYTICS_CACHE_TTL = Duration.ofHours(1);
    private static final String USER_ANALYTICS_TOTAL_CACHE_KEY = "analytics:users:total:v1";
    private static final String USER_ANALYTICS_GROWTH_CACHE_PREFIX = "analytics:users:growth:v1:";

    private static final Set<String> ALLOWED_GRANULARITIES = Set.of("day", "week");
    private static final Set<String> KNOWN_FAILURE_REASONS = Set.of(
            "timeout", "low_confidence", "api_error", "invalid_file");
    private static final Set<String> ALLOWED_TERMINAL_STATUSES = Set.of("done", "ocr_failed");
    private static final int MAX_HISTORY_PAGE_SIZE = 50;

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            UserRepository userRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public UserAnalyticsResponse getUserAnalytics(Instant from, Instant toExclusive) {
        validateUserAnalyticsRange(from, toExclusive);
        return new UserAnalyticsResponse(
                resolveTotalRegisteredProductUsers(),
                resolveMonthlyGrowth(from, toExclusive));
    }

    public static void validateUserAnalyticsRange(Instant from, Instant toExclusive) {
        if (!from.isBefore(toExclusive)) {
            throw new IllegalArgumentException("Thời điểm bắt đầu phải nhỏ hơn thời điểm kết thúc");
        }
        YearMonth start = YearMonth.from(from.atZone(ZoneOffset.UTC).toLocalDate());
        YearMonth end = YearMonth.from(toExclusive.atZone(ZoneOffset.UTC).toLocalDate()).minusMonths(1);
        YearMonth currentMonth = YearMonth.from(LocalDate.now(ZoneOffset.UTC));
        if (start.isAfter(currentMonth) || end.isAfter(currentMonth)) {
            throw new IllegalArgumentException(
                    "Không được chọn khoảng thời gian trong tương lai (UTC)");
        }
        long monthCount = ChronoUnit.MONTHS.between(start, end) + 1;
        if (monthCount > MAX_USER_ANALYTICS_MONTHS) {
            throw new IllegalArgumentException(
                    "Khoảng thời gian tối đa là " + MAX_USER_ANALYTICS_MONTHS + " tháng");
        }
    }

    private long resolveTotalRegisteredProductUsers() {
        Long cached = readCachedLong(USER_ANALYTICS_TOTAL_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        long total = userRepository.countRegisteredProductUsers(UserRole.ROLE_USER);
        writeCachedLong(USER_ANALYTICS_TOTAL_CACHE_KEY, total);
        return total;
    }

    private List<UserAnalyticsResponse.MonthlyGrowthPoint> resolveMonthlyGrowth(
            Instant from,
            Instant toExclusive) {
        String cacheKey = USER_ANALYTICS_GROWTH_CACHE_PREFIX + from + ":" + toExclusive;
        List<UserAnalyticsResponse.MonthlyGrowthPoint> cached = readMonthlyGrowthCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        Map<YearMonth, Long> countsByMonth = new LinkedHashMap<>();
        for (MonthlyUserGrowthProjection row : userRepository.findMonthlyUserGrowth(from, toExclusive)) {
            YearMonth month = YearMonth.from(row.getMonthStart());
            countsByMonth.put(month, row.getNewUsers());
        }

        List<UserAnalyticsResponse.MonthlyGrowthPoint> monthlyGrowth = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from.atZone(ZoneOffset.UTC).toLocalDate());
        YearMonth end = YearMonth.from(toExclusive.atZone(ZoneOffset.UTC).toLocalDate()).minusMonths(1);
        while (!cursor.isAfter(end)) {
            monthlyGrowth.add(new UserAnalyticsResponse.MonthlyGrowthPoint(
                    cursor.toString(),
                    countsByMonth.getOrDefault(cursor, 0L)));
            cursor = cursor.plusMonths(1);
        }

        writeMonthlyGrowthCache(cacheKey, monthlyGrowth);
        return monthlyGrowth;
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
            throw new IllegalArgumentException("Độ chi tiết phải là day hoặc week");
        }
        return normalized;
    }

    static String normalizeTerminalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TERMINAL_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái phải là done hoặc ocr_failed");
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

    /** Six calendar months including the current month (UTC). */
    public static Instant defaultUserAnalyticsFrom() {
        return LocalDate.now(ZoneOffset.UTC)
                .withDayOfMonth(1)
                .minusMonths(5)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
    }

    public static Instant defaultUserAnalyticsToExclusive() {
        return LocalDate.now(ZoneOffset.UTC)
                .plusMonths(1)
                .withDayOfMonth(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
    }

    private Long readCachedLong(String cacheKey) {
        try {
            String raw = redisTemplate.opsForValue().get(cacheKey);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return Long.parseLong(raw);
        } catch (Exception ex) {
            log.warn("Redis user analytics cache read failed for key '{}': {}", cacheKey, ex.getMessage());
            return null;
        }
    }

    private void writeCachedLong(String cacheKey, long value) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    Long.toString(value),
                    USER_ANALYTICS_CACHE_TTL);
        } catch (Exception ex) {
            log.warn("Redis user analytics cache write failed for key '{}': {}", cacheKey, ex.getMessage());
        }
    }

    private List<UserAnalyticsResponse.MonthlyGrowthPoint> readMonthlyGrowthCache(String cacheKey) {
        try {
            String raw = redisTemplate.opsForValue().get(cacheKey);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("Redis user analytics growth cache read failed for key '{}': {}", cacheKey, ex.getMessage());
            return null;
        }
    }

    private void writeMonthlyGrowthCache(
            String cacheKey,
            List<UserAnalyticsResponse.MonthlyGrowthPoint> monthlyGrowth) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(monthlyGrowth),
                    USER_ANALYTICS_CACHE_TTL);
        } catch (JsonProcessingException ex) {
            log.warn("Redis user analytics growth cache serialize failed for key '{}': {}", cacheKey, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Redis user analytics growth cache write failed for key '{}': {}", cacheKey, ex.getMessage());
        }
    }
}
