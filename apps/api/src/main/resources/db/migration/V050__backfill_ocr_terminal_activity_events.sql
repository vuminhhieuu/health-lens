-- One-time backfill for admin OCR quality charts (story 4.2).
-- Idempotent: NOT EXISTS per batch + dedupe + partial unique index after backfill.
--
-- Performance: seek pagination (ORDER BY id, fixed window) keeps runtime O(health_records)
-- instead of re-scanning the tail for each anti-join batch.

DO $$
DECLARE
    batch_size               CONSTANT INT := 5000;
    window_rows              INT;
    completed_last_seen_id   UUID := NULL;
    failed_last_seen_id      UUID := NULL;
BEGIN
    CREATE TEMP TABLE IF NOT EXISTS v050_scan_ids (id UUID PRIMARY KEY) ON COMMIT DROP;

    -- OCR_COMPLETED for terminal success statuses
    LOOP
        TRUNCATE v050_scan_ids;

        INSERT INTO v050_scan_ids (id)
        SELECT h.id
        FROM health_records h
        WHERE h.deleted_at IS NULL
          AND h.status IN ('review_required', 'done')
          AND (completed_last_seen_id IS NULL OR h.id > completed_last_seen_id)
        ORDER BY h.id
        LIMIT batch_size;

        GET DIAGNOSTICS window_rows = ROW_COUNT;
        EXIT WHEN window_rows = 0;

        SELECT MAX(id) INTO completed_last_seen_id FROM v050_scan_ids;

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
            h.updated_at
        FROM health_records h
        INNER JOIN v050_scan_ids w ON w.id = h.id
        WHERE NOT EXISTS (
            SELECT 1
            FROM user_activity_events e
            WHERE e.record_id = h.id
              AND e.event_type = 'OCR_COMPLETED'
        );
    END LOOP;

    -- OCR_FAILED for terminal failure status
    LOOP
        TRUNCATE v050_scan_ids;

        INSERT INTO v050_scan_ids (id)
        SELECT h.id
        FROM health_records h
        WHERE h.deleted_at IS NULL
          AND h.status = 'ocr_failed'
          AND (failed_last_seen_id IS NULL OR h.id > failed_last_seen_id)
        ORDER BY h.id
        LIMIT batch_size;

        GET DIAGNOSTICS window_rows = ROW_COUNT;
        EXIT WHEN window_rows = 0;

        SELECT MAX(id) INTO failed_last_seen_id FROM v050_scan_ids;

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
            h.updated_at
        FROM health_records h
        INNER JOIN v050_scan_ids w ON w.id = h.id
        WHERE NOT EXISTS (
            SELECT 1
            FROM user_activity_events e
            WHERE e.record_id = h.id
              AND e.event_type = 'OCR_FAILED'
        );
    END LOOP;
END $$;

-- Enforce at most one terminal OCR event per (record_id, event_type) going forward.
DELETE FROM user_activity_events u
    USING (
        SELECT id
        FROM (
            SELECT id,
                   ROW_NUMBER() OVER (
                       PARTITION BY record_id, event_type
                       ORDER BY created_at ASC, id ASC
                   ) AS rn
            FROM user_activity_events
            WHERE record_id IS NOT NULL
              AND event_type IN ('OCR_COMPLETED', 'OCR_FAILED')
        ) ranked
        WHERE rn > 1
    ) dup
WHERE u.id = dup.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_activity_terminal_ocr_per_record
    ON user_activity_events (record_id, event_type)
    WHERE record_id IS NOT NULL
      AND event_type IN ('OCR_COMPLETED', 'OCR_FAILED');
