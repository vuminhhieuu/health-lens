# Story 12: Notification Email Preferences

Status: backlog

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P2 | **Depends on:** `pae-1`, `pae-10` (optional)

## Story

As a signed-in user,  
I want to control which emails I receive,  
so that I only get messages I care about while security emails still reach me.

## Acceptance Criteria

1. Table `user_notification_preferences` (user_id FK, boolean columns per category).
2. Categories: `shareInvite`, `shareAccepted`, `followUpReminder`, `security` (security non-disableable or warn on toggle off).
3. `GET` / `PUT /api/v1/users/me/notification-preferences`.
4. `/settings/notifications` UI with toggles; save via `notify.success`.
5. `EmailService` / `FollowUpReminderScheduler` checks prefs before non-security sends.
6. Flyway/Liquibase migration + service tests.

## Tasks / Subtasks

- [ ] Entity + repository + `UserController` or dedicated controller endpoints.
- [ ] Default preferences on user create (all on except explicit opt-out model — document).
- [ ] Replace notifications stub from `pae-1`.
- [ ] Update share invite email path to respect `shareInvite` toggle.

## Dev Notes

- Push notifications (FCM) — PRD Phase 2; out of scope.
- Owner accept email: `remaining-3-3` should respect `shareAccepted` when implemented.

### Likely Files

- `apps/api/src/main/resources/db/migration/V*__user_notification_preferences.sql`
- `apps/api/src/main/java/com/healthlens/api/entity/UserNotificationPreference.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailService.java`
- `apps/web/src/app/(dashboard)/settings/notifications/page.tsx`

## Dev Agent Record

### Agent Model Used

(pending)
