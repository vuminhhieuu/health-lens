ALTER TABLE notification_inbox_read_state
    ADD COLUMN item_type VARCHAR(40),
    ADD COLUMN title VARCHAR(200),
    ADD COLUMN body TEXT,
    ADD COLUMN item_created_at TIMESTAMPTZ,
    ADD COLUMN action_url VARCHAR(500);
