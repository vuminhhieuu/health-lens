# Story 1.4: Profile And History Screen Type Safety And Edge Cases

Status: done

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

- [x] Task 1 - Identify unsafe casts and mapping assumptions (AC: #1)
- [x] Task 2 - Add typed mapping helpers with runtime guards (AC: #1)
- [x] Task 3 - Align destructive/default-profile flows with backend behavior (AC: #2, #3)
- [x] Task 4 - Normalize shared/deleted profile states (AC: #4)
- [x] Task 5 - Test failure, shared access, deleted profile, and form-submit edge cases (AC: #1-#4)

### Review Findings

- [x] [Review][Patch] Shared-profile optimistic update is not rolled back on mutation failure [apps/web/src/app/(dashboard)/profiles/page.tsx:194]
- [x] [Review][Patch] Edit form draft is overwritten before API success on failed update [apps/web/src/app/(dashboard)/profiles/page.tsx:170]
- [x] [Review][Patch] Deleted or stale shared profile can block the whole profiles page [apps/web/src/app/(dashboard)/profiles/page.tsx:300]
- [x] [Review][Patch] Profile limit counts the default profile although the UI limit is for family profiles [apps/web/src/app/(dashboard)/profiles/page.tsx:283]
- [x] [Review][Patch] Create modal can close while submit is pending and lose draft data before API result [apps/web/src/components/features/profiles/CreateProfileModal.tsx:64]
- [x] [Review][Patch] Shared access mapper silently downgrades unknown or whitespace-padded access levels to view [apps/web/src/lib/profileMappings.ts:106]

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

GPT-5 Codex

### Debug Log References

- 2026-05-19: `pnpm exec vitest run src/lib/profileMappings.test.ts src/components/features/profiles/CreateProfileModal.test.tsx` - pass (5 tests, before frontend test files were removed by request).
- 2026-05-19: `pnpm test` - pass; existing warning remains in `apps/web/src/app/admin/audit-log/page.tsx` for unused `actionCategoryVi`.
- 2026-05-19: `pnpm exec tsc --noEmit` - blocked by pre-existing missing type/module errors for `recharts` and `@radix-ui/react-toast` plus unrelated implicit-any chart/toast callbacks.
- 2026-05-19: `pnpm test` - pass after review fixes (6 tests, before frontend test files were removed by request); existing warning remains in `apps/web/src/app/admin/audit-log/page.tsx` for unused `actionCategoryVi`.
- 2026-05-19: `./gradlew test --tests com.healthlens.api.service.ProfileServiceTest` - not run; local environment cannot locate a Java Runtime.
- 2026-05-19: Addressed Copilot PR review comments for mapper trimming/status normalization and stale shared-profile latest-record query filtering.

### Completion Notes List

- Replaced profile/history API response casts with guarded mapper functions that filter malformed or deleted/shared rows and normalize shared access to `view`/`edit`.
- Updated profile history to derive shared profile names from the shared-profile response and use the shared mapper for access checks, keeping owned and shared access separate.
- Removed the history status fallback `as unknown` cast by carrying `recordStatus` and `verificationStatus` through the guarded history mapper.
- Fixed create-profile form behavior so user-entered data remains after submit while the modal stays open and resets only after the modal closes.
- Confirmed the current web UI does not expose unsupported profile delete or set-default controls; profile mutation behavior remains aligned with existing backend create/update/ensure-default capabilities.
- Resolved review findings by removing profile edit optimistic cache mutation, isolating shared-profile query failures from the owned profile screen, skipping stale backend shared profile rows, counting only non-default family profiles for the UI limit, preventing create-modal close during pending submit, and preserving unknown shared access as an explicit fail-closed state.
- Resolved Copilot review comments by trimming mapped string fields, trimming status before history status comparisons, and filtering shared-profile IDs to existing profiles before latest-record lookup with a stale-share warning log.

### File List

- `apps/web/src/lib/profileMappings.ts`
- `apps/web/src/app/(dashboard)/profiles/page.tsx`
- `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx`
- `apps/web/src/components/features/profiles/CreateProfileModal.tsx`
- `apps/api/src/main/java/com/healthlens/api/service/ProfileService.java`
- `apps/api/src/test/java/com/healthlens/api/service/ProfileServiceTest.java`

### Change Log

- 2026-05-19: Implemented profile/history runtime mapping guards, shared access normalization, delayed create-form reset behavior, and focused web tests.
- 2026-05-19: Addressed code review findings; story moved to done.
