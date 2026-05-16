# Story 4.1: Product Event Instrumentation For Admin Analytics

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / admin analytics  
**Area:** Product events, queryable analytics store, upload/OCR instrumentation  
**Priority:** P1

## Story

As an admin,  
I want product events recorded in a queryable store,  
so that analytics charts reflect real usage.

## Acceptance Criteria

1. Events exist for `USER_REGISTERED`, `UPLOAD_STARTED`, `UPLOAD_CONFIRMED`, `OCR_COMPLETED`, and `OCR_FAILED`.
2. Events include required dimensions: user/profile/record/file type/provider/confidence/failure reason where applicable.
3. Events are queryable without scanning raw logs.
4. Event writes do not block user-critical flow unnecessarily and have failure telemetry.

## Tasks / Subtasks

- [ ] Task 1 - Define product event schema and retention assumptions (AC: #1, #2, #3)
- [ ] Task 2 - Persist event records in queryable storage (AC: #3)
- [ ] Task 3 - Instrument registration, upload, and OCR lifecycle paths (AC: #1, #2)
- [ ] Task 4 - Add non-blocking write/error telemetry behavior (AC: #4)
- [ ] Task 5 - Add tests for emitted events and queryability (AC: #1-#4)

## Dev Notes

### Implementation Guardrails

- Do not derive analytics from raw application logs.
- Avoid blocking user-critical upload/OCR flow on analytics write failure.
- Coordinate event names/dimensions with existing admin analytics stories 8.1-8.3.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/entity/*Event*`
- `apps/api/src/main/java/com/healthlens/api/repository/*Event*`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/Ocr*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 4.1
- `_bmad-output/planning-artifacts/review-source/production-review/epic-8-analytics-spec.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
