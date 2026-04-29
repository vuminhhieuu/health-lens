ALTER TABLE health_records
    ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_health_records_deleted_at ON health_records(deleted_at);

CREATE TABLE health_record_audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(80) NOT NULL,
    record_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_record_audit_logs_record_id ON health_record_audit_logs(record_id);
CREATE INDEX idx_health_record_audit_logs_user_id ON health_record_audit_logs(user_id);
