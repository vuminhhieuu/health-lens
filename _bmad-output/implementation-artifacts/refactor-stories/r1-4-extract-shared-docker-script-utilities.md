# Story R1.4: Extract Shared Docker Script Utilities

Status: done

## Story

Là developer,
tôi muốn Docker scripts dùng chung utility shell,
để preflight, color, timing, và root resolution chỉ được maintain một nơi.

## Acceptance Criteria

1. **Given** Docker scripts có duplicate utility code **When** `_common.sh` được thêm **Then** `up.sh`, `down.sh`, `logs.sh`, `cleanup.sh` source file này.
2. **Given** duplicated functions đã được gom **When** review diff **Then** per-script duplicate preflight/color helpers giảm rõ.
3. **Given** script chạy với `--help` hoặc `--ci` **When** command execute **Then** output và no-color behavior vẫn đúng.

## Tasks / Subtasks

- [x] Tạo `scripts/docker/_common.sh` hoặc location tương ứng sau R1.3. (AC: 1)
- [x] Move shared functions: root resolution, colors, `disable_colors_if_needed`, `preflight_docker`, `format_duration`, size helpers nếu dùng. (AC: 1,2)
- [x] Update Docker scripts để source common utilities. (AC: 1)
- [x] Chạy `bash -n` và help checks. (AC: 3)

### Review Findings

- [x] [Review][Defer] Non-interactive `down.sh` destructive mode bypasses explicit confirmation [scripts/docker/down.sh:31] — deferred, pre-existing
- [x] [Review][Defer] Symlinked Docker script launchers cannot find sibling `_common.sh` [scripts/docker/up.sh:28] — deferred, pre-existing invocation pattern risk
- [x] [Review][Defer] `cleanup.sh --ci` without `--force` can no-op and still finish successfully [scripts/docker/cleanup.sh:156] — deferred, pre-existing
- [x] [Review][Defer] `cleanup.sh --dry-run` and `--analyze` still require Docker daemon preflight [scripts/docker/cleanup.sh:152] — deferred, pre-existing
- [x] [Review][Defer] `NO_COLOR` only disables colors when set to `1` [scripts/docker/_common.sh:23] — deferred, pre-existing behavior preserved by this refactor
- [x] [Review][Defer] `format_duration` does not normalize non-numeric input [scripts/docker/_common.sh:59] — deferred, inherited helper behavior

## Dev Notes

- Bash portability: giữ tương thích macOS Bash 3.2, đặc biệt `logs.sh`.
- Không đổi Docker compose files.

### Project Structure Notes

- Story phụ thuộc R1.3 nếu scripts đã move về root.

### References

- `review/constants-scripts-deep-dive.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `bash -n scripts/docker/_common.sh scripts/docker/up.sh scripts/docker/down.sh scripts/docker/logs.sh scripts/docker/cleanup.sh` — passed.
- `scripts/docker/up.sh --help`, `scripts/docker/down.sh --help`, `scripts/docker/logs.sh --help`, `scripts/docker/cleanup.sh --help` — passed.
- `NO_COLOR=1 scripts/docker/up.sh --ci --unknown-option` and `NO_COLOR=1 scripts/docker/down.sh --ci --unknown-option` — passed with no ANSI color output.
- `NO_COLOR=1 scripts/docker/logs.sh --ci --tail 0` — reached Docker preflight and failed because `docker` command is not installed in this shell; error output had no ANSI color.
- `git diff --check` — passed.

### Completion Notes List

- Added shared Docker shell utility file for root resolution, colors/no-color handling, Docker preflight, duration formatting, and size helpers.
- Updated `up.sh`, `down.sh`, `logs.sh`, and `cleanup.sh` to source `_common.sh`, removing duplicated root/color/preflight/helper definitions while preserving existing help output and Bash 3.2-compatible patterns.
- Preserved Docker compose files unchanged.

### File List

- `scripts/docker/_common.sh`
- `scripts/docker/up.sh`
- `scripts/docker/down.sh`
- `scripts/docker/logs.sh`
- `scripts/docker/cleanup.sh`
- `_bmad-output/implementation-artifacts/refactor-stories/r1-4-extract-shared-docker-script-utilities.md`

### Change Log

- 2026-05-27: Extracted shared Docker script utilities and updated Docker helper scripts to source them.
