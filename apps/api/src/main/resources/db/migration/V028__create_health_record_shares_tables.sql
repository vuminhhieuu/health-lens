CREATE TABLE IF NOT EXISTS health_record_invitations (
    id UUID PRIMARY KEY,
    health_record_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    inviter_id UUID NOT NULL,
    invitee_email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS health_record_shares (
    id UUID PRIMARY KEY,
    health_record_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    viewer_id UUID NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_health_record_invitations_record') THEN
        ALTER TABLE health_record_invitations
            ADD CONSTRAINT fk_health_record_invitations_record
                FOREIGN KEY (health_record_id) REFERENCES health_records(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_health_record_invitations_profile') THEN
        ALTER TABLE health_record_invitations
            ADD CONSTRAINT fk_health_record_invitations_profile
                FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_health_record_invitations_inviter') THEN
        ALTER TABLE health_record_invitations
            ADD CONSTRAINT fk_health_record_invitations_inviter
                FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_health_record_shares_record') THEN
        ALTER TABLE health_record_shares
            ADD CONSTRAINT fk_health_record_shares_record
                FOREIGN KEY (health_record_id) REFERENCES health_records(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_health_record_shares_profile') THEN
        ALTER TABLE health_record_shares
            ADD CONSTRAINT fk_health_record_shares_profile
                FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_health_record_shares_owner') THEN
        ALTER TABLE health_record_shares
            ADD CONSTRAINT fk_health_record_shares_owner
                FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_health_record_shares_viewer') THEN
        ALTER TABLE health_record_shares
            ADD CONSTRAINT fk_health_record_shares_viewer
                FOREIGN KEY (viewer_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_health_record_invitations_pending_record_email
    ON health_record_invitations (health_record_id, LOWER(invitee_email))
    WHERE status = 'pending';

CREATE UNIQUE INDEX IF NOT EXISTS uq_health_record_shares_active_record_viewer
    ON health_record_shares (health_record_id, viewer_id)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_health_record_invitations_record_created
    ON health_record_invitations (health_record_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_health_record_shares_viewer_active
    ON health_record_shares (viewer_id, health_record_id)
    WHERE revoked_at IS NULL;
