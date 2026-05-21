# Story 6: Change Password API And Settings UI

Status: done

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1 | **Depends on:** `pae-1`

## Story

As a signed-in user,  
I want to change my password from settings,  
so that I can rotate credentials without using the email reset flow.

## Acceptance Criteria

1. `POST /api/v1/auth/change-password` with `currentPassword`, `newPassword`; requires authenticated session.
2. Wrong current password → 400; weak password → validation error (match registration rules).
3. `/settings/change-password` form: RHF + Zod; `notify.success` / `notify.error`.
4. Profile settings removes `href="#"` for change password → real route.
5. Registered in `ApiRoutes.java`, `packages/shared/constants/api.ts` (constant exists — implement handler).
6. `AuthService` + controller tests; frontend validation test.

## Tasks / Subtasks

- [x] Backend: `AuthController.changePassword`, service method, password encoder verify + update.
- [x] DTO validation; no email enumeration in errors.
- [x] Frontend page replaces stub from `pae-1`.
- [x] Invalidate other sessions optional — document if out of scope.

### Review Findings

- [x] [Review][Patch] `WeakPasswordException` luôn trả `field: "password"` trong khi DTO là `newPassword` — đã thêm `field` trên exception + handler.
- [x] [Review][Patch] `POST /auth/change-password` nằm trong `AUTH_PATTERN` `permitAll()` — đã `authenticated()` riêng trước `AUTH_PATTERN`.
- [x] [Review][Patch] Không chặn `newPassword` trùng `currentPassword` — đã validate trong `AuthService`.
- [x] [Review][Patch] `extractUserId` dựa vào `UUID.fromString` fail với `anonymousUser` — đã reject `anonymousUser` rõ ràng.
- [x] [Review][Patch] UI gộp mọi lỗi 400 thành một thông báo chung — `changePasswordErrorMessage()` đọc `detail` / `errors[]`.
- [x] [Review][Defer] `AdminAuditLogService` chưa có nhãn hiển thị cho `CHANGE_PASSWORD` — deferred, ngoài AC story

## Dev Notes

- Forgot/reset password already exists — do not duplicate.
- REVIEW-FULL-v2 §8: change password missing.
- **Session invalidation:** On success, all refresh tokens for the user are revoked (same as reset-password). Other devices lose refresh capability until re-login.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `packages/shared/constants/api.ts`
- `apps/web/src/app/(dashboard)/settings/change-password/page.tsx`

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes

- `POST /api/v1/auth/change-password` with `ChangePasswordRequest`; JWT required; wrong current password → 400 (`IllegalArgumentException`); weak password → validation / `WeakPasswordException`.
- `AuthService.changePassword` updates hash, revokes refresh tokens, audits `CHANGE_PASSWORD`.
- Settings UI: RHF + `changePasswordSchema`, notify on success/error; profile link → `/settings/change-password`.
- Tests: `AuthServiceTest`, `AuthControllerTest`, `change-password.validation.test.ts` (web vitest passed). API Maven tests not run locally (mvn unavailable in agent environment).

### File List

- `apps/api/src/main/java/com/healthlens/api/dto/request/ChangePasswordRequest.java`
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditActions.java`
- `apps/api/src/test/java/com/healthlens/api/controller/AuthControllerTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java`
- `packages/shared/schemas/auth.ts`
- `packages/shared/schemas/index.ts`
- `apps/web/src/app/(dashboard)/settings/change-password/page.tsx`
- `apps/web/src/app/(dashboard)/settings/change-password/change-password.validation.test.ts`
- `apps/web/src/app/(dashboard)/settings/profile/page.tsx`

### Change Log

- 2026-05-21: Change password API, settings form, profile link, and tests.
