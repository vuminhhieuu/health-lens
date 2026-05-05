# Story 1.6: Quy trình yêu cầu xóa toàn bộ dữ liệu tài khoản

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** Remix of HealthLens-Web-MVP — `projectId`: `2006871874602765093`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Yêu cầu xóa tài khoản - HealthLens | `projects/2006871874602765093/screens/65a88741d456491da0ecff35f87d1f3a` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ7Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpaCiVodG1sXzNjNjBkZjQxOGZmMjQyYmQ4NGRjMGY1ZjljMjdkYzI4EgsSBxDlvJH8zh8YAZIBIwoKcHJvamVjdF9pZBIVQhMyMDA2ODcxODc0NjAyNzY1MDkz&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhx1sNbYuNZQ2RpmT0OrROkTPFNWlzvFUixsmmdIZ3HIvA7Ca5AApB5cLeMQ5OrR9MDE24lFiXzFmIBnRSLFOkdGyVcFX8-XboDdTvigOcAoeLGhDhqCM7fY7Jke-YXKOzLCgm0wnNluTqKkogM34nEzPWNLbLvAL1jaZKxnsVjJGQX6qiOiS2-M0Gbk3GIfDacfo4lZWOeQ7OJQGjesw3UlfPCwDUnihalSokecLrk6nC3b7qK_CKNiA) |
| Yêu cầu xóa đã được gửi - HealthLens | `projects/2006871874602765093/screens/3cf94b4d41be4b3f9e33cb0331e62993` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ7Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpaCiVodG1sXzNmNzAwYzk4YzU1NjRjM2NhNzQ0YjViZDJmN2I2ZWVkEgsSBxDlvJH8zh8YAZIBIwoKcHJvamVjdF9pZBIVQhMyMDA2ODcxODc0NjAyNzY1MDkz&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugcfHsxebreiqTKHy_xABCelMryploAPgjkSgE4h1Bd2PLocCUai3MTk7mJD2T5LWqE18lH3lqFWPd0XaP2fqqYdliXmRr1t2dwLzrE-PB_4q-Wa-c4B4E4Kk5XkYVjCL47EhF9E-TkPGVsPElXdPzjPiJbsXShzzN7pbx3WVPQniedmnTmK1KM37V3ZemhkrcF2j-YiASLz4Al-GL01JxBG8fkuh528KsoihQJeAC_LimPHNEYf_s03t4) |
| Tài khoản đang chờ xóa - HealthLens | `projects/2006871874602765093/screens/a00b4e50d5434fc5b9444d2c21fe5551` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ7Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpaCiVodG1sXzkzZGU4YzRlMWJhZDRmZmRiYTcxOTM0OTExMTVmZjdmEgsSBxDlvJH8zh8YAZIBIwoKcHJvamVjdF9pZBIVQhMyMDA2ODcxODc0NjAyNzY1MDkz&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ujbTBzvFoNlRNCGJGdVJEJYDCYf4Ne_yXeqlzDRrX8owoGZIuraa8qCWtGl88x6KphV86jclh52YNIPgyB9naSUqd-7acwQbdva9wvjatY0TsDKIVIHU0KqTB_KnWYvcpVTE32J0xs7ZXayaIIrSCzS_xl1AyECiplJu5yoiUXSnCA7DuLYZjDzP9d2f_TFuUYgwr2btE5y82Hx3Y_jogpAIa7sgbEtaGsNglV6kh0HhhsE0nGSXzI7I9c) |
| Hủy yêu cầu xóa - HealthLens | `projects/2006871874602765093/screens/060b7ed9f7364f54bb27545a811778bc` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ7Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpaCiVodG1sXzZmYWRkNTg5ODVjOTRmMDk4OGVjMDY1NzdlZmRkZjU3EgsSBxDlvJH8zh8YAZIBIwoKcHJvamVjdF9pZBIVQhMyMDA2ODcxODc0NjAyNzY1MDkz&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugEjZnxrq209QRLte3GT1K0M39HLYoqAVdvStKe0c1B_N9SYRiSk7638cSQcSXu3jCNjiOt3Zw9zVtQTGeQxzOF2j3YeRrhKUS-gHNFTvh7c3Ep8w1zldBGvn-mlib6bbVb0oq1EOQH2HV6yHQJU2U4KkVl38ZO1gEWIOBG5Rs_5MrXh542gD0d18v_i0ubDG_DyS7aH-FbfB0QnDNgKlvsSxUNuDI04iSCE7-g9oDtadvlxEujgEgNJw) |
| Đã hủy yêu cầu xóa - HealthLens | `projects/2006871874602765093/screens/6e107508aa4b476f8999c283872197dd` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ7Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpaCiVodG1sXzc2MjZlZmUyMmU1NTQ0YWU4YjgxY2Y5YzU3ZWI0NjUwEgsSBxDlvJH8zh8YAZIBIwoKcHJvamVjdF9pZBIVQhMyMDA2ODcxODc0NjAyNzY1MDkz&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugvguh4mNrFD08RNhiX4gM2zKHFggO_Av7-WPuLd8Nu81DkkERxymBgrpKk59tF1sxP9U7SUSYVJ9vAAXEee9PdcDOqJhZ7ZggtsZ8YGvLYW4w_qNZD0Ljk-Vk9TH5eeoMxKC1fwr9_mrc9uWIeImzmdkIoEVuK8a8ygeJBOWScLsrud0U3rHrj3Lf-hLag_W5zfe75xuO7XdOTAsKti_ZXYxQs3HE-2YxA6UpmKTMwLEX7pgF8vnFU67A) |
| Hủy yêu cầu xóa - Link không hợp lệ | `projects/2006871874602765093/screens/ac55b9d379de4ac88ee3acaaeb49c04c` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ7Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpaCiVodG1sXzNhNzIzNjBkZjEyMzRkYzc5YzUyZjdiOWViNDc2M2FkEgsSBxDlvJH8zh8YAZIBIwoKcHJvamVjdF9pZBIVQhMyMDA2ODcxODc0NjAyNzY1MDkz&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ui1XA4NWjW94Kbbk_52utB9VXv-qEQKdTVhfWb3Efonyk_XyxvbqhOYqiJtrBnPYGLsJUmxwpaGgrOHK5fe_uHW8xAzeGP6bQAy2u-jEKRFO49b5l2Ccc7noSrng8AZcQWgzUx9fHp2gayJzzAkIHyEB-9Rw4y9NgNZKJaX91xt23gGz5iE4mpalWGZSPDeXVX-Te95Hrl4ESeW2dERaefxup_I90f9IXBvGqQJrI44dmOOqtmmiDJ_yBo) |
| Email xác nhận yêu cầu xóa | `projects/2006871874602765093/screens/c1cb2b18af4b48609df90ef17d2f8782` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ7Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpaCiVodG1sX2I0YWFiZGI2M2Q0ZTRmMzc4YzJjYTc5NzBmY2IwZTg4EgsSBxDlvJH8zh8YAZIBIwoKcHJvamVjdF9pZBIVQhMyMDA2ODcxODc0NjAyNzY1MDkz&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ug6Z5HM7syA9MSkG_vTvrtM-Yc3x3AuU2yP7WbeAx2tD22J89M_sOEc8PZ9PayRcgfv1kmzxNlfN1nf8UC2zjZ5Qv3dS5OjPeuqdX-ZnBM0xIsoRjFA4D0Z63kxQVWYRIKY1uAna3VF8PzU_qz9VEKHFGTtyMS5POMEOufVLiCyuu3l4_p6ehs8CJnqmqm4aNJP8c24wVnBw36Z7WxRLKxRCK2A1TNIddMH3-OsnYZrxwosztz9_fz3TA) |

