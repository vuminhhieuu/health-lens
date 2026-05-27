# Story R1.2: Create Master Refactor Roadmap Source Of Truth

Status: done

## Story

Là developer,
tôi muốn có một roadmap refactor hiện hành duy nhất,
để các review notes lịch sử không còn gây mâu thuẫn khi triển khai.

## Acceptance Criteria

1. **Given** `review/*.md` có khuyến nghị mâu thuẫn **When** master roadmap được tạo **Then** roadmap ghi quyết định cuối cho scripts, Flyway, OpenAPI, backend, frontend, mobile, docs.
2. **Given** review cũ vẫn hữu ích **When** roadmap tham chiếu chúng **Then** chúng được xem là input lịch sử, không phải nguồn lệnh active.
3. **Given** refactor cần sequencing **When** đọc roadmap **Then** thấy phase order, exit criteria, quality gates, và rollback notes.

## Tasks / Subtasks

- [x] Đọc `review/*.md` và artifact `refactor-epics-and-stories.md`. (AC: 1)
- [x] Tạo `review/refactor-master-roadmap-2026-05-27.md`. (AC: 1,3)
- [x] Ghi rõ decisions đã chốt bởi user: root scripts grouped, Flyway multi-baseline analysis, OpenAPI generator, mobile deferred. (AC: 1)
- [x] Thêm “Historical inputs” section trỏ về các review cũ. (AC: 2)

### Review Findings

- [x] [Review][Patch] Source-of-truth precedence is ambiguous [review/refactor-master-roadmap-2026-05-27.md:5]
- [x] [Review][Patch] Phase gates do not define entry/per-story/exit timing for most phases [review/refactor-master-roadmap-2026-05-27.md:58]
- [x] [Review][Patch] Phase 1 rollback scope conflicts with story-scoped rollback [review/refactor-master-roadmap-2026-05-27.md:64]
- [x] [Review][Patch] Baseline freshness has no revalidation guard [review/refactor-master-roadmap-2026-05-27.md:9]
- [x] [Review][Patch] Final docs phase does not require executable command validation [review/refactor-master-roadmap-2026-05-27.md:157]
- [x] [Review][Patch] Conflict resolution is not substantiated with explicit supersession notes [review/refactor-master-roadmap-2026-05-27.md:174]
- [x] [Review][Patch] Phase metadata lines may render as collapsed Markdown paragraphs [review/refactor-master-roadmap-2026-05-27.md:33]
- [x] [Review][Patch] Validation claims do not record reproducible `rg` checks [r1-2-create-master-refactor-roadmap-source-of-truth.md:52]

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

GPT-5 Codex

### Debug Log References

- 2026-05-27: Loaded BMad config, project context, story file, sprint status, `review/*.md`, and `_bmad-output/planning-artifacts/refactor-epics-and-stories.md`.
- 2026-05-27: Expanded `review/refactor-master-roadmap-2026-05-27.md` from R1.1 placeholder into active source-of-truth roadmap.
- 2026-05-27: Validation passed: `git diff --check`; `rg -n "Accepted Decisions|Historical Inputs|Phase Order|Exit criteria|Entry gate|Per-story gates|Exit gate|Rollback notes|Scripts|Flyway|OpenAPI|Backend packaging|Frontend extraction|Mobile|Docs" review/refactor-master-roadmap-2026-05-27.md`; `rg -n "review/api-package-structure-analysis.md|review/constants-scripts-deep-dive.md|review/current-project-refactor-audit-2026-05-26.md|review/full-project-structure-analysis.md|review/sprint-refactoring-scripts-constants.md|refactor-epics-and-stories.md" review/refactor-master-roadmap-2026-05-27.md`.
- 2026-05-27: Code review follow-up resolved 8 patch findings: precedence, gate timing, rollback scope, baseline freshness, command validation, supersession notes, markdown spacing, and reproducible validation logs.

### Completion Notes List

- Created a Vietnamese master roadmap that resolves conflicting historical review recommendations.
- Captured accepted decisions for grouped root scripts, Flyway multi-baseline analysis, OpenAPI generator sequencing, backend package migration, frontend extraction, mobile deferral, and docs handling.
- Added phase order, exit criteria, quality gates, rollback notes, and historical input references.
- Addressed code review findings by clarifying roadmap/backlog precedence, baseline freshness, entry/per-story/exit gates, story-scoped rollback, docs command validation, and supersession notes for historical inputs.
- No application code changed; app/API/web/mobile test suites were not run because this story is documentation-only and validation was limited to roadmap content plus diff hygiene.

### File List

- `review/refactor-master-roadmap-2026-05-27.md`
- `_bmad-output/implementation-artifacts/refactor-stories/r1-2-create-master-refactor-roadmap-source-of-truth.md`

### Change Log

- 2026-05-27: Expanded master refactor roadmap and moved Story R1.2 to review.
- 2026-05-27: Addressed code review findings and prepared Story R1.2 for done status.
