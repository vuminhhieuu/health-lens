ALTER TABLE health_records
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(50);

-- Backfill from raw_ocr_result JSON for existing failed records
UPDATE health_records
SET failure_reason = CASE
    WHEN raw_ocr_result ->> 'failureReason' IN ('timeout', 'low_confidence', 'api_error', 'invalid_file')
        THEN raw_ocr_result ->> 'failureReason'
    WHEN raw_ocr_result ->> 'failureReason' = 'processing_error' THEN 'api_error'
    WHEN raw_ocr_result ->> 'failureReason' IS NOT NULL THEN 'api_error'
    ELSE 'api_error'
END
WHERE status = 'ocr_failed'
  AND failure_reason IS NULL;

CREATE INDEX IF NOT EXISTS idx_health_records_terminal_status_created
    ON health_records (created_at)
    WHERE deleted_at IS NULL AND status IN ('done', 'ocr_failed');
