ALTER TABLE profile_share_audit_logs
    DROP CONSTRAINT IF EXISTS fk_profile_share_audit_logs_profile;

ALTER TABLE profile_share_audit_logs
    ALTER COLUMN profile_id DROP NOT NULL;

ALTER TABLE profile_share_audit_logs
    ADD CONSTRAINT fk_profile_share_audit_logs_profile
        FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE SET NULL;
