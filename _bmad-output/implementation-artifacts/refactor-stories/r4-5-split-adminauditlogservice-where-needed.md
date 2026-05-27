# Story R4.5: Split AdminAuditLogService Where Needed

Status: ready-for-dev

## Story

Là admin feature developer,
tôi muốn tách audit-log query, filter, export, và statistics khi chúng thay đổi độc lập,
để admin audit changes vẫn reviewable.

## Acceptance Criteria

1. **Given** audit log behavior stable **When** split thực hiện **Then** query/filter/export/statistics được tách chỉ khi cohesion thực sự tốt hơn.
2. **Given** audit logs liên quan compliance **When** split hoàn tất **Then** export/filter behavior có tests xác nhận không đổi.
3. **Given** split không cần thiết cho một phần **When** review decision **Then** rationale giữ nguyên phần đó được documented.

## Tasks / Subtasks

- [ ] Map current responsibilities and change pressure. (AC: 1,3)
- [ ] Extract query/filter/export/statistics collaborators where useful. (AC: 1)
- [ ] Update admin audit tests. (AC: 2)
- [ ] Document any “not split” decisions. (AC: 3)

## Dev Notes

- Audit data is compliance-sensitive; avoid changing redaction/export semantics.

### Project Structure Notes

- Relevant file: `apps/api/src/main/java/com/healthlens/api/service/admin/AdminAuditLogService.java`.

### References

- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

