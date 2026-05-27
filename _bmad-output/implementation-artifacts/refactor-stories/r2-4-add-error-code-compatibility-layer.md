# Story R2.4: Add Error Code Compatibility Layer

Status: ready-for-dev

## Story

Là frontend developer,
tôi muốn map backend semantic error codes sang frontend handling,
để người dùng có message nhất quán trong khi chưa đổi canonical error-code format.

## Acceptance Criteria

1. **Given** backend emits `ApiErrorCode` semantic values **When** frontend handles API errors **Then** mọi backend code có mapping hoặc fallback documented.
2. **Given** backend thêm code mới **When** tests chạy **Then** thiếu frontend handling làm test fail.
3. **Given** frontend catalog-style codes tồn tại **When** compatibility layer được thêm **Then** story không yêu cầu đổi toàn bộ backend codes.

## Tasks / Subtasks

- [ ] Inventory `ApiErrorCode.java`. (AC: 1)
- [ ] Add shared/frontend compatibility map. (AC: 1)
- [ ] Add unit tests coverage cho every backend code. (AC: 2)
- [ ] Document migration path nếu sau này chọn `AUTH_001` style làm canonical. (AC: 3)

## Dev Notes

- Không đổi `GlobalExceptionHandler` hàng loạt trong story này trừ khi cần expose stable shape.
- Preserve existing user-facing Vietnamese messages.

### Project Structure Notes

- Relevant backend: `apps/api/src/main/java/com/healthlens/api/exception/ApiErrorCode.java`.
- Relevant shared/web: `packages/shared/constants/error-codes.ts`, web API error handling.

### References

- `review/constants-scripts-deep-dive.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

