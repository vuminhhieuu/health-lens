-- Admin TOTP secrets table for MFA authentication
-- Story 7.1: Admin login with MFA
CREATE TABLE admin_totp_secrets (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    encrypted_secret TEXT NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
