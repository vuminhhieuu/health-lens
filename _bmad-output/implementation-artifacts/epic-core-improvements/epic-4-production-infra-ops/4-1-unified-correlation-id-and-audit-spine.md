# Story 4.1: Unified Correlation ID And Audit Spine

Status: ready-for-dev

## Execution Scope

**Area:** Backend/API, async jobs, audit events, correlation IDs  
**Priority:** P0

## Story

As a compliance and operations owner, I want every user action and background AI/OCR job correlated end-to-end, so that incidents and privacy audits can be reconstructed reliably.

## Acceptance Criteria

1. **Given** request enters API, **When** correlation id exists or not, **Then** system propagates or generates one and echoes it in response metadata.
2. **Given** admin mutation, consent change, share lifecycle, delete request, OCR event, LLM call, or RAG retrieval occurs, **When** event is recorded, **Then** audit row includes actor, action, resource, outcome, correlation id, trace/request id, and safe metadata.
3. **Given** audit/compliance event is written, **When** logs are emitted, **Then** raw OCR text, presigned URL, raw token, auth header, and email/token query params are excluded.
4. **Given** async queue/job processing occurs, **When** downstream logs/events are emitted, **Then** original correlation id is carried through.
5. **Given** retention class is due, **When** purge/anonymize job runs, **Then** retention policy is applied while preserving required evidence.

## Tasks / Subtasks

- [ ] Add correlation filter and MDC propagation.
- [ ] Add canonical audit event schema/store.
- [ ] Add audit publishers for admin/share/delete/consent/OCR/LLM/RAG paths.
- [ ] Add redaction utilities and tests.
- [ ] Add retention/purge handling.

## Dev Notes

- This file supersedes old story `epic-7/7-6-unified-audit-spine-with-correlation-ids.md`.
- Keep compliance audit separate from product analytics events.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/aspect/AuditableAspect.java`
- `apps/api/src/main/java/com/healthlens/api/service/*`
- `apps/api/src/main/resources/db/migration/`

## References

- Old canonical source: `_bmad-output/implementation-artifacts/epic-7/7-6-unified-audit-spine-with-correlation-ids.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 4.1

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
