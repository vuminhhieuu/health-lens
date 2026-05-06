-- One pending invite per (profile, invitee email), case-insensitive; prevents races that bypass the service lookup.
CREATE UNIQUE INDEX uq_profile_invitations_pending_profile_email
    ON profile_invitations (profile_id, LOWER(invitee_email))
    WHERE status = 'pending';
