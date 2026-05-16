# Story 6.1: Frontend Dead Code And Export Cleanup

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / cleanup  
**Area:** Frontend dead exports, unused imports, lint guardrails  
**Priority:** P2

## Story

As a developer,  
I want dead exports and unused code removed,  
so that the frontend codebase is easier to maintain.

## Acceptance Criteria

1. Empty barrel files are removed or populated with intentional exports.
2. Dead `API_TIMEOUT` re-export is removed or documented.
3. Unused imports and redundant `console.error` calls are cleaned where safe.
4. Lint rules catch reintroduced obvious unused exports.

## Tasks / Subtasks

- [ ] Task 1 - Inventory empty barrels and dead exports (AC: #1, #2)
- [ ] Task 2 - Remove or document dead exports (AC: #1, #2)
- [ ] Task 3 - Clean unused imports/redundant console usage where safe (AC: #3)
- [ ] Task 4 - Tighten lint guardrails if missing (AC: #4)
- [ ] Task 5 - Run frontend lint/typecheck where available (AC: #1-#4)

## Dev Notes

### Implementation Guardrails

- Keep cleanup mechanical and low-risk; avoid unrelated refactors.
- Do not remove exports that are consumed by planned generated code without checking references.
- Preserve developer-facing diagnostics that are intentionally useful.

### Likely Files

- `apps/web/src/**/*`
- `apps/web/eslint.config.*`
- `apps/web/package.json`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 6.1
- `_bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
