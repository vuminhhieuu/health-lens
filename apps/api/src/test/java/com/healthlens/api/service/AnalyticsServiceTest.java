package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.response.ActivityAnalyticsResponse;
import com.healthlens.api.dto.response.UploadHistoryPageResponse;
import com.healthlens.api.dto.response.UploadQualityResponse;
import com.healthlens.api.dto.response.UserAnalyticsResponse;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.AnalyticsRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.repository.projection.MonthlyUserGrowthProjection;
import com.healthlens.api.repository.UserActivityEventRepository;
import com.healthlens.api.repository.projection.ActivityUploadBucketProjection;
import com.healthlens.api.repository.projection.ActivityWauBucketProjection;
import com.healthlens.api.repository.projection.UploadFailureBreakdownProjection;
import com.healthlens.api.repository.projection.UploadHistoryProjection;
import com.healthlens.api.repository.projection.UploadQualityBucketProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserActivityEventRepository userActivityEventRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        analyticsService = new AnalyticsService(
                analyticsRepository,
                userActivityEventRepository,
                userRepository,
                redisTemplate,
                objectMapper);
    }

    @Test
    @DisplayName("getUploadQuality tinh success rate va failure breakdown")
    void getUploadQuality_calculatesRatesAndBreakdown() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-03-04T00:00:00Z");

        UploadQualityBucketProjection dayOne = bucket(LocalDate.of(2026, 3, 1), 47, 3);
        UploadQualityBucketProjection dayTwo = bucket(LocalDate.of(2026, 3, 2), 40, 10);

        List<UploadFailureBreakdownProjection> breakdownRows = List.of(
                failure(LocalDate.of(2026, 3, 1), "timeout", 2),
                failure(LocalDate.of(2026, 3, 1), "low_confidence", 1),
                failure(LocalDate.of(2026, 3, 2), "api_error", 10));

        when(analyticsRepository.findUploadQualityBuckets(from, toExclusive, "day"))
                .thenReturn(List.of(dayOne, dayTwo));
        when(analyticsRepository.findUploadFailureBreakdown(from, toExclusive, "day"))
                .thenReturn(breakdownRows);

        UploadQualityResponse response = analyticsService.getUploadQuality(from, toExclusive, "day");

        assertThat(response.summary().totalUploads()).isEqualTo(100);
        assertThat(response.summary().successCount()).isEqualTo(87);
        assertThat(response.summary().failedCount()).isEqualTo(13);
        assertThat(response.summary().successRate()).isEqualTo(0.87);
        assertThat(response.summary().failureRate()).isEqualTo(0.13);

        assertThat(response.buckets()).hasSize(2);
        assertThat(response.buckets().get(0).success()).isEqualTo(47);
        assertThat(response.buckets().get(0).failed()).isEqualTo(3);
        assertThat(response.buckets().get(0).successRate()).isCloseTo(0.94, org.assertj.core.data.Offset.offset(0.001));
        assertThat(response.buckets().get(0).failureBreakdown())
                .isEqualTo(Map.of("timeout", 2L, "low_confidence", 1L));
        assertThat(response.buckets().get(1).failureBreakdown()).isEqualTo(Map.of("api_error", 10L));
    }

    @Test
    @DisplayName("getUploadHistory tra ve danh sach phan trang")
    void getUploadHistory_returnsPaginatedItems() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-03-04T00:00:00Z");
        UUID recordId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        UploadHistoryProjection row = historyRow(
                recordId,
                userId,
                "user@example.com",
                "Nguyen Van A",
                profileId,
                "Bo me",
                "ocr_failed",
                "timeout",
                Instant.parse("2026-03-02T10:00:00Z"));

        Page<UploadHistoryProjection> page = new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1);
        when(analyticsRepository.findUploadHistory(eq(from), eq(toExclusive), eq("ocr_failed"), eq("timeout"), eq(PageRequest.of(0, 20))))
                .thenReturn(page);

        UploadHistoryPageResponse response = analyticsService.getUploadHistory(
                from, toExclusive, "ocr_failed", "timeout", 0, 20);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).userEmail()).isEqualTo("user@example.com");
        assertThat(response.items().get(0).failureReason()).isEqualTo("timeout");
    }

    @Test
    @DisplayName("getUserAnalytics tra ve tong user va monthly breakdown voi thang trong")
    void getUserAnalytics_returnsTotalsAndFillsMissingMonths() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-04-01T00:00:00Z");

        when(userRepository.countRegisteredProductUsers(UserRole.ROLE_USER)).thenReturn(1250L);
        List<MonthlyUserGrowthProjection> growthRows = List.of(
                monthlyGrowth(LocalDate.of(2026, 1, 1), 180),
                monthlyGrowth(LocalDate.of(2026, 3, 1), 220));
        when(userRepository.findMonthlyUserGrowth(from, toExclusive)).thenReturn(growthRows);

        UserAnalyticsResponse response = analyticsService.getUserAnalytics(from, toExclusive);

        assertThat(response.totalUsers()).isEqualTo(1250);
        assertThat(response.monthlyGrowth()).hasSize(3);
        assertThat(response.monthlyGrowth().get(0).month()).isEqualTo("2026-01");
        assertThat(response.monthlyGrowth().get(0).newUsers()).isEqualTo(180);
        assertThat(response.monthlyGrowth().get(1).month()).isEqualTo("2026-02");
        assertThat(response.monthlyGrowth().get(1).newUsers()).isZero();
        assertThat(response.monthlyGrowth().get(2).month()).isEqualTo("2026-03");
        assertThat(response.monthlyGrowth().get(2).newUsers()).isEqualTo(220);
    }

    @Test
    @DisplayName("validateUserAnalyticsRange reject khoang trong tuong lai")
    void validateUserAnalyticsRange_rejectsFutureRange() {
        YearMonth future = YearMonth.now(ZoneOffset.UTC).plusMonths(2);
        Instant from = future.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = future.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        assertThatThrownBy(() -> AnalyticsService.validateUserAnalyticsRange(from, toExclusive))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tương lai");
    }

    @Test
    @DisplayName("validateUserAnalyticsRange reject khoang vuot 24 thang")
    void validateUserAnalyticsRange_rejectsRangeOverMaxMonths() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-02-01T00:00:00Z");

        assertThatThrownBy(() -> AnalyticsService.validateUserAnalyticsRange(from, toExclusive))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24");
    }

    @Test
    @DisplayName("getActivity tinh WAU, upload buckets va so sanh ky truoc")
    void getActivity_calculatesWauUploadAndComparison() {
        Instant from = Instant.parse("2026-03-02T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-03-09T00:00:00Z");
        Duration range = Duration.between(from, toExclusive);
        Instant previousFrom = from.minus(range);

        ActivityWauBucketProjection wauWeek = wauBucket(LocalDate.of(2026, 3, 2), 12);
        ActivityUploadBucketProjection uploadDay = uploadBucket(LocalDate.of(2026, 3, 2), 5, 1);

        when(userActivityEventRepository.findWauBuckets(from, toExclusive)).thenReturn(List.of(wauWeek));
        when(userActivityEventRepository.findUploadBuckets(from, toExclusive, "day"))
                .thenReturn(List.of(uploadDay));
        when(userActivityEventRepository.findWauBuckets(previousFrom, from)).thenReturn(List.of());
        when(userActivityEventRepository.findUploadBuckets(previousFrom, from, "day")).thenReturn(List.of());
        when(userActivityEventRepository.countDistinctActiveUsers(any(), any())).thenReturn(10L, 8L);
        when(userActivityEventRepository.countUploads(any(), any())).thenAnswer(invocation -> {
            Instant rangeFrom = invocation.getArgument(0);
            Instant rangeTo = invocation.getArgument(1);
            if (rangeFrom.equals(from) && rangeTo.equals(toExclusive)) {
                return 5L;
            }
            if (rangeFrom.equals(previousFrom) && rangeTo.equals(from)) {
                return 3L;
            }
            return 2L;
        });

        ActivityAnalyticsResponse response = analyticsService.getActivity(from, toExclusive, "day", true);

        assertThat(response.wauBuckets()).hasSize(1);
        assertThat(response.wauBuckets().get(0).periodStart()).isEqualTo("2026-03-02");
        assertThat(response.wauBuckets().get(0).wau()).isEqualTo(12);
        assertThat(response.uploadBuckets()).hasSize(7);
        assertThat(response.uploadBuckets().get(0).periodStart()).isEqualTo("2026-03-02");
        assertThat(response.uploadBuckets().get(0).count()).isEqualTo(5);
        assertThat(response.uploadBuckets().get(0).retryCount()).isEqualTo(1);
        assertThat(response.summary().uploadsInRange()).isEqualTo(5);
        assertThat(response.summary().uploadsPreviousRange()).isEqualTo(3);
        assertThat(response.summary().uploadChangePercent()).isCloseTo(66.7, org.assertj.core.data.Offset.offset(0.1));
        assertThat(response.wauBucketsPrevious()).hasSize(1);
        assertThat(response.wauBucketsPrevious().get(0).periodStart()).isEqualTo("2026-03-02");
        assertThat(response.wauBucketsPrevious().get(0).wau()).isZero();
        assertThat(response.uploadBucketsPrevious()).hasSize(7);
        assertThat(response.uploadBucketsPrevious().get(0).count()).isZero();
    }

    @Test
    @DisplayName("alignPreviousWauBuckets map theo periodStart lech dung so ngay khoang")
    void alignPreviousWauBuckets_mapsByShiftedPeriodStart() {
        Duration range = Duration.ofDays(7);
        List<ActivityAnalyticsResponse.WauBucket> current = List.of(
                new ActivityAnalyticsResponse.WauBucket("2026-03-10", 20));
        List<ActivityAnalyticsResponse.WauBucket> previousRaw = List.of(
                new ActivityAnalyticsResponse.WauBucket("2026-03-03", 15));

        List<ActivityAnalyticsResponse.WauBucket> aligned =
                AnalyticsService.alignPreviousWauBuckets(current, previousRaw, range);

        assertThat(aligned).hasSize(1);
        assertThat(aligned.get(0).periodStart()).isEqualTo("2026-03-10");
        assertThat(aligned.get(0).wau()).isEqualTo(15);
    }

    @Test
    @DisplayName("fillWauBuckets lap day trong cho moi tuan UTC trong khoang")
    void fillWauBuckets_fillsMissingWeeksWithZero() {
        Instant from = Instant.parse("2026-03-02T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-03-16T00:00:00Z");
        List<ActivityAnalyticsResponse.WauBucket> sparse = List.of(
                new ActivityAnalyticsResponse.WauBucket("2026-03-02", 5));

        List<ActivityAnalyticsResponse.WauBucket> filled =
                AnalyticsService.fillWauBuckets(sparse, from, toExclusive);

        assertThat(filled).hasSize(2);
        assertThat(filled.get(0).periodStart()).isEqualTo("2026-03-02");
        assertThat(filled.get(0).wau()).isEqualTo(5);
        assertThat(filled.get(1).periodStart()).isEqualTo("2026-03-09");
        assertThat(filled.get(1).wau()).isZero();
    }

    @Test
    @DisplayName("fillUploadBuckets lap day trong cho moi ngay UTC trong khoang")
    void fillUploadBuckets_fillsMissingDaysWithZero() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-03-04T00:00:00Z");
        List<ActivityAnalyticsResponse.UploadBucket> sparse = List.of(
                new ActivityAnalyticsResponse.UploadBucket("2026-03-02", 3, 1));

        List<ActivityAnalyticsResponse.UploadBucket> filled =
                AnalyticsService.fillUploadBuckets(sparse, from, toExclusive, "day");

        assertThat(filled).hasSize(3);
        assertThat(filled.get(0).count()).isZero();
        assertThat(filled.get(1).count()).isEqualTo(3);
        assertThat(filled.get(1).retryCount()).isEqualTo(1);
        assertThat(filled.get(2).count()).isZero();
    }

    @Test
    @DisplayName("validateActivityRange reject khoang vuot 90 ngay")
    void validateActivityRange_rejectsRangeOverMaxDays() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-05-01T00:00:00Z");

        assertThatThrownBy(() -> AnalyticsService.validateActivityRange(from, toExclusive))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("90");
    }

    @Test
    @DisplayName("percentChange tra ve null khi ky truoc bang 0 va current lon hon 0")
    void percentChange_nullWhenPreviousZeroAndCurrentPositive() {
        assertThat(AnalyticsService.percentChange(5, 0)).isNull();
        assertThat(AnalyticsService.percentChange(0, 0)).isEqualTo(0.0);
        assertThat(AnalyticsService.percentChange(8, 4)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("normalizeGranularity mac dinh day va reject gia tri khong hop le")
    void normalizeGranularity_defaultsAndRejectsInvalid() {
        assertThat(AnalyticsService.normalizeGranularity(null)).isEqualTo("day");
        assertThat(AnalyticsService.normalizeGranularity("week")).isEqualTo("week");
        assertThatThrownBy(() -> AnalyticsService.normalizeGranularity("month"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("normalizeFailureReason map processing_error sang api_error")
    void normalizeFailureReason_mapsProcessingError() {
        assertThat(AnalyticsService.normalizeFailureReason("processing_error")).isEqualTo("api_error");
        assertThat(AnalyticsService.normalizeFailureReason("timeout")).isEqualTo("timeout");
        assertThat(AnalyticsService.normalizeFailureReason(null)).isEqualTo("api_error");
    }

    private static UploadQualityBucketProjection bucket(LocalDate date, long success, long failed) {
        UploadQualityBucketProjection projection = mock(UploadQualityBucketProjection.class);
        when(projection.getBucketDate()).thenReturn(date);
        when(projection.getSuccessCount()).thenReturn(success);
        when(projection.getFailedCount()).thenReturn(failed);
        return projection;
    }

    private static UploadHistoryProjection historyRow(
            UUID recordId,
            UUID userId,
            String email,
            String fullName,
            UUID profileId,
            String profileName,
            String status,
            String failureReason,
            Instant createdAt) {
        UploadHistoryProjection projection = mock(UploadHistoryProjection.class);
        when(projection.getRecordId()).thenReturn(recordId);
        when(projection.getUserId()).thenReturn(userId);
        when(projection.getUserEmail()).thenReturn(email);
        when(projection.getUserFullName()).thenReturn(fullName);
        when(projection.getProfileId()).thenReturn(profileId);
        when(projection.getProfileDisplayName()).thenReturn(profileName);
        when(projection.getStatus()).thenReturn(status);
        when(projection.getFailureReason()).thenReturn(failureReason);
        when(projection.getCreatedAt()).thenReturn(createdAt);
        when(projection.getHospitalName()).thenReturn("BV Cho Ray");
        when(projection.getRecordType()).thenReturn("blood_test");
        return projection;
    }

    private static UploadFailureBreakdownProjection failure(LocalDate date, String reason, long count) {
        UploadFailureBreakdownProjection projection = mock(UploadFailureBreakdownProjection.class);
        when(projection.getBucketDate()).thenReturn(date);
        when(projection.getFailureReason()).thenReturn(reason);
        when(projection.getFailureCount()).thenReturn(count);
        return projection;
    }

    private static MonthlyUserGrowthProjection monthlyGrowth(LocalDate monthStart, long newUsers) {
        MonthlyUserGrowthProjection projection = mock(MonthlyUserGrowthProjection.class);
        when(projection.getMonthStart()).thenReturn(monthStart);
        when(projection.getNewUsers()).thenReturn(newUsers);
        return projection;
    }

    private static ActivityWauBucketProjection wauBucket(LocalDate periodStart, long wau) {
        ActivityWauBucketProjection projection = mock(ActivityWauBucketProjection.class);
        when(projection.getPeriodStart()).thenReturn(periodStart);
        when(projection.getWau()).thenReturn(wau);
        return projection;
    }

    private static ActivityUploadBucketProjection uploadBucket(
            LocalDate periodStart, long count, long retryCount) {
        ActivityUploadBucketProjection projection = mock(ActivityUploadBucketProjection.class);
        when(projection.getPeriodStart()).thenReturn(periodStart);
        when(projection.getUploadCount()).thenReturn(count);
        when(projection.getRetryCount()).thenReturn(retryCount);
        return projection;
    }
}
