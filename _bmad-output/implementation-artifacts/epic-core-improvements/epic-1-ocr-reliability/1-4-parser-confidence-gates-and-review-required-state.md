# Story 1.4: Parser Confidence Gates And Review Required State

Status: ready-for-dev

## Execution Scope

**Phase:** Core feature improvement / OCR reliability  
**Area:** Backend OCR/parser, health record status, review UI, metric provenance  
**Priority:** P0

## Story

As a user reviewing extracted health metrics,  
I want low-confidence OCR/parse results to be clearly flagged,  
so that incorrect medical values are not treated as reliable.

## Context

Current parsing relies heavily on line/text heuristics and can misread values, units, or reference ranges. For health data, uncertain extraction must be surfaced to the user as review-required, not silently treated as complete.

This story adds confidence gates and review-required state so OCR/parser uncertainty becomes a product-visible workflow.

## Acceptance Criteria

1. **Given** OCR confidence is below configured threshold, **When** metrics are parsed, **Then** the record status becomes `review_required` and the user sees clear reasons before saving.
2. **Given** parser cannot confidently identify unit or reference range, **When** review page loads, **Then** the affected metric is highlighted for correction.
3. **Given** parser detects ambiguous table layout, **When** the record is processed, **Then** the result remains editable/review-required instead of auto-finalized.
4. **Given** the user confirms corrected values, **When** the record is saved, **Then** metric source captures OCR/manual/corrected provenance.
5. **Given** extraction confidence is sufficient, **When** review page loads, **Then** the user still has the ability to edit before final confirmation.

## Tasks / Subtasks

- [ ] Task 1 - Define confidence/failure categories (AC: #1-#3)
  - [ ] `low_ocr_confidence`
  - [ ] `low_parse_confidence`
  - [ ] `ambiguous_table_layout`
  - [ ] `missing_reference_range`
  - [ ] `unit_mismatch`
- [ ] Task 2 - Add review-required status/state handling (AC: #1, #3, #5)
  - [ ] Persist review-required reason(s).
  - [ ] Ensure status transitions do not bypass user confirmation.
  - [ ] Keep existing OCR failure/manual entry behavior compatible.
- [ ] Task 3 - Add parser confidence scoring (AC: #1-#3)
  - [ ] Score metric name match confidence.
  - [ ] Score value/unit/reference range extraction confidence.
  - [ ] Mark ambiguous rows/lines when table layout cannot be trusted.
- [ ] Task 4 - Add metric provenance (AC: #4)
  - [ ] Store whether each metric is OCR-extracted, manually entered, or user-corrected.
  - [ ] Preserve original OCR value where useful for audit/debugging.
- [ ] Task 5 - Update review UI (AC: #1-#5)
  - [ ] Show review-required banner/reasons.
  - [ ] Highlight affected metrics.
  - [ ] Keep save/edit flow clear and accessible.
- [ ] Task 6 - Tests (AC: #1-#5)
  - [ ] Low OCR confidence test.
  - [ ] Missing unit/reference range test.
  - [ ] Ambiguous layout test.
  - [ ] User correction/provenance test.

## Dev Notes

### Implementation Guardrails

- Do not let low-confidence results flow directly into AI explanation/recommendation as if they are final.
- Keep thresholds configurable, with conservative defaults.
- Do not block manual entry. Low confidence should guide review, not dead-end the user.
- If schema changes are needed for provenance/reasons, add migrations and response DTO updates.
- Avoid relying only on aggregate OCR confidence; metric-level parse confidence matters.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/entity/HealthRecord.java`
- `apps/api/src/main/java/com/healthlens/api/dto/MetricDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/HealthRecordDetailResponse.java`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `apps/web/src/components/features/upload/OcrFailureScreen.tsx`
- `apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`

### Dependencies

- Strongly benefits from Story 1.2 normalized OCR contract.
- Can be implemented after Story 1.1 if contract work is staged.

### References

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 3
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 1.4
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
