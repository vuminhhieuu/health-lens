# Story RD1.2: Consolidate Mobile Source Structure

Status: ready-for-dev

## Story

Là mobile developer,
tôi muốn mobile source folders theo một convention duy nhất,
để feature mobile sau này có ownership rõ.

## Acceptance Criteria

1. **Given** mobile có root-level folders và `src/` folders **When** consolidation hoàn tất **Then** components, hooks, lib, stores, constants theo approved convention.
2. **Given** Expo app cần chạy **When** files move **Then** Expo start/lint behavior vẫn valid.
3. **Given** reset script có thể liên quan paths **When** consolidation hoàn tất **Then** script behavior được review để không xóa nhầm root scripts hoặc source mới.

## Tasks / Subtasks

- [ ] Apply convention from RD1.1. (AC: 1)
- [ ] Move files and update imports. (AC: 1)
- [ ] Review `apps/mobile/scripts/reset-project.js`. (AC: 3)
- [ ] Run mobile lint/start smoke if available. (AC: 2)

## Dev Notes

- Deferred until mobile work resumes.
- Avoid mixing with web/API refactor.

### Project Structure Notes

- Depends on RD1.1.

### References

- `review/full-project-structure-analysis.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

