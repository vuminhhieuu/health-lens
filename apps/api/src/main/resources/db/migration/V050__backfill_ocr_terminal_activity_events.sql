-- One-time backfill for admin OCR quality charts (story 4.2).
-- Idempotent: skips records that already have a matching terminal OCR event.
--
-- Performance: partial indexes on terminal health_records + existing
-- idx_activity_events_record_type_created (V047); anti-join via LEFT JOIN;
-- batched INSERT loops to limit per-statement work on large datasets.

CREATE INDEX IF NOT EXISTS idx_health_records_backfill_ocr_completed
    ON health_records (id)
    WHERE deleted_at IS NULL AND status IN ('review_required', 'done');

CREATE INDEX IF NOT EXISTS idx_health_records_backfill_ocr_failed
    ON health_records (id)
    WHERE deleted_at IS NULL AND status = 'ocr_failed';

DO $$
DECLARE
    batch_size CONSTANT INT := 5000;
    inserted     INT;
BEGIN
    LOOP
        INSERT INTO user_activity_events (
            id, user_id, event_type, is_retry, profile_id, record_id, failure_reason, created_at
        )
        SELECT
            gen_random_uuid(),
            h.user_id,
            'OCR_COMPLETED',
            false,
            h.profile_id,
            h.id,
            NULL,
            h.updated_at AT TIME ZONE 'UTC'
        FROM health_records h
        LEFT JOIN user_activity_events e
            ON e.record_id = h.id AND e.event_type = 'OCR_COMPLETED'
        WHERE h.deleted_at IS NULL
          AND h.status IN ('review_required', 'done')
          AND e.record_id IS NULL
        LIMIT batch_size;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        EXIT WHEN inserted = 0;
    END LOOP;

    LOOP
        INSERT INTO user_activity_events (
            id, user_id, event_type, is_retry, profile_id, record_id, failure_reason, created_at
        )
        SELECT
            gen_random_uuid(),
            h.user_id,
            'OCR_FAILED',
            false,
            h.profile_id,
            h.id,
            CASE
                WHEN COALESCE(NULLIF(TRIM(h.failure_reason), ''), '') = '' THEN 'api_error'
                WHEN LOWER(TRIM(h.failure_reason)) = 'processing_error' THEN 'api_error'
                WHEN LOWER(TRIM(h.failure_reason)) IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
                    THEN LOWER(TRIM(h.failure_reason))
                ELSE 'api_error'
            END,
            h.updated_at AT TIME ZONE 'UTC'
        FROM health_records h
        LEFT JOIN user_activity_events e
            ON e.record_id = h.id AND e.event_type = 'OCR_FAILED'
        WHERE h.deleted_at IS NULL
          AND h.status = 'ocr_failed'
          AND e.record_id IS NULL
        LIMIT batch_size;

        GET DIAGNOSTICS inserted = ROW_COUNT;
        EXIT WHEN inserted = 0;
    END LOOP;
END $$;
