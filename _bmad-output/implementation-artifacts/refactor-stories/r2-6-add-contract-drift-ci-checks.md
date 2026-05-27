# Story R2.6: Add Contract Drift CI Checks

Status: ready-for-dev

## Story

Là developer,
tôi muốn CI phát hiện generated contract stale và active contract drift,
để backend/frontend compatibility không bị lệch âm thầm.

## Acceptance Criteria

1. **Given** OpenAPI generation đã tồn tại **When** CI chạy **Then** nó verify generated output current.
2. **Given** shared build là prerequisite **When** drift check chạy **Then** nó chạy sau shared build hoặc setup tương đương.
3. **Given** có route frontend-only/planned **When** drift check chạy **Then** exclusions explicit và documented.

## Tasks / Subtasks

- [ ] Add CI script under root scripts/ci hoặc scripts/openapi. (AC: 1)
- [ ] Integrate vào GitHub Actions path phù hợp. (AC: 1,2)
- [ ] Add explicit exclusion config/documentation. (AC: 3)
- [ ] Verify CI command locally if possible. (AC: 1)

## Dev Notes

- Không dùng full OpenAPI client migration làm điều kiện pass trong story này.
- Avoid flaky checks dependent on running local server unless documented.

### Project Structure Notes

- Relevant files: `.github/workflows/ci.yml`, root scripts, generated OpenAPI output.

### References

- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

