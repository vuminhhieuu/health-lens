# Story 4.2: Admin Analytics Dashboard DB-Backed Charts

Status: done

## Execution Scope

**Phase:** Remaining production review / admin analytics  
**Area:** Admin analytics API, admin dashboard charts, date filtering, empty/error states  
**Priority:** P1

## Story

As an admin,  
I want analytics charts backed by stored events,  
so that user growth, WAU/upload volume, and OCR success/failure rates are trustworthy.

## Acceptance Criteria

1. `/admin/analytics` renders DB-backed user growth.
2. `/admin/analytics` renders WAU and upload volume.
3. `/admin/analytics` renders upload/OCR success and failure rate.
4. Charts support date range and empty/error states.
5. Stub/static chart data is removed.

## Tasks / Subtasks

- [x] Task 1 - Add analytics query endpoints/services over product events (AC: #1, #2, #3)
- [x] Task 2 - Wire `/admin/analytics` charts to real API data (AC: #1-#3)
- [x] Task 3 - Implement date range filtering and empty/error states (AC: #4)
- [x] Task 4 - Remove static/stub chart data (AC: #5)
- [x] Task 5 - Add backend query tests and frontend rendering verification (AC: #1-#5)

### Review Findings

- [x] [Review][Decision] Định nghĩa “success” chart khác drill-down — **Resolved:** Chart = `OCR_COMPLETED`/`OCR_FAILED`; drill-down join events on `e.created_at`; API `status=done` → `OCR_COMPLETED`; UI label “OCR thành công/thất bại”.
- [x] [Review][Decision] Không có backfill event — **Resolved:** `V050__backfill_ocr_terminal_activity_events.sql` idempotent từ `health_records` terminal.
- [x] [Review][Patch] Chuẩn hóa `failure_reason` trong SQL breakdown — **Resolved:** CASE trong `findUploadFailureBreakdown` khớp `FailureReasonNormalizer`.
- [x] [Review][Patch] Task 5 thiếu frontend test — **Resolved:** `uploadQualityAnalytics.test.ts` + label/empty-state updates.
- [x] [Review][Defer] Dead `AnalyticsRepository` upload-quality queries — **Resolved:** Removed; history uses `findUploadHistoryByTerminalOcrEvent` only.

## Dev Notes

### Implementation Guardrails

- Story 4.1 should exist before this story or this story must include a temporary migration path for existing events.
- Admin charts should never silently fall back to fake data.
- Use the existing admin auth/authorization model.

### Likely Files

- `apps/web/src/app/(admin)/admin/analytics/*`
- `apps/web/src/lib/api.ts`
- `apps/api/src/main/java/com/healthlens/api/controller/AdminAnalyticsController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AnalyticsService.java`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 4.2
- `_bmad-output/planning-artifacts/review-source/production-review/epic-8-analytics-spec.md`

## Dev Agent Record

### Agent Model Used

Composer

### Debug Log References

- Upload quality migrated from `health_records` to `user_activity_events` (`OCR_COMPLETED` / `OCR_FAILED`).
- User growth remains `users` table per epic-8 spec 8.1; WAU/upload already event-backed from 8.2.
- `/admin/analytics` redirects to `/admin` with three API-backed panels (unchanged routing).
- Post-review: drill-down aligned to terminal OCR events; V050 backfill; dead repository queries removed.

### Completion Notes List

- Added `findUploadQualityBuckets` and `findUploadFailureBreakdown` on `UserActivityEventRepository` (terminal OCR events only).
- `AnalyticsService.getUploadQuality` now reads event store; upload-history drill-down joins `user_activity_events` on event `created_at` (API `status=done` → `OCR_COMPLETED`).
- Removed unused `AdminGeneralStats` (superseded by `ActivityVolumePanel`).
- Extended `AnalyticsServiceTest` and `UserActivityEventRepositoryIntegrationTest` for event-backed quality aggregation.
- Frontend: `uploadQualityAnalytics.test.ts`; metric labels clarify OCR pipeline vs record `done`.
- Migration `V050` backfills terminal OCR events from historical `health_records` (idempotent; `failure_reason` matches `FailureReasonNormalizer`).

### File List

- `apps/api/src/main/resources/db/migration/V050__backfill_ocr_terminal_activity_events.sql`
- `apps/api/src/main/java/com/healthlens/api/repository/UserActivityEventRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/AnalyticsRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/AnalyticsService.java`
- `apps/api/src/test/java/com/healthlens/api/service/AnalyticsServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/repository/UserActivityEventRepositoryIntegrationTest.java`
- `apps/web/src/components/admin/UploadQualityPanel.tsx`
- `apps/web/src/components/admin/UploadHistoryModal.tsx`
- `apps/web/src/lib/admin/uploadQualityAnalytics.test.ts`
- `apps/web/src/components/admin/AdminGeneralStats.tsx` (deleted)

## Change Log

- 2026-05-22: Story 4.2 — event-backed OCR upload quality charts; remove dead admin stats component.
- 2026-05-22: Code review fixes — aligned drill-down, V050 backfill, SQL normalization, frontend tests, removed dead queries.
- 2026-05-22: Final review — staged V050 + frontend tests; backfill `failure_reason` CASE aligned with analytics SQL; story file list synced.
