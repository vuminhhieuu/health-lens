# Story 2.4: Public Auth Endpoint Rate Limit And Error Semantics

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / public endpoint hardening  
**Area:** Register, verify email, forgot password, invite accept, cancel deletion, OCR trigger  
**Priority:** P0/P1

## Story

As a security owner,  
I want public auth endpoints to be rate-limited and to avoid leaking sensitive state,  
so that brute-force and enumeration risks are reduced.

## Acceptance Criteria

1. Register, verify-email, forgot-password, invite accept, cancel deletion, and OCR trigger endpoints have documented rate limits.
2. Rate-limit responses are consistently mapped in the web UI.
3. Forgot-password does not silently swallow email provider failures without telemetry.
4. Register handles `429` and invitation token propagation correctly.
5. Pending-deletion auth text matches backend error semantics.

## Tasks / Subtasks

- [ ] Task 1 - Inventory public endpoints and existing rate limit coverage (AC: #1)
- [ ] Task 2 - Apply documented rate limit policies (AC: #1)
- [ ] Task 3 - Normalize frontend handling of `429` and sensitive errors (AC: #2, #4, #5)
- [ ] Task 4 - Add telemetry for email provider failures (AC: #3)
- [ ] Task 5 - Add endpoint tests and UI verification for rate-limit states (AC: #1-#5)

## Dev Notes

### Implementation Guardrails

- Use the existing security/filtering approach if present; avoid one-off endpoint logic.
- Public responses should be specific enough for UX but not reveal whether an account/token exists.
- OCR trigger rate limiting must not break authenticated retry flows without clear messaging.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`
- `apps/api/src/main/java/com/healthlens/api/security/*`
- `apps/web/src/app/(auth)/*`
- `apps/web/src/lib/api.ts`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 2.4
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 5.1

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
