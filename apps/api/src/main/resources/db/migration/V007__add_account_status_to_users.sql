-- Add account_status column to users table (Story 1.6)
-- Enables tracking of user account states: ACTIVE, PENDING_DELETION, DELETED

ALTER TABLE users
ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (
    account_status IN (
        'ACTIVE',
        'PENDING_DELETION',
        'DELETED'
    )
);

-- Index for queries filtering by account status (used in auth and dashboard)
CREATE INDEX idx_users_account_status ON users (account_status);
