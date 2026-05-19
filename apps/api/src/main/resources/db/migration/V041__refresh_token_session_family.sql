ALTER TABLE refresh_tokens
    ADD COLUMN session_family_id UUID;

UPDATE refresh_tokens
SET session_family_id = id
WHERE session_family_id IS NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN session_family_id SET NOT NULL;

CREATE INDEX idx_refresh_tokens_session_family_id ON refresh_tokens(session_family_id);
