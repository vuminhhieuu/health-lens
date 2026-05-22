ALTER TABLE users
    ADD COLUMN IF NOT EXISTS personal_description TEXT,
    ADD COLUMN IF NOT EXISTS personal_notes TEXT;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS chronic_conditions TEXT,
    ADD COLUMN IF NOT EXISTS current_medications TEXT,
    ADD COLUMN IF NOT EXISTS allergies TEXT;
