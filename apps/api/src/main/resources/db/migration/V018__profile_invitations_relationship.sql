ALTER TABLE profile_invitations
    ADD COLUMN IF NOT EXISTS relationship_label VARCHAR(120) NULL;
