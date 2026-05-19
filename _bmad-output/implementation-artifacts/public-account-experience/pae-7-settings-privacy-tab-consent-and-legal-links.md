# Story 7: Settings Privacy Tab — Consent And Legal Links

Status: backlog

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P2 | **Depends on:** `pae-1`, `pae-5`

## Story

As a signed-in user,  
I want to review my consent status and open legal documents from settings,  
so that I understand how my health data is handled.

## Acceptance Criteria

1. `/settings/privacy` shows consent acceptance status from existing Consent API (`GET` consent status endpoint).
2. Links to `/privacy`, `/terms` open in new tab.
3. Prominent link to `/settings/delete-account` for right-to-delete.
4. Does not replace `ConsentModal` first-login gate.
5. Loading/error states via shared dashboard patterns.

## Tasks / Subtasks

- [ ] Replace stub from `pae-1` with real privacy settings page.
- [ ] Fetch consent status on mount; display accepted date/version if API provides.
- [ ] `DashboardPageShell` + breadcrumb `Trang chủ / Cài đặt / Riêng tư`.

## Dev Notes

- `ConsentController` already implemented — wire read-only view.
- REVIEW-FULL-v2 §8.4 privacy settings tab.

### Likely Files

- `apps/web/src/app/(dashboard)/settings/privacy/page.tsx`
- `apps/api/src/main/java/com/healthlens/api/controller/ConsentController.java`

## Dev Agent Record

### Agent Model Used

(pending)
