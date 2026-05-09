ALTER TABLE profiles
    ADD COLUMN last_record_at TIMESTAMP;

-- Set existing last_record_at for profiles that already have records
UPDATE profiles p
SET last_record_at = (
    SELECT MAX(created_at)
    FROM health_records hr
    WHERE hr.profile_id = p.id AND hr.deleted_at IS NULL
);
