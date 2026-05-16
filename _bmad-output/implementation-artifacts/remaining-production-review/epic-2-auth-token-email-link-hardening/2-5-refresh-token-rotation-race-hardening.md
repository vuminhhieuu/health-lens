# Story 2.5: Refresh Token Rotation Race Hardening

Status: ready-for-dev

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

- [ ] Task 1 - Audit current refresh token persistence and rotation path (AC: #1, #2)
- [ ] Task 2 - Add atomic rotation/reuse detection (AC: #1, #2)
- [ ] Task 3 - Implement session-family invalidation policy (AC: #2)
- [ ] Task 4 - Add non-sensitive audit events (AC: #3)
- [ ] Task 5 - Add concurrency and replay tests (AC: #1-#4)

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
