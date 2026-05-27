# Story R4.1: Split HealthRecordService Responsibilities

Status: ready-for-dev

## Story

Là backend developer,
tôi muốn tách query, command, analysis, sharing, và context responsibilities của health record,
để thay đổi một workflow không phải sửa service 1700 dòng.

## Acceptance Criteria

1. **Given** package boundaries đã ổn định **When** decomposition hoàn tất **Then** responsibilities được tách thành services cohesive với method ownership rõ.
2. **Given** controllers đang gọi public behavior **When** split hoàn tất **Then** controller behavior không đổi.
3. **Given** tests cover health record workflows **When** split hoàn tất **Then** tests update theo collaborators mới và pass hoặc existing failures documented.

## Tasks / Subtasks

- [ ] Map current methods in `HealthRecordService`. (AC: 1)
- [ ] Extract services incrementally: query, command, analysis/context, possibly share/pdf if still mixed. (AC: 1)
- [ ] Update injection/call sites. (AC: 2)
- [ ] Update `HealthRecordServiceTest` and targeted tests. (AC: 3)

## Dev Notes

- Không đổi API contract.
- Không đổi domain behavior hoặc DB schema trong story này.

### Project Structure Notes

- Relevant file: `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`.

### References

- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

