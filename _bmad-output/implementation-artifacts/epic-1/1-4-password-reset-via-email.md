# Story 1.4: Đặt lại mật khẩu qua email

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Quên mật khẩu? - HealthLens | `projects/578519912546445367/screens/f1b10549c80e4f98ab45fa121a50f009` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzllNmI0OGNiNGYwNDRkZjRhYWE4NDZlYzU1ZTQ5MTNhEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhdY0BI6WxCTrVAEZiZ0wF9BHSBJsI2Jwrog1W2lWIPoN35RvmQAJRMtS3PJodVldSuKOKwddDBH5hkHy8FJKZzq9WcOsXimCkElwgO28A9R11ipQ3rp5qY21pX9B4YT79guN-LSSfpV_AjmE0IhOqGTla4P6Zr-VF6fr-3hdIQ3zHTz97OmmH4Rmtk1InsFr_o-BHu_Yb8oIj30XSWGBf4roDN8D13BY3idzcl9Fbghx253fX3ZUyYGA) |
| Yêu cầu đã gửi - HealthLens | `projects/578519912546445367/screens/c8952e57aec54ba0aa5da4bdd913cddb` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2UyNWFkNjg0ZTNjZTQyZDZiYjlkMjE1MTFiMGRlODNkEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhVYatTtOtnH4ZwbdM4dGx2NIxiSNCDei-kP60z6SLssAsYbx2aqICfaFWmTLVpHJTqXXEimgsvDiHM43nPB8iAstG3lbV2M27fMF_A6u6JozxSzvTCbv9wsR-M19V93G-UQSVUFCPHo_DbW6pGsgsfTKWW__Jy5RR8HMB21PqVMUfF2MEbhFRV-j2YnOi0pPU4pTiPu1LAhhjRO8DgepNAvLhQAZxtRZTeVHMFC1r-qTjKZxXcg5aOWQ) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng quên mật khẩu,
I want yêu cầu đặt lại mật khẩu qua email,
so that tôi khôi phục quyền truy cập mà không cần hỗ trợ thủ công.

## Acceptance Criteria

1. **Given** email đã đăng ký, **When** gửi yêu cầu quên mật khẩu, **Then** hệ thống phát hành reset token và gửi email đặt lại (tiếng Việt).
2. **Given** email chưa đăng ký, **When** gửi yêu cầu, **Then** vẫn trả về 200 OK (không lộ email có tồn tại hay không — security by obscurity).
3. **Given** reset token hợp lệ chưa hết hạn, **When** submit password mới, **Then** password được cập nhật và token bị vô hiệu hóa ngay.
4. **Given** reset token đã dùng hoặc hết hạn (1 giờ), **When** thử dùng lại, **Then** trả về lỗi 400 với thông báo rõ ràng.
5. **Given** đặt lại mật khẩu thành công, **When** đăng nhập bằng mật khẩu cũ, **Then** 401 Unauthorized.
6. **Given** đặt lại mật khẩu thành công, **When** kiểm tra, **Then** tất cả refresh tokens hiện tại của user bị revoke.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Forgot password endpoint (AC: #1, #2)
  - [ ] Tạo `POST /api/v1/auth/forgot-password` với body `{ "email": "..." }`
  - [ ] Nếu email tồn tại: tạo reset token (UUID, TTL 1 giờ), lưu bảng `password_reset_tokens`, gửi email
  - [ ] Nếu email không tồn tại: vẫn trả 200 OK (không lộ thông tin)
  - [ ] Rate limit: tối đa 3 request mỗi email mỗi giờ (Redis counter)
- [ ] Task 2 — Database: Password reset tokens table (AC: #3, #4)
  - [ ] Flyway migration `V004__create_password_reset_tokens_table.sql`
  - [ ] Schema: id, user_id, token, expires_at, used_at, created_at
- [ ] Task 3 — Backend: Reset password endpoint (AC: #3, #4, #5, #6)
  - [ ] Tạo `POST /api/v1/auth/reset-password` với body `{ "token": "...", "newPassword": "..." }`
  - [ ] Validate token: tồn tại, chưa dùng, chưa hết hạn
  - [ ] Cập nhật `password_hash` của user, đánh dấu token `used_at`, revoke tất cả refresh tokens
  - [ ] Validate `newPassword` theo policy (min 8 ký tự, 1 chữ hoa, 1 số)
- [ ] Task 4 — Backend: Email template tiếng Việt (AC: #1)
  - [ ] HTML email với subject "HealthLens - Đặt lại mật khẩu"
  - [ ] Link reset: `{frontendUrl}/auth/reset-password?token={token}`
  - [ ] Ghi rõ: "Link có hiệu lực trong 1 giờ"
- [ ] Task 5 — Web: Forgot password page (AC: #1, #2)
  - [ ] Tạo `apps/web/src/app/(auth)/forgot-password/page.tsx`
  - [ ] Form nhập email, submit → POST `/api/v1/auth/forgot-password`
  - [ ] Success state: "Kiểm tra email của bạn" (không phân biệt email có tồn tại hay không)
- [ ] Task 6 — Web: Reset password page (AC: #3, #4)
  - [ ] Tạo `apps/web/src/app/(auth)/reset-password/page.tsx`
  - [ ] Đọc `?token=` từ URL params
  - [ ] Form: newPassword, confirmPassword với Zod validation (dùng shared `passwordSchema`)
  - [ ] Submit → POST `/api/v1/auth/reset-password`, redirect đến `/login` khi thành công
  - [ ] Error state: "Link đã hết hạn hoặc đã được sử dụng"
- [ ] Task 7 — Tests (AC: #1, #2, #3, #4, #6)
  - [ ] `AuthServiceTest`: forgot-password email tồn tại, không tồn tại, reset token hợp lệ, hết hạn, đã dùng

## Dev Notes

### Database Schema

```sql
-- V004__create_password_reset_tokens_table.sql
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_prt_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_token ON password_reset_tokens(token);
```

### API Contracts

```
POST /api/v1/auth/forgot-password
Body: { "email": "user@example.com" }
Response 200: { "data": { "message": "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu." } }

POST /api/v1/auth/reset-password
Body: { "token": "uuid-token", "newPassword": "NewPass123!" }
Response 200: { "data": { "message": "Mật khẩu đã được đặt lại thành công." } }
Response 400 (token không hợp lệ): RFC 7807 { "title": "Token không hợp lệ hoặc đã hết hạn" }
```

### Redis Rate Limiting

```
Key: forgot_password_rate:{email}
TTL: 3600 seconds (1 giờ)
Value: count (max 3)
```

### Security Notes

- Token là UUID random, không dự đoán được
- Token TTL 1 giờ (không phải 24h như email verification)
- Sau khi reset: revoke **tất cả** refresh tokens của user (prevent session hijacking)
- Không dùng token reset như session — chỉ dùng một lần

### Shared Password Schema

```typescript
// packages/shared/schemas/auth.ts (thêm vào file đã có)
export const passwordSchema = z
  .string()
  .min(8, 'Mật khẩu tối thiểu 8 ký tự')
  .regex(/[A-Z]/, 'Cần ít nhất 1 chữ hoa')
  .regex(/[0-9]/, 'Cần ít nhất 1 chữ số');

export const resetPasswordSchema = z.object({
  token: z.string().uuid(),
  newPassword: passwordSchema,
  confirmPassword: z.string()
}).refine(d => d.newPassword === d.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword']
});
```

### References

- [Source: architecture.md#Xác-Thực-&-Bảo-Mật]
- [Source: epics.md#Story-1.4]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
