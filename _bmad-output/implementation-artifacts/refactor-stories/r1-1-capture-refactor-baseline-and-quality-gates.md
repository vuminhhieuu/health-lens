# Story R1.1: Capture Refactor Baseline And Quality Gates

Status: done

## Story

Là developer,
tôi muốn ghi nhận trạng thái repo và baseline checks trước khi refactor,
để các lỗi về sau được phân biệt rõ giữa lỗi có sẵn và lỗi do refactor.

## Acceptance Criteria

1. **Given** repo hiện có dirty working tree **When** story được thực hiện **Then** tài liệu baseline ghi lại `git status --short`, branch hiện tại, và các nhóm file đang thay đổi trước refactor.
2. **Given** baseline commands được chạy hoặc bị bỏ qua **When** ghi nhận kết quả **Then** mỗi command có trạng thái pass/fail/skipped và lý do rõ ràng.
3. **Given** một baseline command fail **When** kết quả được lưu **Then** failure được đánh dấu là pre-existing nếu story không đổi file liên quan.
4. **Given** các epic refactor sau cần gate **When** baseline hoàn tất **Then** master roadmap có minimum quality gates cho shared/web/mobile/api.

## Tasks / Subtasks

- [x] Ghi nhận `git status --short`, branch, ngày giờ, và danh sách nhóm thay đổi hiện có. (AC: 1)
- [x] Chạy hoặc ghi rõ lý do skip: `pnpm --filter @healthlens/shared build`, `pnpm --filter web test`, `pnpm --filter mobile lint`, `cd apps/api && ./gradlew test`. (AC: 2)
- [x] Tạo baseline report trong `review/` hoặc `_bmad-output/implementation-artifacts/refactor/`. (AC: 1,2,3)
- [x] Thêm section quality gates vào master roadmap. (AC: 4)

### Review Findings

- [x] [Review][Decision] Quality gates are not in the dedicated master roadmap — resolved by creating `review/refactor-master-roadmap-2026-05-27.md` as the R1.1 master roadmap placeholder and linking it from the refactor epic breakdown.
- [x] [Review][Patch] Untracked directory contents are not reproducibly captured [_bmad-output/implementation-artifacts/refactor/refactor-baseline-2026-05-27.md:50]
- [x] [Review][Patch] API quality gate can be interpreted as skippable for API-changing stories [_bmad-output/implementation-artifacts/refactor/refactor-baseline-2026-05-27.md:83]
- [x] [Review][Patch] Mobile lint command status does not use required pass/fail/skipped vocabulary [_bmad-output/implementation-artifacts/refactor/refactor-baseline-2026-05-27.md:69]
- [x] [Review][Defer] Refactor story status tracking is not represented in central sprint-status.yaml [_bmad-output/implementation-artifacts/refactor-stories/r1-1-capture-refactor-baseline-and-quality-gates.md:54] — deferred, pre-existing

## Dev Notes

- Không revert thay đổi hiện có của user.
- Nếu command quá lâu hoặc phụ thuộc env, ghi `skipped` với lý do cụ thể thay vì giả định pass.
- Không sửa source code trong story này.

### Project Structure Notes

- Output nên nằm ở `review/` hoặc `_bmad-output/implementation-artifacts/refactor/`.
- Không ghi đè `_bmad-output/planning-artifacts/epics.md`.

### References

- `_bmad-output/planning-artifacts/refactor-epics-and-stories.md`
- `docs/project-context.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-05-27T13:34:48+0700: Captured branch `tmp/task-refactor` and pre-story `git status --short` for baseline.
- `pnpm --filter @healthlens/shared build`: pass.
- `pnpm --filter web test`: pass; Vitest reported 22 files / 82 tests passed.
- `pnpm --filter mobile lint`: pass; Node runtime warning recorded separately (`v18.16.0`, required `>=20.19.4`).
- `cd apps/api && ./gradlew test`: skipped because shell cannot locate a Java Runtime.
- Sprint tracking note: `_bmad-output/implementation-artifacts/sprint-status.yaml` exists, but it does not contain key `r1-1-capture-refactor-baseline-and-quality-gates`; story status is tracked in this story file.
- Code review follow-up: created master roadmap placeholder, expanded untracked refactor story inventory, tightened API gate wording, and normalized mobile lint status vocabulary.

### Completion Notes List

- Created baseline report at `_bmad-output/implementation-artifacts/refactor/refactor-baseline-2026-05-27.md`.
- Recorded dirty working tree groups, branch, capture timestamp, baseline command outcomes, and pre-existing/environment labels.
- Added minimum refactor quality gates for shared, web, mobile, and API to `review/refactor-master-roadmap-2026-05-27.md` and linked them from `_bmad-output/planning-artifacts/refactor-epics-and-stories.md`.
- No application source code was changed.
- Resolved code-review patch findings; one status-tracking concern remains deferred as pre-existing workflow debt.

### File List

- `_bmad-output/implementation-artifacts/refactor/refactor-baseline-2026-05-27.md`
- `_bmad-output/planning-artifacts/refactor-epics-and-stories.md`
- `_bmad-output/implementation-artifacts/refactor-stories/r1-1-capture-refactor-baseline-and-quality-gates.md`
- `_bmad-output/implementation-artifacts/deferred-work.md`
- `review/refactor-master-roadmap-2026-05-27.md`

### Change Log

- 2026-05-27: Captured refactor baseline, documented quality gates, and moved story to review.
- 2026-05-27: Resolved code-review findings and moved story to done.
