# Story 7.2: Split Health Record Review Page Into Feature Components And Hooks

Status: ready-for-dev

## Execution Scope

**Area:** Frontend refactor, record review page, hooks/components  
**Priority:** P1/P2

## Story

As a frontend developer, I want the record review page split into focused hooks and components, so that OCR failure, metric editing, saving, and metadata forms can evolve safely.

## Acceptance Criteria

1. **Given** record review page loads, **When** user edits and saves metrics, **Then** behavior matches pre-refactor flow.
2. **Given** OCR failed, **When** user chooses manual entry or keep partial, **Then** extracted components handle the same transitions.
3. **Given** tests run, **When** refactor is complete, **Then** save/edit/delete/retry flows remain covered.
4. **Given** page component is reviewed, **When** code is inspected, **Then** API calls/state/rendering are separated into focused modules.

## Tasks / Subtasks

- [ ] Extract `useRecordReviewState`.
- [ ] Extract `useRecordSave`.
- [ ] Extract `MetricTable`.
- [ ] Extract `RecordMetadataForm`.
- [ ] Extract `OcrFailurePanel` and `ReviewActions`.
- [ ] Preserve route behavior and tests.

## Dev Notes

- This is a refactor story; do not change business rules unless explicitly required by active OCR stories.
- Watch for accidental regression in manual/partial save flow.

## Likely Files

- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `apps/web/src/components/features/health-records/`
- `apps/web/src/hooks/`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 7.2

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
