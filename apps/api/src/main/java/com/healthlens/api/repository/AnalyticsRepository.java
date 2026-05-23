package com.healthlens.api.repository;

import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.repository.projection.UploadHistoryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AnalyticsRepository extends JpaRepository<HealthRecord, UUID> {

    /**
     * Drill-down rows aligned with OCR terminal events (same time axis as upload-quality charts).
     * {@code eventType}: {@code OCR_COMPLETED}, {@code OCR_FAILED}, or null for both.
     */
    @Query(value = """
        SELECT record_id,
               user_id,
               user_email,
               user_full_name,
               profile_id,
               profile_display_name,
               status,
               failure_reason,
               created_at,
               hospital_name,
               record_type
        FROM (
            SELECT DISTINCT ON (h.id)
                   h.id AS record_id,
                   h.user_id AS user_id,
                   u.email AS user_email,
                   u.full_name AS user_full_name,
                   h.profile_id AS profile_id,
                   p.display_name AS profile_display_name,
                   h.status AS status,
                   CASE
                       WHEN e.event_type = 'OCR_FAILED' THEN
                           CASE
                               WHEN COALESCE(NULLIF(TRIM(e.failure_reason), ''), '') = '' THEN 'api_error'
                               WHEN LOWER(TRIM(e.failure_reason)) = 'processing_error' THEN 'api_error'
                               WHEN LOWER(TRIM(e.failure_reason)) IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                                   THEN LOWER(TRIM(e.failure_reason))
                               ELSE 'api_error'
                           END
                       ELSE NULL
                   END AS failure_reason,
                   e.created_at AS created_at,
                   h.hospital_name AS hospital_name,
                   h.record_type AS record_type
            FROM health_records h
            INNER JOIN user_activity_events e ON e.record_id = h.id
            INNER JOIN users u ON u.id = h.user_id
            INNER JOIN profiles p ON p.id = h.profile_id
            WHERE h.deleted_at IS NULL
              AND e.event_type IN ('OCR_COMPLETED', 'OCR_FAILED')
              AND e.created_at >= :from
              AND e.created_at < :toExclusive
              AND (CAST(:eventType AS TEXT) IS NULL OR e.event_type = :eventType)
              AND (CAST(:failureReason AS TEXT) IS NULL OR (
                   e.event_type = 'OCR_FAILED'
                   AND CASE
                           WHEN COALESCE(NULLIF(TRIM(e.failure_reason), ''), '') = '' THEN 'api_error'
                           WHEN LOWER(TRIM(e.failure_reason)) = 'processing_error' THEN 'api_error'
                           WHEN LOWER(TRIM(e.failure_reason)) IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                               THEN LOWER(TRIM(e.failure_reason))
                           ELSE 'api_error'
                       END = :failureReason))
            ORDER BY h.id, e.created_at DESC
        ) latest_terminal_event
        ORDER BY created_at DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT h.id)
        FROM health_records h
        INNER JOIN user_activity_events e ON e.record_id = h.id
        WHERE h.deleted_at IS NULL
          AND e.event_type IN ('OCR_COMPLETED', 'OCR_FAILED')
          AND e.created_at >= :from
          AND e.created_at < :toExclusive
          AND (CAST(:eventType AS TEXT) IS NULL OR e.event_type = :eventType)
          AND (CAST(:failureReason AS TEXT) IS NULL OR (
               e.event_type = 'OCR_FAILED'
               AND CASE
                       WHEN COALESCE(NULLIF(TRIM(e.failure_reason), ''), '') = '' THEN 'api_error'
                       WHEN LOWER(TRIM(e.failure_reason)) = 'processing_error' THEN 'api_error'
                       WHEN LOWER(TRIM(e.failure_reason)) IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                           THEN LOWER(TRIM(e.failure_reason))
                       ELSE 'api_error'
                   END = :failureReason))
        """,
            nativeQuery = true)
    Page<UploadHistoryProjection> findUploadHistoryByTerminalOcrEvent(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            @Param("eventType") String eventType,
            @Param("failureReason") String failureReason,
            Pageable pageable);
}
