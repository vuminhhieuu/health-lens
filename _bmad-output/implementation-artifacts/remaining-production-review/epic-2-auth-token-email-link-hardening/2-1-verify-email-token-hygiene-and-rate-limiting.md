# Story 2.1: Verify Email Token Hygiene And Rate Limiting

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / auth hardening  
**Area:** Verify email frontend, public auth API, token hygiene, rate limiting, audit  
**Priority:** P0/P1

## Story

As a user verifying email,  
I want the verification link to avoid leaking token state,  
so that account verification remains safe even through browser history and logs.

## Acceptance Criteria

1. Verification token is removed from URL immediately after extraction.
2. Referrer behavior prevents leaking token to third parties.
3. Verify-email endpoint has IP/email-based rate limiting.
4. User-facing messages do not reveal whether token exists, expired, or was already used.
5. Verify attempts are audit logged without raw token.

## Tasks / Subtasks

- [ ] Task 1 - Harden frontend token handling (AC: #1, #2)
- [ ] Task 2 - Add rate limiting to verify-email endpoint (AC: #3)
- [ ] Task 3 - Normalize error semantics and copy (AC: #4)
- [ ] Task 4 - Add audit events without raw token (AC: #5)
- [ ] Task 5 - Test token URL cleanup, 429, invalid/expired/used, and audit behavior (AC: #1-#5)

## Dev Notes

### Implementation Guardrails

- Remove token from browser URL/history before any third-party resource can observe it.
- Do not log raw tokens, even at debug level.
- Keep public error messages enumeration-safe.

### Likely Files

- `apps/web/src/app/(auth)/verify-email/page.tsx`
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/security/*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 2.1
- `_bmad-output/planning-artifacts/review-source/production-review/p0-gates-checklist.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
