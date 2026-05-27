# Story R5.3: Extract Health Record Review Hook And Domain Utilities

Status: ready-for-dev

## Story

Là frontend developer,
tôi muốn tách state, queries, mutations, và mapping logic khỏi review page,
để orchestration test được ngoài route file.

## Acceptance Criteria

1. **Given** review page quản lý data/UI state inline **When** hooks/utilities được extract **Then** query/mutation orchestration, derived state, helper mapping nằm dưới health-record feature.
2. **Given** derived state phức tạp **When** unit tests chạy **Then** non-trivial state và error paths được cover.
3. **Given** route behavior hiện tại **When** refactor hoàn tất **Then** loading/error/success/review actions vẫn hoạt động.

## Tasks / Subtasks

- [ ] Identify state/query/mutation groups. (AC: 1)
- [ ] Extract `useHealthRecordReview` or equivalent hook. (AC: 1)
- [ ] Move pure mapping utilities into feature/domain lib. (AC: 1)
- [ ] Add unit tests for derived state and errors. (AC: 2)
- [ ] Run `pnpm --filter web test`. (AC: 3)

## Dev Notes

- Depends on R5.2.
- Use existing TanStack Query/API client patterns.

### Project Structure Notes

- Relevant API client: `apps/web/src/lib/api/apiClient.ts`.

### References

- `docs/project-context.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

