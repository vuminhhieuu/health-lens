ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS outcome VARCHAR(30),
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS trace_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS metadata_json JSONB;

UPDATE audit_logs
SET outcome = CASE
    WHEN action = 'LOGIN_FAILED'
        OR action LIKE '%\_FAILED' ESCAPE '\'
        OR action LIKE '%\_FAILED\_%' ESCAPE '\'
        OR action LIKE '%\_DEAD\_LETTERED' ESCAPE '\'
        OR action LIKE '%FAILURE%' THEN 'FAILURE'
    ELSE 'SUCCESS'
END
WHERE outcome IS NULL;

ALTER TABLE audit_logs
    ALTER COLUMN outcome SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id ON audit_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_id ON audit_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_outcome ON audit_logs(outcome);
