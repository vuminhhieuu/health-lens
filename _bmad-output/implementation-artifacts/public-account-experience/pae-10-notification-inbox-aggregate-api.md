# Story 10: Notification Inbox Aggregate API

Status: done

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1

## Story

As a signed-in user,  
I want a single API listing my actionable notifications,  
so that the web app can show a unified inbox without duplicating business rules.

## Acceptance Criteria

1. `GET /api/v1/notifications/inbox` returns JSON array of items.
2. Item types (minimum): `PROFILE_INVITATION`, `HEALTH_RECORD_INVITATION`; optional: `REMINDER_UPCOMING`.
3. Fields: `id`, `type`, `title`, `body`, `createdAt`, `actionUrl`, `read` (default false).
4. Implementation aggregates existing invitation services — **no** duplicate accept/revoke logic.
5. Cap 50 items; sorted `createdAt` desc.
6. Auth required; integration tests prove no cross-user data.

## Tasks / Subtasks

- [x] `NotificationController` + `NotificationInboxService` (or extend existing controller).
- [x] Map from `InvitationController` / `HealthRecordInvitationController` data.
- [x] OpenAPI or shared DTO in `packages/shared` if pattern exists.
- [x] Register route in `ApiRoutes.java`, `SecurityConfig` authenticated only.

### Review Findings

- [x] [Review][Patch] Unit test tên "caps at 50" nhưng chỉ assert 2 item — thiếu coverage giới hạn 50 [apps/api/src/test/java/com/healthlens/api/service/NotificationInboxServiceTest.java:32]
- [x] [Review][Patch] Integration test chỉ cover PROFILE_INVITATION, chưa có HEALTH_RECORD_INVITATION [apps/api/src/test/java/com/healthlens/api/service/NotificationInboxIntegrationTest.java:45]
- [x] [Review][Patch] `id` inbox là UUID thuần — nên prefix theo `type` để tránh trùng key UI (pae-11) [apps/api/src/main/java/com/healthlens/api/service/NotificationInboxService.java:54]

## Dev Notes

- Architecture.md mentions `NotificationService` — implement minimal aggregate first, not full push system.
- Owner email on accept: **`remaining-3-3`**, not this story.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/controller/NotificationController.java` (new)
- `apps/api/src/main/java/com/healthlens/api/service/NotificationInboxService.java` (new)
- `packages/shared/constants/api.ts`

## Dev Agent Record

### Agent Model Used

Composer

### Implementation Plan

- `NotificationInboxService` delegates to `ProfileShareService.listIncomingInvitations` and `HealthRecordShareService.listIncomingInvitations`, maps to unified DTO, sorts by `createdAt` desc, limits 50.
- `REMINDER_UPCOMING` enum value reserved; not populated in this story (optional AC).
- Security: default `anyRequest().authenticated()` covers `/api/v1/notifications/inbox`.

### Completion Notes

- `GET /api/v1/notifications/inbox` — gộp lời mời profile + health record; `id` dạng `{TYPE}:{uuid}`; tối đa 50; `read` từ DB.
- `POST /api/v1/notifications/inbox/read` và `.../read-all` — lưu trạng thái đã đọc; snapshot (V045) giữ item đã đọc sau khi invite biến mất.
- Flyway: `V044__create_notification_inbox_read_state.sql`, `V045__notification_inbox_read_snapshot.sql` (bắt buộc trước deploy).
- Shared: `ApiPaths.NOTIFICATIONS` (`INBOX`, `INBOX_READ`, `INBOX_READ_ALL`), type `NotificationInboxItem`.
- Tests backend (Gradle): `NotificationInboxServiceTest`, `NotificationControllerTest`, `NotificationInboxIntegrationTest`.

### File List

- `apps/api/src/main/java/com/healthlens/api/controller/NotificationController.java`
- `apps/api/src/main/java/com/healthlens/api/service/NotificationInboxService.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/MarkNotificationInboxReadRequest.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/NotificationInboxItemResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/NotificationInboxItemType.java`
- `apps/api/src/main/java/com/healthlens/api/entity/NotificationInboxReadState.java`
- `apps/api/src/main/java/com/healthlens/api/repository/NotificationInboxReadStateRepository.java`
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/resources/db/migration/V044__create_notification_inbox_read_state.sql`
- `apps/api/src/main/resources/db/migration/V045__notification_inbox_read_snapshot.sql`
- `apps/api/src/test/java/com/healthlens/api/controller/NotificationControllerTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/NotificationInboxServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/NotificationInboxIntegrationTest.java`
- `packages/shared/constants/api.ts`
- `packages/shared/types/index.ts`

### Change Log

- 2026-05-21: Inbox aggregate API for invitations.
- 2026-05-21: Read-state persistence, mark-read endpoints, and read snapshots for archived items.
