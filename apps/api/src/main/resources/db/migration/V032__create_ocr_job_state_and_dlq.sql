CREATE TABLE ocr_job_executions (
    id UUID PRIMARY KEY,
    record_id UUID NOT NULL REFERENCES health_records(id) ON DELETE CASCADE,
    job_id VARCHAR(120) NOT NULL,
    file_key VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(800) NOT NULL UNIQUE,
    state VARCHAR(40) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_failure_reason VARCHAR(120),
    next_retry_at TIMESTAMPTZ,
    correlation_id VARCHAR(120),
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ocr_job_executions_record_id ON ocr_job_executions(record_id);
CREATE INDEX idx_ocr_job_executions_state_retry ON ocr_job_executions(state, next_retry_at);
CREATE INDEX idx_ocr_job_executions_correlation_id ON ocr_job_executions(correlation_id);

CREATE TABLE ocr_dead_letters (
    id UUID PRIMARY KEY,
    record_id UUID REFERENCES health_records(id) ON DELETE SET NULL,
    job_id VARCHAR(120),
    idempotency_key VARCHAR(800),
    sanitized_payload JSONB NOT NULL,
    failure_category VARCHAR(120) NOT NULL,
    attempts INTEGER NOT NULL,
    correlation_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ocr_dead_letters_record_id ON ocr_dead_letters(record_id);
CREATE INDEX idx_ocr_dead_letters_correlation_id ON ocr_dead_letters(correlation_id);
CREATE INDEX idx_ocr_dead_letters_created_at ON ocr_dead_letters(created_at);
