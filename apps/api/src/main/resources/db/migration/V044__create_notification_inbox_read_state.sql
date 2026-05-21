CREATE TABLE notification_inbox_read_state (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    inbox_item_id VARCHAR(120) NOT NULL,
    read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_inbox_read_user_item UNIQUE (user_id, inbox_item_id)
);

CREATE INDEX idx_notification_inbox_read_user ON notification_inbox_read_state (user_id);
