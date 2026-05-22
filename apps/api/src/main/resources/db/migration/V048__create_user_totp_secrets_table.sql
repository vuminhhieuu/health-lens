-- User TOTP secrets for end-user MFA (separate from admin_totp_secrets)
CREATE TABLE user_totp_secrets (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    encrypted_secret TEXT NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    backup_codes_hash TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);
