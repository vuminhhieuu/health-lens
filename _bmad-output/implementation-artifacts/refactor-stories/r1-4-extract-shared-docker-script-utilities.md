# Story R1.4: Extract Shared Docker Script Utilities

Status: ready-for-dev

## Story

Là developer,
tôi muốn Docker scripts dùng chung utility shell,
để preflight, color, timing, và root resolution chỉ được maintain một nơi.

## Acceptance Criteria

1. **Given** Docker scripts có duplicate utility code **When** `_common.sh` được thêm **Then** `up.sh`, `down.sh`, `logs.sh`, `cleanup.sh` source file này.
2. **Given** duplicated functions đã được gom **When** review diff **Then** per-script duplicate preflight/color helpers giảm rõ.
3. **Given** script chạy với `--help` hoặc `--ci` **When** command execute **Then** output và no-color behavior vẫn đúng.

## Tasks / Subtasks

- [ ] Tạo `scripts/docker/_common.sh` hoặc location tương ứng sau R1.3. (AC: 1)
- [ ] Move shared functions: root resolution, colors, `disable_colors_if_needed`, `preflight_docker`, `format_duration`, size helpers nếu dùng. (AC: 1,2)
- [ ] Update Docker scripts để source common utilities. (AC: 1)
- [ ] Chạy `bash -n` và help checks. (AC: 3)

## Dev Notes

- Bash portability: giữ tương thích macOS Bash 3.2, đặc biệt `logs.sh`.
- Không đổi Docker compose files.

### Project Structure Notes

- Story phụ thuộc R1.3 nếu scripts đã move về root.

### References

- `review/constants-scripts-deep-dive.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

