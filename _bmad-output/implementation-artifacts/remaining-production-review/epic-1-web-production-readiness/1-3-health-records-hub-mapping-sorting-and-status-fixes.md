# Story 1.3: Health Records Hub Mapping, Sorting And Status Fixes

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / web correctness  
**Area:** Health records list, status mapping, sorting, filters, state components  
**Priority:** P1

## Story

As a user browsing health records,  
I want record statuses, timestamps, and filters to be accurate,  
so that I can find and interpret records reliably.

## Acceptance Criteria

1. `lastUpdated` and status mappings match backend record state.
2. Sorting is deterministic and handles missing dates safely.
3. Filter labels use standardized Vietnamese with diacritics.
4. Empty/loading/error states use shared components from the UI consistency backlog.

## Tasks / Subtasks

- [ ] Task 1 - Audit list mapping against backend contract (AC: #1)
- [ ] Task 2 - Fix status and timestamp derivation (AC: #1, #2)
- [ ] Task 3 - Normalize Vietnamese filter labels (AC: #3)
- [ ] Task 4 - Reuse shared state components where available (AC: #4)
- [ ] Task 5 - Add tests or verification cases for missing dates and mixed statuses (AC: #1-#4)

## Dev Notes

### Implementation Guardrails

- Do not sort by unvalidated strings if backend dates can be null or malformed.
- Keep Vietnamese copy aligned with core language normalization story.
- Avoid changing backend status values unless the API contract is explicitly updated.

### Likely Files

- `apps/web/src/app/(dashboard)/health-records/page.tsx`
- `apps/web/src/lib/api.ts`
- `apps/api/src/main/java/com/healthlens/api/dto/response/*HealthRecord*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 1.3
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Epic 6

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
