-- Product analytics dimensions for admin charts (story 4.1).
-- Retention: align with admin activity analytics max range (90 days); no purge job in this migration.

ALTER TABLE user_activity_events
    ADD COLUMN IF NOT EXISTS profile_id UUID REFERENCES profiles(id),
    ADD COLUMN IF NOT EXISTS record_id UUID,
    ADD COLUMN IF NOT EXISTS file_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS confidence DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS has_low_confidence_metrics BOOLEAN,
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_activity_events_record_type_created
    ON user_activity_events (record_id, event_type, created_at)
    WHERE record_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_activity_events_ocr_outcomes
    ON user_activity_events (event_type, created_at)
    WHERE event_type IN ('OCR_COMPLETED', 'OCR_FAILED');
