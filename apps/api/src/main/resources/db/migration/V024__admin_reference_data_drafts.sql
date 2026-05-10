ALTER TABLE reference_metrics
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active';

CREATE INDEX idx_reference_metrics_status
    ON reference_metrics(status);

CREATE TABLE reference_data_change_sets (
    id UUID PRIMARY KEY,
    admin_id UUID NOT NULL REFERENCES users(id),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    operation VARCHAR(20) NOT NULL,
    changes_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMP
);

CREATE INDEX idx_reference_data_change_sets_status
    ON reference_data_change_sets(status);

CREATE INDEX idx_reference_data_change_sets_entity
    ON reference_data_change_sets(entity_type, entity_id);
