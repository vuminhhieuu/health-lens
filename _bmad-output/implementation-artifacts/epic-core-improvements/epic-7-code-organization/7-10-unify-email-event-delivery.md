# Story 7.10: Unify Email Event Delivery

Status: done

## Execution Scope

**Area:** Backend email architecture, async delivery consistency, maintainability  
**Priority:** P1/P2

## Story

As a backend developer, I want user-facing email delivery unified behind one event-driven pattern, so that retries, failure handling, and latency semantics stay consistent across verification, reset, invitation, deletion, and reminder flows.

## Acceptance Criteria

1. **Given** user-facing email flows are reviewed, **When** this story is complete, **Then** targeted email categories use one agreed event-delivery path.
2. **Given** the email consumer processes events, **When** supported email types are published, **Then** delivery behavior is consistent across categories.
3. **Given** failures occur during async email processing, **When** the system handles them, **Then** retry or dead-letter behavior follows one defined rule.

## Tasks / Subtasks

- [x] Define supported email event types and payload shapes.
- [x] Move targeted direct email dispatch paths behind the chosen event-delivery pattern.
- [x] Keep template rendering responsibilities separate from delivery orchestration.
- [x] Add or update tests for event serialization, consumer handling, and failure semantics.
- [x] Avoid changing user-visible URLs or token security behavior beyond dispatch path unification.

### Review Findings

- [x] [Review][Patch] Document controlled exception for token-bearing email delivery events — Decision resolved 2026-05-20: accept single-use email action tokens/links in the bounded `email.events` delivery stream as a controlled exception, and document required retention/access/monitoring guardrails. `EmailEventPublisher` writes verification/password-reset tokens, deletion cancellation links, profile invitation links, and health-record invitation links into durable stream payloads (`EmailEventPublisher.java:31`, `EmailEventPublisher.java:40`, `EmailEventPublisher.java:49`, `EmailEventPublisher.java:82`, `EmailEventPublisher.java:91`).
- [x] [Review][Patch] Failed async delivery can still be acknowledged instead of retried [`apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java:132`]
- [x] [Review][Patch] Follow-up reminders can be resent if SMTP succeeds but Redis ack fails [`apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java:183`]
- [x] [Review][Patch] Poison or malformed email events can retry forever without quarantine or dead-letter handling [`apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java:147`]
- [x] [Review][Patch] Email publish failures are swallowed, bypassing caller audit/error paths [`apps/api/src/main/java/com/healthlens/api/service/EmailEventPublisher.java:123`]
- [x] [Review][Patch] Follow-up reminder SMTP delivery still lives in the domain service instead of the unified email handling path [`apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java:46`]
- [x] [Review][Defer] Sprint status includes unrelated `core-7-9-confirm-and-document-event-driven-architecture` status movement [`_bmad-output/implementation-artifacts/sprint-status.yaml:226`] — deferred, pre-existing

## Dev Notes

- This story depends on the architecture decision from `7-9`.
- Coordinate with `7-4` so template cleanup and delivery unification do not fight each other.
- Keep behavior-preserving semantics for successful sends unless the agreed architecture explicitly changes them.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailService.java`
- `apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java`
- `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordShareService.java`
- `apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java`
- `apps/api/src/test/java/com/healthlens/api/service/`

## References

- `epic-7-issues-and-proposed-stories.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- Red phase: `./gradlew test --tests com.healthlens.api.service.EmailEventPublisherTest` initially exposed stale direct-email test compilation errors in `DataDeletionServiceTest`.
- Red phase: `./gradlew test --tests com.healthlens.api.service.EmailEventPublisherTest --tests com.healthlens.api.service.FollowUpReminderServiceTest` failed on deletion-confirmation payload serialization and reminder double-claim behavior.
- Targeted validation: `./gradlew test --tests com.healthlens.api.service.EmailEventPublisherTest --tests com.healthlens.api.service.EmailConsumerTest --tests com.healthlens.api.service.FollowUpReminderServiceTest --tests com.healthlens.api.service.DataDeletionServiceTest --tests com.healthlens.api.service.AuthServiceTest --tests com.healthlens.api.service.ProfileShareServiceTest`.
- Full validation: `./gradlew test`.
- Static diff validation: `git diff --check`.
- Code review patch validation: `./gradlew test --tests com.healthlens.api.service.EmailEventPublisherTest --tests com.healthlens.api.service.EmailConsumerTest --tests com.healthlens.api.service.FollowUpReminderServiceTest`.
- Code review full validation: `./gradlew test`.

### Completion Notes List

- Unified supported user-facing email categories behind typed `EmailEvent` values and `EmailEventPublisher`: verification, password reset, deletion confirmation/cancellation/completion, profile invitation, health-record invitation, and follow-up reminder.
- Confirmed targeted domain services publish email events instead of directly invoking SMTP delivery; email template rendering remains in `EmailService`.
- Fixed deletion-confirmation event payload to serialize `scheduledDeletionAt` so the consumer can reconstruct the deadline-bearing request passed to the template renderer.
- Fixed follow-up reminder delivery so scheduler/create paths claim and publish once, while `EmailConsumer` sends the already-claimed reminder without re-claiming and marks sent only after successful SMTP delivery.
- Added consumer tests for supported event routing and failure semantics: successful handling acknowledges stream records, failed handling leaves records pending for retry.
- Preserved existing user-visible verification/reset/invitation/cancellation URLs and token generation/security behavior; only dispatch path semantics changed.
- Resolved code review findings by documenting the controlled email-token stream exception, adding invalid email-event DLQ handling, preventing duplicate reminder sends after successful mark-sent, moving reminder SMTP delivery into `EmailConsumer`, making missing SMTP configuration retryable, and auditing Redis publish failures.

### File List

- apps/api/src/main/java/com/healthlens/api/dto/event/EmailEvent.java
- apps/api/src/main/java/com/healthlens/api/service/AuthService.java
- apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java
- apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java
- apps/api/src/main/java/com/healthlens/api/service/EmailEventPublisher.java
- apps/api/src/main/java/com/healthlens/api/service/EmailService.java
- apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordShareService.java
- apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java
- apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/DataDeletionServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/EmailConsumerTest.java
- apps/api/src/test/java/com/healthlens/api/service/EmailEventPublisherTest.java
- apps/api/src/test/java/com/healthlens/api/service/FollowUpReminderServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/ProfileShareServiceTest.java
- docs/event-driven-architecture.md
- _bmad-output/implementation-artifacts/deferred-work.md
- _bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/7-10-unify-email-event-delivery.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-05-20: Unified email event delivery, fixed deletion/reminder event edge cases, added serialization/consumer/failure tests, and moved story to review.
- 2026-05-20: Addressed code review findings, documented email-token stream exception, hardened retry/DLQ semantics, and moved story to done.
