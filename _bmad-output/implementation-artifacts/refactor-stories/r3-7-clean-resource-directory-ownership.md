# Story R3.7: Clean Resource Directory Ownership

Status: ready-for-dev

## Story

Là backend developer,
tôi muốn resources được tổ chức theo purpose,
để seed data, prompts, fonts, migrations, và templates không bị lẫn nghĩa.

## Acceptance Criteria

1. **Given** `resources/ai` chứa cả seed data và prompts **When** reorganize hoàn tất **Then** prompts và seed data ở locations tách biệt, documented.
2. **Given** code load resources **When** paths đổi **Then** code/tests/startup checks được cập nhật.
3. **Given** migrations và email templates đang ổn **When** cleanup thực hiện **Then** không move chúng nếu không cần.

## Tasks / Subtasks

- [ ] Inventory resource files and loaders. (AC: 1,2)
- [ ] Move prompts/seed data theo convention đã chốt. (AC: 1)
- [ ] Update code loading paths. (AC: 2)
- [ ] Run relevant API tests/startup checks. (AC: 2)

## Dev Notes

- Avoid changing prompt content or seed JSON semantics.
- Preserve `db/migration`, `templates/email`, `fonts` unless explicitly decided.

### Project Structure Notes

- Current resources: `apps/api/src/main/resources/ai`, `ai/prompts`, `db/migration`, `templates/email`.

### References

- `review/full-project-structure-analysis.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

