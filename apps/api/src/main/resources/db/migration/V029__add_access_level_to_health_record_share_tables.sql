ALTER TABLE health_record_invitations
    ADD COLUMN IF NOT EXISTS access_level VARCHAR(20) NOT NULL DEFAULT 'view';

ALTER TABLE health_record_shares
    ADD COLUMN IF NOT EXISTS access_level VARCHAR(20) NOT NULL DEFAULT 'view';

UPDATE health_record_invitations
SET access_level = 'view'
WHERE access_level IS NULL OR TRIM(access_level) = '';

UPDATE health_record_shares
SET access_level = 'view'
WHERE access_level IS NULL OR TRIM(access_level) = '';
