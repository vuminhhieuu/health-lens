package com.healthlens.api.repository;

import com.healthlens.api.entity.UserActivityEvent;
import com.healthlens.api.repository.projection.ActivityUploadBucketProjection;
import com.healthlens.api.repository.projection.ActivityWauBucketProjection;
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
}
