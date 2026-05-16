# Story 2.3: Cancel Deletion Link And Token Flow Hardening

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / account deletion safety  
**Area:** Cancel deletion email link, frontend state mapping, backend token validation  
**Priority:** P0/P1

## Story

As a user cancelling account deletion,  
I want cancellation to be secure and accurately explained,  
so that I do not lose data because of URL leaks or client-side expiry bugs.

## Acceptance Criteria

1. Email and sensitive token values are not exposed in durable URLs, logs, or browser history beyond unavoidable one-time link constraints.
2. Backend remains source of truth for token expiry; client-side expiry never blocks a valid backend cancellation.
3. `409`, `401`, `403`, `429`, and `500` outcomes map to distinct frontend states.
4. Token normalization matches backend encoding behavior.
5. Cancellation flow has tests for happy path, expired token, replay, 409, 429, 500, missing token, loading, and double-submit.

## Tasks / Subtasks

- [ ] Task 1 - Audit current cancel-deletion URL and token handling (AC: #1, #4)
- [ ] Task 2 - Move expiry authority to backend result (AC: #2)
- [ ] Task 3 - Implement distinct frontend state mapping (AC: #3)
- [ ] Task 4 - Add double-submit and replay protection UX (AC: #3, #5)
- [ ] Task 5 - Add backend/frontend tests for listed outcomes (AC: #1-#5)

## Dev Notes

### Implementation Guardrails

- Never reject a token solely because the browser computed expiry locally.
- Remove sensitive query params from history as early as practical.
- Do not log full email or raw cancellation token.

### Likely Files

- `apps/web/src/app/(auth)/*cancel*`
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AccountDeletionService.java`
- `apps/api/src/test/java/com/healthlens/api/*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 2.3
- `_bmad-output/planning-artifacts/review-source/REVIEW-PRODUCTION-MASTER.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
