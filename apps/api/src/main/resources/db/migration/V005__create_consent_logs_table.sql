-- V005__create_consent_logs_table.sql
-- Keep SQL portable for both PostgreSQL and H2 (CI tests).
CREATE TABLE IF NOT EXISTS consent_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consent_version VARCHAR(20) NOT NULL,
    consented_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    revoked_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_consent_logs_user_id ON consent_logs(user_id);
