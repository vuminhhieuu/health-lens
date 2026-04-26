CREATE TABLE reference_metrics (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name_vi VARCHAR(255) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE reference_ranges (
    id UUID PRIMARY KEY,
    metric_id UUID NOT NULL REFERENCES reference_metrics(id) ON DELETE CASCADE,
    min_value NUMERIC(10, 4) NOT NULL,
    max_value NUMERIC(10, 4) NOT NULL,
    attention_min NUMERIC(10, 4) NOT NULL,
    attention_max NUMERIC(10, 4) NOT NULL,
    gender VARCHAR(10),
    min_age INTEGER,
    max_age INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'active'
);

CREATE INDEX idx_reference_ranges_metric_id ON reference_ranges(metric_id);
CREATE INDEX idx_reference_ranges_metric_status ON reference_ranges(metric_id, status);

CREATE TABLE reference_metric_aliases (
    id UUID PRIMARY KEY,
    metric_id UUID NOT NULL REFERENCES reference_metrics(id) ON DELETE CASCADE,
    alias VARCHAR(255) NOT NULL,
    alias_normalized VARCHAR(255) NOT NULL,
    locale VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uq_reference_metric_aliases_alias_normalized
    ON reference_metric_aliases(alias_normalized);

CREATE INDEX idx_reference_metric_aliases_metric_id
    ON reference_metric_aliases(metric_id);

INSERT INTO reference_metrics (id, name, display_name_vi, unit) VALUES
    ('c5dbcb90-0b71-451d-b396-7f4c55e7577b', 'Glucose', 'Duong huyet', 'mmol/L'),
    ('e22ed302-16f4-4708-a045-f21f1c5f7f5b', 'HbA1c', 'HbA1c', '%'),
    ('db6855f6-0f1c-4909-bf6d-9ff6f3a6a570', 'Cholesterol', 'Cholesterol toan phan', 'mmol/L'),
    ('3f3bc903-b43f-46f5-83ba-a88f9cc1579d', 'Triglycerides', 'Triglycerides', 'mmol/L'),
    ('7e63c4b7-a12a-450f-9ef1-e7e3394f3414', 'HDL', 'HDL Cholesterol', 'mmol/L'),
    ('484f5300-95fb-4d8d-85bb-e6821dbf30a1', 'LDL', 'LDL Cholesterol', 'mmol/L'),
    ('6b7d8dd7-6f93-4352-afc4-e77f32f178fc', 'Hemoglobin', 'Huyet sac to', 'g/L'),
    ('dd3ce6f8-5242-4c45-ab60-a08fc35f4f3f', 'WBC', 'Bach cau', '10^9/L');

INSERT INTO reference_ranges (
    id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status
) VALUES
    ('439f307f-5d8f-44de-8016-8fd79b174dba', 'c5dbcb90-0b71-451d-b396-7f4c55e7577b', 3.9, 5.5, 3.5, 6.9, NULL, 18, NULL, 'active'),
    ('881d64c0-6358-4881-ac4d-0946a1902fb9', 'e22ed302-16f4-4708-a045-f21f1c5f7f5b', 4.0, 5.6, 3.7, 6.4, NULL, 18, NULL, 'active'),
    ('e9010249-1e33-4cab-938a-d8b04f52f2c8', 'db6855f6-0f1c-4909-bf6d-9ff6f3a6a570', 0.0, 5.2, 0.0, 6.2, NULL, 18, NULL, 'active'),
    ('0dca2de1-03c4-46f1-88f3-66853d91cb89', '3f3bc903-b43f-46f5-83ba-a88f9cc1579d', 0.0, 1.7, 0.0, 2.3, NULL, 18, NULL, 'active'),
    ('ef49f01f-9c54-4539-adf0-8aa4c9674ccf', '7e63c4b7-a12a-450f-9ef1-e7e3394f3414', 1.0, 999.0, 0.9, 999.0, NULL, 18, NULL, 'active'),
    ('f2965458-8fdc-4bca-8663-86f45f474f9a', '484f5300-95fb-4d8d-85bb-e6821dbf30a1', 0.0, 3.4, 0.0, 4.1, NULL, 18, NULL, 'active'),
    ('30caec81-aa4b-4f0f-b9ef-dddf4dc3fe6a', '6b7d8dd7-6f93-4352-afc4-e77f32f178fc', 130.0, 170.0, 120.0, 180.0, 'male', 18, NULL, 'active'),
    ('5907dbb0-0b0f-4772-8f43-d71ca731f9f4', '6b7d8dd7-6f93-4352-afc4-e77f32f178fc', 120.0, 150.0, 110.0, 160.0, 'female', 18, NULL, 'active'),
    ('88586b7b-2a04-48d0-b11a-fbaf0067d701', 'dd3ce6f8-5242-4c45-ab60-a08fc35f4f3f', 4.0, 10.0, 3.0, 12.0, NULL, 18, NULL, 'active');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active) VALUES
    ('10adca7c-82f2-41f6-a6d7-57f2b4f43b5d', 'c5dbcb90-0b71-451d-b396-7f4c55e7577b', 'Glucose', 'glucose', 'en', TRUE),
    ('2df61eac-ccbf-4f31-862b-d62cd4e8c898', 'c5dbcb90-0b71-451d-b396-7f4c55e7577b', 'Duong huyet', 'duonghuyet', 'vi', TRUE),
    ('a33e3560-9794-42d8-bf82-736c5c6a2e22', 'e22ed302-16f4-4708-a045-f21f1c5f7f5b', 'HbA1c', 'hba1c', 'en', TRUE),
    ('649df6d0-5560-4b6c-a40b-5315fe69cd5f', 'db6855f6-0f1c-4909-bf6d-9ff6f3a6a570', 'Cholesterol toan phan', 'cholesteroltoanphan', 'vi', TRUE),
    ('6f63f296-f0a0-4fc1-a47c-8f6ac3726fad', 'db6855f6-0f1c-4909-bf6d-9ff6f3a6a570', 'Cholesterol', 'cholesterol', 'en', TRUE),
    ('3d6f1700-42b9-4e40-a702-a29722a5a45f', '3f3bc903-b43f-46f5-83ba-a88f9cc1579d', 'Triglycerides', 'triglycerides', 'en', TRUE),
    ('7e0844fb-a8bc-4e7f-bf26-703341f30cd6', '3f3bc903-b43f-46f5-83ba-a88f9cc1579d', 'Triglyceride', 'triglyceride', 'en', TRUE),
    ('f2a5f7e4-99ea-4d4a-b3de-ca662e26de18', '7e63c4b7-a12a-450f-9ef1-e7e3394f3414', 'HDL', 'hdl', 'en', TRUE),
    ('f37cd0d5-1f70-4414-a1f6-797b00dbf05d', '7e63c4b7-a12a-450f-9ef1-e7e3394f3414', 'HDL-C', 'hdlc', 'en', TRUE),
    ('20aa7f29-b8be-46bb-91e5-d55156decefd', '484f5300-95fb-4d8d-85bb-e6821dbf30a1', 'LDL', 'ldl', 'en', TRUE),
    ('f23c5465-9787-4741-99db-f2f1669704e6', '484f5300-95fb-4d8d-85bb-e6821dbf30a1', 'LDL-C', 'ldlc', 'en', TRUE),
    ('7a9a324c-96bd-4185-86b9-d9d6e52ec735', '6b7d8dd7-6f93-4352-afc4-e77f32f178fc', 'Hemoglobin', 'hemoglobin', 'en', TRUE),
    ('223c13fc-a43f-4f70-83f0-0f10fe19649b', '6b7d8dd7-6f93-4352-afc4-e77f32f178fc', 'HGB', 'hgb', 'en', TRUE),
    ('00303e42-58c2-4616-9ec8-31cac07309ad', 'dd3ce6f8-5242-4c45-ab60-a08fc35f4f3f', 'WBC', 'wbc', 'en', TRUE),
    ('22f45456-96c9-4f50-a130-a66962d7bc9d', 'dd3ce6f8-5242-4c45-ab60-a08fc35f4f3f', 'Bach cau', 'bachcau', 'vi', TRUE);

ALTER TABLE health_records
    ADD COLUMN analyzer_model VARCHAR(120),
    ADD COLUMN test_method VARCHAR(120),
    ADD COLUMN lab_site VARCHAR(255);
