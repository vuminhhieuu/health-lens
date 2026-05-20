# Story 1: Settings Hub And Navigation Fix

Status: done

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

## Tasks / Subtasks

- [x] Create `apps/web/src/app/(dashboard)/settings/page.tsx` with `DashboardPageShell` and no hub breadcrumb.
- [x] Hub cards: profile, change-password, privacy, notifications, about, delete-account routes.
- [x] Stub pages for change-password, privacy, notifications, about (Vietnamese "Sắp có", back to `/settings`).
- [x] Optional `settings/layout.tsx` secondary nav if it reduces duplication. Not added because shared stub component covers the duplication cleanly.
- [x] Verify `layout.tsx` nav + mobile tab href `/settings`.

### Review Findings

- [x] [Review][Patch] Settings nav active state is wrong/incomplete on mobile and settings subpages [`apps/web/src/app/(dashboard)/layout.tsx:311`]

## Dev Notes

- Do not implement change-password API (see `pae-6`).
- Do not change `/` redirect (see `pae-2`).
- Broken today: nav → `/settings` 404; only `profile` and `delete-account` exist.
- Pattern: `apps/web/src/app/(dashboard)/help/page.tsx`.

### Likely Files

- `apps/web/src/app/(dashboard)/settings/page.tsx`
- `apps/web/src/app/(dashboard)/settings/{change-password,privacy,notifications,about}/page.tsx` (stubs)
- `apps/web/src/app/(dashboard)/layout.tsx`

### References

- `_bmad-output/planning-artifacts/public-account-experience-epics-and-stories.md`
- `_bmad-output/design-artifacts/SCREENS-FULL-LIST-VI.md` §1.6

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Implementation Plan

- Add a dashboard settings hub at `/settings` using the existing `DashboardPageShell` pattern.
- Add reusable "Sắp có" settings stub content for routes that are intentionally deferred.
- Verify dashboard layout already links "Cài đặt" and the mobile settings tab to `/settings`.

### Debug Log

- Green/refactor phase: added settings hub, stub pages, and shared stub component.
- Validation: `pnpm test` passed after frontend test removal (ESLint + Vitest with `--passWithNoTests`).
- Build note: `pnpm build` no longer reports the `section.danger` type error; local build remains blocked by unresolved existing dependencies/fonts (`@radix-ui/react-toast`, `recharts`, Google Fonts fetch).

### Completion Notes

- `/settings` now renders a responsive settings hub instead of 404.
- Hub includes links to profile, change-password, privacy, notifications, about, and delete-account.
- Deferred settings sections render a Vietnamese "Sắp có" page with a back link to `/settings`.
- Existing dashboard desktop navigation and mobile settings tab already use `/settings`.
- Review patch resolved: desktop and mobile navigation now derive active settings state from `pathname`, including settings subpages while preserving `/settings/profile` as the profile tab.
- Frontend smoke test file was removed per request.

### File List

- `apps/web/src/app/(dashboard)/settings/page.tsx`
- `apps/web/src/app/(dashboard)/settings/_components/SettingsComingSoonPage.tsx`
- `apps/web/src/app/(dashboard)/settings/change-password/page.tsx`
- `apps/web/src/app/(dashboard)/settings/privacy/page.tsx`
- `apps/web/src/app/(dashboard)/settings/notifications/page.tsx`
- `apps/web/src/app/(dashboard)/settings/about/page.tsx`
- `apps/web/src/app/(dashboard)/layout.tsx`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/public-account-experience/pae-1-settings-hub-and-navigation-fix.md`

## Change Log

- 2026-05-19: Renumbered to pae-1 (single epic consolidation).
- 2026-05-20: Implemented settings hub and deferred section stubs.
- 2026-05-20: Resolved review finding for settings navigation active state.
- 2026-05-20: Removed frontend smoke test artifact per request and updated story documentation.
