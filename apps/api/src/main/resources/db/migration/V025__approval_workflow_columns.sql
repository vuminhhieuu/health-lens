-- V025: Add reviewer and rejection columns for approval workflow (Story 7.4)

ALTER TABLE reference_data_change_sets
    ADD COLUMN reviewer_id UUID REFERENCES users(id),
    ADD COLUMN rejection_reason TEXT;

CREATE INDEX idx_reference_data_change_sets_reviewer
    ON reference_data_change_sets(reviewer_id);
