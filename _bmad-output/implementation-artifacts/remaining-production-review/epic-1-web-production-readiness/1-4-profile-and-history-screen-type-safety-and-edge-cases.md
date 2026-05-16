# Story 1.4: Profile And History Screen Type Safety And Edge Cases

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / web correctness  
**Area:** Profile screens, history screens, shared/deleted profile behavior, frontend type safety  
**Priority:** P1

## Story

As a user managing profiles and history,  
I want profile/history screens to handle edge cases safely,  
so that shared or deleted profile state does not show broken data.

## Acceptance Criteria

1. Unsafe casts in history/profile mapping are replaced with validated mapping functions.
2. Profile delete/set-default behavior is aligned with backend capabilities.
3. Profile CRUD forms do not reset before API success.
4. Shared profile access state is rendered consistently.

## Tasks / Subtasks

- [ ] Task 1 - Identify unsafe casts and mapping assumptions (AC: #1)
- [ ] Task 2 - Add typed mapping helpers with runtime guards (AC: #1)
- [ ] Task 3 - Align destructive/default-profile flows with backend behavior (AC: #2, #3)
- [ ] Task 4 - Normalize shared/deleted profile states (AC: #4)
- [ ] Task 5 - Test failure, shared access, deleted profile, and form-submit edge cases (AC: #1-#4)

## Dev Notes

### Implementation Guardrails

- Do not reset user-entered form data until the API call succeeds.
- Treat shared profile access separately from owned profile access.
- Prefer local mapper functions or shared DTO validators over broad `as` casts.

### Likely Files

- `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx`
- `apps/web/src/app/(dashboard)/profiles/*`
- `apps/web/src/lib/api.ts`
- `apps/api/src/main/java/com/healthlens/api/controller/ProfileController.java`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 1.4
- `_bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
