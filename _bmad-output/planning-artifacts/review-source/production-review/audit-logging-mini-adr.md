# Mini ADR: Audit Logging Spine

## Context

Production scope của HealthLens cần một audit layer thống nhất cho compliance, ops, và admin viewer.
Bảng log rời rạc hiện tại không đủ cho truy vết end-to-end.

## Decision

- Dùng một `correlation_id` thật từ ingress.
- Tách rõ 2 luồng:
  - `audit_events` cho compliance/security.
  - `user_activity_events` cho analytics.
- OCR có thể có bảng riêng `ocr_job_events` nếu muốn tách observability khỏi compliance.
- Không ghi audit trong read-only GET path.
- Retention phải được enforce bằng job riêng.

## Event taxonomy

### `ADMIN_REFERENCE_DATA_MUTATION`
- Fields: `actor_id`, `actor_role`, `action`, `entity_type`, `entity_id`, `before_json`, `after_json`, `diff_json`, `ip_address`, `user_agent`, `request_id`, `correlation_id`, `trace_id`, `outcome`.

### `ADMIN_AUTH_EVENT`
- Fields: `actor_id` or `attempted_email_hash`, `action`, `result`, `reason_code`, `ip_address`, `user_agent`, `request_id`, `correlation_id`, `trace_id`, `jti`.

### `HEALTH_DATA_ACCESS`
- Fields: `actor_id`, `subject_user_id`, `profile_id`, `record_id`, `access_type`, `grant_basis`, `consent_version`, `ip_address`, `user_agent`, `request_id`, `correlation_id`, `trace_id`, `outcome`.

### `PROFILE_SHARE_EVENT`
- Fields: `actor_id`, `owner_id`, `viewer_id`, `profile_id`, `invitation_id`, `action`, `access_level`, `invitee_email_hash`, `ip_address`, `user_agent`, `request_id`, `correlation_id`, `trace_id`, `outcome`.

### `RIGHT_TO_DELETE_EVENT`
- Fields: `user_id`, `deletion_request_id`, `action`, `requested_at`, `scheduled_deletion_at`, `completed_at`, `deleted_counts_by_table`, `ip_address`, `user_agent`, `request_id`, `correlation_id`, `trace_id`, `outcome`.

### `OCR_PIPELINE_EVENT`
- Fields: `record_id`, `profile_id`, `job_id`, `mime_type`, `provider`, `provider_request_id`, `provider_region`, `retention_mode`, `latency_ms`, `confidence`, `failure_reason`, `file_key_hash`, `request_id`, `correlation_id`, `trace_id`, `outcome`.

### `CONSENT_EVENT`
- Fields: `user_id`, `consent_version`, `consent_action`, `consent_scope`, `ip_address`, `user_agent`, `request_id`, `correlation_id`, `trace_id`, `outcome`.

### `USER_ACTIVITY_EVENT`
- Fields: `user_id`, `event_type`, `route`, `profile_id`, `record_id`, `request_id`, `correlation_id`, `trace_id`, `outcome`.

## Logging rules

- Không log raw OCR text, pre-signed URL, full `fileKey`, authorization header, hoặc token/query PII.
- OCR logs chỉ ghi provider metadata, latency, confidence, reason, and retention mode.
- Async events phải carry `correlation_id` + `job_id` qua queue/stream.
- Retention phải được purge bằng scheduled job, không trông vào manual cleanup.

## Suggested schema

- `audit_events`: canonical compliance store.
- `user_activity_events`: nguồn cho analytics.
- `ocr_job_events`: nếu muốn tách monitoring OCR.
- `consent_logs`: giữ lại, nhưng thêm `request_id`, `correlation_id`, `trace_id`, `consent_action`.
- `deletion_receipts`: giữ bằng chứng đã xóa mà không giữ PII liên kết user.

## Gaps to close

- User access logging còn thiếu ở read surfaces chính.
- `requestId` hiện tại chưa đủ làm correlation id xuyên hệ thống.
- Right-to-delete chưa purge hết audit rows liên quan user.
- PII redaction trong log hiện tại vẫn còn rủi ro.

## Rollout

1. Add correlation middleware.
2. Add canonical audit/event tables.
3. Wire write paths for admin, share, delete, consent, OCR.
4. Add purge jobs and retention verification.
