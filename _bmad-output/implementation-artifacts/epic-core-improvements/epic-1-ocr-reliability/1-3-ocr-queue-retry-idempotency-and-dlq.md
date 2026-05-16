# Story 1.3: OCR Queue Retry, Idempotency And DLQ

Status: ready-for-dev

## Execution Scope

**Phase:** Core feature improvement / OCR reliability  
**Area:** Backend API, Redis stream consumer, health record OCR state, retry/DLQ, idempotency  
**Priority:** P0

## Story

As an operations owner,  
I want OCR jobs to retry safely and dead-letter poison messages,  
so that jobs are not lost, duplicated, or silently acknowledged after failure.

## Context

Review findings identified reliability risks in the OCR queue flow: DB write and stream ack are not modeled as a durable state machine, failures can lose jobs or duplicate processing, and there is no clear DLQ/poison message handling. This is a production blocker for asynchronous OCR.

This story introduces durable OCR job state, retry policy, idempotency, and DLQ behavior.

## Acceptance Criteria

1. **Given** DB persistence fails after provider OCR succeeds, **When** the job handler exits, **Then** the Redis message is not acknowledged and retry can resume idempotently.
2. **Given** a provider timeout or transient provider error occurs, **When** attempts remain, **Then** the job is marked `failed_retryable` and scheduled with backoff.
3. **Given** a poison message exceeds max attempts, **When** the final attempt fails, **Then** it is moved to DLQ with failure reason, attempt count, and correlation ID.
4. **Given** the same job is delivered twice, **When** the idempotency key already succeeded, **Then** the second delivery does not duplicate metrics or overwrite confirmed user data.
5. **Given** OCR terminal failure occurs, **When** the user opens the review/upload flow, **Then** the UI can show retry/manual entry options based on persisted failure reason.

## Tasks / Subtasks

- [ ] Task 1 - Add OCR job state model (AC: #1-#5)
  - [ ] Define states: `queued`, `processing`, `succeeded`, `failed_retryable`, `failed_terminal`, `dead_lettered`.
  - [ ] Persist attempt count, last failure reason, next retry time, idempotency key.
  - [ ] Ensure state transitions are explicit and tested.
- [ ] Task 2 - Implement idempotency (AC: #1, #4)
  - [ ] Use key based on `recordId + jobId + fileKey/version`.
  - [ ] Avoid duplicate metric inserts or overwrites when job replays.
  - [ ] Prevent replay from modifying user-confirmed records unless explicitly allowed.
- [ ] Task 3 - Implement retry/backoff (AC: #2)
  - [ ] Classify retryable vs terminal failures.
  - [ ] Add max attempts and backoff config.
  - [ ] Expose attempt metrics/logs.
- [ ] Task 4 - Implement DLQ (AC: #3)
  - [ ] Create DLQ stream/table or both.
  - [ ] Store sanitized payload, failure category, attempts, correlation id.
  - [ ] Add operator-visible logging/metrics.
- [ ] Task 5 - Ack ordering and transaction safety (AC: #1)
  - [ ] Ack only after durable DB state is persisted.
  - [ ] Ensure ack failure after DB success is safe through idempotency.
- [ ] Task 6 - Tests (AC: #1-#5)
  - [ ] DB fail before ack.
  - [ ] Ack fail after DB success.
  - [ ] Retryable timeout.
  - [ ] Terminal invalid file.
  - [ ] DLQ after max attempts.
  - [ ] Duplicate delivery replay.

## Dev Notes

### Implementation Guardrails

- Do not ack messages in broad exception paths before durable state is persisted.
- Keep failure reasons structured; free-form exception text should be logged separately and sanitized.
- Avoid infinite retry loops. Poison messages must eventually DLQ.
- Do not overwrite manually corrected or confirmed health record metrics from replayed OCR jobs.
- Correlation ID should propagate through retries and DLQ records. If Story 4.1 is not implemented yet, store a local correlation field now.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/entity/HealthRecord.java`
- `apps/api/src/main/resources/db/migration/`
- `apps/api/src/test/java/com/healthlens/api/service/OcrJobConsumerTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`

### Dependencies

- Should follow or coordinate with Story 1.1 job payload changes.
- Benefits from Story 1.2 diagnostics but can define minimal failure codes first.

### References

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 4
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 1.3
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
