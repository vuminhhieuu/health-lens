# Story 5.3: Admin Session Storage Hardening

Status: done

## Execution Scope

**Area:** Admin auth, token storage, HttpOnly cookies/session, CSRF  
**Priority:** P0/P1

## Story

As an admin, I want admin authentication tokens protected from script-accessible storage, so that XSS impact is reduced.

## Acceptance Criteria

1. **Given** admin logs in successfully, **When** browser JavaScript inspects storage, **Then** admin access token is not available in `sessionStorage` or localStorage.
2. **Given** admin calls protected endpoint, **When** CSRF/session requirements are missing, **Then** request is rejected.
3. **Given** admin logs out, **When** session state is inspected, **Then** server/client session artifacts are invalidated.
4. **Given** TOTP setup flow succeeds, **When** tokens are issued, **Then** storage behavior follows hardened admin session policy.

## Tasks / Subtasks

- [x] Choose HttpOnly cookie or server-backed session strategy for admin.
- [x] Update admin login/logout/TOTP flow.
- [x] Add CSRF protection for cookie-based admin requests.
- [x] Update admin API client.
- [x] Add tests that token is not exposed in storage.

## Dev Notes

- This supersedes relevant session/token storage scope from old `epic-10/10-2-refresh-token-session-security.md`.
- Coordinate with Story 5.1 CSRF strategy.

## Likely Files

- `apps/web/src/app/admin/login/page.tsx`
- `apps/web/src/lib/api/adminApiClient.ts`
- `apps/api/src/main/java/com/healthlens/api/controller/AdminAuthController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AdminAuthService.java`

## References

- Old source: `_bmad-output/implementation-artifacts/epic-10/10-2-refresh-token-session-security.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 5.3

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `./gradlew test --tests com.healthlens.api.service.AdminAuthServiceTest` (pass)
- `./gradlew test --tests com.healthlens.api.controller.AdminAuthControllerTest --tests com.healthlens.api.service.AdminAuthServiceTest` (pass)
- `pnpm --filter web build` (không chạy được do thiếu `node_modules` trong `apps/web`)

### Completion Notes List

- Chuyển admin access token sang cookie `HttpOnly + SameSite=Strict + Path=/api/v1/admin` để loại bỏ truy cập từ JavaScript storage.
- Bổ sung endpoint `POST /api/v1/admin/auth/logout` và `GET /api/v1/admin/auth/session` để quản lý phiên admin theo cookie.
- Cập nhật `JwtAuthenticationFilter` để nhận token từ cookie admin (ưu tiên Authorization header nếu có).
- Mở rộng CSRF matcher: bảo vệ các request mutating cho admin khi có admin auth cookie.
- Cập nhật frontend admin flow (`login`, `totp setup/verify`, `layout`, `approvals`) để bỏ phụ thuộc `sessionStorage`.
- Bổ sung test `logout_blacklistAdminToken` cho `AdminAuthService`.
- Sau code review: loại bỏ `accessToken` khỏi response body của admin auth để tránh lộ JWT cho JavaScript, nhưng vẫn dùng nội bộ để set `HttpOnly` cookie.
- Sau code review: siết `GET /admin/auth/session` và `POST /admin/auth/logout` về quyền admin, đồng thời trả thêm `email` và `totpVerified` để frontend dùng làm nguồn sự thật cho admin session.
- Sau code review: frontend admin layout chặn phiên pre-TOTP, approvals page chuyển sang đọc identity từ `/admin/auth/session` thay vì `/users/me`.
- Bổ sung regression test `AdminAuthControllerTest` cho login/session/logout + CSRF.

### File List

- `apps/api/src/main/java/com/healthlens/api/constants/SecurityConstants.java`
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/security/JwtAuthenticationFilter.java`
- `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`
- `apps/api/src/main/java/com/healthlens/api/controller/AdminAuthController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AdminAuthService.java`
- `apps/api/src/test/java/com/healthlens/api/service/AdminAuthServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/controller/AdminAuthControllerTest.java`
- `packages/shared/constants/api.ts`
- `apps/web/src/lib/api/adminApiClient.ts`
- `apps/web/src/app/admin/login/page.tsx`
- `apps/web/src/app/admin/layout.tsx`
- `apps/web/src/app/admin/reference-data/approvals/page.tsx`

## Change Log

- 2026-05-19: Implemented admin session hardening with HttpOnly cookie storage, CSRF enforcement for admin mutating routes, admin logout/session endpoints, frontend cookie-based admin flow migration, and admin auth logout blacklist test.
- 2026-05-19: Addressed code review findings by removing token exposure from admin auth responses, tightening admin session/logout authorization and CSRF behavior, wiring frontend admin identity to `/admin/auth/session`, and adding controller regression tests.
