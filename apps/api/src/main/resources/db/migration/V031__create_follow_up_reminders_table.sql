CREATE TABLE follow_up_reminders (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    reminder_date DATE NOT NULL,
    reminder_type VARCHAR(50) NOT NULL,
    note TEXT,
    email_claimed_at TIMESTAMP,
    email_sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_follow_up_reminders_profile_date
    ON follow_up_reminders (profile_id, reminder_date);

CREATE INDEX idx_follow_up_reminders_due_email
    ON follow_up_reminders (reminder_date, email_claimed_at)
    WHERE email_sent_at IS NULL;
