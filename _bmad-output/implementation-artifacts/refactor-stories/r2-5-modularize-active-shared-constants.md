# Story R2.5: Modularize Active Shared Constants

Status: ready-for-dev

## Story

Là developer,
tôi muốn shared constants được tách theo active concern,
để API routes, profile values, upload policy, metric sources, và planned items dễ maintain.

## Acceptance Criteria

1. **Given** constants đang mixed trong file lớn/barrel **When** modularization hoàn tất **Then** active constants nằm trong modules focused.
2. **Given** barrel exports được dùng rộng **When** update exports **Then** chỉ active runtime contracts export mặc định.
3. **Given** gender/profile constants có casing inconsistency **When** profile constants được chuẩn hóa **Then** active casing contract documented và schema tests cover usage.

## Tasks / Subtasks

- [ ] Tách upload/profile/metric constants ra file riêng. (AC: 1)
- [ ] Giữ API routes và errors theo module riêng. (AC: 1)
- [ ] Update `constants/index.ts` exports. (AC: 2)
- [ ] Add/update tests cho profile schema/constants. (AC: 3)

## Dev Notes

- Ưu tiên backwards-compatible imports nếu có nhiều consumers.
- Không trộn với OpenAPI generated adoption.

### Project Structure Notes

- Relevant files: `packages/shared/constants/*`, `packages/shared/schemas/profile.ts`.

### References

- `review/constants-scripts-deep-dive.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

