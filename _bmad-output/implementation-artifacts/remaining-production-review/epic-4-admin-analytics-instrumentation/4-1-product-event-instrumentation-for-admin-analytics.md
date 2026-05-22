# Story 4.1: Product Event Instrumentation For Admin Analytics

Status: done

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

- [x] Task 1 - Define product event schema and retention assumptions (AC: #1, #2, #3)
- [x] Task 2 - Persist event records in queryable storage (AC: #3)
- [x] Task 3 - Instrument registration, upload, and OCR lifecycle paths (AC: #1, #2)
- [x] Task 4 - Add non-blocking write/error telemetry behavior (AC: #4)
- [x] Task 5 - Add tests for emitted events and queryability (AC: #1-#4)

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

Composer

### Debug Log References

- Extended `user_activity_events` (V047) with product dimensions; retention aligned to 90-day admin activity window (documented in migration).
- `REQUIRES_NEW` for register/upload-started; `MANDATORY` for confirm/OCR in caller transaction; failures logged, never propagated.

### Completion Notes List

- Added event types `USER_REGISTERED`, `UPLOAD_STARTED`, extended `UPLOAD_CONFIRMED`, `OCR_COMPLETED`, `OCR_FAILED` on `user_activity_events`.
- Instrumented `AuthService.register`, `HealthRecordService.createUploadUrl` / `confirmUpload`, `markOcrCompleted` / `markOcrFailed`, OCR consumer → `OcrJobStateService.completeSucceeded` with provider/confidence.
- Repository `countProductEvents` supports SQL aggregation for story 4.2.
- Unit tests cover emit paths; Postgres integration test gated on Docker availability.
- **Code review fixes:** `USER_REGISTERED` uses `MANDATORY` tx (FK-safe); upload events use profile owner `user_id`; `OCR_FAILED` deduped when already terminal; `OCR_COMPLETED` only from `processing`; `FailureReasonNormalizer` shared helper.

### File List

- `apps/api/src/main/resources/db/migration/V047__extend_user_activity_events_product_dimensions.sql`
- `apps/api/src/main/java/com/healthlens/api/activity/FailureReasonNormalizer.java`
- `apps/api/src/main/java/com/healthlens/api/activity/UserActivityEventType.java`
- `apps/api/src/main/java/com/healthlens/api/entity/UserActivityEvent.java`
- `apps/api/src/main/java/com/healthlens/api/repository/UserActivityEventRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/UserActivityService.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/correlation/CorrelationContext.java`
- `apps/api/src/test/java/com/healthlens/api/service/UserActivityServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/repository/UserActivityEventRepositoryIntegrationTest.java`
- `apps/api/src/test/java/com/healthlens/api/activity/FailureReasonNormalizerTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceIntegrationTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrJobStateServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrJobConsumerTest.java`
- `apps/api/src/test/java/com/healthlens/api/support/PostgresTestContainerBase.java`
