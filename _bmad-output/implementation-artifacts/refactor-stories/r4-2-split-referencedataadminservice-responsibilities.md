# Story R4.2: Split ReferenceDataAdminService Responsibilities

Status: ready-for-dev

## Story

Là admin feature developer,
tôi muốn tách import, preview, apply, approval, và audit responsibilities của reference data,
để thay đổi admin reference-data an toàn hơn khi review.

## Acceptance Criteria

1. **Given** `ReferenceDataAdminService` xử lý nhiều workflow **When** decomposition hoàn tất **Then** import parsing, preview validation, change-set application, và audit có boundary rõ.
2. **Given** admin reference-data behavior đang được test **When** split hoàn tất **Then** tests vẫn behavior-focused và pass hoặc existing failures documented.
3. **Given** story chỉ là decomposition **When** review diff **Then** không đổi API contract hoặc approval rules.

## Tasks / Subtasks

- [ ] Map current methods and workflows. (AC: 1)
- [ ] Extract focused collaborators. (AC: 1)
- [ ] Update injection/call sites. (AC: 1)
- [ ] Update `ReferenceDataAdminServiceTest`. (AC: 2)

## Dev Notes

- Reference-data changes are high-risk because they affect medical interpretation.
- Preserve audit log behavior.

### Project Structure Notes

- Relevant file: `apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java`.

### References

- `docs/reference-data/core-feature-data-requirements.md`
- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

