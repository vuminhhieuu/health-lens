# Story 6.1: Global Toast Notification Foundation

Status: ready-for-dev

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

- [ ] Add global toast provider.
- [ ] Add `notify.success/error/info/loading` wrapper.
- [ ] Define notification policy.
- [ ] Add accessibility behavior.
- [ ] Add tests or smoke checks.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
