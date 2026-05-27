# Story R2.3: Quarantine Dead And Planned Shared Contracts

Status: ready-for-dev

## Story

Là frontend developer,
tôi muốn inactive contracts không còn nằm trong active exports,
để frontend không phụ thuộc vào API hoặc enum chưa có implementation.

## Acceptance Criteria

1. **Given** `DOCUMENTS_*`, document status/type, subscription constants chưa có backend implementation **When** cleanup hoàn tất **Then** chúng bị remove khỏi active barrel exports hoặc chuyển vào planned area không runtime.
2. **Given** active consumers build **When** constants bị move/remove **Then** imports được cập nhật và tests/build pass.
3. **Given** future feature cần planned constant **When** developer đọc docs **Then** planned contracts không được hiểu là active API guarantee.

## Tasks / Subtasks

- [ ] Verify absence/presence của Document controller/service/entity/migration. (AC: 1)
- [ ] Update `packages/shared/constants/api.ts` và `status.ts` theo strategy đã chốt. (AC: 1)
- [ ] Update barrel exports và active imports. (AC: 1,2)
- [ ] Add docs/comment cho planned contract nếu giữ lại. (AC: 3)

## Dev Notes

- Không xóa feature roadmap khỏi docs nếu team vẫn muốn giữ ý tưởng.
- Build shared và web after changes.

### Project Structure Notes

- Relevant files: `packages/shared/constants/api.ts`, `packages/shared/constants/status.ts`, `packages/shared/constants/index.ts`.

### References

- `review/constants-scripts-deep-dive.md`
- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

