ALTER TABLE profiles
    ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill one default profile per user for existing data.
WITH ranked_profiles AS (
    SELECT id,
           user_id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at ASC, id ASC) AS rn
    FROM profiles
)
UPDATE profiles p
SET is_default = TRUE
FROM ranked_profiles rp
WHERE p.id = rp.id
  AND rp.rn = 1;

-- Enforce at most one default profile per user.
CREATE UNIQUE INDEX uq_profiles_user_default_true
    ON profiles (user_id)
    WHERE is_default = TRUE;
