package com.healthlens.api.repository;

import com.healthlens.api.entity.UserActivityEvent;
import com.healthlens.api.repository.projection.ActivityUploadBucketProjection;
import com.healthlens.api.repository.projection.ActivityWauBucketProjection;
import com.healthlens.api.repository.projection.UploadFailureBreakdownProjection;
import com.healthlens.api.repository.projection.UploadQualityBucketProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserActivityEventRepository extends JpaRepository<UserActivityEvent, UUID> {

    @Query(value = """
        SELECT CAST(DATE_TRUNC('week', created_at AT TIME ZONE 'UTC') AS DATE) AS period_start,
               COUNT(DISTINCT user_id) AS wau
        FROM user_activity_events
        WHERE event_type = 'AUTHENTICATED_API_CALL'
          AND created_at >= :from
          AND created_at < :toExclusive
        GROUP BY 1
        ORDER BY 1
        """, nativeQuery = true)
    List<ActivityWauBucketProjection> findWauBuckets(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    @Query(value = """
        SELECT CAST(
                   CASE
                       WHEN :granularity = 'week' THEN DATE_TRUNC('week', created_at AT TIME ZONE 'UTC')
                       ELSE DATE_TRUNC('day', created_at AT TIME ZONE 'UTC')
                   END AS DATE) AS period_start,
               COUNT(*) AS upload_count,
               COALESCE(SUM(CASE WHEN is_retry THEN 1 ELSE 0 END), 0) AS retry_count
        FROM user_activity_events
        WHERE event_type = 'UPLOAD_CONFIRMED'
          AND created_at >= :from
          AND created_at < :toExclusive
        GROUP BY 1
        ORDER BY 1
        """, nativeQuery = true)
    List<ActivityUploadBucketProjection> findUploadBuckets(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            @Param("granularity") String granularity);

    @Query(value = """
        SELECT COUNT(DISTINCT user_id)
        FROM user_activity_events
        WHERE event_type = 'AUTHENTICATED_API_CALL'
          AND created_at >= :from
          AND created_at < :toExclusive
        """, nativeQuery = true)
    long countDistinctActiveUsers(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    @Query(value = """
        SELECT COUNT(*)
        FROM user_activity_events
        WHERE event_type = 'UPLOAD_CONFIRMED'
          AND created_at >= :from
          AND created_at < :toExclusive
        """, nativeQuery = true)
    long countUploads(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    /**
     * Single round-trip, atomic idempotent insert for daily {@code AUTHENTICATED_API_CALL} marker.
     * {@code event_type} is fixed in SQL so callers cannot bypass {@code idx_activity_auth_daily}.
     */
    @Modifying
    @Query(value = """
        INSERT INTO user_activity_events (id, user_id, event_type, is_retry, created_at)
        VALUES (:id, :userId, 'AUTHENTICATED_API_CALL', false, CURRENT_TIMESTAMP)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertAuthEventIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId);

    /**
     * Immediate INSERT for product analytics (story 4.1). Unlike {@code save()}, executes in the
     * current round-trip so constraint errors are catchable without deferring to commit flush.
     */
    @Modifying
    @Query(value = """
        INSERT INTO user_activity_events (
            id, user_id, event_type, is_retry, profile_id, record_id,
            file_type, provider, confidence, has_low_confidence_metrics,
            failure_reason, created_at
        ) VALUES (
            :id, :userId, :eventType, :isRetry, :profileId, :recordId,
            :fileType, :provider, :confidence, :hasLowConfidenceMetrics,
            :failureReason, :createdAt
        )
        """, nativeQuery = true)
    int insertProductEvent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("eventType") String eventType,
            @Param("isRetry") boolean isRetry,
            @Param("profileId") UUID profileId,
            @Param("recordId") UUID recordId,
            @Param("fileType") String fileType,
            @Param("provider") String provider,
            @Param("confidence") Double confidence,
            @Param("hasLowConfidenceMetrics") Boolean hasLowConfidenceMetrics,
            @Param("failureReason") String failureReason,
            @Param("createdAt") Instant createdAt);

    @Query(value = """
        SELECT COUNT(*)
        FROM user_activity_events
        WHERE event_type = :eventType
          AND created_at >= :from
          AND created_at < :toExclusive
        """, nativeQuery = true)
    long countProductEvents(
            @Param("eventType") String eventType,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive);

    /**
     * Terminal OCR outcomes per bucket (story 4.2). Denominator = {@code OCR_COMPLETED} + {@code OCR_FAILED} only.
     */
    @Query(value = """
        SELECT CAST(
                   CASE
                       WHEN :granularity = 'week' THEN DATE_TRUNC('week', created_at AT TIME ZONE 'UTC')
                       ELSE DATE_TRUNC('day', created_at AT TIME ZONE 'UTC')
                   END AS DATE) AS bucket_date,
               SUM(CASE WHEN event_type = 'OCR_COMPLETED' THEN 1 ELSE 0 END) AS success_count,
               SUM(CASE WHEN event_type = 'OCR_FAILED' THEN 1 ELSE 0 END) AS failed_count
        FROM user_activity_events
        WHERE event_type IN ('OCR_COMPLETED', 'OCR_FAILED')
          AND created_at >= :from
          AND created_at < :toExclusive
        GROUP BY 1
        ORDER BY 1
        """, nativeQuery = true)
    List<UploadQualityBucketProjection> findUploadQualityBuckets(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            @Param("granularity") String granularity);

    @Query(value = """
        SELECT CAST(
                   CASE
                       WHEN :granularity = 'week' THEN DATE_TRUNC('week', created_at AT TIME ZONE 'UTC')
                       ELSE DATE_TRUNC('day', created_at AT TIME ZONE 'UTC')
                   END AS DATE) AS bucket_date,
               CASE
                   WHEN COALESCE(NULLIF(TRIM(failure_reason), ''), '') = '' THEN 'api_error'
                   WHEN LOWER(TRIM(failure_reason)) = 'processing_error' THEN 'api_error'
                   WHEN LOWER(TRIM(failure_reason)) IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                       THEN LOWER(TRIM(failure_reason))
                   ELSE 'api_error'
               END AS failure_reason,
               COUNT(*) AS failure_count
        FROM user_activity_events
        WHERE event_type = 'OCR_FAILED'
          AND created_at >= :from
          AND created_at < :toExclusive
        GROUP BY 1, 2
        ORDER BY 1, 2
        """, nativeQuery = true)
    List<UploadFailureBreakdownProjection> findUploadFailureBreakdown(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            @Param("granularity") String granularity);
}
