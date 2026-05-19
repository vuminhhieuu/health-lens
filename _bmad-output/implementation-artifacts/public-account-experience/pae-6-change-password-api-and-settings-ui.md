# Story 6: Change Password API And Settings UI

Status: backlog

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

- [ ] Backend: `AuthController.changePassword`, service method, password encoder verify + update.
- [ ] DTO validation; no email enumeration in errors.
- [ ] Frontend page replaces stub from `pae-1`.
- [ ] Invalidate other sessions optional — document if out of scope.

## Dev Notes

- Forgot/reset password already exists — do not duplicate.
- REVIEW-FULL-v2 §8: change password missing.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `packages/shared/constants/api.ts`
- `apps/web/src/app/(dashboard)/settings/change-password/page.tsx`

## Dev Agent Record

### Agent Model Used

(pending)
