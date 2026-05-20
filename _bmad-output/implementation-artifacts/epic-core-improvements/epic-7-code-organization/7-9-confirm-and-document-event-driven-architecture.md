# Story 7.9: Confirm And Document Event-Driven Architecture

Status: ready-for-dev

## Execution Scope

**Area:** Architecture confirmation, async task standards, event-driven boundaries  
**Priority:** P1

## Story

As a technical lead, I want the event-driven architecture rules explicitly confirmed and documented, so that future refactors and async features align to one agreed delivery model.

## Acceptance Criteria

1. **Given** the current OCR and email async flows are reviewed, **When** this story is complete, **Then** there is a documented rule for which post-commit side effects must use event-driven delivery and which may remain direct.
2. **Given** backend package boundaries are planned, **When** developers review the guidance, **Then** the intended roles of `events.*`, `ocr.*`, `ai.*`, and related domains are explicit.
3. **Given** later Epic 7 stories are prepared, **When** they reference this architecture decision, **Then** they no longer rely on implicit assumptions about Redis Streams or direct service calls.
4. **Given** reminder, audit, notification, and email flows are reviewed, **When** this story is completed, **Then** each async category has an explicit delivery semantic such as synchronous, after-commit direct, DB-claimed job, or stream-based event.
5. **Given** Story 7.1 and later backend refactors are reviewed, **When** developers inspect the decision record, **Then** package ownership for `events.*`, `ocr.*`, `ai.*`, and email/event publishing is concrete enough to avoid immediate rework.

## Tasks / Subtasks

- [ ] Confirm whether Redis Streams are the standard event bus for all durable async tasks or only selected flows.
- [ ] Confirm which email categories must move to event-driven delivery.
- [ ] Confirm whether after-commit stream publishing is sufficient or whether an outbox pattern is required later.
- [ ] Confirm explicit delivery semantics for OCR, email, reminders, audit, and notifications.
- [ ] Document the agreed package and ownership boundaries for OCR, email, and event publishing.
- [ ] Update Epic 7 guidance so later stories align with the decision.
- [ ] Produce a short decision table with rationale and non-goals.

## Dev Notes

- This is a decision and documentation story, not an implementation story.
- Use the existing review evidence first: `source-code-architecture-review.md`, `code-organization-assessment.md`, and current async code paths in `AuthService`, `HealthRecordService`, `OcrJobConsumer`, `OcrJobStateService`, `EmailConsumer`, and `FollowUpReminderService`.
- Non-goals: do not move packages, do not introduce a stream abstraction yet, and do not migrate all email paths during this story.
- Complete this before expanding `7-1` into a broader backend architecture refactor.
- Keep the outcome concrete enough that later stories can cite it as a guardrail.

## Likely Files

- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/source-code-architecture-review.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/code-organization-assessment.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/epic-7-issues-and-proposed-stories.md`
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java`
- `docs/`

## References

- `epic-7-issues-and-proposed-stories.md`
- `source-code-architecture-review.md`
- `code-organization-assessment.md`
- `_bmad-output/planning-artifacts/sprint-change-proposal-2026-05-20.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
