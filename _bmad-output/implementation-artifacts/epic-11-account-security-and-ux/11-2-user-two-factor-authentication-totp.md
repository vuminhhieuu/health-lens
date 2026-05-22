# Story 11.2: Xác thực hai yếu tố (2FA) cho người dùng

Status: done

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

- [x] Task 1 — DB & entity (AC: #2, #6, #7)
  - [x] Migration `user_totp_secrets` (pattern `V021__create_admin_totp_secrets_table.sql`)
  - [x] Entity `UserTotpSecret` + repository
  - [x] Backup codes hashed (bcrypt hoặc SHA-256 + salt) — tham chiếu admin migration `V022`
- [x] Task 2 — Crypto service reuse (AC: #7)
  - [x] Tái sử dụng `TotpSecretCryptoService` + env `USER_TOTP_ENCRYPTION_KEY` (hoặc dùng chung key với doc rõ ràng)
  - [x] Document trong `docs/environment-reference.md`
- [x] Task 3 — API endpoints (AC: #2–#6, #8)
  - [x] `UserTotpController` under `/users/me/totp/*`
  - [x] Đồng bộ `ApiRoutes.java`, `api.ts`, `API_ROUTES`
  - [x] SecurityConfig: setup/verify cần JWT; login step public với preAuthToken
- [x] Task 4 — Auth flow (AC: #4, #5, #8)
  - [x] Sửa `AuthService.login` phân nhánh user TOTP
  - [x] Endpoint `POST /api/v1/auth/totp/verify` (user, không trùng admin path)
  - [x] Pre-auth token TTL ≤5 phút, single-use
- [x] Task 5 — Web UI (AC: #1)
  - [x] Trang `apps/web/src/app/(dashboard)/settings/security/page.tsx`
  - [x] Thêm mục nav trong `SettingsAccountNav` / settings hub
  - [x] Cập nhật `/settings/profile`: link "Xác thực hai yếu tố (2FA)" → `/settings/security`, xóa badge "Sắp có"
  - [x] Login page: bước nhập mã TOTP khi `totpRequired`
- [x] Task 6 — Tests (AC: #1–#8)
  - [x] `UserTotpServiceTest`, `AuthServiceTotpTest`, `SecurityConfigCsrfTest` (totp/verify CSRF)
  - [x] Không thêm web smoke test (kiểm thử thủ công UI)

### Review Findings

- [x] [Review][Patch] `setup()` ghi đè secret khi 2FA đã verified — session bị chiếm có thể thay TOTP mà không cần disable [`UserTotpService.java:109`]
- [x] [Review][Patch] `preAuthToken` single-use không atomic — hai request song song có thể cùng pass `hasKey` trước khi `set` [`AuthService.java:312`]
- [x] [Review][Patch] `verifyLoginTotp` không kiểm tra lại `AccountStatus.PENDING_DELETION` sau bước mật khẩu [`AuthService.java:317`]
- [x] [Review][Patch] `loadStatus` nuốt mọi lỗi API → UI hiển thị 2FA tắt khi thực tế lỗi mạng/401 [`security/page.tsx:68`]
- [x] [Review][Patch] Task 6 — bỏ web smoke test; kiểm thử thủ công `/settings/security` và login TOTP
- [x] [Review][Defer] Thiếu audit `USER_TOTP_SETUP` khi bắt đầu setup (admin có `ADMIN_TOTP_SETUP`) — cải thiện observability, không chặn AC

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

Auto (Cursor)

### Completion Notes List

- Bảng `user_totp_secrets` tách biệt `admin_totp_secrets`; backup codes lưu bcrypt hash (JSON array).
- `UserTotpSecretCryptoService` + `USER_TOTP_ENCRYPTION_KEY` (fallback admin key trong dev).
- API: `GET/POST/DELETE /users/me/totp`, `POST /auth/totp/verify`; login trả `totpRequired` + `preAuthToken` (5 phút, single-use qua Redis).
- UI: `/settings/security` (layout đồng bộ settings), nav, profile link, login bước TOTP; admin thấy hướng dẫn MFA riêng (không setup user TOTP).
- Tests: `UserTotpServiceTest`, `AuthServiceTotpTest`, `SecurityConfigCsrfTest` — pass.
- Code review batch-fix: chặn setup khi đã verified, `setIfAbsent` pre-auth, re-check pending deletion, UI status error.
- Runtime fix: gửi CSRF cho `POST /auth/totp/verify`; hiển thị `detail` từ API trên login/security.

### File List

- apps/api/src/main/resources/db/migration/V048__create_user_totp_secrets_table.sql
- apps/api/src/main/java/com/healthlens/api/entity/UserTotpSecret.java
- apps/api/src/main/java/com/healthlens/api/repository/UserTotpSecretRepository.java
- apps/api/src/main/java/com/healthlens/api/service/UserTotpSecretCryptoService.java
- apps/api/src/main/java/com/healthlens/api/security/UserTotpRateLimiter.java
- apps/api/src/main/java/com/healthlens/api/service/UserTotpService.java
- apps/api/src/main/java/com/healthlens/api/controller/UserTotpController.java
- apps/api/src/main/java/com/healthlens/api/dto/request/UserTotpVerifyRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/request/UserTotpDisableRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/request/UserAuthTotpVerifyRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/response/UserTotpSetupResponse.java
- apps/api/src/main/java/com/healthlens/api/dto/response/UserTotpStatusResponse.java
- apps/api/src/main/java/com/healthlens/api/service/AuthService.java
- apps/api/src/main/java/com/healthlens/api/controller/AuthController.java
- apps/api/src/main/java/com/healthlens/api/util/JwtUtil.java
- apps/api/src/main/java/com/healthlens/api/security/JwtAuthenticationFilter.java
- apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java
- apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java
- apps/api/src/main/java/com/healthlens/api/audit/AuditActions.java
- apps/api/src/main/resources/application.yml
- apps/api/src/test/java/com/healthlens/api/service/UserTotpServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/AuthServiceTotpTest.java
- apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java
- apps/api/src/test/java/com/healthlens/api/controller/AuthControllerTest.java
- apps/api/src/test/java/com/healthlens/api/config/SecurityConfigCsrfTest.java
- packages/shared/constants/api.ts
- apps/web/src/lib/api/routes.ts
- apps/web/src/app/(dashboard)/settings/security/page.tsx
- apps/web/src/app/(dashboard)/settings/_components/SettingsAccountNav.tsx
- apps/web/src/app/(dashboard)/settings/profile/page.tsx
- apps/web/src/app/(auth)/login/page.tsx
- apps/web/src/lib/api/apiClient.ts
- apps/web/src/lib/i18n/messages.ts
- docs/environment-reference.md
- _bmad-output/implementation-artifacts/sprint-status.yaml
