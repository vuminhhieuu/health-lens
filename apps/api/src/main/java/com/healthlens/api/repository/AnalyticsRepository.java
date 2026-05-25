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
     * {@code status} in results is derived from the terminal event (not {@code health_records.status}):
     * {@code OCR_COMPLETED} → {@code done}, {@code OCR_FAILED} → {@code ocr_failed}.
     * {@code failure_reason} is normalized once per row in the {@code terminal_events} CTE (matches
     * {@link com.healthlens.api.activity.FailureReasonNormalizer} / upload-quality breakdown SQL).
     */
    @Query(value = """
        WITH terminal_events AS (
            SELECT
                h.id AS record_id,
                h.user_id AS user_id,
                u.email AS user_email,
                u.full_name AS user_full_name,
                h.profile_id AS profile_id,
                p.display_name AS profile_display_name,
                CASE
                    WHEN e.event_type = 'OCR_COMPLETED' THEN 'done'
                    WHEN e.event_type = 'OCR_FAILED' THEN 'ocr_failed'
                    ELSE 'ocr_failed'
                END AS status,
                CASE
                    WHEN e.event_type = 'OCR_FAILED' THEN
                        CASE
                            WHEN COALESCE(NULLIF(TRIM(e.failure_reason), ''), '') = '' THEN 'api_error'
                            WHEN LOWER(TRIM(e.failure_reason)) = 'processing_error' THEN 'api_error'
                            WHEN LOWER(TRIM(e.failure_reason)) IN (
                                'timeout', 'low_confidence', 'api_error', 'invalid_file'
                            ) THEN LOWER(TRIM(e.failure_reason))
                            ELSE 'api_error'
                        END
                    ELSE NULL
                END AS normalized_failure_reason,
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
        )
        SELECT record_id,
               user_id,
               user_email,
               user_full_name,
               profile_id,
               profile_display_name,
               status,
               normalized_failure_reason AS failure_reason,
               created_at,
               hospital_name,
               record_type
        FROM (
            SELECT DISTINCT ON (record_id)
                   record_id,
                   user_id,
                   user_email,
                   user_full_name,
                   profile_id,
                   profile_display_name,
                   status,
                   normalized_failure_reason,
                   created_at,
                   hospital_name,
                   record_type
            FROM terminal_events
            WHERE CAST(:failureReason AS TEXT) IS NULL
               OR normalized_failure_reason = :failureReason
            ORDER BY record_id, created_at DESC
        ) latest_terminal_event
        ORDER BY created_at DESC
        """,
            countQuery = """
        WITH terminal_events AS (
            SELECT
                h.id AS record_id,
                CASE
                    WHEN e.event_type = 'OCR_FAILED' THEN
                        CASE
                            WHEN COALESCE(NULLIF(TRIM(e.failure_reason), ''), '') = '' THEN 'api_error'
                            WHEN LOWER(TRIM(e.failure_reason)) = 'processing_error' THEN 'api_error'
                            WHEN LOWER(TRIM(e.failure_reason)) IN (
                                'timeout', 'low_confidence', 'api_error', 'invalid_file'
                            ) THEN LOWER(TRIM(e.failure_reason))
                            ELSE 'api_error'
                        END
                    ELSE NULL
                END AS normalized_failure_reason
            FROM health_records h
            INNER JOIN user_activity_events e ON e.record_id = h.id
            WHERE h.deleted_at IS NULL
              AND e.event_type IN ('OCR_COMPLETED', 'OCR_FAILED')
              AND e.created_at >= :from
              AND e.created_at < :toExclusive
              AND (CAST(:eventType AS TEXT) IS NULL OR e.event_type = :eventType)
        )
        SELECT COUNT(DISTINCT record_id)
        FROM terminal_events
        WHERE CAST(:failureReason AS TEXT) IS NULL
           OR normalized_failure_reason = :failureReason
        """,
            nativeQuery = true)
    Page<UploadHistoryProjection> findUploadHistoryByTerminalOcrEvent(
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive,
            @Param("eventType") String eventType,
            @Param("failureReason") String failureReason,
            Pageable pageable);
}
