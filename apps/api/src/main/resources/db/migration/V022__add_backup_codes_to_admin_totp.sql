-- Add backup_codes to admin_totp_secrets
ALTER TABLE admin_totp_secrets ADD COLUMN backup_codes TEXT;
