# Story R6.3: Update Developer And Operations Guides After Refactor Phases

Status: ready-for-dev

## Story

Là developer hoặc operator,
tôi muốn guides phản ánh final script paths, generated contract commands, package structure, và test gates,
để daily work theo đúng repo đã refactor.

## Acceptance Criteria

1. **Given** refactor phases đã đổi commands/structure **When** docs update **Then** README, development guide, testing guide, deployment guide, operations runbook, environment reference reflect current commands.
2. **Given** paths cũ đã removed **When** search active docs **Then** docs không reference removed paths như active commands.
3. **Given** generated contracts tồn tại **When** docs update **Then** generation/check commands được document.

## Tasks / Subtasks

- [ ] Update active guides after implementation phases. (AC: 1)
- [ ] Document OpenAPI generation and drift checks. (AC: 3)
- [ ] Document backend package/testing rules. (AC: 1)
- [ ] Search for stale commands/paths. (AC: 2)

## Dev Notes

- This is a final alignment story, not first roadmap creation.
- Keep docs concise and task-oriented.

### Project Structure Notes

- Active docs: `README.md`, `docs/*.md`, `infisical/*.md`.

### References

- Master roadmap and completed implementation diffs.

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

