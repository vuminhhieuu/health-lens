# Story R1.2: Create Master Refactor Roadmap Source Of Truth

Status: ready-for-dev

## Story

Là developer,
tôi muốn có một roadmap refactor hiện hành duy nhất,
để các review notes lịch sử không còn gây mâu thuẫn khi triển khai.

## Acceptance Criteria

1. **Given** `review/*.md` có khuyến nghị mâu thuẫn **When** master roadmap được tạo **Then** roadmap ghi quyết định cuối cho scripts, Flyway, OpenAPI, backend, frontend, mobile, docs.
2. **Given** review cũ vẫn hữu ích **When** roadmap tham chiếu chúng **Then** chúng được xem là input lịch sử, không phải nguồn lệnh active.
3. **Given** refactor cần sequencing **When** đọc roadmap **Then** thấy phase order, exit criteria, quality gates, và rollback notes.

## Tasks / Subtasks

- [ ] Đọc `review/*.md` và artifact `refactor-epics-and-stories.md`. (AC: 1)
- [ ] Tạo `review/refactor-master-roadmap-2026-05-27.md`. (AC: 1,3)
- [ ] Ghi rõ decisions đã chốt bởi user: root scripts grouped, Flyway multi-baseline analysis, OpenAPI generator, mobile deferred. (AC: 1)
- [ ] Thêm “Historical inputs” section trỏ về các review cũ. (AC: 2)

## Dev Notes

- Roadmap phải ngắn gọn nhưng đủ actionable cho các story sau.
- Không sửa nội dung review cũ trong story này.

### Project Structure Notes

- File mới nằm trong `review/`.
- Dùng Vietnamese cho nội dung.

### References

- `review/api-package-structure-analysis.md`
- `review/constants-scripts-deep-dive.md`
- `review/current-project-refactor-audit-2026-05-26.md`
- `review/full-project-structure-analysis.md`
- `review/sprint-refactoring-scripts-constants.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

