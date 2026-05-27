# Story R3.4: Consolidate Activity And Notification Packages

Status: ready-for-dev

## Story

Là backend developer,
tôi muốn activity và notification code nằm trong cohesive feature packages,
để inbox, preferences, filters, events, và analytics không bị phân tán.

## Acceptance Criteria

1. **Given** activity code split across constants/service/filter/entity/repository **When** migration hoàn tất **Then** activity có một feature package với internal layers.
2. **Given** notification inbox/preferences code tồn tại **When** migration hoàn tất **Then** notification theo cùng convention feature-first.
3. **Given** tests tồn tại **When** migration hoàn tất **Then** relevant tests pass hoặc existing failures documented.

## Tasks / Subtasks

- [ ] Inventory activity and notification classes/tests. (AC: 1,2)
- [ ] Move activity package declarations/imports. (AC: 1)
- [ ] Move notification package declarations/imports. (AC: 2)
- [ ] Update tests and package boundary rules. (AC: 3)

## Dev Notes

- Preserve activity event type strings and audit/correlation behavior.
- Do not change notification API contract in this story.

### Project Structure Notes

- Current areas include `activity`, `notification`, `service`, `security`, `entity`, `repository`.

### References

- `review/constants-scripts-deep-dive.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

