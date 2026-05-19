# Story 11: Notification Center UI And Bell Wiring

Status: backlog

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1 | **Depends on:** `pae-10`

## Story

As a signed-in user,  
I want to open notifications from the header bell,  
so that I do not hunt for invites on Profiles and Health Records pages.

## Acceptance Criteria

1. Bell in `layout.tsx` is a functional `button` with click handler (not inert).
2. Opens drawer **or** navigates to `/notifications` listing inbox API items.
3. Badge shows count of unread/pending items (minimum: pending invitations sum).
4. Empty state: "Không có thông báo mới".
5. Item click → correct route (`/profiles`, invitation accept flow, `/health-records`, etc.).
6. `aria-expanded` on bell when panel open; focus trap in drawer if used.
7. Refresh on route focus or interval (reuse polling pattern if exists — 30s PRD).

## Tasks / Subtasks

- [ ] `NotificationDrawer.tsx` or `notifications/page.tsx`.
- [ ] Hook `useNotificationInbox` calling API.
- [ ] Wire badge on bell; remove decorative-only red dot.
- [ ] Vitest: bell toggles panel; mock API returns items.

## Dev Notes

- Inline invites on `/profiles` and `/health-records` may remain — inbox is additive unified view.
- UX: PHASE-2-STITCH bell dropdown spec.

### Likely Files

- `apps/web/src/app/(dashboard)/layout.tsx`
- `apps/web/src/app/(dashboard)/notifications/page.tsx` (optional)
- `apps/web/src/components/features/notifications/NotificationDrawer.tsx`

## Dev Agent Record

### Agent Model Used

(pending)
