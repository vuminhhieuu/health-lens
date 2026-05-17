# Story 6.4: Standard Loading, Empty And Error State Components

Status: done

## Execution Scope

**Area:** Frontend reusable state components, accessibility, UX consistency  
**Priority:** P1/P2

## Story

As a user, I want loading, empty, and error states to be clear and consistent, so that I know what is happening and what to do next.

## Acceptance Criteria

1. **Given** a query is loading, **When** page renders, **Then** standardized loading state appears without layout jump.
2. **Given** no health records exist, **When** user visits history, **Then** standardized empty state includes clear next action.
3. **Given** an error is recoverable, **When** error state appears, **Then** user sees retry or next-step action.
4. **Given** an error message is associated with an input, **When** screen reader is active, **Then** input is linked via `aria-describedby`.

## Tasks / Subtasks

- [x] Create `LoadingState`, `EmptyState`, `ErrorState`, `InlineFieldError`.
- [x] Apply to profile list, history, record review, upload/OCR status, admin reference data.
- [x] Add accessibility patterns.
- [x] Add visual smoke tests/manual checklist.

### Review Findings

- [x] [Review][Patch] Profile list ignores loading/error states for related profile queries [apps/web/src/app/(dashboard)/profiles/page.tsx:97]
- [x] [Review][Patch] History empty state can render without a next action for read-only viewers [apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx:311]
- [x] [Review][Patch] Add-metric select is not linked to its inline validation error [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:1745]
- [x] [Review][Patch] Add-metric shared error marks multiple controls invalid instead of the failing control [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx:1762]
- [x] [Review][Patch] Reference metric form error is only associated with the English-name input [apps/web/src/app/admin/reference-data/page.tsx:755]
- [x] [Review][Patch] State component class overrides rely on conflicting Tailwind utility order [apps/web/src/components/ui/StateComponents.tsx:22]
- [x] [Review][Patch] State action props allow incomplete action configuration [apps/web/src/components/ui/StateComponents.tsx:10]

## Dev Notes

- Do not mix this with global toast work; page state components are persistent UI, not transient notifications.

## Likely Files

- `apps/web/src/components/ui/`
- `apps/web/src/app/(dashboard)/**`
- `apps/web/src/app/admin/reference-data/page.tsx`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 6.4

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `cd apps/web && pnpm exec eslint src/components/ui/StateComponents.tsx src/app/admin/reference-data/page.tsx src/components/admin/UploadQualityPanel.tsx src/components/admin/UploadHistoryModal.tsx 'src/app/(dashboard)/profiles/page.tsx' 'src/app/(dashboard)/profiles/[profileId]/history/page.tsx' 'src/app/(dashboard)/health-records/review/[recordId]/page.tsx'`
- `cd apps/web && pnpm test`
- `cd apps/web && pnpm exec tsc --noEmit` failed on pre-existing unresolved `recharts` / `@radix-ui/react-toast` type resolution and implicit-any issues in chart/toast files.
- Code review patch pass: targeted ESLint and `cd apps/web && pnpm test`.

### Completion Notes List

- Added reusable persistent page-state components for loading, empty, recoverable error, and inline field errors.
- Replaced ad hoc state UI in profile list, profile history, record review/OCR processing, admin upload quality/history, and admin reference data.
- Added accessible roles/live regions plus `aria-describedby` links for inline field errors in record metric add and reference metric edit flows.
- Embedded a manual visual smoke checklist for QA.
- Resolved code review findings by covering related profile query states, adding a read-only history next action, fixing field-specific inline error wiring, and hardening state component class/action APIs.

### Visual Smoke Checklist

**Loading**

- Profile list keeps a stable centered loading panel while profiles load.
- History page keeps a stable loading panel while health records load.
- Record review keeps a stable OCR processing panel without content jump.
- Admin reference data and upload quality panels use the same loading treatment.

**Empty**

- Profile list empty state includes a visible "Tạo hồ sơ đầu tiên" action.
- History empty state includes a visible upload action when the viewer can upload.
- Upload quality and upload history empty states explain that no data matches the current range.
- Admin reference metric groups use the shared empty state when a group has no metrics.

**Error**

- Recoverable query errors show a persistent error panel with a retry or next-step action.
- Record review missing-data state sends the user back to the health records list.

**Accessibility**

- Loading panels expose `role="status"` with polite live updates.
- Error panels expose `role="alert"`.
- Inline validation errors expose stable IDs and affected inputs set `aria-describedby`.

### File List

- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-6-frontend-consistency/6-4-standard-loading-empty-and-error-state-components.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `apps/web/src/components/ui/StateComponents.tsx`
- `apps/web/src/components/ui/index.ts`
- `apps/web/src/app/(dashboard)/profiles/page.tsx`
- `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `apps/web/src/app/admin/reference-data/page.tsx`
- `apps/web/src/components/admin/UploadQualityPanel.tsx`
- `apps/web/src/components/admin/UploadHistoryModal.tsx`

## Change Log

- 2026-05-17: Implemented standardized frontend state components, applied them to scoped high-traffic flows, added accessibility wiring, and visual smoke checklist.
- 2026-05-17: Removed temporary StateComponents test file per request.
- 2026-05-17: Addressed code review findings and marked story done.
