CREATE TABLE IF NOT EXISTS profile_share_audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    viewer_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_profile_share_audit_logs_actor'
    ) THEN
        ALTER TABLE profile_share_audit_logs
            ADD CONSTRAINT fk_profile_share_audit_logs_actor
                FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_profile_share_audit_logs_profile'
    ) THEN
        ALTER TABLE profile_share_audit_logs
            ADD CONSTRAINT fk_profile_share_audit_logs_profile
                FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_profile_share_audit_logs_viewer'
    ) THEN
        ALTER TABLE profile_share_audit_logs
            ADD CONSTRAINT fk_profile_share_audit_logs_viewer
                FOREIGN KEY (viewer_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_profile_share_audit_logs_actor_id
    ON profile_share_audit_logs (actor_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_profile_share_audit_logs_profile_id
    ON profile_share_audit_logs (profile_id, created_at DESC);
