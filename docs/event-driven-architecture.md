# HealthLens Event-Driven Architecture Decision

**Status:** Accepted  
**Date:** 2026-05-20  
**Scope:** Backend async side effects, Redis Streams usage, package ownership, and Epic 7 refactor guardrails.

## Decision Summary

HealthLens will use a hybrid async model with explicit delivery semantics:

- Redis Streams are the standard for durable, cross-boundary asynchronous work that must survive request completion and can be retried independently.
- Direct synchronous calls remain valid for domain invariants, authorization, validation, persistence transitions, and audit writes that must be part of the current command result.
- DB-claimed jobs remain valid for scheduled work where the database row itself is the work item and claim/release semantics are already the source of truth.
- After-commit direct calls are allowed only as an interim pattern for low-risk compatibility paths. New durable side effects should not add more ad hoc after-commit direct calls.

This means Redis Streams are not a universal event bus for every side effect today. They are the approved delivery mechanism for durable application events that cross package or runtime boundaries.

## Current Evidence Reviewed

Reviewed sources:

- `AuthService`: verification email is published to `email.events` after commit; password reset still calls `EmailService` directly.
- `HealthRecordService`: confirmed uploads publish OCR work to `ocr.events` after commit.
- `OcrJobConsumer`: consumes `ocr.events`, handles pending and new entries, acknowledges successful handling, tracks consumer failures, and sends repeated failures to DLQ state.
- `OcrJobStateService`: stores OCR job execution state, schedules retries through DB state, and re-enqueues due retries onto `ocr.events`.
- `EmailConsumer`: consumes `email.events`, but currently handles only `eventType = verification`.
- `FollowUpReminderService`: uses DB claim/release/timeout semantics and direct email sending, including after-commit send for newly due reminders.
- `DataDeletionService`, `ProfileShareService`, and `HealthRecordShareService`: still call `EmailService` directly for user-facing emails.
- `AuditEventRecorder` and related audit classes: record audit entries synchronously from service commands.

## Delivery Semantics Table

| Category | Current delivery | Target delivery | Rationale |
| --- | --- | --- | --- |
| OCR upload processing | After-commit Redis Stream event on `ocr.events` | Redis Stream event | OCR is durable, expensive, retryable, and independent of the upload confirmation response. |
| OCR retries | DB job state claim plus Redis Stream re-enqueue | DB job state plus Redis Stream event | The DB row owns retry timing and idempotency; Redis delivers the next processing attempt. |
| Email verification | After-commit Redis Stream event on `email.events` | Redis Stream event | User-facing email should not block registration and should have consistent retry/failure semantics. |
| Password reset email | Direct in-command `EmailService` call | Redis Stream event | Should align with verification email so auth email behavior has one delivery model. |
| Account deletion emails | Direct `EmailService` calls | Redis Stream event | User-facing privacy emails are durable notifications and should not depend on request thread SMTP availability. |
| Profile and health-record invitation emails | Direct `EmailService` calls | Redis Stream event | Invitations are user-facing notifications and should use the same async email contract. |
| Follow-up reminder emails | DB-claimed scheduled job plus direct send | Keep DB claim as source of truth; publish/send through unified email event boundary later | Reminder rows already provide due-date, claim, release, and timeout behavior. The later email unification story should preserve that claim model while routing actual email delivery consistently. |
| Audit writes | Synchronous audit recorder call | Synchronous write for command audit; revisit outbox only for external audit export | Audit entries are part of command accountability and should remain close to the command unless a later export pipeline is introduced. |
| In-app notifications | Not yet centralized | Redis Stream event or DB notification row depending on UX requirements | Future notification work should choose a durable event boundary before implementation, not direct ad hoc service calls. |
| Product analytics events | Not yet centralized | Redis Stream event or append-only analytics table | Analytics should be decoupled from user commands and should tolerate delayed processing. |

## Rules For New Work

1. Use synchronous service calls for validation, authorization, aggregate state transitions, and data required to return the current API response.
2. Use Redis Stream events for durable side effects that can complete after commit, especially OCR, user-facing emails, owner notifications, analytics, and future mobile sync triggers.
3. Publish durable stream events only after the transaction commits, so consumers never observe rolled-back state.
4. Use DB-claimed jobs when the database row is already the durable schedule and claim state, such as follow-up reminders.
5. Do not introduce new ad hoc `redisTemplate.opsForStream().add(...)` calls inside arbitrary domain services. Story 7.11 should centralize this behind a small application event boundary.
6. Do not add new direct SMTP calls from domain services for user-facing emails. Story 7.10 should route email categories through typed email events.
7. Keep event payloads free of secrets, raw OCR text, health document contents, and long unbounded user text.
   Story 7.10 defines one controlled exception: the bounded `email.events` delivery stream may carry single-use email action tokens or links needed to render verification, password-reset, deletion-cancellation, and invitation emails. This exception must remain limited to email delivery, use restricted Redis access, avoid logging payload values, and be revisited if stream retention or replay requirements expand.

## Outbox Position

After-commit Redis publish is sufficient for current OCR and email flows because HealthLens already accepts asynchronous retry and eventual delivery for those flows. A relational outbox is not required for Story 7.9, 7.10, or 7.11.

Add a transactional outbox later only if one of these becomes true:

- Losing an event after DB commit is unacceptable for a regulated or financial-grade workflow.
- The app needs replayable event history across deployments.
- Multiple downstream consumers must independently process the same committed domain event.
- Redis availability gaps during commit become a measured production reliability problem.

Until then, use after-commit publishing and make publisher failures observable without rolling back the user command unless the story explicitly changes the user contract.

## Package Ownership Boundaries

Target package ownership for Epic 7 refactors:

| Package area | Owns | Does not own |
| --- | --- | --- |
| `events.*` | Stream names, typed event payloads, event publisher interfaces/implementations, consumer bootstrap helpers, shared serialization rules | Domain validation, domain persistence, template rendering, OCR provider calls |
| `events.email.*` | Email event kinds, email event payloads, email event publisher, email stream consumer routing | HTML template internals and direct business decisions about when to send |
| `events.ocr.*` | OCR event payloads, OCR stream publisher constants/helpers | OCR provider selection, parsing, health-record state transitions |
| `ocr.*` | OCR queue/job metadata, provider routing, retry/DLQ state, OCR extraction/parsing orchestration | Health-record ownership/access checks and final health-record domain policy |
| `ai.*` | LLM chat, embeddings, RAG retrieval, prompt templates, provider configuration | OCR queue delivery and health-record persistence |
| `service.email` or future `email.*` | Template rendering and SMTP adapter behavior | Deciding cross-domain event delivery semantics |
| Health record domain | Upload reservation, record ownership/access, health-record state transitions, profile timestamps, record detail/history assembly | Low-level stream mechanics and OCR provider implementation |

## Epic 7 Guardrails

- Story 7.1 may move AI/OCR/RAG classes into clearer package boundaries, but should not introduce a new stream abstraction or migrate email behavior.
- Story 7.10 should unify user-facing email categories behind typed events and one consumer strategy.
- Story 7.11 should introduce a small application stream/event boundary and remove direct Redis Stream publishing from domain services where practical.
- Story 7.13 should reference this document from refreshed architecture/source-tree docs.

## Non-Goals

- No package moves are performed by this decision.
- No email categories are migrated in this decision.
- No Redis abstraction or transactional outbox is introduced in this decision.
- No guarantee is made that every asynchronous task must use Redis Streams.
