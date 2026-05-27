# Story R5.1: Define Web Feature Extraction Pattern

Status: ready-for-dev

## Story

Là frontend developer,
tôi muốn có pattern extraction cho App Router pages,
để các page lớn được refactor nhất quán.

## Acceptance Criteria

1. **Given** web pages trộn route shell, data fetching, orchestration, UI **When** pattern được viết **Then** nó định nghĩa responsibilities cho route shell, feature components, hooks, lib, và tests.
2. **Given** health-record review page là ứng dụng đầu tiên **When** pattern được đọc **Then** nó nêu rõ cách áp dụng cho page đó.
3. **Given** refactor không được đổi UX **When** pattern hoàn tất **Then** nó nêu rule preserve loading/error/empty states và accessibility.

## Tasks / Subtasks

- [ ] Inspect current large pages and feature folders. (AC: 1)
- [ ] Write web extraction pattern in roadmap/docs. (AC: 1)
- [ ] Include health-record review page example target structure. (AC: 2)
- [ ] Define test/smoke expectations. (AC: 3)

## Dev Notes

- Không refactor page code trong story này.
- Không introduce UI library mới.

### Project Structure Notes

- Relevant areas: `apps/web/src/app`, `apps/web/src/components/features`, `apps/web/src/lib`.

### References

- `review/current-project-refactor-audit-2026-05-26.md`
- `_bmad-output/planning-artifacts/ux-design-specification.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

