# Story 8: Settings About Tab — Version And Support

Status: done

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

- [x] About page with `DashboardPageShell`.
- [x] Reuse support contact constants once `remaining-2-6` defines them (or duplicate minimally with TODO to consolidate).

### Review Findings

- [x] [Review][Patch] `apps/web/.env.example` bị `.env*` trong `apps/web/.gitignore` chặn — thêm `!.env.example`.
- [x] [Review][Patch] `/help` cùng tab (`Link`); `/privacy`, `/terms` giữ `openInNewTab`.
- [x] [Review][Defer] AC4: profile vẫn hiển thị 85% giả — chờ `pae-9` [`profile/page.tsx`](apps/web/src/app/(dashboard)/settings/profile/page.tsx)
- [x] [Review][Defer] `ResourceLinkRow` / `AboutMetricTile` trùng pattern với privacy — gom component sau
- [x] [Review][Defer] Khối “Liên hệ trực tiếp” trùng `SettingsSupportCard` — consolidate sau

## Dev Notes

- SCREENS-FULL-LIST-VI §1.6 Tab 4 About.

### Likely Files

- `apps/web/src/app/(dashboard)/settings/about/page.tsx`
- `apps/web/src/lib/supportContact.ts` (shared with public support if exists)

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes

- `/settings/about`: layout bento 3 cột đồng bộ privacy/change-password; hero + metric phiên bản/build.
- `getAppVersionLabel()` / `NEXT_PUBLIC_APP_VERSION` (mặc định từ `package.json` qua `next.config.ts`); `NEXT_PUBLIC_BUILD_ID` tùy chọn.
- `supportContact.ts` re-export `marketingContact`, `PUBLIC_SUPPORT_HREF` = `/help`.
- Liên kết `/privacy`, `/terms`, `/help` (tab mới); sidebar nav + liên hệ phone/email.
- `SettingsAccountNav` thêm mục Giới thiệu (active trên trang about).
- Tests: `appVersion.test.ts`, `about-settings.page.test.ts` (6 tests passed).

### File List

- `apps/web/next.config.ts`
- `apps/web/.env.example`
- `apps/web/src/lib/appVersion.ts`
- `apps/web/src/lib/appVersion.test.ts`
- `apps/web/src/lib/supportContact.ts`
- `apps/web/src/app/(dashboard)/settings/about/page.tsx`
- `apps/web/src/app/(dashboard)/settings/about/about-settings.page.test.ts`
- `apps/web/src/app/(dashboard)/settings/_components/SettingsAccountNav.tsx`

### Change Log

- 2026-05-21: PAE-8 — about tab với version, legal/help links, support contact.
- 2026-05-21: Code review batch-fix — gitignore `.env.example`, help link cùng tab.
