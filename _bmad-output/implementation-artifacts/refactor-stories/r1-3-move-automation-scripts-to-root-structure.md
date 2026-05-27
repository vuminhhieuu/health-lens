# Story R1.3: Move Automation Scripts To Root Structure

Status: ready-for-dev

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

- [ ] Tạo root `scripts/` structure theo concern. (AC: 1)
- [ ] Move scripts hiện có từ `docker/scripts`, `infisical/scripts`, `apps/web/scripts`, `apps/mobile/scripts`. (AC: 1)
- [ ] Cập nhật path resolution trong từng script. (AC: 2)
- [ ] Chạy `--help`/syntax checks cho shell và node scripts. (AC: 3)

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

TBD

### Debug Log References

### Completion Notes List

### File List

