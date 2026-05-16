# Story 5.2: Mobile Minimum Functional Surface

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / mobile readiness  
**Area:** Mobile dashboard, upload/OCR, results, profiles, settings, family sharing, CI  
**Priority:** P1 if mobile is in release scope

## Story

As a mobile user,  
I want the basic HealthLens flows to work on mobile,  
so that dashboard, upload/OCR, results, profiles, settings, and family sharing are usable.

## Acceptance Criteria

1. Mobile dashboard, upload OCR, results, profiles, settings, and family sharing routes are implemented or explicitly scoped out.
2. Camera OCR path has permission/error handling.
3. Offline-readonly history behavior is aligned with existing mobile epic.
4. Mobile CI covers build and smoke tests.

## Tasks / Subtasks

- [ ] Task 1 - Inventory implemented and missing mobile routes (AC: #1)
- [ ] Task 2 - Implement or scope out each minimum mobile flow (AC: #1)
- [ ] Task 3 - Harden camera/OCR permission and error handling (AC: #2)
- [ ] Task 4 - Align offline history behavior with mobile epic 9 (AC: #3)
- [ ] Task 5 - Add mobile build/smoke CI coverage if mobile is in scope (AC: #4)

## Dev Notes

### Implementation Guardrails

- This story should not be started until Story 5.1 confirms mobile is in release scope.
- If mobile remains deferred, keep this story `ready-for-dev` but out of the active sprint.
- Do not claim feature parity without route-level verification.

### Likely Files

- `apps/mobile/*`
- `.github/workflows/*`
- `_bmad-output/implementation-artifacts/epic-9/*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 5.2
- `_bmad-output/implementation-artifacts/epic-9/9-1-mobile-camera-capture-ux.md`
- `_bmad-output/implementation-artifacts/epic-9/9-2-mobile-offline-readonly-history.md`
- `_bmad-output/implementation-artifacts/epic-9/9-3-auto-sync-when-back-online.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