*Ghi chú:* Link HTML prototype và screenshot từ Google Stitch có thể hết hạn. Làm mới: MCP `get_screen` với `name` = `projects/2006871874602765093/screens/{screenId}` (hoặc `list_screens` + `projectId` trên).

### Route/Code Mapping (Web App)

| Màn hình Story 1.6 | Route hiện tại | File code chính |
|---|---|---|
| Yêu cầu xóa tài khoản | `/settings/delete-account` | `apps/web/src/app/(dashboard)/settings/delete-account/page.tsx` |
| Yêu cầu xóa đã được gửi (success state) | `/settings/delete-account` (success state nội bộ sau submit) | `apps/web/src/app/(dashboard)/settings/delete-account/page.tsx` |
| Tài khoản đang chờ xóa (blocked login) | `/login?pendingDeletion=1` | `apps/web/src/app/(auth)/login/page.tsx` |
| Hủy yêu cầu xóa (xác nhận) | `/cancel-deletion?token=...` | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx` |
| Đã hủy yêu cầu xóa (success sau API) | `/cancel-deletion?token=...` | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx` |
| Hủy yêu cầu xóa - token không hợp lệ | `/cancel-deletion?token=invalid` | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx` |

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
  - [ ] `DELETE /api/v1/users/deletion-requests/cancel?token={cancellationToken}` — **public** (`permitAll`), xác thực bằng token trong email (không JWT; vì vậy **không** nằm dưới `/api/v1/users/me/...`)
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

Codex 5.3

### Debug Log References

- Web tests: `pnpm exec vitest run "src/app/(auth)/login/page.test.tsx" "src/app/(dashboard)/settings/delete-account/page.test.tsx" "src/app/cancel-deletion/CancelDeletionClient.test.tsx"` (4 tests passed)
- Lint check for story files passed after cleanup (`ReadLints` on updated files)
### Completion Notes List

- Hoàn thiện UI luồng Story 1.6 theo Stitch cho 3 màn hình chính: gửi yêu cầu xóa, chặn đăng nhập khi pending deletion, hủy yêu cầu xóa.
- Bổ sung template email xác nhận yêu cầu xóa (`email/deletion-request`) và tích hợp render bằng Thymeleaf trong `EmailService`.
- Bổ sung test cho login pending-deletion, delete-account page và cancel-deletion invalid-link.
- Cập nhật nhẹ header email theo yêu cầu UX: bỏ icon emoji, giữ header trung tính tương thích email client.
### File List

- `apps/api/src/main/java/com/healthlens/api/service/EmailService.java`
- `apps/api/src/main/resources/templates/email/deletion-request.html`
- `apps/web/src/app/(auth)/login/page.tsx`
- `apps/web/src/app/(auth)/login/page.test.tsx`
- `apps/web/src/app/(dashboard)/settings/delete-account/page.tsx`
- `apps/web/src/app/(dashboard)/settings/delete-account/page.test.tsx`
- `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx`
- `apps/web/src/app/cancel-deletion/CancelDeletionClient.test.tsx`
- `apps/web/src/lib/api/routes.ts`
- `apps/web/vitest.setup.ts`
