# Story 7.10: Unify Email Event Delivery

Status: proposed

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

- [ ] Define supported email event types and payload shapes.
- [ ] Move targeted direct email dispatch paths behind the chosen event-delivery pattern.
- [ ] Keep template rendering responsibilities separate from delivery orchestration.
- [ ] Add or update tests for event serialization, consumer handling, and failure semantics.
- [ ] Avoid changing user-visible URLs or token security behavior beyond dispatch path unification.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
