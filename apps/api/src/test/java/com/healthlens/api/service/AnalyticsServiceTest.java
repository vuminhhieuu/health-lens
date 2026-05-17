package com.healthlens.api.service;

import com.healthlens.api.dto.response.UploadHistoryPageResponse;
import com.healthlens.api.dto.response.UploadQualityResponse;
import com.healthlens.api.repository.AnalyticsRepository;
import com.healthlens.api.repository.projection.UploadFailureBreakdownProjection;
import com.healthlens.api.repository.projection.UploadHistoryProjection;
import com.healthlens.api.repository.projection.UploadQualityBucketProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsRepository);
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
}
