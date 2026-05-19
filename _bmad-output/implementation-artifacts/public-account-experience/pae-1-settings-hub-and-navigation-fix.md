# Story 1: Settings Hub And Navigation Fix

Status: ready-for-dev

## Execution Scope

**Epic:** Public Account Experience (single epic)  
**Area:** `/settings` hub, dashboard navigation, mobile bottom tab  
**Priority:** P0

## Story

As a signed-in user,  
I want `/settings` to open a settings hub,  
so that navigation from the sidebar and mobile tab works and I can find account options in one place.

## Acceptance Criteria

1. **Given** an authenticated user, **When** they open `/settings`, **Then** they see a settings hub (not 404) with links to Profile, Security, Privacy, Notifications, About, and Delete account.
2. **Given** dashboard navigation, **When** user clicks "Cài đặt", **Then** they land on `/settings` successfully.
3. **Given** hub links to not-yet-built sections, **When** clicked, **Then** stub pages render "Sắp có" (not 404).
4. **Given** mobile viewport, **When** hub renders, **Then** layout is usable without horizontal overflow.
5. **Given** Vitest smoke tests, **When** run, **Then** hub renders with expected `href` values.

## Tasks / Subtasks

- [ ] Create `apps/web/src/app/(dashboard)/settings/page.tsx` with `DashboardPageShell`, breadcrumb `Trang chủ / Cài đặt`.
- [ ] Hub cards: profile, change-password, privacy, notifications, about, delete-account routes.
- [ ] Stub pages for change-password, privacy, notifications, about (Vietnamese "Sắp có", back to `/settings`).
- [ ] Optional `settings/layout.tsx` secondary nav if it reduces duplication.
- [ ] Verify `layout.tsx` nav + mobile tab href `/settings`.
- [ ] `settings/page.test.tsx` with mocked auth.

## Dev Notes

- Do not implement change-password API (see `pae-6`).
- Do not change `/` redirect (see `pae-2`).
- Broken today: nav → `/settings` 404; only `profile` and `delete-account` exist.
- Pattern: `apps/web/src/app/(dashboard)/help/page.tsx`.

### Likely Files

- `apps/web/src/app/(dashboard)/settings/page.tsx`
- `apps/web/src/app/(dashboard)/settings/{change-password,privacy,notifications,about}/page.tsx` (stubs)
- `apps/web/src/app/(dashboard)/layout.tsx`
- `apps/web/src/app/(dashboard)/settings/page.test.tsx`

### References

- `_bmad-output/planning-artifacts/public-account-experience-epics-and-stories.md`
- `_bmad-output/design-artifacts/SCREENS-FULL-LIST-VI.md` §1.6

## Dev Agent Record

### Agent Model Used

(pending)

### File List

## Change Log

- 2026-05-19: Renumbered to pae-1 (single epic consolidation).
