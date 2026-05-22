CREATE TABLE user_notification_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    share_invite BOOLEAN NOT NULL DEFAULT TRUE,
    share_accepted BOOLEAN NOT NULL DEFAULT TRUE,
    follow_up_reminder BOOLEAN NOT NULL DEFAULT TRUE,
    security BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE follow_up_reminders
    ADD COLUMN email_skipped_opt_out_at TIMESTAMPTZ;

DROP INDEX IF EXISTS idx_follow_up_reminders_due_email;

CREATE INDEX idx_follow_up_reminders_due_email
    ON follow_up_reminders (reminder_date, email_claimed_at)
    WHERE email_sent_at IS NULL
      AND email_skipped_opt_out_at IS NULL;
