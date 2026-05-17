# Story 6.1: Global Toast Notification Foundation

Status: done

## Execution Scope

**Area:** Frontend notification provider, accessibility, notification policy  
**Priority:** P1

## Story

As a user, I want success and failure notifications to look and behave consistently, so that I can trust app feedback without disruptive browser alerts.

## Acceptance Criteria

1. **Given** any page triggers success notification, **When** `notify.success` is called, **Then** a consistent toast appears with accessible live region.
2. **Given** background action fails, **When** `notify.error` is called, **Then** error toast uses standardized styling/copy and does not block UI.
3. **Given** a field validation error occurs, **When** form renders, **Then** field-level error remains near the field instead of global toast only.
4. **Given** toast is shown, **When** screen reader is active, **Then** announcement uses `aria-live`.

## Tasks / Subtasks

- [x] Add global toast provider.
- [x] Add `notify.success/error/info/loading` wrapper.
- [x] Define notification policy.
- [x] Add accessibility behavior.
- [x] Add tests or smoke checks.

### Review Findings

- [x] [Review][Patch] Toast width class emits invalid CSS for the intended mobile constraint [apps/web/src/components/notifications/ToastProvider.tsx:68]
- [x] [Review][Patch] ToastProvider tests leak the global notification singleton between cases [apps/web/src/components/notifications/ToastProvider.test.tsx:6]
- [x] [Review][Patch] `notify.getSnapshot()` exposes the mutable internal notification array [apps/web/src/lib/notify.ts:131]

## Dev Notes

- This is not the same as old error catch audit. It is UX foundation.

## Likely Files

- `apps/web/src/components/providers.tsx`
- `apps/web/src/lib/notify.ts`
- `apps/web/package.json`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 6.1

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `pnpm --dir apps/web exec vitest run src/lib/notify.test.ts src/components/notifications/ToastProvider.test.tsx` (red phase: failed on missing notification module/provider)
- `pnpm --dir apps/web exec vitest run src/lib/notify.test.ts src/components/notifications/ToastProvider.test.tsx` (green phase: 5 tests passed)
- `pnpm --dir apps/web test` (lint + Vitest passed)
- `pnpm --dir apps/web lint` (passed after removing toast test files)
- `pnpm --dir apps/web exec tsc --noEmit` (passed after Copilot review fixes)
- `pnpm --dir apps/web lint` (passed after Radix Toast refactor)
- `pnpm --dir apps/web exec tsc --noEmit` (passed after Radix Toast refactor)
- `pnpm --dir apps/web test` (passed with `--passWithNoTests` after Radix Toast refactor)

### Completion Notes List

- Added a dependency-free global notification store with `notify.success`, `notify.error`, `notify.info`, `notify.loading`, `dismiss`, `clear`, and snapshot/subscribe helpers.
- Added a global toast provider wired into the web app providers, with standardized success/error/info/loading styling and dismiss controls.
- Refactored the toast renderer to use `@radix-ui/react-toast` primitives while preserving the existing `notify.*` app API.
- Defined notification policy durations and explicit field validation guidance so field-level validation errors remain near fields instead of being replaced by global toasts.
- Added accessible toast announcements using `aria-live`, `aria-atomic`, `role="status"` for non-error messages, and `role="alert"` for errors.
- Removed the temporary toast Vitest files at user request; verification now relies on lint, TypeScript, and manual smoke checks.
- Resolved code review findings by correcting the responsive toast width class, isolating ToastProvider tests from the notification singleton, and returning snapshot copies from `notify.getSnapshot()`.
- Resolved Copilot PR review findings by awaiting clipboard copy failures and normalizing reset-password Vietnamese copy.

### File List

- `apps/web/src/components/notifications/ToastProvider.tsx`
- `apps/web/src/components/providers.tsx`
- `apps/web/src/lib/notify.ts`
- `apps/web/package.json`
- `pnpm-lock.yaml`

## Change Log

- 2026-05-17: Implemented global toast notification foundation and moved story to review.
- 2026-05-17: Addressed code review findings and moved story to done.
- 2026-05-17: Addressed Copilot PR comments and removed toast test files at user request.
- 2026-05-17: Refactored global toast rendering to use Radix Toast primitives.
