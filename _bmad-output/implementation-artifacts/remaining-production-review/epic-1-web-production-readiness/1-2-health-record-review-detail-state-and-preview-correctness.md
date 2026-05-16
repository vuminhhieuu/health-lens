# Story 1.2: Health Record Review Detail State And Preview Correctness

Status: ready-for-dev

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

- [ ] Task 1 - Normalize review detail state rendering (AC: #1, #3, #4)
  - [ ] Audit all status branches for preview, metric table, explanation, and actions.
  - [ ] Extract shared sections where duplication causes drift.
- [ ] Task 2 - Preserve original document preview (AC: #1)
  - [ ] Render preview across supported states when file URL/access exists.
  - [ ] Add explicit unavailable state when file access is missing.
- [ ] Task 3 - Remove unsafe actions and duplicated content (AC: #2, #4)
  - [ ] Hide or disable share action for pending/failed review states.
  - [ ] Ensure AI explanation has one canonical location.
- [ ] Task 4 - Error handling and telemetry (AC: #5)
  - [ ] Replace empty catch blocks with visible error feedback.
  - [ ] Log non-sensitive error context with correlation id where available.
- [ ] Task 5 - Tests (AC: #1-#5)
  - [ ] Verify `processing`, `ocr_failed`, `review_required`, and `done` rendering.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
