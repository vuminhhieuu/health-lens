-- At most one PENDING deletion request per user (Story 1.6 race: parallel creates).
CREATE UNIQUE INDEX uq_data_deletion_requests_one_pending_per_user ON data_deletion_requests (user_id)
WHERE
    status = 'PENDING';
