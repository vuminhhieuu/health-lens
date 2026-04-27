CREATE TABLE reference_range_audit_logs (
    id UUID PRIMARY KEY,
    metric_id UUID NOT NULL REFERENCES reference_metrics(id) ON DELETE CASCADE,
    reference_range_id UUID NOT NULL REFERENCES reference_ranges(id) ON DELETE CASCADE,
    profile_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reference_range_audit_logs_metric_id
    ON reference_range_audit_logs(metric_id);

CREATE INDEX idx_reference_range_audit_logs_reference_range_id
    ON reference_range_audit_logs(reference_range_id);

-- Seed additional reference metrics/ranges commonly found in uploaded lab sheets.
-- This block is idempotent via NOT EXISTS guards.

-- 1) Missing metrics
INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT 'fd4ee178-08be-4af6-915a-e31ca2d8fd77', 'CRP', 'CRP (định lượng)', 'mg/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'CRP');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT 'efd49174-2bd0-4f3a-98a4-2f3fd8b08bf0', 'Urea', 'Ure', 'mmol/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Urea');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT 'd6e04f51-4f89-4fc0-9f6a-84f02f4ef6ea', 'Creatinine', 'Creatinin', 'umol/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Creatinine');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT 'f9fa8aef-5f48-4f1a-8b8a-39dca95bb79f', 'Bilirubin Total', 'Bilirubin (toan phan)', 'umol/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Bilirubin Total');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT 'e4cd06b6-4e14-4576-8f41-46879f28c293', 'Bilirubin Direct', 'Bilirubin (truc tiep)', 'umol/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Bilirubin Direct');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT '53f70992-af8e-4a22-b0da-aa8b8b9f148b', 'Total Protein', 'Protein toan phan', 'g/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Total Protein');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT '68f7c1eb-2fcd-4e07-959d-74d7f3c5dc56', 'Albumin', 'Albumin', 'g/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Albumin');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT '66bb1fdb-136e-47fc-b3bc-d0f64da44355', 'ALT', 'SGPT (ALT)', 'U/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'ALT');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT 'ee9a1f70-2f97-44b6-84e5-a07085defeb3', 'ALP', 'Phosphatase kiem (ALP)', 'U/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'ALP');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT 'acf7b797-d2e6-453b-a294-4ec872d95f5e', 'GGT', 'GGT', 'U/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'GGT');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT '5dd95e5a-5c4d-4c56-a1fd-15617738db09', 'LDH', 'LDH', 'U/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'LDH');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT '6bf6db95-4045-44fc-9c53-dcc58b1832ab', 'Sodium', 'Natri (Na+)', 'mmol/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Sodium');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT 'b2f32cca-cfe2-4d1c-8553-652f166f09ea', 'Potassium', 'Kali (K+)', 'mmol/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Potassium');

INSERT INTO reference_metrics (id, name, display_name_vi, unit)
SELECT '4e8f64c3-d4f0-4d0a-8aeb-0c63d72f67ec', 'Chloride', 'Clorua (Cl-)', 'mmol/L'
WHERE NOT EXISTS (SELECT 1 FROM reference_metrics WHERE name = 'Chloride');

-- 2) Missing ranges
INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT 'fcbfaeca-7b36-4538-a7f8-b16f5f6c5162', rm.id, 0.0, 10.0, 0.0, 15.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'CRP'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT 'fc4c781c-f556-4cab-a30d-5f898669dc65', rm.id, 2.5, 8.3, 2.0, 10.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Urea'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '75c3d8a3-6095-466f-b4d9-fdb4c8f2cc35', rm.id, 60.0, 120.0, 50.0, 140.0, 'male', NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Creatinine'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender = 'male' AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '06de2fba-6f3f-43e0-95a9-f0faa57de586', rm.id, 40.0, 90.0, 35.0, 110.0, 'female', NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Creatinine'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender = 'female' AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT 'f6c538d7-0688-4770-a055-95d7762f0f1c', rm.id, 0.0, 17.1, 0.0, 25.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Bilirubin Total'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '7d8f4fe2-a99d-4fce-ab4a-56c5797ac45b', rm.id, 0.0, 5.1, 0.0, 8.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Bilirubin Direct'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '84ab3ad3-f427-45f5-83e2-c0f13f3d07fb', rm.id, 65.0, 85.0, 60.0, 90.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Total Protein'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '7286db02-2320-47b9-a1af-58d87ca3f297', rm.id, 35.0, 50.0, 30.0, 55.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Albumin'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '69f49e98-58f9-4392-b492-b53f68f5f084', rm.id, 0.0, 40.0, 0.0, 60.0, 'male', NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'ALT'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender = 'male' AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT 'c89b691e-ee59-4ef7-b12f-3f6c9f0df87b', rm.id, 0.0, 32.0, 0.0, 45.0, 'female', NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'ALT'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender = 'female' AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT 'ef2f4ec7-bcb8-4d1c-b0f7-4de2be067378', rm.id, 40.0, 120.0, 35.0, 150.0, 'male', NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'ALP'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender = 'male' AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '40d05baa-67f3-4ff9-a386-21e70ca7638e', rm.id, 35.0, 100.0, 30.0, 120.0, 'female', NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'ALP'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender = 'female' AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '5ec1fd24-f4d8-4f8d-968d-b7f8f4fc8942', rm.id, 0.0, 50.0, 0.0, 70.0, 'male', NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'GGT'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender = 'male' AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT 'e6506866-f9ca-4f1d-a80b-8a7d6b6f55f4', rm.id, 0.0, 40.0, 0.0, 60.0, 'female', NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'GGT'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender = 'female' AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '4dff8f95-7637-4bae-b9fd-5cdc1576cf59', rm.id, 0.0, 480.0, 0.0, 550.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'LDH'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT 'f95d3b71-2514-45b1-b646-a7a23f6a0b4d', rm.id, 135.0, 145.0, 130.0, 150.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Sodium'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT 'f52e6f09-f406-4754-a79a-455f72d0cf57', rm.id, 3.5, 5.0, 3.0, 5.5, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Potassium'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

