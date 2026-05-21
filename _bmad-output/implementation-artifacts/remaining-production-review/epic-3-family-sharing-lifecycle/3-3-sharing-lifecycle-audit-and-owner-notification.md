# Story 3.3: Sharing Lifecycle Audit And Owner Notification

Status: done

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

- [x] Task 1 - Define sharing lifecycle audit event names and metadata (AC: #1, #3, #4)
- [x] Task 2 - Emit events from invite/accept/reject/cancel/revoke/failure paths (AC: #1)
- [x] Task 3 - Add owner notification on accepted invite if policy requires it (AC: #2)
- [x] Task 4 - Mask sensitive metadata (AC: #4)
- [x] Task 5 - Add audit and notification tests (AC: #1-#4)

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

Composer

### Debug Log References

### Completion Notes List

- Added `SharingAuditSupport` + `AuditPiiMasker` for canonical sharing audit payloads (`actorId`, `ownerId`, `viewerId`, `invitationId`, `correlationId`, `outcome`, `inviteeEmailMasked`).
- Added `PROFILE_SHARE_ACCESS_DENIED_FAILED` for non-owner access and invitee email mismatch; invalid token logged anonymously without token value.
- Owner email on first accept via `PROFILE_SHARE_ACCEPTED` event; toggle `app.sharing.notify-owner-on-accept` (default `true`).
- Extended `AuditRedactor` to strip raw `inviteeEmail` / invitation tokens from any audit JSON.

### File List

- apps/api/src/main/java/com/healthlens/api/audit/AuditPiiMasker.java
- apps/api/src/main/java/com/healthlens/api/audit/SharingAuditSupport.java
- apps/api/src/main/java/com/healthlens/api/audit/AuditActions.java
- apps/api/src/main/java/com/healthlens/api/audit/AuditRedactor.java
- apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java
- apps/api/src/main/java/com/healthlens/api/dto/event/EmailEvent.java
- apps/api/src/main/java/com/healthlens/api/service/EmailEventPublisher.java
- apps/api/src/main/java/com/healthlens/api/service/EmailService.java
- apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java
- apps/api/src/main/java/com/healthlens/api/service/admin/AdminAuditLogService.java
- apps/api/src/main/resources/application.yml
- apps/api/src/main/resources/templates/email/profile-share-accepted.html
- apps/api/src/test/java/com/healthlens/api/audit/AuditPiiMaskerTest.java
- apps/api/src/test/java/com/healthlens/api/audit/SharingAuditSupportTest.java
- apps/api/src/test/java/com/healthlens/api/audit/AuditRedactorTest.java
- apps/api/src/test/java/com/healthlens/api/service/ProfileShareServiceTest.java
