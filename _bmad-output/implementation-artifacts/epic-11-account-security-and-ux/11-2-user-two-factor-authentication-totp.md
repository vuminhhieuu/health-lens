# Story 11.2: Xác thực hai yếu tố (2FA) cho người dùng

Status: ready-for-dev

## Story

As a người dùng,
I want bật xác thực TOTP cho tài khoản của tôi,
so that tài khoản an toàn hơn khi mật khẩu bị lộ.

## Acceptance Criteria

1. **Given** người dùng đã đăng nhập, **When** mở `/settings/security`, **Then** thấy trạng thái 2FA và luồng bật/tắt — thay badge "Sắp có" tại `/settings/profile`.
2. **Given** bắt đầu setup, **When** `POST /api/v1/users/me/totp/setup`, **Then** trả QR data URL (hoặc otpauth URI) + secret tạm (chỉ hiển thị một lần) + 8–10 backup codes.
3. **Given** nhập mã TOTP đúng, **When** `POST /api/v1/users/me/totp/verify`, **Then** kích hoạt 2FA; ghi audit `USER_TOTP_ENABLED`.
4. **Given** 2FA đã bật, **When** `POST /api/v1/auth/login` với password đúng, **Then** response trả `totpRequired: true` + `preAuthToken` ngắn hạn (không cấp access token đầy đủ) cho đến khi verify TOTP.
5. **Given** bước TOTP, **When** gửi mã hợp lệ hoặc backup code chưa dùng, **Then** cấp access + refresh token như login thường.
6. **Given** người dùng tắt 2FA, **When** `DELETE /api/v1/users/me/totp` với password + TOTP hiện tại, **Then** vô hiệu hóa và xóa secret đã mã hóa.
7. **Given** admin MFA (`AdminTotpSecret`), **When** triển khai, **Then** bảng/endpoint **riêng** — không tái sử dụng entity admin.
8. **Given** nhập sai TOTP >5 lần/15 phút, **When** lock tạm, **Then** thông báo tiếng Việt + audit `USER_TOTP_VERIFY_FAILED`.

## Tasks / Subtasks

- [ ] Task 1 — DB & entity (AC: #2, #6, #7)
  - [ ] Migration `user_totp_secrets` (pattern `V021__create_admin_totp_secrets_table.sql`)
  - [ ] Entity `UserTotpSecret` + repository
  - [ ] Backup codes hashed (bcrypt hoặc SHA-256 + salt) — tham chiếu admin migration `V022`
- [ ] Task 2 — Crypto service reuse (AC: #7)
  - [ ] Tái sử dụng `TotpSecretCryptoService` + env `USER_TOTP_ENCRYPTION_KEY` (hoặc dùng chung key với doc rõ ràng)
  - [ ] Document trong `docs/environment-reference.md`
- [ ] Task 3 — API endpoints (AC: #2–#6, #8)
  - [ ] `UserTotpController` under `/users/me/totp/*`
  - [ ] Đồng bộ `ApiRoutes.java`, `api.ts`, `API_ROUTES`
  - [ ] SecurityConfig: setup/verify cần JWT; login step public với preAuthToken
- [ ] Task 4 — Auth flow (AC: #4, #5, #8)
  - [ ] Sửa `AuthService.login` phân nhánh user TOTP
  - [ ] Endpoint `POST /api/v1/auth/totp/verify` (user, không trùng admin path)
  - [ ] Pre-auth token TTL ≤5 phút, single-use
- [ ] Task 5 — Web UI (AC: #1)
  - [ ] Trang `apps/web/src/app/(dashboard)/settings/security/page.tsx`
  - [ ] Thêm mục nav trong `SettingsAccountNav` / settings hub
  - [ ] Cập nhật `/settings/profile`: link "Quản lý 2FA" → `/settings/security`, xóa badge "Sắp có"
  - [ ] Login page: bước nhập mã TOTP khi `totpRequired`
- [ ] Task 6 — Tests (AC: #1–#8)
  - [ ] `UserTotpServiceTest`, `AuthServiceTotpTest`
  - [ ] Web smoke: setup flow mock API

## Dev Notes

### Trạng thái hiện tại

- Admin MFA **đã có**: `AdminTotpSecret`, `ADMIN_AUTH_TOTP_SETUP`, `ADMIN_AUTH_TOTP_VERIFY`.
- User UI **stub** tại settings profile:

```527:549:apps/web/src/app/(dashboard)/settings/profile/page.tsx
              <div
                className="flex items-center justify-between py-2"
                aria-describedby="profile-2fa-coming-soon-hint"
              >
                ...
                    Xác thực hai yếu tố (2FA)
                ...
                  Sắp có
```

- `pae-9` ghi rõ: user 2FA Phase 2 — story này **kích hoạt** scope đó.

### Pattern tái sử dụng (admin)

| Admin (có sẵn) | User (mới) |
|----------------|------------|
| `AdminTotpSecret` | `UserTotpSecret` |
| `AdminAuthService` TOTP branch | `UserTotpService` |
| `ApiRoutes.ADMIN_AUTH_TOTP_*` | `ApiRoutes.USERS_ME_TOTP_*` |
| Audit `ADMIN_TOTP_*` | Audit `USER_TOTP_*` |

**Không copy-paste** logic TOTP generation — extract shared `TotpService` nếu >30 dòng trùng (optional trong story, ưu tiên ship).

### Login response contract (mở rộng)

```typescript
// Khi 2FA bật, sau password OK:
{
  data: {
    totpRequired: true,
    preAuthToken: string,
    expiresInSeconds: 300
  }
}

// Sau POST /auth/totp/verify:
{
  data: {
    accessToken, refreshToken, user: { ... }
  }
}
```

### Security

- Secret TOTP **không** lưu plaintext — dùng `TotpSecretCryptoService`.
- Backup codes: hiển thị **một lần** khi setup; lưu hash.
- Không log mã TOTP.
- Rate limit verify endpoint (reuse `remaining-2-4` semantics nếu có filter chung).

### File structure (dự kiến)

**Backend:**
- `entity/UserTotpSecret.java`
- `service/UserTotpService.java`
- `controller/UserTotpController.java`
- `db/migration/V0xx__create_user_totp_secrets.sql`
- Sửa: `AuthService.java`, `SecurityConfig.java`, `ApiRoutes.java`

**Frontend:**
- `settings/security/page.tsx`
- Sửa: `login` flow, `settings/profile/page.tsx`, `SettingsAccountNav`

**Shared:**
- Zod schemas cho verify request nếu cần

### Testing

- `./gradlew test --tests '*UserTotp*' '*Auth*Totp*'`
- Manual: bật 2FA → logout → login 2 bước → dùng backup code

### References

- [Source: apps/api/.../entity/AdminTotpSecret.java]
- [Source: apps/api/.../service/TotpSecretCryptoService.java]
- [Source: _bmad-output/implementation-artifacts/public-account-experience/pae-9-profile-settings-stub-cleanup.md]
- [Source: _bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md#82-user-2fa-setup]
- [Source: docs/environment-reference.md — ADMIN_TOTP_ENCRYPTION_KEY pattern]

## Dev Agent Record

### Agent Model Used

(pending)

### Completion Notes List

### File List
