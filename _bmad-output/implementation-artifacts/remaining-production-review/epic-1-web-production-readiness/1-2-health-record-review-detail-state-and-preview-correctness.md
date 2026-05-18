# Story 1.2: Health Record Review Detail State And Preview Correctness

Status: done

## Execution Scope

**Phase:** Remaining production review / web correctness  
**Area:** Health record review detail, document preview, AI explanation placement, telemetry  
**Priority:** P0

## Story

As a user reviewing a health record,  
I want the original document and extracted AI/metric context to remain visible across processing, failed, review, and done states,  
so that I can verify medical values before trusting them.

## Context

Review findings call out inconsistent state rendering in the health record review detail page. A medical review screen must preserve the original document preview whenever permissions allow it, avoid unsafe sharing affordances before review is complete, and avoid duplicated explanation sections.

## Acceptance Criteria

1. Original document preview is available in `processing`, `ocr_failed`, `review_required`, and `done` states where file permissions allow it.
2. Share action is removed from contexts where sharing a pending review result is unsafe or confusing.
3. AI explanation is positioned consistently with the intended review flow.
4. Explanation sections are not duplicated or fragmented.
5. Empty catch blocks are replaced with user-visible errors and telemetry.

## Tasks / Subtasks

- [x] Task 1 - Normalize review detail state rendering (AC: #1, #3, #4)
  - [x] Audit all status branches for preview, metric table, explanation, and actions.
  - [x] Extract shared sections where duplication causes drift.
- [x] Task 2 - Preserve original document preview (AC: #1)
  - [x] Render preview across supported states when file URL/access exists.
  - [x] Add explicit unavailable state when file access is missing.
- [x] Task 3 - Remove unsafe actions and duplicated content (AC: #2, #4)
  - [x] Hide or disable share action for pending/failed review states.
  - [x] Ensure AI explanation has one canonical location.
- [x] Task 4 - Error handling and telemetry (AC: #5)
  - [x] Replace empty catch blocks with visible error feedback.
  - [x] Log non-sensitive error context with correlation id where available.
- [x] Task 5 - Tests (AC: #1-#5)
  - [x] Verify `processing`, `ocr_failed`, `review_required`, and `done` rendering.

### Review Findings

- [x] [Review][Patch] Fullscreen preview action is nonfunctional in early-return states [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:971]
- [x] [Review][Patch] Fullscreen preview action is nonfunctional in the confirmed result view [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:1169]
- [x] [Review][Patch] AI explanation remains split across popup and HealthMetricCard implementations [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:411]
- [x] [Review][Patch] OCR failure preview nests a preview card inside the OCR warning card [apps/web/src/components/features/upload/OcrFailureScreen.tsx:51]
- [x] [Review][Patch] Client telemetry logs potentially sensitive record id and raw error messages [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:210]
- [x] [Review][Patch] Error telemetry can throw if a rejected promise provides null or undefined [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:210]
- [x] [Review][Patch] PDF detection depends on `.pdf` appearing anywhere in the URL [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:827]
- [x] [Review][Patch] Existing but expired or forbidden file URLs render broken media instead of unavailable state [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:863]
- [x] [Review][Patch] Fullscreen modal uses untrimmed file URL while inline preview uses trimmed URL [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:1056]
- [x] [Review][Patch] Share hidden behavior lacks explicit regression coverage for pending or failed states [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.test.tsx:98]

## Dev Notes

### Implementation Guardrails

- Do not hide the source document just because OCR failed; failed OCR is exactly when manual verification matters.
- Do not add new page-level cards inside existing cards. Keep layout consistent with the current dashboard shell.
- Coordinate shared loading/error components with core Story 6.4 if implemented in parallel.

### Likely Files

- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `apps/web/src/components/health-records/*`
- `apps/web/src/lib/api.ts`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 1.2
- `_bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md`
- `_bmad-output/planning-artifacts/review-source/PRODUCTION-READINESS.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-05-18T11:20:00+07:00 - Started implementation; story and sprint status moved to in-progress.
- 2026-05-18T16:19:00+07:00 - Added shared source document preview rendering, guarded sharing to confirmed records, replaced silent catches with non-sensitive telemetry, and added state coverage tests.
- 2026-05-18T16:54:00+07:00 - Batch-applied all code review patches and moved story/sprint status to done.

### Completion Notes List

- Source document preview now appears in processing, OCR failed, review required/manual review, and confirmed result views when `fileUrl` is available.
- Missing file access now renders an explicit unavailable state instead of an empty image/iframe.
- Record sharing is only exposed for confirmed `done` records owned by the user.
- Retry upload, delete, and PDF download failures now show user feedback and log non-sensitive action context with correlation/request id when present.
- Added Vitest coverage for `processing`, `ocr_failed`, `review_required` missing-file, and `done` preview behavior.
- Resolved code review findings: shared fullscreen modal works in all preview states, preview failures fall back to unavailable UI, telemetry avoids record ids/raw messages, PDF detection uses URL pathname, OCR failure layout avoids nested cards, and share-hidden states have regression coverage.

### File List

- apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx
- apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.test.tsx
- apps/web/src/components/features/upload/OcrFailureScreen.tsx

### Change Log

- 2026-05-18 - Implemented health record review detail state and preview correctness; story moved to review.
- 2026-05-18 - Addressed code review findings; story moved to done.
