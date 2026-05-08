-- Create data_deletion_requests table for Account Deletion (Story 1.6)
-- Implements right-to-delete feature per Nghị định 13/2023/NĐ-CP

CREATE TABLE data_deletion_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_deletion_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (
        status IN (
            'PENDING',
            'COMPLETED',
            'CANCELLED'
        )
    ),
    cancellation_token VARCHAR(255) NOT NULL UNIQUE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for finding overdue deletion requests (scheduled job)
CREATE INDEX idx_deletion_requests_status_scheduled ON data_deletion_requests (status, scheduled_deletion_at)
WHERE
    status = 'PENDING';

-- Index for user lookup
CREATE INDEX idx_deletion_requests_user_id ON data_deletion_requests (user_id);
