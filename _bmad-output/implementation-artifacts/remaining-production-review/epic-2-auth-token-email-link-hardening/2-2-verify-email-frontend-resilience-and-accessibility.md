# Story 2.2: Verify Email Frontend Resilience And Accessibility

Status: ready-for-dev

## Execution Scope

**Phase:** Remaining production review / auth UX hardening  
**Area:** Verify email page, async lifecycle, accessibility, runtime error parsing  
**Priority:** P2

## Story

As a user opening a verification link,  
I want clear loading, retry, and accessibility behavior,  
so that slow or failed verification is understandable.

## Acceptance Criteria

1. Suspense/loading state uses a skeleton or stable loading component.
2. Request uses cleanup/abort behavior on unmount.
3. Expired/used token state offers a resend-verification path where supported.
4. Async state changes use `aria-live`/`role="status"` where appropriate.
5. Axios/backend errors are parsed with runtime-safe guards.

## Tasks / Subtasks

- [ ] Task 1 - Replace unstable loading UI with shared loading component (AC: #1)
- [ ] Task 2 - Add request cleanup/abort behavior (AC: #2)
- [ ] Task 3 - Add resend path where backend supports it (AC: #3)
- [ ] Task 4 - Add live region semantics and safe error parsing (AC: #4, #5)
- [ ] Task 5 - Verify with keyboard/screen-reader semantics and slow network cases (AC: #1-#5)

## Dev Notes

### Implementation Guardrails

- Do not expose token-specific backend details to the user.
- Avoid browser `alert`; use the shared toast/status pattern from core frontend consistency stories.
- Runtime guards should handle unknown error shapes without throwing from the error path.

### Likely Files

- `apps/web/src/app/(auth)/verify-email/page.tsx`
- `apps/web/src/components/ui/*`
- `apps/web/src/lib/api.ts`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 2.2
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 6.1 and 6.4

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
