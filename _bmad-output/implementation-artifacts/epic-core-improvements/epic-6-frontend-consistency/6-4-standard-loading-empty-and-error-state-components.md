# Story 6.4: Standard Loading, Empty And Error State Components

Status: ready-for-dev

## Execution Scope

**Area:** Frontend reusable state components, accessibility, UX consistency  
**Priority:** P1/P2

## Story

As a user, I want loading, empty, and error states to be clear and consistent, so that I know what is happening and what to do next.

## Acceptance Criteria

1. **Given** a query is loading, **When** page renders, **Then** standardized loading state appears without layout jump.
2. **Given** no health records exist, **When** user visits history, **Then** standardized empty state includes clear next action.
3. **Given** an error is recoverable, **When** error state appears, **Then** user sees retry or next-step action.
4. **Given** an error message is associated with an input, **When** screen reader is active, **Then** input is linked via `aria-describedby`.

## Tasks / Subtasks

- [ ] Create `LoadingState`, `EmptyState`, `ErrorState`, `InlineFieldError`.
- [ ] Apply to profile list, history, record review, upload/OCR status, admin reference data.
- [ ] Add accessibility patterns.
- [ ] Add visual smoke tests/manual checklist.

## Dev Notes

- Do not mix this with global toast work; page state components are persistent UI, not transient notifications.

## Likely Files

- `apps/web/src/components/ui/`
- `apps/web/src/app/(dashboard)/**`
- `apps/web/src/app/admin/reference-data/page.tsx`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 6.4

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
