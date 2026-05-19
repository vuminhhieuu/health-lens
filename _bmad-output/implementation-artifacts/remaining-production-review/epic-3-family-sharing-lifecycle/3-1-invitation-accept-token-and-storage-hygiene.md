# Story 3.1: Invitation Accept Token And Storage Hygiene

Status: done

## Execution Scope

**Phase:** Remaining production review / family sharing correctness  
**Area:** Invitation accept page, token hygiene, storage fallback, accept endpoint  
**Priority:** P0/P1

## Story

As an invited family member,  
I want invitation acceptance to avoid token leaks and storage crashes,  
so that accepting access is safe across browsers.

## Acceptance Criteria

1. Invitation token is removed from URL/history as early as possible.
2. `sessionStorage` access is wrapped with safe fallback behavior.
3. Expired/invalid invitation state does not silently redirect to a confusing page.
4. Accept outcome type matches backend response contract.
5. Accept endpoint has rate limiting.

## Tasks / Subtasks

- [x] Task 1 - Harden token extraction and URL cleanup (AC: #1)
- [x] Task 2 - Wrap browser storage access safely (AC: #2)
- [x] Task 3 - Normalize invalid/expired invitation UI states (AC: #3)
- [x] Task 4 - Align frontend accept result type with backend contract (AC: #4)
- [x] Task 5 - Add accept endpoint rate limiting and tests (AC: #5)

## Dev Notes

### Implementation Guardrails

- Do not keep invitation tokens in durable browser history.
- `sessionStorage` may throw or be unavailable; handle that without breaking invite acceptance.
- Avoid redirects that hide the true invitation outcome.

### Likely Files

- `apps/web/src/app/(auth)/invitations/accept/page.tsx`
- `apps/api/src/main/java/com/healthlens/api/controller/FamilySharingController.java`
- `apps/api/src/main/java/com/healthlens/api/service/FamilySharingService.java`
- `apps/api/src/test/java/com/healthlens/api/*Sharing*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 3.1
- `_bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md`

## Dev Agent Record

### Agent Model Used

Composer

### Debug Log References

### Completion Notes List

- Profile accept page strips `token` from URL via `history.replaceState` before POST; token kept in closure/ref for API and login return flow.
- `sessionStorage` wrapped via `@/lib/browser/sessionStorage`; in-memory ref fallback prevents 401 redirect loops when storage unavailable.
- `outcome === "expired"` shows dedicated message instead of redirecting to `/profiles`.
- Response validated with `parseAcceptProfileInvitationResult` (Zod: `accepted` | `require-login` | `expired`).
- AC #5 already satisfied: `InvitationController` + `PublicEndpointRateLimiter` + controller tests.

### File List

- `apps/web/src/app/(auth)/invitations/accept/page.tsx`
- `apps/web/src/lib/browser/sessionStorage.ts`
- `apps/web/src/lib/browser/sessionStorage.test.ts`
- `apps/web/src/lib/sharing/acceptInvitationResult.ts`
- `apps/web/src/lib/sharing/acceptInvitationResult.test.ts`
- `apps/web/src/lib/i18n/messages.ts`
