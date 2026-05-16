# Story 6.3: Frontend Bundle And Image Rule Cleanup

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / frontend maintainability  
**Area:** Bundle impact, image lint warnings, Next.js frontend hygiene  
**Priority:** P3

## Story

As a maintainer,  
I want frontend bundle and image warnings addressed,  
so that build output remains clean and predictable.

## Acceptance Criteria

1. `@radix-ui/themes` usage is reviewed for tree-shaking impact.
2. `@next/next/no-img-element` warnings are resolved or explicitly justified.
3. Bundle-impact changes are measured before/after where practical.

## Tasks / Subtasks

- [ ] Task 1 - Review `@radix-ui/themes` usage and import patterns (AC: #1)
- [ ] Task 2 - Resolve or justify raw `<img>` usages (AC: #2)
- [ ] Task 3 - Measure bundle/build impact where tooling exists (AC: #3)
- [ ] Task 4 - Document any intentional lint exceptions (AC: #2, #3)

## Dev Notes

### Implementation Guardrails

- Do not replace image handling mechanically if it breaks authenticated/private images.
- Measure before introducing broad component/library changes.
- Keep this story lower priority than production correctness and security stories.

### Likely Files

- `apps/web/src/**/*`
- `apps/web/next.config.*`
- `apps/web/eslint.config.*`
- `apps/web/package.json`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 6.3
- `_bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
