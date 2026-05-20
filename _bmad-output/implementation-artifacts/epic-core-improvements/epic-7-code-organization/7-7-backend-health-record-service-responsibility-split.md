# Story 7.7: Backend Health Record Service Responsibility Split

Status: proposed

## Execution Scope

**Area:** Backend health-record domain organization, service responsibility split  
**Priority:** P2

## Story

As a backend developer, I want `HealthRecordService` responsibilities split into focused collaborators, so that upload, access, mapping, OCR coordination, and lifecycle changes can evolve with lower regression risk.

## Acceptance Criteria

1. **Given** health-record upload, review, sharing-aware access, and delete flows run, **When** the refactor is complete, **Then** external behavior remains unchanged.
2. **Given** `HealthRecordService` is reviewed, **When** code is inspected, **Then** access-policy logic, response assembly, and OCR-publication or state-transition concerns are extracted into focused collaborators.
3. **Given** backend tests run, **When** the story is completed, **Then** the health-record service regression surface remains covered.

## Tasks / Subtasks

- [ ] Extract health-record access policy or helper logic.
- [ ] Extract upload reservation and related validation/publisher logic where it is safe.
- [ ] Extract history/detail mapping or assembler code from the main service.
- [ ] Keep audit behavior, OCR callbacks, and public API behavior unchanged.
- [ ] Add focused tests around extracted collaborators if current coverage is too coarse.

## Dev Notes

- Do not combine this with `7-1`; that would make the refactor too risky.
- Treat this as behavior-preserving service decomposition, not feature work.
- If needed during implementation, split this story later into access/upload and mapping/state-transition follow-ups.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/`
- `apps/api/src/test/java/com/healthlens/api/service/`

## References

- `epic-7-issues-and-proposed-stories.md`
- `code-organization-assessment.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
