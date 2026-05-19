CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE data_deletion_requests
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE data_deletion_requests
    ADD COLUMN IF NOT EXISTS cancellation_token_hash VARCHAR(64);

UPDATE data_deletion_requests
SET cancellation_token_hash = encode(digest(cancellation_token, 'sha256'), 'hex')
WHERE cancellation_token_hash IS NULL;

ALTER TABLE data_deletion_requests
    ALTER COLUMN cancellation_token_hash SET NOT NULL;

ALTER TABLE data_deletion_requests
    DROP CONSTRAINT IF EXISTS data_deletion_requests_cancellation_token_key;

ALTER TABLE data_deletion_requests
    DROP COLUMN IF EXISTS cancellation_token;

CREATE UNIQUE INDEX IF NOT EXISTS uq_data_deletion_requests_cancellation_token_hash
    ON data_deletion_requests (cancellation_token_hash);
