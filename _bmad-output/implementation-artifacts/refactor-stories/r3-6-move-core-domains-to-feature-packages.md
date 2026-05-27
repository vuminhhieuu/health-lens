# Story R3.6: Move Core Domains To Feature Packages

Status: ready-for-dev

## Story

Là backend developer,
tôi muốn auth, profile, healthrecord, referencedata, và admin code được tổ chức theo domain,
để feature work sau này không chạm nhiều root packages không liên quan.

## Acceptance Criteria

1. **Given** core domains lớn và rủi ro cao **When** migration thực hiện **Then** mỗi story/PR chỉ move một domain hoặc subdomain rõ.
2. **Given** tests mirror source **When** domain move hoàn tất **Then** tests được move/update trong cùng story.
3. **Given** architecture test cần update **When** domain move hoàn tất **Then** package boundary tests reflect target structure.

## Tasks / Subtasks

- [ ] Split implementation plan per domain/subdomain before moving. (AC: 1)
- [ ] Move package declarations/imports domain by domain. (AC: 1)
- [ ] Move/update tests for each domain. (AC: 2)
- [ ] Update architecture tests. (AC: 3)

## Dev Notes

- Không split `HealthRecordService`/`ReferenceDataAdminService` trong story này.
- Nếu diff quá lớn, stop and split into separate story files.

### Project Structure Notes

- Current core roots: `controller`, `service`, `entity`, `repository`, `dto`.

### References

- `review/api-package-structure-analysis.md`
- `review/current-project-refactor-audit-2026-05-26.md`

## Dev Agent Record

### Agent Model Used

TBD

### Debug Log References

### Completion Notes List

### File List

