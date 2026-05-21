# Story 11: Notification Center UI And Bell Wiring

Status: done

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1 | **Depends on:** `pae-10`

## Story

As a signed-in user,  
I want to open notifications from the header bell,  
so that I do not hunt for invites on Profiles and Health Records pages.

## Acceptance Criteria

1. Bell in header is a functional `button` with click handler (not inert).
2. Opens **dropdown** under bell (or full page at `/settings/notifications`) listing inbox API items.
3. Badge shows count of unread items.
4. Empty state: "Không có thông báo mới".
5. Item click → `/profiles` or `/health-records` with `inboxRead` query; mark read on destination view.
6. `aria-expanded`, `aria-controls` on bell when panel open; Escape closes dropdown.
7. Poll inbox every 30s; refetch on window focus.
8. Read items remain visible with "Đã đọc" styling (via API read snapshots).

## Tasks / Subtasks

- [x] `NotificationBell` + `NotificationDropdown` (popover, not drawer).
- [x] `useNotificationInbox` + mark read / mark all read.
- [x] `/settings/notifications` — real inbox (not "Coming Soon").
- [x] `MarkInboxReadFromUrl` on profiles & health-records hubs.
- [x] Accept invitation from hub via API (no full-page redirect to login).
- [x] `refreshSessionOnce` — fix logout on rapid page reload (refresh token race).
- [x] Manual smoke: bell, badge, read/unread, navigation, reload session.

### Review Findings

- [x] [Review][Patch] Drawer → dropdown per UX spec; focus/a11y on popover.
- [x] [Review][Patch] Per-item read via destination URL, not mark-all on single click.

## Dev Notes

- Inline invites on `/profiles` and `/health-records` remain — inbox is additive.
- Email preferences: **`pae-12`**, not this story.
- No frontend Vitest in this PR; verify via manual smoke + backend tests.

### Likely Files

- `apps/web/src/components/features/notifications/`
- `apps/web/src/hooks/useNotificationInbox.ts`, `useMarkInboxReadFromSearchParams.ts`
- `apps/web/src/app/(dashboard)/settings/notifications/page.tsx`
- `apps/web/src/components/layout/AuthenticatedTopHeader.tsx`
- `apps/web/src/app/(dashboard)/profiles/page.tsx`, `health-records/page.tsx`
- `apps/web/src/lib/auth/refreshSession.ts`, `hooks/useAuthBootstrap.ts`, `(dashboard)/layout.tsx`

## Dev Agent Record

### Agent Model Used

Composer

### Implementation Plan

- Header `NotificationBell` toggles `NotificationDropdown` (popover under bell).
- `useNotificationInbox` → inbox API; optimistic mark read / mark all; poll 30s + focus refetch.
- `notificationDestination.ts` routes by type; `inboxRead` query triggers mark on hub pages.
- `profiles` / `health-records`: `extractInvitationToken` + accept API, then client navigation.
- Session: shared `refreshSessionOnce()` for bootstrap and 401 interceptor; layout waits hydrate before login redirect.

### Completion Notes

- Chuông + dropdown: badge unread, toolbar "Đánh dấu tất cả đã đọc", empty state tiếng Việt.
- Settings → Thông báo: cùng `NotificationInboxList`, không stub.
- Đã đọc vẫn hiển thị trong list; styling phân biệt unread/read.
- Reload nhiều lần không còn đẩy về `/login` (single-flight refresh).
- Kiểm tra thủ công theo AC; không thêm file `*.test.ts(x)` frontend cho story này.

### File List

- `apps/web/src/hooks/useNotificationInbox.ts`
- `apps/web/src/hooks/useMarkInboxReadFromSearchParams.ts`
- `apps/web/src/components/features/notifications/NotificationBell.tsx`
- `apps/web/src/components/features/notifications/NotificationDropdown.tsx`
- `apps/web/src/components/features/notifications/NotificationInboxList.tsx`
- `apps/web/src/components/features/notifications/NotificationInboxItemRow.tsx`
- `apps/web/src/components/features/notifications/NotificationInboxToolbar.tsx`
- `apps/web/src/components/features/notifications/notificationDestination.ts`
- `apps/web/src/components/features/notifications/MarkInboxReadFromUrl.tsx`
- `apps/web/src/app/(dashboard)/settings/notifications/page.tsx`
- `apps/web/src/components/layout/AuthenticatedTopHeader.tsx`
- `apps/web/src/app/(dashboard)/profiles/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/page.tsx`
- `apps/web/src/lib/sharing/extractInvitationToken.ts`
- `apps/web/src/lib/auth/refreshSession.ts`
- `apps/web/src/lib/auth/restoreSession.ts`
- `apps/web/src/hooks/useAuthBootstrap.ts`
- `apps/web/src/lib/api/apiClient.ts`
- `apps/web/src/app/(dashboard)/layout.tsx`
- `apps/web/src/lib/api/routes.ts`

### Change Log

- 2026-05-21: Header bell wired to notification dropdown with badge and polling.
- 2026-05-21: Read/unread UX, settings notifications page, hub mark-read and API accept flows.
- 2026-05-21: `refreshSessionOnce` to prevent session loss on rapid reload.
