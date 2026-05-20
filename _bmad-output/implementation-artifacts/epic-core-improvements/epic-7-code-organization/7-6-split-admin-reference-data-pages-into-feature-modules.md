# Story 7.6: Split Admin Reference Data Pages Into Feature Modules

Status: proposed

## Execution Scope

**Area:** Frontend admin reference-data surface, page decomposition, shared feature modules  
**Priority:** P2

## Story

As a frontend developer, I want admin reference-data pages reorganized into feature modules, so that query logic, payload builders, modals, and approval flows stay maintainable as the admin surface grows.

## Acceptance Criteria

1. **Given** the reference-data list, approval, and import flows run, **When** the refactor is complete, **Then** behavior remains unchanged.
2. **Given** a developer reviews the source tree, **When** navigating the admin reference-data feature, **Then** shared query keys, helper functions, and modal components are no longer embedded inside route pages.
3. **Given** import preview or approval actions are exercised, **When** tests or manual verification run, **Then** no regressions appear in admin workflow state transitions.

## Tasks / Subtasks

- [ ] Extract reference-data query keys and API helper functions into shared feature modules.
- [ ] Extract metric editor, diff/approval, and import-preview UI pieces into focused components.
- [ ] Reduce page files so they mainly coordinate route-level composition.
- [ ] Preserve existing admin workflow behavior and validation messages.
- [ ] Add or update focused coverage if the current tests are too page-coupled.

## Dev Notes

- This is a refactor story; do not alter approval rules or import business logic.
- Keep compatibility with existing admin route constants and API contracts.
- Avoid mixing backend service restructuring into this frontend story.

## Likely Files

- `apps/web/src/app/admin/reference-data/page.tsx`
- `apps/web/src/app/admin/reference-data/approvals/page.tsx`
- `apps/web/src/app/admin/reference-data/import/page.tsx`
- `apps/web/src/components/admin/reference-data/`
- `apps/web/src/hooks/admin/`
- `apps/web/src/lib/admin/`

## References

- `epic-7-issues-and-proposed-stories.md`
- `code-organization-assessment.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
