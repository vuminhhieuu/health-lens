# Story 8: Settings About Tab — Version And Support

Status: backlog

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P2 | **Depends on:** `pae-1`

## Story

As a signed-in user,  
I want app version and support contact in settings,  
so that I can troubleshoot or report issues.

## Acceptance Criteria

1. `/settings/about` shows app name, version/build (from `NEXT_PUBLIC_APP_VERSION` or package.json at build time).
2. Links: public `/privacy`, `/terms`, public support route from `remaining-2-6` (`/support` or equivalent).
3. Replaces stub from `pae-1`.
4. No hardcoded fake profile completion on profile page (coordinate `pae-9`).

## Tasks / Subtasks

- [ ] About page with `DashboardPageShell`.
- [ ] Reuse support contact constants once `remaining-2-6` defines them (or duplicate minimally with TODO to consolidate).

## Dev Notes

- SCREENS-FULL-LIST-VI §1.6 Tab 4 About.

### Likely Files

- `apps/web/src/app/(dashboard)/settings/about/page.tsx`
- `apps/web/src/lib/supportContact.ts` (shared with public support if exists)

## Dev Agent Record

### Agent Model Used

(pending)
