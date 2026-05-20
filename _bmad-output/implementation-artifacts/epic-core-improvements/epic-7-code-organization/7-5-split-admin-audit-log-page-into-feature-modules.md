# Story 7.5: Split Admin Audit Log Page Into Feature Modules

Status: proposed

## Execution Scope

**Area:** Frontend admin audit surface, page decomposition, feature modules  
**Priority:** P2

## Story

As a frontend developer, I want the admin audit-log page split into focused modules, so that filters, citation review, CSV export, detail panels, and rendering can evolve without one oversized route file.

## Acceptance Criteria

1. **Given** the admin audit-log page loads, **When** filters, pagination, and detail drill-down are used, **Then** behavior matches the pre-refactor flow.
2. **Given** citation review and CSV export are used, **When** the refactor is complete, **Then** those actions still work without route-level regressions.
3. **Given** the page source is reviewed, **When** code is inspected, **Then** route composition is separated from feature hooks, helpers, and UI modules.

## Tasks / Subtasks

- [ ] Extract audit-log filter and query parameter helpers into a focused `lib` module.
- [ ] Extract audit-log table, detail modal, and pagination UI into feature components.
- [ ] Extract citation-review panel logic from the route page.
- [ ] Keep URL synchronization and export behavior unchanged.
- [ ] Preserve existing tests or add focused coverage where needed.

## Dev Notes

- This is a refactor story; do not redesign the admin UX.
- Keep route params, search params, and API behavior stable.
- Keep admin-specific session and auth behavior where it already exists.

## Likely Files

- `apps/web/src/app/admin/audit-log/page.tsx`
- `apps/web/src/components/admin/audit-log/`
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
