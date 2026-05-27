# Story R3.1: Define Backend Package Boundary Rules

Status: ready-for-dev

## Story

Là backend developer,
tôi muốn có package boundary rules trước khi move code,
để mọi migration đi theo cùng target architecture.

## Acceptance Criteria

1. **Given** backend đang trộn layer và feature packages **When** rules được viết **Then** chúng định nghĩa target top-level packages, allowed dependencies, và `common` ownership.
2. **Given** package moves dễ trộn logic rewrite **When** rules hoàn tất **Then** rules cấm rewrite service responsibilities trong cùng story package move.
3. **Given** architecture tests đang có **When** rules hoàn tất **Then** test strategy enforcement được documented.

## Tasks / Subtasks

- [ ] Tạo backend package map target trong roadmap/docs. (AC: 1)
- [ ] Define `common` criteria và anti-patterns. (AC: 1,2)
- [ ] Define test/package boundary strategy. (AC: 3)
- [ ] Link rules vào master refactor roadmap. (AC: 1)

## Dev Notes

- Không move source code trong story này.
- Target package names phải phù hợp với current source và review notes.

### Project Structure Notes

- Current root package: `apps/api/src/main/java/com/healthlens/api`.

### References

- `review/api-package-structure-analysis.md`
- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

