ALTER TABLE profile_shares
    ADD COLUMN IF NOT EXISTS access_level VARCHAR(20);

UPDATE profile_shares
SET access_level = 'view'
WHERE access_level IS NULL;

ALTER TABLE profile_shares
    ALTER COLUMN access_level SET NOT NULL;

ALTER TABLE profile_invitations
    ADD COLUMN IF NOT EXISTS access_level VARCHAR(20);

UPDATE profile_invitations
SET access_level = 'view'
WHERE access_level IS NULL;

ALTER TABLE profile_invitations
    ALTER COLUMN access_level SET NOT NULL;
