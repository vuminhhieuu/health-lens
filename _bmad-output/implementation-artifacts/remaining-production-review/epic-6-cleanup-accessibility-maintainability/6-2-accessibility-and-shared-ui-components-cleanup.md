# Story 6.2: Accessibility And Shared UI Components Cleanup

Status: review

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

- [x] Task 1 - Audit forms for label/input linkage (AC: #1)
- [x] Task 2 - Add live regions to dynamic status/error states (AC: #2)
- [x] Task 3 - Extract duplicated metric/reference components where appropriate (AC: #3)
- [x] Task 4 - Reuse loading/empty components consistently (AC: #4)
- [x] Task 5 - Run accessibility-focused verification on changed screens (AC: #1-#4)

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

Composer

### Completion Notes List

- Audited and fixed `htmlFor`/`id` linkage across dashboard auth flows, profile modals, consent, settings, follow-up reminders, and history filters.
- Wired `aria-live` / `role="alert"` / `role="status"` for async and form errors; extended shared `ErrorState` with `aria-live="assertive"`.
- Refactored `HealthMetricsGrid` to native `<button>` controls with descriptive `aria-label`; kept `ReferenceRangeIndicator` live region for loading explanations.
- Reused `LoadingState`, `EmptyState`, `ErrorState`, and `InlineFieldError` on invitation accept flows, follow-up reminders list states, and existing review/upload surfaces.
- Added `InvitationFlowShell` helpers and Vitest coverage for shared metric UI and auth accessibility wiring.

### File List

- `apps/web/src/components/ui/HealthMetricsGrid.tsx`
- `apps/web/src/components/ui/ReferenceRangeIndicator.tsx`
- `apps/web/src/components/ui/StateComponents.tsx`
- `apps/web/src/components/ui/health-metrics-shared.test.tsx`
- `apps/web/src/components/auth/InvitationFlowShell.tsx`
- `apps/web/src/components/features/profiles/CreateProfileModal.tsx`
- `apps/web/src/components/features/profiles/EditProfileModal.tsx`
- `apps/web/src/components/features/profiles/InviteMemberModal.tsx`
- `apps/web/src/components/features/consent/ConsentModal.tsx`
- `apps/web/src/components/features/upload/UploadButton.tsx`
- `apps/web/src/app/(auth)/invitations/accept/page.tsx`
- `apps/web/src/app/(auth)/health-record-invitations/accept/page.tsx`
- `apps/web/src/app/(auth)/auth-accessibility.test.ts`
- `apps/web/src/app/(auth)/login/page.tsx`
- `apps/web/src/app/(auth)/register/page.tsx`
- `apps/web/src/app/(auth)/forgot-password/page.tsx`
- `apps/web/src/app/(auth)/reset-password/page.tsx`
- `apps/web/src/app/(dashboard)/layout.tsx`
- `apps/web/src/app/(dashboard)/home/page.tsx`
- `apps/web/src/app/(dashboard)/visit-summary/page.tsx`
- `apps/web/src/app/(dashboard)/settings/profile/page.tsx`
- `apps/web/src/app/(dashboard)/settings/delete-account/page.tsx`
- `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx`
- `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx`
- `apps/web/src/app/(dashboard)/dashboard-accessibility.test.ts`

### Change Log

- 2026-05-21: Completed story 6.2 full-scope accessibility audit across auth, dashboard, and shared feature components.
