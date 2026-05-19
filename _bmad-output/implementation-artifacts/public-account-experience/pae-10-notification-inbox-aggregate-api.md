# Story 10: Notification Inbox Aggregate API

Status: backlog

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

- [ ] `NotificationController` + `NotificationInboxService` (or extend existing controller).
- [ ] Map from `InvitationController` / `HealthRecordInvitationController` data.
- [ ] OpenAPI or shared DTO in `packages/shared` if pattern exists.
- [ ] Register route in `ApiRoutes.java`, `SecurityConfig` authenticated only.

## Dev Notes

- Architecture.md mentions `NotificationService` — implement minimal aggregate first, not full push system.
- Owner email on accept: **`remaining-3-3`**, not this story.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/controller/NotificationController.java` (new)
- `apps/api/src/main/java/com/healthlens/api/service/NotificationInboxService.java` (new)
- `packages/shared/constants/api.ts`

## Dev Agent Record

### Agent Model Used

(pending)
