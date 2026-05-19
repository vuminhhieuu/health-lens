# Story 2.5: Refresh Token Rotation Race Hardening

Status: done

## Execution Scope

**Phase:** Remaining production review / session security  
**Area:** Refresh token rotation, session family invalidation, audit, concurrency tests  
**Priority:** P0

## Story

As a user with an active session,  
I want refresh token rotation to be race-safe,  
so that stolen or concurrently reused tokens cannot silently preserve access.

## Acceptance Criteria

1. Concurrent refresh requests cannot both mint valid sessions from the same old token.
2. Reuse of an already-rotated token is detected and invalidates the affected session family according to policy.
3. Token reuse events are audit logged without raw token.
4. Tests cover normal rotation, concurrent rotation, stolen-token replay, and logout.

## Tasks / Subtasks

- [x] Task 1 - Audit current refresh token persistence and rotation path (AC: #1, #2)
- [x] Task 2 - Add atomic rotation/reuse detection (AC: #1, #2)
- [x] Task 3 - Implement session-family invalidation policy (AC: #2)
- [x] Task 4 - Add non-sensitive audit events (AC: #3)
- [x] Task 5 - Add concurrency and replay tests (AC: #1-#4)

## Dev Notes

### Implementation Guardrails

- Rotation must be enforced server-side with transactional or atomic database behavior.
- Do not store or log raw refresh tokens; hash/token-id patterns should be used.
- Coordinate with admin/session storage hardening from core improvements if implemented in parallel.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/security/*`
- `apps/api/src/main/java/com/healthlens/api/repository/*Token*`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 2.5
- `_bmad-output/planning-artifacts/review-source/production-review/p0-gates-checklist.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-05-19T20:54:43+07:00 - Started story and moved sprint status to in-progress.
- 2026-05-19T20:56:00+07:00 - Audited existing refresh path: active token lookup followed by entity mutation was not atomic under concurrent refresh.
- 2026-05-19T21:05:00+07:00 - Added red tests for atomic rotation, replay detection, family invalidation, and non-sensitive audit metadata.
- 2026-05-19T21:08:39+07:00 - Targeted validation passed: `JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home ./gradlew test --tests com.healthlens.api.service.AuthServiceTest --tests com.healthlens.api.service.AuthServiceIntegrationTest`.
- 2026-05-19T21:09:45+07:00 - Full API regression passed: `JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home ./gradlew test`.

### Implementation Plan

- Add session family tracking to refresh token storage with a Flyway migration.
- Replace read-then-save rotation with an atomic repository update guarded by `revokedAt IS NULL` and `expiresAt > now`.
- Treat already-rotated token reuse and atomic update losers as replay, revoke the affected session family, and audit without raw token values.
- Cover normal rotation, race loser, stolen-token replay, and existing logout behavior in service/integration tests.

### Completion Notes List

- Refresh token rotation now uses an atomic DB update before minting a replacement token.
- Concurrent refresh losers and replay of already-rotated tokens revoke the affected session family.
- Refresh token replay audit uses `REFRESH_TOKEN_REUSE_FAILED` with reason, session family id, and refresh token id only; raw refresh tokens are not recorded.
- Targeted AuthService tests and the full API test suite passed.

### File List

- `apps/api/src/main/java/com/healthlens/api/audit/AuditActions.java`
- `apps/api/src/main/java/com/healthlens/api/entity/RefreshToken.java`
- `apps/api/src/main/java/com/healthlens/api/repository/RefreshTokenRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminAuditLogService.java`
- `apps/api/src/main/resources/db/migration/V041__refresh_token_session_family.sql`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceIntegrationTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java`
- `_bmad-output/implementation-artifacts/remaining-production-review/epic-2-auth-token-email-link-hardening/2-5-refresh-token-rotation-race-hardening.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
