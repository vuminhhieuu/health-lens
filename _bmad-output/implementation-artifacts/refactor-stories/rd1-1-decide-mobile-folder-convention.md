# Story RD1.1: Decide Mobile Folder Convention

Status: ready-for-dev

## Story

Là mobile developer,
tôi muốn có quyết định rõ về root `app/` và `src/` ownership,
để mobile work sau này không tiếp tục dual-structure confusion.

## Acceptance Criteria

1. **Given** mobile work được resume **When** convention được quyết định **Then** decision nêu Expo Router routes ở root `app/` hay `src/app`, và components/hooks/lib/stores nằm ở đâu.
2. **Given** migration cần an toàn **When** decision hoàn tất **Then** có migration steps và lint/start gates.
3. **Given** mobile đang deferred **When** story được đọc **Then** nó không block active refactor epics.

## Tasks / Subtasks

- [ ] Inspect current mobile tree. (AC: 1)
- [ ] Write ADR/decision for mobile folder convention. (AC: 1)
- [ ] Define migration/test gates. (AC: 2)

## Dev Notes

- Do not move files in this story.
- Respect Expo Router constraints.

### Project Structure Notes

- Current areas: `apps/mobile/app`, `apps/mobile/src`, root `components`, `hooks`, `lib`, `stores`.

### References

- `review/full-project-structure-analysis.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

