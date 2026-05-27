# Story R3.5: Move Support Domains To Feature Packages

Status: ready-for-dev

## Story

Là backend developer,
tôi muốn consent, reminder, deletion, và email support domains được colocate,
để compliance và lifecycle features dễ hiểu hơn.

## Acceptance Criteria

1. **Given** support domains đang spread across layer packages **When** migration hoàn tất **Then** controller/service/entity/repository/scheduler/event files nằm trong feature packages phù hợp.
2. **Given** cross-cutting pieces có thể dùng chung **When** move hoàn tất **Then** chỉ phần thật sự reused mới vào `common`.
3. **Given** behavior compliance quan trọng **When** tests chạy **Then** consent/deletion/reminder/email behavior không đổi.

## Tasks / Subtasks

- [ ] Inventory support domain files. (AC: 1)
- [ ] Move one domain at a time or split into sub-PRs if diff lớn. (AC: 1)
- [ ] Update imports and tests. (AC: 3)
- [ ] Verify deletion cancel public route remains unauthenticated. (AC: 3)

## Dev Notes

- Preserve `DELETE /api/v1/users/deletion-requests/cancel?token=...` public behavior.
- Healthcare privacy behavior is high-risk; avoid logic changes.

### Project Structure Notes

- Relevant docs: `docs/project-context.md`.

### References

- `review/api-package-structure-analysis.md`
- `docs/project-context.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

