# Story 3.3: Sharing Lifecycle Audit And Owner Notification

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / family sharing observability  
**Area:** Sharing audit events, owner notifications, correlation id metadata  
**Priority:** P1

## Story

As a profile owner,  
I want sharing lifecycle events to be auditable and visible,  
so that I know who gained or lost access to my health profile.

## Acceptance Criteria

1. Invite, accept, reject, cancel, revoke, and failed access attempts produce audit events.
2. Owner receives notification when invitation is accepted if product policy requires it.
3. Audit metadata includes actor, owner, viewer, profile, invitation, correlation id, and outcome.
4. No raw invitation token or full email is logged.

## Tasks / Subtasks

- [ ] Task 1 - Define sharing lifecycle audit event names and metadata (AC: #1, #3, #4)
- [ ] Task 2 - Emit events from invite/accept/reject/cancel/revoke/failure paths (AC: #1)
- [ ] Task 3 - Add owner notification on accepted invite if policy requires it (AC: #2)
- [ ] Task 4 - Mask sensitive metadata (AC: #4)
- [ ] Task 5 - Add audit and notification tests (AC: #1-#4)

## Dev Notes

### Dedup — Public Account Experience epic

- Owner notification on share accept: **chỉ implement ở story này** — đã lược owner-notify khỏi epic PAE (12 stories).
- Khi có `pae-12-notification-email-preferences`, email owner notify nên tôn trọng toggle `shareAccepted` (nếu đã merge).

### Implementation Guardrails

- Reuse the unified audit/correlation approach from core improvements when available.
- Do not log raw invitation tokens or full emails.
- If product policy for owner notification is undecided, document the default and keep behavior configurable.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/FamilySharingService.java`
- `apps/api/src/main/java/com/healthlens/api/service/Audit*`
- `apps/api/src/main/java/com/healthlens/api/service/Email*`
- `apps/api/src/test/java/com/healthlens/api/*Sharing*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 3.3
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 4.1

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
