# Story R5.5: Refactor Admin Audit And Admin Login Pages

Status: ready-for-dev

## Story

Là admin frontend developer,
tôi muốn admin audit và admin login pages giảm thành shells với components focused,
để admin workflows maintainable và consistent.

## Acceptance Criteria

1. **Given** admin audit/login pages lớn **When** refactor hoàn tất **Then** filters, tables, modals, auth form, session state nằm trong modules focused.
2. **Given** audit/admin auth behavior quan trọng **When** tests chạy **Then** existing admin tests/smoke vẫn pass.
3. **Given** story là extraction **When** review diff **Then** không đổi auth/audit API behavior.

## Tasks / Subtasks

- [ ] Inventory admin audit and login page sections. (AC: 1)
- [ ] Extract audit filters/table/modal components/hooks. (AC: 1)
- [ ] Extract admin login form/session hook as needed. (AC: 1)
- [ ] Run web tests. (AC: 2)

## Dev Notes

- Preserve MFA/admin auth flows.
- Audit logs are compliance-sensitive; do not drop fields.

### Project Structure Notes

- Main files: `apps/web/src/app/admin/audit-log/page.tsx`, `apps/web/src/app/admin/login/page.tsx`.

### References

- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

