# Story 7.11: Introduce Application Stream/Event Boundary

Status: done

## Execution Scope

**Area:** Backend event publishing boundary, stream abstraction, source organization  
**Priority:** P1/P2

## Story

As a backend developer, I want a small application event boundary around stream publishing and consumer support code, so that Redis Stream usage is consistent and no longer duplicated across domain services.

## Acceptance Criteria

1. **Given** stream publishing occurs for OCR or email flows, **When** the story is complete, **Then** domain services publish through a focused event boundary instead of direct ad hoc stream calls.
2. **Given** event payloads and stream names are reviewed, **When** developers inspect the source tree, **Then** those concerns live under an explicit event package structure.
3. **Given** existing OCR and email flows run, **When** tests execute, **Then** behavior remains unchanged after the publishing boundary is introduced.

## Tasks / Subtasks

- [x] Introduce a small event package structure for stream names, payload records, and publisher interfaces.
- [x] Move direct `redisTemplate.opsForStream().add(...)` usage behind the chosen boundary where appropriate.
- [x] Extract shared consumer bootstrap or support code only where duplication is real.
- [x] Keep the abstraction small and aligned to current OCR and email use cases.
- [x] Preserve retry, pending, and DLQ behavior already working in OCR.

### Review Findings

- [x] [Review][Patch] Email consumer no longer reads new events when pending events exist [apps/api/src/main/java/com/healthlens/api/events/RedisStreamConsumerSupport.java:66]
- [x] [Review][Defer] Consumer group bootstrap can skip first real event under startup race [apps/api/src/main/java/com/healthlens/api/events/RedisStreamConsumerSupport.java:39] — deferred, pre-existing
- [x] [Review][Defer] Retry enqueue publishes before durable queued state is saved [apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java:245] — deferred, pre-existing

## Dev Notes

- This is not a generic event framework story.
- Use only enough abstraction to centralize ownership and reduce duplication.
- Align package names and responsibilities with the architecture decision from `7-9`.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/dto/event/EmailEvent.java`
- `apps/api/src/main/java/com/healthlens/api/`

## References

- `epic-7-issues-and-proposed-stories.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- Red phase: `./gradlew test --tests com.healthlens.api.service.EmailEventPublisherTest --tests com.healthlens.api.events.ocr.OcrJobEventPublisherTest` failed because the new `events.*` boundary classes did not exist yet.
- Targeted validation: `./gradlew test --tests com.healthlens.api.service.EmailEventPublisherTest --tests com.healthlens.api.events.ocr.OcrJobEventPublisherTest --tests com.healthlens.api.service.HealthRecordServiceTest --tests com.healthlens.api.service.OcrJobStateServiceTest --tests com.healthlens.api.service.OcrJobConsumerTest --tests com.healthlens.api.service.EmailConsumerTest`.
- Full validation: `./gradlew test`.
- Boundary validation: `rg -n "opsForStream\(\)\.add|streamOps\.add\(" apps/api/src/main/java/com/healthlens/api` confirms raw Redis Stream adds are centralized in `events/RedisApplicationStreamPublisher`.
- Static diff validation: `git diff --check`.
- Code review patch validation: `./gradlew test --tests com.healthlens.api.events.RedisStreamConsumerSupportTest --tests com.healthlens.api.service.EmailConsumerTest --tests com.healthlens.api.service.OcrJobConsumerTest`.
- Code review full validation: `./gradlew test`.

### Completion Notes List

- Added `events.*`, `events.email.*`, and `events.ocr.*` package boundaries for stream names, stream publisher abstraction, typed email/OCR payloads, and event-specific publisher interfaces/implementations.
- Moved OCR upload and OCR retry stream publishing behind `OcrJobEventPublisher`, preserving after-commit upload publishing and immediate retry re-enqueue semantics.
- Moved email event payload/publisher ownership from `dto.event` and `service` into `events.email`, while preserving email publish failure audit behavior from Story 7.10.
- Extracted duplicated consumer bootstrap/read support into `RedisStreamConsumerSupport`; email and OCR consumers now reuse it for group setup and pending-then-new reads.
- Preserved OCR retry, pending entry handling, consumer failure DLQ, malformed payload DLQ, and email invalid-event DLQ behavior through targeted and full API tests.
- Resolved code review finding by preserving pending-plus-new stream reads when capacity remains, preventing pending email entries from starving newer events in the same poll.

### File List

- apps/api/src/main/java/com/healthlens/api/events/ApplicationStreamNames.java
- apps/api/src/main/java/com/healthlens/api/events/ApplicationStreamPublisher.java
- apps/api/src/main/java/com/healthlens/api/events/RedisApplicationStreamPublisher.java
- apps/api/src/main/java/com/healthlens/api/events/RedisStreamConsumerSupport.java
- apps/api/src/main/java/com/healthlens/api/events/email/EmailEvent.java
- apps/api/src/main/java/com/healthlens/api/events/email/EmailEventPublisher.java
- apps/api/src/main/java/com/healthlens/api/events/email/RedisEmailEventPublisher.java
- apps/api/src/main/java/com/healthlens/api/events/ocr/OcrJobEvent.java
- apps/api/src/main/java/com/healthlens/api/events/ocr/OcrJobEventPublisher.java
- apps/api/src/main/java/com/healthlens/api/events/ocr/RedisOcrJobEventPublisher.java
- apps/api/src/main/java/com/healthlens/api/service/AuthService.java
- apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java
- apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java
- apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordShareService.java
- apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java
- apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java
- apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java
- apps/api/src/main/java/com/healthlens/api/dto/event/EmailEvent.java
- apps/api/src/main/java/com/healthlens/api/service/EmailEventPublisher.java
- apps/api/src/test/java/com/healthlens/api/events/ocr/OcrJobEventPublisherTest.java
- apps/api/src/test/java/com/healthlens/api/events/RedisStreamConsumerSupportTest.java
- apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/DataDeletionServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/EmailConsumerTest.java
- apps/api/src/test/java/com/healthlens/api/service/EmailEventPublisherTest.java
- apps/api/src/test/java/com/healthlens/api/service/FollowUpReminderServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/OcrJobConsumerTest.java
- apps/api/src/test/java/com/healthlens/api/service/OcrJobStateServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/ProfileShareServiceTest.java
- _bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/7-11-introduce-application-stream-event-boundary.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-05-20: Introduced application stream/event boundary for OCR and email events, centralized Redis Stream publishing, extracted shared consumer support, preserved retry/DLQ behavior, and moved story to review.
- 2026-05-20: Addressed code review finding for pending/new email stream starvation, added shared support regression tests, and moved story to done.
