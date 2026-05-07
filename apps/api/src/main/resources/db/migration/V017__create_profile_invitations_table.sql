CREATE TABLE profile_invitations (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invitee_email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_invitations_profile_created
    ON profile_invitations (profile_id, created_at DESC);

CREATE INDEX idx_profile_invitations_token
    ON profile_invitations (token);

CREATE UNIQUE INDEX uq_data_deletion_requests_one_pending_per_user ON data_deletion_requests (user_id)
WHERE
    status = 'PENDING';

ALTER TABLE profile_shares
    ADD COLUMN IF NOT EXISTS owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS granted_at TIMESTAMPTZ;

UPDATE profile_shares ps
SET owner_id = p.user_id
FROM profiles p
WHERE ps.profile_id = p.id
  AND ps.owner_id IS NULL;

UPDATE profile_shares
SET granted_at = created_at
WHERE granted_at IS NULL;

ALTER TABLE profile_shares
    ALTER COLUMN owner_id SET NOT NULL,
    ALTER COLUMN granted_at SET NOT NULL;
