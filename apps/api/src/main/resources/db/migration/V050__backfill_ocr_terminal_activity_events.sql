-- One-time backfill for admin OCR quality charts (story 4.2).
-- Idempotent: skips records that already have a matching terminal OCR event.

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
WHERE h.deleted_at IS NULL
  AND h.status IN ('review_required', 'done')
  AND NOT EXISTS (
      SELECT 1
      FROM user_activity_events e
      WHERE e.record_id = h.id
        AND e.event_type = 'OCR_COMPLETED'
  );

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
WHERE h.deleted_at IS NULL
  AND h.status = 'ocr_failed'
  AND NOT EXISTS (
      SELECT 1
      FROM user_activity_events e
      WHERE e.record_id = h.id
        AND e.event_type = 'OCR_FAILED'
  );
