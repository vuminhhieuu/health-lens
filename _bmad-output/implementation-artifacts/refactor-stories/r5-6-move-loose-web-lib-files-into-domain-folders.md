# Story R5.6: Move Loose Web Lib Files Into Domain Folders

Status: ready-for-dev

## Story

Là frontend developer,
tôi muốn move loose domain utilities vào feature/domain folders,
để imports thể hiện ownership rõ ràng.

## Acceptance Criteria

1. **Given** `healthRecordHub.ts`, `profileMappings.ts`, `notify.ts` nằm trực tiếp dưới `lib` **When** move hoàn tất **Then** chúng nằm trong folders domain phù hợp.
2. **Given** imports hiện có **When** files move **Then** mọi imports được cập nhật và tests pass.
3. **Given** utilities có behavior hiện tại **When** tests chạy **Then** behavior không đổi.

## Tasks / Subtasks

- [ ] Move `healthRecordHub.ts` vào health-records domain folder. (AC: 1)
- [ ] Move `profileMappings.ts` vào profiles domain folder. (AC: 1)
- [ ] Move `notify.ts` vào notifications/toast domain folder. (AC: 1)
- [ ] Update imports and tests. (AC: 2,3)

## Dev Notes

- Prefer existing folder conventions in `apps/web/src/lib`.
- Avoid changing exported function behavior.

### Project Structure Notes

- Current files: `apps/web/src/lib/healthRecordHub.ts`, `apps/web/src/lib/profileMappings.ts`, `apps/web/src/lib/notify.ts`.

### References

- `review/constants-scripts-deep-dive.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

