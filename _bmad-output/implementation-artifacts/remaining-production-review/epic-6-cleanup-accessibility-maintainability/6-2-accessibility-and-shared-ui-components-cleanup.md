# Story 6.2: Accessibility And Shared UI Components Cleanup

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / accessibility and UI reuse  
**Area:** Forms, async state accessibility, shared health metric components  
**Priority:** P2

## Story

As a user relying on assistive technology,  
I want forms and dynamic states to be accessible,  
so that HealthLens remains usable beyond visual interaction.

## Acceptance Criteria

1. Form labels have correct `htmlFor/id`.
2. Async status/error states use appropriate live regions.
3. Shared HealthMetricsGrid and ReferenceRangeIndicator are extracted where duplication exists.
4. Loading skeleton and empty state components are reused consistently.

## Tasks / Subtasks

- [ ] Task 1 - Audit forms for label/input linkage (AC: #1)
- [ ] Task 2 - Add live regions to dynamic status/error states (AC: #2)
- [ ] Task 3 - Extract duplicated metric/reference components where appropriate (AC: #3)
- [ ] Task 4 - Reuse loading/empty components consistently (AC: #4)
- [ ] Task 5 - Run accessibility-focused verification on changed screens (AC: #1-#4)

## Dev Notes

### Implementation Guardrails

- Keep shared components small and shaped by real duplication.
- Do not change medical meaning or copy while doing accessibility cleanup unless covered by medical copy review.
- Coordinate with core frontend consistency stories to avoid duplicate component work.

### Likely Files

- `apps/web/src/components/*`
- `apps/web/src/app/(dashboard)/**/*`
- `apps/web/src/app/(auth)/**/*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 6.2
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Epic 6

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
