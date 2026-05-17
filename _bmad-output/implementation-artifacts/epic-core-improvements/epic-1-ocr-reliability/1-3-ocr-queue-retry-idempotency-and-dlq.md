# Story 1.3: OCR Queue Retry, Idempotency And DLQ

Status: done

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

- [x] Task 1 - Add OCR job state model (AC: #1-#5)
  - [x] Define states: `queued`, `processing`, `succeeded`, `failed_retryable`, `failed_terminal`, `dead_lettered`.
  - [x] Persist attempt count, last failure reason, next retry time, idempotency key.
  - [x] Ensure state transitions are explicit and tested.
- [x] Task 2 - Implement idempotency (AC: #1, #4)
  - [x] Use key based on `recordId + jobId + fileKey/version`.
  - [x] Avoid duplicate metric inserts or overwrites when job replays.
  - [x] Prevent replay from modifying user-confirmed records unless explicitly allowed.
- [x] Task 3 - Implement retry/backoff (AC: #2)
  - [x] Classify retryable vs terminal failures.
  - [x] Add max attempts and backoff config.
  - [x] Expose attempt metrics/logs.
- [x] Task 4 - Implement DLQ (AC: #3)
  - [x] Create DLQ stream/table or both.
  - [x] Store sanitized payload, failure category, attempts, correlation id.
  - [x] Add operator-visible logging/metrics.
- [x] Task 5 - Ack ordering and transaction safety (AC: #1)
  - [x] Ack only after durable DB state is persisted.
  - [x] Ensure ack failure after DB success is safe through idempotency.
- [x] Task 6 - Tests (AC: #1-#5)
  - [x] DB fail before ack.
  - [x] Ack fail after DB success.
  - [x] Retryable timeout.
  - [x] Terminal invalid file.
  - [x] DLQ after max attempts.
  - [x] Duplicate delivery replay.

### Review Findings

- [x] [Review][Patch] Pending Redis messages are never recovered, so AC #1 retry does not resume after unacked failures [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java:69]
- [x] [Review][Patch] OCR event is published to Redis inside `confirmUpload` before the DB transaction commits [apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java:201]
- [x] [Review][Patch] Post-provider failures can leave jobs stuck in `PROCESSING` with no retry/DLQ state [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java:184]
- [x] [Review][Patch] Health-record completion and OCR job success state are not committed atomically [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java:210]
- [x] [Review][Patch] `FAILED_TERMINAL` replay is not treated as terminal and can overwrite confirmed manual recovery [apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java:70]
- [x] [Review][Patch] `startAttempt` find-or-create is not concurrency-safe for duplicate deliveries [apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java:61]
- [x] [Review][Patch] Retry dispatcher can enqueue duplicate retry messages if Redis add succeeds but DB save fails [apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java:154]
- [x] [Review][Patch] Final-attempt DLQ can be skipped if UI-facing failure persistence throws first [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java:215]
- [x] [Review][Patch] Idempotency key omits the required file version/object generation component [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java:268]
- [x] [Review][Patch] Malformed payloads fail before durable job/DLQ state exists [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java:106]
- [x] [Review][Patch] Payload sanitization uses a URL blacklist instead of a safe-field allowlist [apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java:200]

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

GPT-5 Codex

### Debug Log References

- 2026-05-17T12:26:49+07:00 - Loaded story, BMad config, sprint status, and current OCR consumer/service context.
- 2026-05-17T12:39:00+07:00 - Targeted OCR consumer/state tests passed after adding durable state, retry, DLQ, and idempotency coverage.
- 2026-05-17T12:52:13+07:00 - Full backend `./gradlew --no-daemon test` passed after final DLQ ordering adjustment.
- 2026-05-17T13:33:09+07:00 - Code review findings batch-applied; full backend `./gradlew --no-daemon test` passed.

### Completion Notes List

- Added durable OCR job execution state with explicit queued/processing/succeeded/retryable/terminal/DLQ states.
- Changed Redis consumer ack ordering so broad processing exceptions leave messages unacked; retryable provider failures are persisted with backoff before ack.
- Added DB-backed retry dispatcher and DLQ table with sanitized payload, attempts, failure category, and correlation ID.
- Added idempotency key handling and duplicate terminal delivery skip; confirmed records are not overwritten by OCR replay.
- Preserved UI failure reason flow via structured `raw_ocr_result.failureReason` on terminal/final failures.

### File List

- `apps/api/src/main/java/com/healthlens/api/entity/OcrJobState.java`
- `apps/api/src/main/java/com/healthlens/api/entity/OcrJobExecution.java`
- `apps/api/src/main/java/com/healthlens/api/entity/OcrDeadLetter.java`
- `apps/api/src/main/java/com/healthlens/api/repository/OcrJobExecutionRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/OcrDeadLetterRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/resources/db/migration/V032__create_ocr_job_state_and_dlq.sql`
- `apps/api/src/test/java/com/healthlens/api/service/OcrJobConsumerTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrJobStateServiceTest.java`

### Change Log

- 2026-05-17T12:46:41+07:00 - Implemented OCR queue retry/idempotency/DLQ state machine and moved story to review.
- 2026-05-17T13:33:09+07:00 - Resolved code review findings and moved story to done.
