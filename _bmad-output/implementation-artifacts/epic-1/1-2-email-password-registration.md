# Story 1.2: Đăng ký tài khoản bằng email và mật khẩu

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Đăng Ký - HealthLens | `projects/578519912546445367/screens/a3b22ddf15924e8c860ea3239e312044` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2NmOTFmMGM2MjQ4YjQ5ZDU4MGUwZTY5MmNjOGJkZDMzEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugc0IcPLUZ6jH8uWUsN_ZwsR8mGznbOFnJ5e_ARJWjRhKIcZn_zN7jYvZyvOimmyvsseGtFve-yL967dOlx2kpQkN5wMGSJbwvYKzQGqW1FrNtj4E1_t1X73C2drMnz0H3E4rfVHZP2cXJV0jHhdMRfwsEs2DJyCtaBuWDD8WmuTv-1P8mpgJga1tXgdunn6UUVt7-xCeJRWuP9rGqXRAqtDRwvdf84ZhNgWlf3RBZHONwVgo4rrK20) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng mới,
I want tạo tài khoản bằng email và mật khẩu hợp lệ,
so that tôi có thể bắt đầu sử dụng HealthLens.

## Acceptance Criteria

1. **Given** người dùng chưa có tài khoản, **When** nhập email hợp lệ + mật khẩu đạt policy và bấm đăng ký, **Then** hệ thống tạo tài khoản mới và gửi email xác thực.
2. **Given** email đã tồn tại trong hệ thống, **When** đăng ký với email đó, **Then** trả về lỗi RFC 7807 với status 409 và message rõ ràng.
3. **Given** mật khẩu không đạt policy (< 8 ký tự, thiếu chữ hoa/số), **When** submit form, **Then** trả về validation error RFC 7807 với field-level details.
4. **Given** đăng ký thành công, **When** kiểm tra database, **Then** user record được lưu với password đã hash (bcrypt), email chưa verified, role `ROLE_USER`.
5. **Given** form đăng ký trên Web, **When** user tương tác, **Then** có real-time validation feedback (Zod) trước khi submit.
6. **Given** đăng ký thành công, **When** kiểm tra email service, **Then** email xác thực được gửi với link token hết hạn sau 24 giờ.

## Tasks / Subtasks

- [ ] Task 1 — Tạo User entity và Flyway migration (AC: #4)
  - [ ] Tạo `apps/api/src/main/java/com/healthlens/api/entity/User.java` với fields: id (UUID), email, passwordHash, emailVerified, role, createdAt, updatedAt
  - [ ] Tạo Flyway migration `V001__create_users_table.sql` trong `apps/api/src/main/resources/db/migration/`
  - [ ] Index trên `users_uk_email` (unique constraint)
- [ ] Task 2 — Backend: Register API endpoint (AC: #1, #2, #3)
  - [ ] Tạo `RegisterRequest` DTO với `@Email`, `@NotBlank` validation
  - [ ] Tạo `AuthController.java` với `POST /api/v1/auth/register`
  - [ ] Tạo `AuthService.java` với method `register()`: check duplicate, hash password (BCrypt), save user, send email
  - [ ] Tạo `UserRepository.java` extends `JpaRepository<User, UUID>`
  - [ ] Implement `GlobalExceptionHandler` bắt `ConstraintViolationException`, `MethodArgumentNotValidException` → RFC 7807 format
  - [ ] Tạo email verification token (UUID, lưu bảng `email_verification_tokens`, TTL 24h)
  - [ ] Tạo Flyway migration `V002__create_email_verification_tokens_table.sql`
- [ ] Task 3 — Backend: Email service integration (AC: #6)
  - [ ] Tạo `EmailService.java` với method `sendVerificationEmail(user, token)`
  - [ ] Cấu hình Spring Mail trong `application.yml` (dev: Mailhog/MailDev local)
  - [ ] Tạo email template HTML đơn giản tiếng Việt
- [ ] Task 4 — Web: Register form (AC: #5)
  - [ ] Tạo page `apps/web/src/app/(auth)/register/page.tsx`
  - [ ] Form fields: email, password, confirmPassword với React Hook Form + Zod
  - [ ] Zod schema trong `packages/shared/schemas/auth.ts`
  - [ ] Error display theo field-level
  - [ ] Submit → gọi `POST /api/v1/auth/register` qua axios
  - [ ] Success state: redirect hoặc hiển thị "Kiểm tra email của bạn"
- [ ] Task 5 — Mobile (Phase 2): Register screen (AC: #5)
  - [ ] Tạo screen `apps/mobile/app/(auth)/register.tsx`
  - [ ] Form fields tương tự web, React Hook Form + Zod shared schema
  - [ ] Native keyboard handling và accessibility labels
- [ ] Task 6 — Tests (AC: #1, #2, #3, #4)
  - [ ] Backend unit test: `AuthServiceTest` — register thành công, email trùng, password yếu
  - [ ] Backend integration test: `AuthControllerTest` — POST /api/v1/auth/register với MockMvc
  - [ ] Web component test: form validation với Vitest

## Dev Notes

### API Endpoint

```
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}

Response 201:
{
  "data": { "message": "Tài khoản đã tạo. Kiểm tra email để xác thực." },
  "meta": { "timestamp": "...", "requestId": "..." }
}

Response 409 (email trùng):
{
  "type": "https://healthlens.vn/errors/email-already-exists",
  "title": "Email đã tồn tại",
  "status": 409,
  "detail": "Email này đã được đăng ký",
  "instance": "/api/v1/auth/register"
}
```

### Database Schema

```sql
-- V001__create_users_table.sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    consent_given BOOLEAN NOT NULL DEFAULT FALSE,
    consent_given_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT users_uk_email UNIQUE (email)
);

-- V002__create_email_verification_tokens_table.sql
CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Password Policy

- Tối thiểu 8 ký tự
- Ít nhất 1 chữ hoa
- Ít nhất 1 chữ số
- Validation ở cả Zod (frontend) lẫn Spring Validator (backend)

### Zod Schema (packages/shared)

```typescript
// packages/shared/schemas/auth.ts
import { z } from 'zod';

export const registerSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z
    .string()
    .min(8, 'Mật khẩu tối thiểu 8 ký tự')
    .regex(/[A-Z]/, 'Cần ít nhất 1 chữ hoa')
    .regex(/[0-9]/, 'Cần ít nhất 1 chữ số'),
  confirmPassword: z.string()
}).refine(d => d.password === d.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword']
});

export type RegisterInput = z.infer<typeof registerSchema>;
```

### Security Notes

- Password hash với **BCrypt** (strength 12) — dùng `PasswordEncoder` bean của Spring Security
- Không trả về password hash trong response
- Email verification token là UUID ngẫu nhiên (không phải JWT)

### Project Structure Notes

- Backend packages: `com.healthlens.api.controller`, `com.healthlens.api.service`, `com.healthlens.api.entity`, `com.healthlens.api.dto.request`, `com.healthlens.api.dto.response`
- Web: form ở `(auth)` route group — **không** cần authentication middleware
- Shared Zod schema phải export từ `packages/shared/schemas/index.ts`

### References

- [Source: architecture.md#Xác-Thực-&-Bảo-Mật]
- [Source: architecture.md#Mẫu-Cấu-Trúc#Cấu-Trúc-Backend]
- [Source: architecture.md#Định-Dạng-API-Response]
- [Source: architecture.md#Xử-Lý-Form]
- [Source: epics.md#Story-1.2]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