INSERT INTO reference_ranges (id, metric_id, min_value, max_value, attention_min, attention_max, gender, min_age, max_age, status)
SELECT '47bd4f03-ab8d-439f-9890-6cc8e1601459', rm.id, 96.0, 106.0, 92.0, 110.0, NULL, NULL, NULL, 'active'
FROM reference_metrics rm
WHERE rm.name = 'Chloride'
  AND NOT EXISTS (
      SELECT 1 FROM reference_ranges rr
      WHERE rr.metric_id = rm.id AND rr.gender IS NULL AND rr.min_age IS NULL AND rr.max_age IS NULL
  );

-- 3) Aliases for OCR/user-entered variants
INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT 'a0952937-8f45-43bc-b67f-baecf6bd35a3', rm.id, 'C-Reactive Protein', 'creactiveprotein', 'en', TRUE
FROM reference_metrics rm
WHERE rm.name = 'CRP'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'creactiveprotein');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '0e7f989d-5c13-4f57-8d56-26be62e71c7d', rm.id, 'CRP (dinh luong)', 'crpdinhluong', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'CRP'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'crpdinhluong');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT 'd4b20b5d-b2e5-4f87-a8bf-38424717acbe', rm.id, 'CRP (định lượng)', 'crpdinhluong', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'CRP'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'crpdinhluong');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '4e9cb545-33ad-430d-a898-fcc4bf4fa9bb', rm.id, 'Ure', 'ure', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Urea'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'ure');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '8fd31542-e3b8-4fc2-8583-c0859da798fa', rm.id, 'Creatinin', 'creatinin', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Creatinine'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'creatinin');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '2de993c5-f2cf-4fe5-b64d-61de1dd8a777', rm.id, 'Creatinin*', 'creatinin', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Creatinine'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'creatinin');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT 'e2966f08-17ea-4950-b8c4-eb1fb4d4d7d6', rm.id, 'Bilirubin toan phan', 'bilirubintoanphan', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Bilirubin Total'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'bilirubintoanphan');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '3f3688d5-822f-4a78-8301-682f7c67f347', rm.id, 'Bilirubin truc tiep', 'bilirubintructiep', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Bilirubin Direct'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'bilirubintructiep');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '0fd8c994-c8d2-4582-96f8-b2d568b530df', rm.id, 'Protein toan phan', 'proteintoanphan', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Total Protein'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'proteintoanphan');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT 'a8f191a0-3c45-4308-b8d6-af817c2cb6e2', rm.id, 'SGPT', 'sgpt', 'en', TRUE
FROM reference_metrics rm
WHERE rm.name = 'ALT'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'sgpt');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '5d47f4d4-f16d-4f3d-b1f3-9a26738d0f18', rm.id, 'SGPT (ALT)', 'sgptalt', 'en', TRUE
FROM reference_metrics rm
WHERE rm.name = 'ALT'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'sgptalt');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '47d657b8-17d9-4bf0-b030-87dd514f2878', rm.id, 'SGPT (ALT)*', 'sgptalt', 'en', TRUE
FROM reference_metrics rm
WHERE rm.name = 'ALT'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'sgptalt');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '4f78995b-7f47-4c95-a220-4f26bcd972c6', rm.id, 'Phosphatase kiem (ALP)', 'phosphatasekiemalp', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'ALP'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'phosphatasekiemalp');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT 'ba51596b-e95b-49df-b8b5-9d7ee1dc37fe', rm.id, 'Phosphatase kiềm (ALP)', 'phosphatasekiemalp', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'ALP'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'phosphatasekiemalp');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '864b217f-1d63-4b66-97a8-df77591c72b5', rm.id, 'ALP.', 'alp', 'en', TRUE
FROM reference_metrics rm
WHERE rm.name = 'ALP'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'alp');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '2f7dea2e-c42c-4890-b75d-4a3bc6dca6e6', rm.id, 'Na+', 'na', 'en', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Sodium'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'na');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '8c9d69a5-aa4b-4b7f-b901-74e05e1596b8', rm.id, 'Natri', 'natri', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Sodium'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'natri');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT 'f2a48929-97c7-42f2-a6b6-3020a6715184', rm.id, 'K+', 'k', 'en', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Potassium'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'k');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '0d35f330-4cd0-4ddb-b454-90163eefd6e6', rm.id, 'Ka', 'ka', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Potassium'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'ka');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '5e92874c-f511-42e3-a6de-dfb9b63db1dc', rm.id, 'Kali', 'kali', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Potassium'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'kali');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '16717bf7-5cff-4c55-a550-4203f06287a5', rm.id, 'Cl-', 'cl', 'en', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Chloride'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'cl');

INSERT INTO reference_metric_aliases (id, metric_id, alias, alias_normalized, locale, is_active)
SELECT '72d659f9-8914-415e-ae34-d122577f8098', rm.id, 'Clorua', 'clorua', 'vi', TRUE
FROM reference_metrics rm
WHERE rm.name = 'Chloride'
  AND NOT EXISTS (SELECT 1 FROM reference_metric_aliases a WHERE a.alias_normalized = 'clorua');
