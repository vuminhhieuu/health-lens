CREATE TABLE profile_shares (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    viewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX uq_profile_shares_profile_viewer_active
    ON profile_shares (profile_id, viewer_id)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_profile_shares_viewer_active
    ON profile_shares (viewer_id, revoked_at);
