# Story R5.4: Refactor Admin Reference Data Pages

Status: ready-for-dev

## Story

Là admin frontend developer,
tôi muốn admin reference-data pages được tách thành feature components/hooks,
để import, approval, và CRUD workflows dễ maintain.

## Acceptance Criteria

1. **Given** health-record extraction pattern đã proven **When** admin reference-data pages refactor **Then** page files thành route shells.
2. **Given** workflows có rủi ro y tế **When** components/hooks được extract **Then** preview, approve, reject, publish, error states vẫn explicit.
3. **Given** existing tests/smoke **When** refactor hoàn tất **Then** admin reference-data behavior vẫn pass hoặc gaps documented.

## Tasks / Subtasks

- [ ] Inventory admin reference-data pages and workflows. (AC: 1,2)
- [ ] Extract components for list/edit/import/approval sections. (AC: 1,2)
- [ ] Extract hooks/utilities for API orchestration. (AC: 1)
- [ ] Add/update tests for preview/approval paths. (AC: 3)

## Dev Notes

- Do not hide approval workflow behind overly generic components.
- Preserve admin notices and audit context.

### Project Structure Notes

- Main files: `apps/web/src/app/admin/reference-data/page.tsx`, `apps/web/src/app/admin/reference-data/approvals/page.tsx`, `apps/web/src/app/admin/reference-data/import/page.tsx`.

### References

- `docs/reference-data/core-feature-data-requirements.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

