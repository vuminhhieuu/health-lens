# Story 1.6: Quy trình yêu cầu xóa toàn bộ dữ liệu tài khoản

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Bảo mật & MFA - Admin HealthLens | `projects/578519912546445367/screens/fc0abf8742984269b5a25a0e0ab20782` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzgwODQwM2RhZmU1ZjQxYzVhZjlkMmM3M2ZhYjZlNTRlEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugyW7fgyBR0_jEdEXcjJfxOc-6CikrdsiczJhlmYeYx06neSyys78DHloUAPnRGIPR0XUM-FfdkIQfkCuMiyyGWP_D809TnwtZI6UiT2Qva9syxUZw3-VLdJMq9hOZ32beEUYcHZMV6u172JDnvzcJxBToc3yjtRX4QALmrM65BksPWyZEsOBvzll_uAk6GD_dAfZznnXv_RjbRu7Tp-y3KzwEr9BlywF2XpRJDYYsGDS-Z2hXU5biO9g) |
| Cài đặt hệ thống - Admin HealthLens | `projects/578519912546445367/screens/f6198b62522b49d88bd34fad6c9cee84` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzExNDJmMmIzZGJjMzQ5NDRiNzE2ZTJiYTExMThmZjAwEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uguwdmxJMCbxtT_MJxyXkaWRm-i6TXm49DBhdMUFgh18aER7jtytjHXhxWAnlOp9FKlKGCxCFYFDV_JWI5YRAHjKFc9B8mrzq3vp41YbW5E8pJmRrxjiLvQHfIjrQWk4f_zZ3CWPJKYYqbHylwy3JU4SNYAdPzJNRlYwSSfa0pBD0qh8mClBoKDNPYP6wERtt-Gu6gtm6ngH9X0Owpgk_lpM2Ojv4ZPCd-oPlnLsQV41vYcgn2ANad_jA) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want gửi yêu cầu right-to-delete,
so that dữ liệu của tôi được xóa theo NĐ 13/2023.

## Acceptance Criteria

1. **Given** người dùng đã xác thực danh tính (nhập lại password), **When** gửi yêu cầu xóa tài khoản, **Then** hệ thống tạo yêu cầu xóa và thông báo thời hạn hoàn tất ≤72 giờ.
2. **Given** yêu cầu xóa đã tạo, **When** kiểm tra sau 72 giờ, **Then** toàn bộ PII, health records, file PDF/ảnh gốc, consent logs, audit logs liên quan đến user bị xóa vĩnh viễn.
3. **Given** yêu cầu xóa đang pending, **When** user cố đăng nhập, **Then** tài khoản bị vô hiệu hóa và hiển thị thông báo "Tài khoản đang chờ xóa".
4. **Given** yêu cầu xóa đã submit, **When** user nhận email xác nhận, **Then** email ghi rõ những dữ liệu nào sẽ bị xóa và thời hạn.
5. **Given** yêu cầu đang pending (trong 72h), **When** user muốn hủy yêu cầu (grace period), **Then** có thể hủy yêu cầu xóa bằng link trong email.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Data deletion request table + endpoint (AC: #1, #4)
  - [ ] Flyway migration `V006__create_data_deletion_requests_table.sql`
  - [ ] Schema: id, user_id, requested_at, scheduled_deletion_at (requested_at + 72h), status (pending/completed/cancelled), cancellation_token
  - [ ] `POST /api/v1/users/me/deletion-request` — xác thực password trước, tạo request, gửi email
  - [ ] Vô hiệu hóa tài khoản ngay (set `account_status = 'pending_deletion'` trong users table)
- [ ] Task 2 — Backend: Thêm cột account_status vào users (AC: #3)
  - [ ] Flyway migration `V007__add_account_status_to_users.sql`
  - [ ] Thêm cột `account_status` với values: `active`, `pending_deletion`, `deleted`
  - [ ] Cập nhật `JwtAuthenticationFilter` để reject users với status không phải `active`
- [ ] Task 3 — Backend: Cancellation endpoint (AC: #5)
  - [ ] `DELETE /api/v1/users/me/deletion-request?token={cancellationToken}`
  - [ ] Validate cancellation token, chỉ cho phép nếu status còn `pending`
  - [ ] Khôi phục account_status về `active`
- [ ] Task 4 — Backend: Scheduled deletion job (AC: #2)
  - [ ] Spring `@Scheduled` chạy mỗi giờ: tìm requests quá 72h
  - [ ] Xóa theo thứ tự: files (S3), health_records, profiles, refresh_tokens, consent_logs, user record
  - [ ] Ghi audit log cuối cùng: "User data deleted per right-to-delete request"
  - [ ] Sau khi xóa: cập nhật status = 'completed'
- [ ] Task 5 — Web: Account deletion UI (AC: #1, #5)
  - [ ] Tạo page `apps/web/src/app/(dashboard)/settings/delete-account/page.tsx`
  - [ ] Confirm dialog: 2 bước — cảnh báo + nhập lại password
  - [ ] Sau submit: logout và hiển thị "Yêu cầu xóa đã được gửi"
  - [ ] Link "Hủy yêu cầu xóa" trong email (redirect đến trang hủy)
- [ ] Task 6 — Tests (AC: #1, #2, #3)
  - [ ] Service test: tạo request, hủy request, scheduled deletion logic

## Dev Notes

### Database Schema

```sql
-- V006__create_data_deletion_requests_table.sql
CREATE TABLE data_deletion_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    scheduled_deletion_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    cancellation_token VARCHAR(255) NOT NULL UNIQUE,
    completed_at TIMESTAMP
);

-- V007__add_account_status_to_users.sql
ALTER TABLE users ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'active';
```

### Deletion Sequence (thứ tự quan trọng)

```
1. S3/MinIO files (raw PDFs/images)
2. health_records (JSONB data)
3. profiles
4. email_verification_tokens
5. password_reset_tokens
6. refresh_tokens
7. consent_logs
8. data_deletion_requests (cuối cùng)
9. users (xóa soft: set deleted_at, xóa PII)
```

### Spring Scheduler

```java
@Scheduled(cron = "0 0 * * * *")  // Mỗi giờ
public void processDeletionRequests() {
    List<DeletionRequest> overdue = repo.findByStatusAndScheduledBefore("pending", LocalDateTime.now());
    overdue.forEach(this::executeDataDeletion);
}
```

### Email Templates

- Confirmation email: "Yêu cầu xóa dữ liệu của bạn"
- Danh sách dữ liệu sẽ xóa, link hủy (có token), deadline 72h
- Completion email: "Dữ liệu của bạn đã được xóa"

### References

- [Source: architecture.md#Chiến-Lược-Audit-Logging]
- [Source: epics.md#Story-1.6]
- [Source: prd.md#FR7]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
