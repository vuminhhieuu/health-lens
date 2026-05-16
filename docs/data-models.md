# Data Models

**Last updated:** 2026-05-16

## Persistence Strategy

The backend uses PostgreSQL with Spring Data JPA entities and Flyway migrations under `apps/api/src/main/resources/db/migration`.

## Main Tables And Entities

| Table/entity | Purpose |
| --- | --- |
| `users` / `User` | Account identity, profile basics, role, account status, verification state |
| `email_verification_tokens` / `EmailVerificationToken` | Email verification lifecycle |
| `refresh_tokens` / `RefreshToken` | Refresh-token hashes and revocation |
| `password_reset_tokens` / `PasswordResetToken` | Password reset token lifecycle |
| `consent_logs` / `ConsentLog` | Consent version acceptance and revocation history |
| `data_deletion_requests` / `DataDeletionRequest` | Scheduled account deletion and cancellation token |
| `profiles` / `Profile` | Family/self health profiles, default profile marker, demographics |
| `health_records` / `HealthRecord` | Uploaded record metadata, OCR JSON, metrics JSON, analysis metadata, soft delete |
| `reference_metrics` / `ReferenceMetric` | Canonical health metric definitions |
| `reference_metric_aliases` / `ReferenceMetricAlias` | Aliases for metric matching/search |
| `reference_ranges` / `ReferenceRange` | Reference ranges by metric, sex, age, unit |
| `reference_range_audit_logs` / `ReferenceRangeAuditLog` | Applied range audit trail |
| `reference_data_change_sets` / `ReferenceDataChangeSet` | Admin approval workflow changes |
| `profile_invitations` / `ProfileInvitation` | Family sharing invitations |
| `profile_shares` / `ProfileShare` | Granted profile access |
| `profile_share_audit_logs` / `ProfileShareAuditLog` | Profile sharing audit events |
| `health_record_audit_logs` / `HealthRecordAuditLog` | Health record access/action audit events |
| `admin_totp_secrets` / `AdminTotpSecret` | Admin TOTP secret and backup code storage |

## Important Relationships

- `User` owns default and custom `Profile` records.
- `HealthRecord` is associated with both `profile_id` and `user_id`.
- `ProfileShare` links owner, viewer, profile, access level, granted time, and revocation time.
- `ProfileInvitation` tracks pending/accepted/rejected invitation state by token and invitee email.
- `ReferenceRange` belongs to `ReferenceMetric`.
- `ReferenceMetricAlias` belongs to `ReferenceMetric`.
- Token tables use expiration and used/revoked fields instead of deleting historical state immediately.

## JSON Fields

| Field | Entity | Notes |
| --- | --- | --- |
| `raw_ocr_result` | `HealthRecord` | JSONB OCR output |
| `metrics` | `HealthRecord` | JSONB extracted/confirmed metrics |
| `changes_json` | `ReferenceDataChangeSet` | JSONB admin change payload |
| `backup_codes` | `AdminTotpSecret` | Text-encoded backup codes |

## Migration Coverage

Flyway migrations currently cover:

- User/auth bootstrap: `V001`-`V008`
- Profiles: `V009`-`V011`, `V023`
- Health records and audit: `V012`, `V016`
- Reference data and audit: `V013`, `V014`, `V024`-`V026`
- Sharing/invitations: `V015`, `V017`-`V020`, `V027`
- Admin TOTP: `V021`-`V022`

