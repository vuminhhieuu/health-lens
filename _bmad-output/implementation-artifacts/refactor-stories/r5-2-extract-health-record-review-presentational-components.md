# Story R5.2: Extract Health Record Review Presentational Components

Status: ready-for-dev

## Story

Là frontend developer,
tôi muốn tách các section UI thuần khỏi health-record review page,
để route page dễ đọc hơn mà không đổi behavior.

## Acceptance Criteria

1. **Given** review page chứa UI sections inline **When** components được extract **Then** header, metric panels, explanation/recommendation, status/error/empty, action sections nằm trong feature components.
2. **Given** user-visible UX hiện tại **When** refactor hoàn tất **Then** route behavior và visible UX không đổi.
3. **Given** page có accessibility/health status semantics **When** components được extract **Then** labels/status/disclaimer vẫn preserved.

## Tasks / Subtasks

- [ ] Inventory UI sections trong review page. (AC: 1)
- [ ] Create feature component folder. (AC: 1)
- [ ] Extract pure/presentational components first. (AC: 1,2)
- [ ] Run web tests and targeted smoke if available. (AC: 2,3)

## Dev Notes

- Không extract data hooks trong story này; đó là R5.3.
- Keep route file as composition shell but avoid huge behavior rewrite.

### Project Structure Notes

- Main file: `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`.

### References

- `review/current-project-refactor-audit-2026-05-26.md`
- `_bmad-output/planning-artifacts/ux-design-specification.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

