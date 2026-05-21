# Story 7: Settings Privacy Tab — Consent And Legal Links

Status: done

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

- [x] Replace stub from `pae-1` with real privacy settings page.
- [x] Fetch consent status on mount; display accepted date/version if API provides.
- [x] `DashboardPageShell` + breadcrumb `Trang chủ / Cài đặt / Riêng tư`.

### Review Findings

- [x] [Review][Patch] Hiển thị sai “Phiên bản chính sách” khi đồng thuận lỗi thời — tách `activePolicyVersion` (store/`CONSENT_VERSION`) và `recordedVersion` khi stale.
- [x] [Review][Patch] Ẩn `consentedAt` khi đồng thuận hết hạn — hiển thị ngày khi API trả về bất kể `consentGiven`.
- [x] [Review][Patch] Trạng thái lỗi chặn toàn trang — `LegalAndDeleteSections` luôn render; lỗi chỉ trong `ConsentStatusPanel` + nút Thử lại.
- [x] [Review][Patch] Cache `consentStatus` có thể cũ sau `ConsentModal` — `queryKey` gồm `sessionConsentGiven`, `sessionConsentVersion`, `activePolicyVersion`.
- [x] [Review][Patch] Liên kết pháp lý thiếu gợi ý a11y tab mới — `sr-only` “(mở trong tab mới)”.
- [x] [Review][Patch] Spinner loading thiếu text cho screen reader — `sr-only` “Đang tải trạng thái đồng thuận”.
- [x] [Review][Defer] Test chỉ đọc source tĩnh, không mock API/React Query [`privacy-settings.page.test.ts`](apps/web/src/app/(dashboard)/settings/privacy/privacy-settings.page.test.ts) — deferred, pattern giống các story PAE khác.
- [x] [Review][Defer] Lặp cấu hình `DashboardPageShell` 3 lần (loading/error/success) [`page.tsx`](apps/web/src/app/(dashboard)/settings/privacy/page.tsx) — deferred, có thể extract helper sau.

## Dev Notes

- `ConsentController` already implemented — wire read-only view.
- REVIEW-FULL-v2 §8.4 privacy settings tab.

### Likely Files

- `apps/web/src/app/(dashboard)/settings/privacy/page.tsx`
- `apps/api/src/main/java/com/healthlens/api/controller/ConsentController.java`

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes

- Thay stub `SettingsComingSoonPage` bằng trang read-only: `GET` `API_ROUTES.CONSENT.ME` qua React Query; hiển thị `consentGiven`, phiên bản và `consentedAt` (định dạng `vi-VN`).
- Liên kết `/privacy`, `/terms` mở tab mới (`target="_blank"`, `rel="noopener noreferrer"`); CTA nổi bật tới `/settings/delete-account`.
- Không ghi đồng thuận trên trang này — `ConsentModal` vẫn là cổng đăng nhập lần đầu.
- Loading/error theo pattern dashboard (spinner / thông báo lỗi trong `DashboardPageShell`).
- Vitest: `privacy-settings.page.test.ts` (2 tests passed).

### File List

- `apps/web/src/app/(dashboard)/settings/privacy/page.tsx`
- `apps/web/src/app/(dashboard)/settings/privacy/privacy-settings.page.test.ts`

### Change Log

- 2026-05-21: PAE-7 — privacy settings tab với consent status, legal links, delete-account CTA.
- 2026-05-21: Code review batch-fix — version/date display, partial error UI, query cache key, a11y.
