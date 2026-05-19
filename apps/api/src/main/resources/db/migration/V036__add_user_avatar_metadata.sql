ALTER TABLE users
    ADD COLUMN avatar_storage_key VARCHAR(512),
    ADD COLUMN avatar_content_type VARCHAR(64),
    ADD COLUMN avatar_size_bytes BIGINT,
    ADD COLUMN avatar_checksum_sha256 VARCHAR(64),
    ADD COLUMN avatar_updated_at TIMESTAMPTZ;
