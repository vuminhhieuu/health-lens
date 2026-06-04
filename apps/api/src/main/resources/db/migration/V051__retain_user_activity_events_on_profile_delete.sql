DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = current_schema()
          AND table_name = 'user_activity_events'
          AND constraint_name = 'user_activity_events_profile_id_fkey'
    ) THEN
        ALTER TABLE user_activity_events
            DROP CONSTRAINT user_activity_events_profile_id_fkey;
    END IF;
END $$;

ALTER TABLE user_activity_events
    ADD CONSTRAINT user_activity_events_profile_id_fkey
    FOREIGN KEY (profile_id) REFERENCES profiles(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_activity_events_profile_id
    ON user_activity_events (profile_id)
    WHERE profile_id IS NOT NULL;
