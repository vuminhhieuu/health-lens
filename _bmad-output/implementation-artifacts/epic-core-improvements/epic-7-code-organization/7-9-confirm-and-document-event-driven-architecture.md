# Story 7.9: Confirm And Document Event-Driven Architecture

Status: done

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

- [x] Confirm whether Redis Streams are the standard event bus for all durable async tasks or only selected flows.
- [x] Confirm which email categories must move to event-driven delivery.
- [x] Confirm whether after-commit stream publishing is sufficient or whether an outbox pattern is required later.
- [x] Confirm explicit delivery semantics for OCR, email, reminders, audit, and notifications.
- [x] Document the agreed package and ownership boundaries for OCR, email, and event publishing.
- [x] Update Epic 7 guidance so later stories align with the decision.
- [x] Produce a short decision table with rationale and non-goals.

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

GPT-5

### Debug Log References

- Reviewed existing Epic 7 architecture evidence and current async code paths for OCR, email, reminders, and audit.
- Validation run: `rg -n "Event-Driven Architecture|event-driven-architecture|Async And Event Delivery|hybrid async model|core-7-9-confirm" docs _bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization _bmad-output/implementation-artifacts/sprint-status.yaml`.
- Validation run: `git diff --check`.

### Completion Notes List

- Accepted a hybrid async model: Redis Streams for durable cross-boundary events, DB-claimed jobs for scheduled row-owned work, and synchronous direct calls for command invariants and command audit writes.
- Documented email categories that should move to event delivery in Story 7.10: verification, password reset, deletion, profile invitation, health-record invitation, and reminder email dispatch behind the reminder claim model.
- Confirmed after-commit Redis publishing is sufficient for current OCR and email flows; transactional outbox is deferred until reliability requirements justify it.
- Added package ownership guidance for `events.*`, `events.email.*`, `events.ocr.*`, `ocr.*`, `ai.*`, email/template ownership, and health-record domain boundaries.
- Updated Epic 7 guidance and canonical docs so later refactor stories can cite the decision.

### File List

- docs/event-driven-architecture.md
- docs/architecture.md
- docs/index.md
- _bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/epic-7-issues-and-proposed-stories.md
- _bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/7-9-confirm-and-document-event-driven-architecture.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-05-20: Added accepted event-driven architecture decision, updated canonical architecture/index docs, aligned Epic 7 guidance, and moved story to review.

## Senior Developer Review (AI)

**Review Date:** 2026-05-20
**Review Outcome:** Approve
**Review Scope:** Documentation/architecture decision review for Story 7.9.

### Findings

Clean review. No decision-needed, patch, or defer findings remained after checking acceptance-criteria coverage, downstream Story 7.10/7.11 guardrails, package ownership clarity, and documentation references.

### Review Notes

- Confirmed `docs/event-driven-architecture.md` covers Redis Streams scope, email categories, outbox stance, delivery semantics, package ownership boundaries, non-goals, and downstream guardrails.
- Confirmed canonical docs reference the decision from `docs/index.md` and `docs/architecture.md`.
- Confirmed Epic 7 guidance points later stories at the accepted decision.
