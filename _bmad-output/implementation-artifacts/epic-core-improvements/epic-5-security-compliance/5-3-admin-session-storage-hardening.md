# Story 5.3: Admin Session Storage Hardening

Status: ready-for-dev

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

- [ ] Choose HttpOnly cookie or server-backed session strategy for admin.
- [ ] Update admin login/logout/TOTP flow.
- [ ] Add CSRF protection for cookie-based admin requests.
- [ ] Update admin API client.
- [ ] Add tests that token is not exposed in storage.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
