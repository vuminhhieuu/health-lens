CREATE TABLE user_activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(50) NOT NULL,
    is_retry BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_events_type_created ON user_activity_events (event_type, created_at);
CREATE INDEX idx_activity_events_user_created ON user_activity_events (user_id, created_at);

-- Daily AUTH uniqueness must follow UTC calendar day regardless of DB/session timezone.
CREATE UNIQUE INDEX idx_activity_auth_daily
    ON user_activity_events (user_id, ((created_at AT TIME ZONE 'UTC')::date))
    WHERE event_type = 'AUTHENTICATED_API_CALL';
