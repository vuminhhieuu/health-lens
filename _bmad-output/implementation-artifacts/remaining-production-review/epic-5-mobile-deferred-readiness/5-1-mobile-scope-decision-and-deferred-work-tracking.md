# Story 5.1: Mobile Scope Decision And Deferred Work Tracking

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / mobile scope management  
**Area:** Release scope, deferred work docs, mobile dependency/build status  
**Priority:** P1 if mobile is in scope, otherwise P2

## Story

As a product owner,  
I want mobile scope explicitly decided,  
so that production readiness does not silently assume mobile is ready.

## Acceptance Criteria

1. Mobile release scope is documented as in-scope or deferred.
2. Deferred mobile findings are linked from sprint/deferred-work docs.
3. Missing `@healthlens/shared` dependency is resolved or explicitly deferred.
4. Mobile CI/EAS Build status is documented.

## Tasks / Subtasks

- [ ] Task 1 - Decide and document mobile release scope (AC: #1)
- [ ] Task 2 - Link deferred mobile work from BMad implementation docs (AC: #2)
- [ ] Task 3 - Resolve or explicitly defer `@healthlens/shared` dependency issue (AC: #3)
- [ ] Task 4 - Document mobile CI/EAS status (AC: #4)
- [ ] Task 5 - Update sprint/deferred tracking (AC: #1-#4)

## Dev Notes

### Implementation Guardrails

- Do not mark the whole product production-ready while mobile status is ambiguous.
- If mobile is deferred, make that visible in sprint/deferred-work artifacts.
- If mobile is in scope, Story 5.2 becomes a prerequisite for release.

### Likely Files

- `_bmad-output/implementation-artifacts/deferred-work.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `apps/mobile/*`
- `package.json`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 5.1
- `_bmad-output/planning-artifacts/review-source/PRODUCTION-READINESS.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
