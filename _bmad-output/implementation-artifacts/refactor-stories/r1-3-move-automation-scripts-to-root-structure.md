# Story R1.3: Move Automation Scripts To Root Structure

Status: done

## Story

Là developer,
tôi muốn các automation scripts nằm dưới root `scripts/` và được nhóm theo concern,
để command dễ tìm mà vẫn giữ rõ ownership.

## Acceptance Criteria

1. **Given** scripts đang phân tán **When** move hoàn tất **Then** scripts nằm dưới `scripts/docker`, `scripts/infisical`, `scripts/web`, `scripts/mobile`, `scripts/db` và chừa chỗ cho `scripts/openapi`/`scripts/ci`.
2. **Given** script được chạy từ bất kỳ working directory nào **When** script cần repo root **Then** `PROJECT_ROOT` resolve đúng.
3. **Given** behavior hiện có **When** script được move **Then** help output và non-destructive checks vẫn hoạt động.
4. **Given** story chỉ là move **When** review diff **Then** không có behavior app/source code change.

## Tasks / Subtasks

- [x] Tạo root `scripts/` structure theo concern. (AC: 1)
- [x] Move scripts hiện có từ `docker/scripts`, `infisical/scripts`, `apps/web/scripts`, `apps/mobile/scripts`. (AC: 1)
- [x] Cập nhật path resolution trong từng script. (AC: 2)
- [x] Chạy `--help`/syntax checks cho shell và node scripts. (AC: 3)

### Review Findings

- [x] [Review][Defer] Non-interactive `down.sh` deletes volumes without explicit confirmation [scripts/docker/down.sh:58] — deferred, pre-existing
- [x] [Review][Defer] Documentation still points to deleted script paths [README.md:88] — deferred, explicitly assigned to later docs story R1.6
- [x] [Review][Defer] Mobile reset can fail partially but still exit successfully [scripts/mobile/reset-project.js:94] — deferred, pre-existing

## Dev Notes

- Preserve executable bits.
- Với mobile reset script, đảm bảo không vô tình xóa root `scripts/` mới.
- Không chỉnh docs trong story này nếu muốn giữ diff nhỏ; docs thuộc R1.6.

### Project Structure Notes

- Source paths hiện tại: `docker/scripts/*`, `infisical/scripts/infisical.sh`, `apps/web/scripts/smoke-sitemap-urls.mjs`, `apps/mobile/scripts/reset-project.js`.

### References

- `review/sprint-refactoring-scripts-constants.md`
- `review/constants-scripts-deep-dive.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-05-27: Loaded BMad config, project context, sprint status, and Story R1.3. Sprint status exists but does not contain `r1-3-move-automation-scripts-to-root-structure`; story status is tracked in this file.
- 2026-05-27: Moved automation scripts into root `scripts/` concern folders and removed old empty script directories.
- 2026-05-27: Validation passed: `bash -n scripts/docker/up.sh scripts/docker/down.sh scripts/docker/logs.sh scripts/docker/cleanup.sh scripts/infisical/infisical.sh`.
- 2026-05-27: Validation passed: `node --check scripts/web/smoke-sitemap-urls.mjs && node --check scripts/mobile/reset-project.js`.
- 2026-05-27: Validation passed from non-root working directories: Docker script `--help` checks, Infisical `help`, and cleanup `--help`.
- 2026-05-27: Validation passed: `pnpm --filter web test`, `pnpm test`, and `git diff --check`.

### Completion Notes List

- Created root script groups: `scripts/docker`, `scripts/infisical`, `scripts/web`, `scripts/mobile`, plus placeholder folders `scripts/db`, `scripts/openapi`, and `scripts/ci`.
- Moved Docker, Infisical, web sitemap smoke, and mobile reset scripts into the root `scripts/` structure while preserving executable bits.
- Updated shell scripts to resolve `PROJECT_ROOT` from their new root `scripts/<concern>/` location and verified help output works from nested working directories.
- Updated mobile reset script to target `apps/mobile` from its script location and removed deletion/move of a local `scripts` directory so it cannot affect the new root `scripts/` tree.
- Updated web smoke-script assertions and the mobile package script command to point at the new root script locations.

### File List

- `apps/mobile/package.json`
- `apps/mobile/scripts/reset-project.js` (deleted)
- `apps/web/scripts/smoke-sitemap-urls.mjs` (deleted)
- `apps/web/src/app/(marketing)/public-legal-pages.test.ts`
- `apps/web/src/app/seo-foundation.test.ts`
- `docker/scripts/cleanup.sh` (deleted)
- `docker/scripts/down.sh` (deleted)
- `docker/scripts/logs.sh` (deleted)
- `docker/scripts/up.sh` (deleted)
- `infisical/scripts/infisical.sh` (deleted)
- `scripts/ci/.gitkeep`
- `scripts/db/.gitkeep`
- `scripts/docker/cleanup.sh`
- `scripts/docker/down.sh`
- `scripts/docker/logs.sh`
- `scripts/docker/up.sh`
- `scripts/infisical/infisical.sh`
- `scripts/mobile/reset-project.js`
- `scripts/openapi/.gitkeep`
- `scripts/web/smoke-sitemap-urls.mjs`
- `_bmad-output/implementation-artifacts/refactor-stories/r1-3-move-automation-scripts-to-root-structure.md`

### Change Log

- 2026-05-27: Moved automation scripts to root concern-based `scripts/` structure, updated path resolution and command references, validated syntax/help/tests, and moved Story R1.3 to review.
- 2026-05-27: Code review completed with no required patch findings for R1.3; deferred pre-existing/docs-follow-up findings and moved story to done.
