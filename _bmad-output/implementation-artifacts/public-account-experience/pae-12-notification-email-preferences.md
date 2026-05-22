# Story 12: Notification Email Preferences

Status: done

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

- [x] Entity + repository + `UserController` or dedicated controller endpoints.
- [x] Default preferences on user create (all on except explicit opt-out model — document).
- [x] Replace notifications stub from `pae-1`.
- [x] Update share invite email path to respect `shareInvite` toggle.

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

Composer

### Implementation Plan

- Flyway `V046` table `user_notification_preferences` with boolean columns per category (defaults `TRUE`).
- `UserNotificationPreferenceService`: lazy-create on GET; PUT forces `security=true`; `createDefaultPreferences` on register.
- `UserNotificationPreferenceController` at `GET`/`PUT /api/v1/users/me/notification-preferences`.
- `EmailConsumer` skips non-security sends when recipient/user opted out (`shareInvite`, `shareAccepted`, `followUpReminder`).
- Web `/settings/notifications`: email toggles + `notify.success` on save; in-app inbox retained from `pae-10`.

### Default preference model

All categories default **on** at registration. Users opt out per category via settings. `security` is always on (non-disableable in API and UI).

### Completion Notes

- ✅ Migration, entity, repository, service, controller, shared API path
- ✅ Register hook creates default row
- ✅ Email consumer respects prefs; follow-up skip marks reminder sent to avoid retry loops
- ✅ Settings UI replaces “Sắp có” email stub
- ✅ Tests: `UserNotificationPreferenceServiceTest`, updated `EmailConsumerTest` + `AuthServiceTest`

### File List

- apps/api/src/main/resources/db/migration/V046__user_notification_preferences.sql
- apps/api/src/main/java/com/healthlens/api/entity/UserNotificationPreference.java
- apps/api/src/main/java/com/healthlens/api/notification/NotificationEmailCategory.java
- apps/api/src/main/java/com/healthlens/api/repository/UserNotificationPreferenceRepository.java
- apps/api/src/main/java/com/healthlens/api/dto/request/UpdateNotificationPreferencesRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/response/NotificationPreferenceResponse.java
- apps/api/src/main/java/com/healthlens/api/service/UserNotificationPreferenceService.java
- apps/api/src/main/java/com/healthlens/api/controller/UserNotificationPreferenceController.java
- apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java
- apps/api/src/main/java/com/healthlens/api/service/AuthService.java
- apps/api/src/main/java/com/healthlens/api/service/EmailConsumer.java
- apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java
- apps/api/src/main/java/com/healthlens/api/entity/FollowUpReminder.java
- apps/api/src/main/java/com/healthlens/api/repository/FollowUpReminderRepository.java
- apps/api/src/test/java/com/healthlens/api/service/UserNotificationPreferenceServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/EmailConsumerTest.java
- apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java
- packages/shared/constants/api.ts
- apps/web/src/lib/api/routes.ts
- apps/web/src/hooks/useNotificationEmailPreferences.ts
- apps/web/src/app/(dashboard)/settings/notifications/page.tsx
- apps/web/src/app/(dashboard)/settings/notifications/_components/EmailPreferenceToggle.tsx

### Change Log

- 2026-05-22: PAE-12 notification email preferences (API, migration, email gating, settings UI)
- 2026-05-22: Code review — `skipped_opt_out` column, GET transaction fix, tests

### Review Findings

- [x] [Review][Decision] Nhắc tái khám opt-out — Chọn **C**: cột `email_skipped_opt_out_at`, `markEmailSkippedOptOut`, loại khỏi scheduler; bật lại `followUpReminder` thì `clearEmailSkippedOptOutForUser` cho nhắc còn due.
- [x] [Review][Patch] `getPreferences` read-only + save — Đã bỏ `readOnly`, dùng `@Transactional` ghi khi lazy-create.
- [x] [Review][Patch] Test skip follow-up — `EmailConsumerTest.handleRecord_followUpReminder_optOut_marksSkippedNotSent` + test re-enable clear skipped.
- [x] [Review][Defer] AC5 ghi `EmailService`/`FollowUpReminderScheduler` — Thực tế check nằm ở `EmailConsumer` (đường gửi duy nhất qua Redis stream). Chấp nhận được về mặt chức năng; có thể bổ sung guard trong `EmailService` sau nếu có gọi trực tiếp.
