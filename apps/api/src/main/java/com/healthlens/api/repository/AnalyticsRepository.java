package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.repository.projection.UploadFailureBreakdownProjection;
import com.healthlens.api.repository.projection.UploadHistoryProjection;
import com.healthlens.api.repository.projection.UploadQualityBucketProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsRepository extends JpaRepository<HealthRecord, UUID> {

    @Query(value = """
        SELECT CAST(
                   CASE
                       WHEN :granularity = 'week' THEN DATE_TRUNC('week', h.created_at)
                       ELSE DATE_TRUNC('day', h.created_at)
                   END AS DATE) AS bucket_date,
               SUM(CASE WHEN h.status = 'done' THEN 1 ELSE 0 END) AS success_count,
               SUM(CASE WHEN h.status = 'ocr_failed' THEN 1 ELSE 0 END) AS failed_count
        FROM health_records h
        WHERE h.deleted_at IS NULL
          AND h.status IN ('done', 'ocr_failed')
          AND h.created_at >= :from
          AND h.created_at < :toExclusive
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
                       WHEN :granularity = 'week' THEN DATE_TRUNC('week', h.created_at)
                       ELSE DATE_TRUNC('day', h.created_at)
                   END AS DATE) AS bucket_date,
               COALESCE(
                   NULLIF(h.failure_reason, ''),
                   CASE
                       WHEN h.raw_ocr_result ->> 'failureReason' IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                           THEN h.raw_ocr_result ->> 'failureReason'
                       WHEN h.raw_ocr_result ->> 'failureReason' = 'processing_error' THEN 'api_error'
                       ELSE 'api_error'
                   END
               ) AS failure_reason,
               COUNT(*) AS failure_count
        FROM health_records h
        WHERE h.deleted_at IS NULL
          AND h.status = 'ocr_failed'
          AND h.created_at >= :from
          AND h.created_at < :toExclusive
        GROUP BY 1, 2
        ORDER BY 1, 2
        """, nativeQuery = true)
    List<UploadFailureBreakdownProjection> findUploadFailureBreakdown(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            @Param("granularity") String granularity);

    @Query(value = """
        SELECT h.id AS record_id,
               h.user_id AS user_id,
               u.email AS user_email,
               u.full_name AS user_full_name,
               h.profile_id AS profile_id,
               p.display_name AS profile_display_name,
               h.status AS status,
               COALESCE(
                   NULLIF(h.failure_reason, ''),
                   CASE
                       WHEN h.raw_ocr_result ->> 'failureReason' IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                           THEN h.raw_ocr_result ->> 'failureReason'
                       WHEN h.raw_ocr_result ->> 'failureReason' = 'processing_error' THEN 'api_error'
                       ELSE 'api_error'
                   END
               ) AS failure_reason,
               h.created_at AS created_at,
               h.hospital_name AS hospital_name,
               h.record_type AS record_type
        FROM health_records h
        INNER JOIN users u ON u.id = h.user_id
        INNER JOIN profiles p ON p.id = h.profile_id
        WHERE h.deleted_at IS NULL
          AND h.status IN ('done', 'ocr_failed')
          AND h.created_at >= :from
          AND h.created_at < :toExclusive
          AND (CAST(:status AS TEXT) IS NULL OR h.status = :status)
          AND (CAST(:failureReason AS TEXT) IS NULL OR COALESCE(
                   NULLIF(h.failure_reason, ''),
                   CASE
                       WHEN h.raw_ocr_result ->> 'failureReason' IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                           THEN h.raw_ocr_result ->> 'failureReason'
                       WHEN h.raw_ocr_result ->> 'failureReason' = 'processing_error' THEN 'api_error'
                       ELSE 'api_error'
                   END
               ) = :failureReason)
        ORDER BY h.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM health_records h
        WHERE h.deleted_at IS NULL
          AND h.status IN ('done', 'ocr_failed')
          AND h.created_at >= :from
          AND h.created_at < :toExclusive
          AND (CAST(:status AS TEXT) IS NULL OR h.status = :status)
          AND (CAST(:failureReason AS TEXT) IS NULL OR COALESCE(
                   NULLIF(h.failure_reason, ''),
                   CASE
                       WHEN h.raw_ocr_result ->> 'failureReason' IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                           THEN h.raw_ocr_result ->> 'failureReason'
                       WHEN h.raw_ocr_result ->> 'failureReason' = 'processing_error' THEN 'api_error'
                       ELSE 'api_error'
                   END
               ) = :failureReason)
        """,
            nativeQuery = true)
    Page<UploadHistoryProjection> findUploadHistory(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            @Param("status") String status,
            @Param("failureReason") String failureReason,
            Pageable pageable);
}
