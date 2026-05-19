# Story 2.1: Verify Email Token Hygiene And Rate Limiting

Status: done

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

- [x] Task 1 - Harden frontend token handling (AC: #1, #2)
- [x] Task 2 - Add rate limiting to verify-email endpoint (AC: #3)
- [x] Task 3 - Normalize error semantics and copy (AC: #4)
- [x] Task 4 - Add audit events without raw token (AC: #5)
- [x] Task 5 - Test token URL cleanup, 429, invalid/expired/used, and audit behavior (AC: #1-#5)

### Review Findings

- [x] [Review][Patch] Rate-limited verify attempts are not audit logged [apps/api/src/main/java/com/healthlens/api/service/AuthService.java:153]
- [x] [Review][Patch] Verify-email IP limiter trusts spoofable forwarding headers [apps/api/src/main/java/com/healthlens/api/controller/AuthController.java:113]
- [x] [Review][Patch] Used-token audit/test coverage is missing [apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java:144]

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

GPT-5 Codex

### Debug Log References

- `pnpm test` in `apps/web` - passed, with pre-existing lint warning in `src/app/admin/audit-log/page.tsx`
- `./gradlew test --tests com.healthlens.api.security.VerifyEmailRateLimiterTest --tests com.healthlens.api.service.AuthServiceTest --tests com.healthlens.api.controller.AuthControllerTest` - blocked because no Java Runtime is installed
- `/usr/libexec/java_home -V` - blocked because no Java Runtime is installed
- Code review follow-ups fixed in source and tests; backend validation still requires a Java Runtime.
- API validation accepted as passing per user instruction on 2026-05-19; story moved to done.
- Copilot follow-ups addressed: audit writes use a new transaction, verify-email rate limit uses consume-on-increment semantics and `RATE_LIMITED` response metadata, return URL values are constrained to internal paths, and backend regression tests were expanded.

### Completion Notes List

- Hardened verify-email frontend URL handling: token is extracted, `window.history.replaceState` removes it before the API request, and the route exports `referrer: "no-referrer"` metadata.
- Removed the temporary frontend verify-email unit test file per user instruction.
- Implemented backend verify-email hardening in code: IP/email Redis rate limiting, generic invalid/expired/used error message, and anonymous failure audit events without raw token values.
- Fixed review findings: rate-limited verify attempts now audit before rethrowing 429, verify-email IP key no longer trusts client-supplied forwarding headers, and used-token coverage was added.
- Story accepted as complete and moved to `done` per user instruction.
- Fixed Copilot findings: anonymous audit persistence now uses `REQUIRES_NEW`, verify-email rate limiting returns `RATE_LIMITED` instead of account-lock semantics, limiter tests include email bucket exhaustion, controller tests assert `remoteAddr` is used over forwarded headers, and UI copy no longer references a resend flow that does not exist.

### File List

- `apps/web/src/app/(auth)/verify-email/page.tsx`
- `apps/web/src/app/(auth)/verify-email/VerifyEmailClient.tsx`
- `apps/web/src/app/(auth)/login/page.tsx`
- `apps/web/src/app/(auth)/register/page.tsx`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditActions.java`
- `apps/api/src/main/java/com/healthlens/api/audit/UnifiedAuditLogWriter.java`
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/exception/ApiErrorCode.java`
- `apps/api/src/main/java/com/healthlens/api/exception/GlobalExceptionHandler.java`
- `apps/api/src/main/java/com/healthlens/api/exception/RateLimitExceededException.java`
- `apps/api/src/main/java/com/healthlens/api/security/VerifyEmailRateLimiter.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/test/java/com/healthlens/api/controller/AuthControllerTest.java`
- `apps/api/src/test/java/com/healthlens/api/security/VerifyEmailRateLimiterTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/remaining-production-review/epic-2-auth-token-email-link-hardening/2-1-verify-email-token-hygiene-and-rate-limiting.md`

### Change Log

- 2026-05-19: Implemented verify-email frontend token hygiene and backend rate-limit/audit/error hardening; web validation passed, backend validation blocked by missing Java Runtime.
- 2026-05-19: Marked story done and completed remaining tasks per user instruction.
