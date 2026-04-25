CREATE TABLE health_records (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_type VARCHAR(20) NOT NULL DEFAULT 'ocr',
    status VARCHAR(30) NOT NULL DEFAULT 'processing',
    file_key VARCHAR(500) NOT NULL,
    raw_ocr_result JSONB,
    metrics JSONB NOT NULL DEFAULT '[]',
    exam_date DATE,
    record_type VARCHAR(100),
    hospital_name VARCHAR(255),
    diagnosis TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_health_records_profile_id ON health_records(profile_id);
CREATE INDEX idx_health_records_user_id ON health_records(user_id);