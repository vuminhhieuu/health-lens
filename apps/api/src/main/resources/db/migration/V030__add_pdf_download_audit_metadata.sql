ALTER TABLE health_record_audit_logs
    ADD COLUMN IF NOT EXISTS profile_id UUID NULL,
    ADD COLUMN IF NOT EXISTS viewer_id UUID NULL,
    ADD COLUMN IF NOT EXISTS share_scope VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS resource_type VARCHAR(50) NULL;

CREATE INDEX IF NOT EXISTS idx_health_record_audit_logs_profile_id
    ON health_record_audit_logs(profile_id);
