# Story 7.4: Phê duyệt thay đổi trước khi có hiệu lực

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Phê duyệt nội dung - Admin HealthLens | `projects/578519912546445367/screens/1704331f19474eb9b8345ee6bb68c27f` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzVkYTE1N2I3Nzk0OTRhZWE5ODE0NjM5YjJiYzIwZjllEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugz4bHkRjaQj_9XSPoTupVzGwZZ7YK_8cxVeu26nz-CWFKfcxnHl47y-cq3g1BaL4vKje6fX3rODkTyzjTor_oMGQtjKfWKI9VYVLKJM0Gyb4zpKcY0XCzF8Mu-WLe5KPeekNOZPPGYrQ5_9CjchvUHC6yL9j6GQhT4wD9gjc8rfZbeG4UPHtnpw0zusc0s0Zs8WFb0jL7AMoHrdzDt_y4Y8C4AXZYymY8qadCtaJ4RsrPw3uNGYVOuWQ) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As an admin reviewer,
I want approve/reject thay đổi reference data,
so that không có chỉnh sửa chưa kiểm soát đi thẳng vào production.

## Acceptance Criteria

1. **Given** có change set ở trạng thái `pending`, **When** reviewer chọn approve, **Then** trạng thái = `approved` và data có hiệu lực ngay.
2. **Given** reviewer chọn reject, **When** với lý do, **Then** status = `rejected` và data không thay đổi.
3. **Given** change set approved, **When** rule engine phân loại metrics, **Then** chỉ dùng approved data.
4. **Given** chính admin tạo change set, **When** thử approve change set của chính mình, **Then** hệ thống cho phép (single admin) hoặc require different user (multi-admin config).

## Tasks / Subtasks

- [x] Task 1 — Backend: Approval workflow endpoints (AC: #1, #2, #3)
  - [x] `GET /api/v1/admin/change-sets?status=pending` — danh sách change sets chờ duyệt
  - [x] `POST /api/v1/admin/change-sets/{id}/approve` — approve, apply changes to production data
  - [x] `POST /api/v1/admin/change-sets/{id}/reject` với body `{ reason }` — reject
  - [x] Khi approve: apply changes_json vào bảng reference data thực
  - [x] Ghi audit log cho mỗi approve/reject action
- [x] Task 2 — Web: Approval queue page (AC: #1, #2)
  - [x] Tạo `apps/web/src/app/admin/reference-data/approvals/page.tsx`
  - [x] Table: entity type, operation, changes summary, created by, created at, actions
  - [x] "Chi tiết" expand để xem diff của changes_json
  - [x] Nút "Phê duyệt" và "Từ chối" (reject form với reason field)
- [x] Task 3 — Tests (AC: #1, #2, #3)
  - [x] `ChangeSetServiceTest`: approve applies data, reject does not, audit logging

## Dev Notes

### Apply Changes Logic

```java
@Transactional
void approveChangeSet(UUID changeSetId, UUID reviewerId) {
    ChangeSet cs = repo.findById(changeSetId).orElseThrow();
    
    // Apply changes based on entity type and operation
    if ("METRIC".equals(cs.entityType)) {
        if ("CREATE".equals(cs.operation)) {
            referenceMetricRepo.save(parseMetric(cs.changesJson));
        } else if ("UPDATE".equals(cs.operation)) {
            referenceMetricRepo.findById(cs.entityId).ifPresent(m -> updateMetric(m, cs.changesJson));
        }
        // etc.
    }
    
    cs.setStatus("approved");
    cs.setReviewedAt(LocalDateTime.now());
    cs.setReviewerId(reviewerId);
    repo.save(cs);
    
    auditLog("APPROVE_CHANGE_SET", changeSetId, reviewerId);
}
```

### References

- [Source: architecture.md#Chiến-Lược-Audit-Logging]
- [Source: epics.md#Story-7.4]

## Dev Agent Record

### Agent Model Used

Claude Opus 4.6 (Thinking)

### Debug Log References

### Completion Notes List

- **Task 1 — Backend:** Implemented full approval workflow with endpoints: `GET /change-sets` (list pending), `POST /change-sets/{id}/approve`, `POST /change-sets/{id}/reject`, `POST /change-sets/{id}/submit`, and `POST /change-sets/{id}/publish`. The `isMultiAdminMode()` logic automatically detects if there are multiple admins. In single-admin mode, drafts are published directly without self-review. In multi-admin mode, self-approval is blocked and drafts must go through the queue.
- **Task 2 — Web:** Created approval queue page at `/admin/reference-data/approvals` which shows an informational message in single-admin mode, and the full queue in multi-admin mode. Updated the existing reference data page's `PendingChangeSetPanel` to show either "Kích hoạt" (single-admin) or "Gửi duyệt" (multi-admin).
- **Task 3 — Tests:** Added comprehensive tests for all single/multi admin rules, including multi-admin self-approval blocks, single-admin direct publishing, and status transitions. All 18 tests pass.

### Change Log

- 2026-05-10: Story 7.4 implementation complete — multi-admin aware approval workflow backend, web UI, and tests.

### File List

- `apps/api/src/main/resources/db/migration/V025__approval_workflow_columns.sql` (new)
- `apps/api/src/main/java/com/healthlens/api/entity/ReferenceDataChangeSet.java` (modified — added reviewerId, rejectionReason)
- `apps/api/src/main/java/com/healthlens/api/repository/UserRepository.java` (modified — added countByRole)
- `apps/api/src/main/java/com/healthlens/api/repository/ReferenceDataChangeSetRepository.java` (modified — added query methods)
- `apps/api/src/main/java/com/healthlens/api/dto/request/AdminRejectChangeSetRequest.java` (new)
- `apps/api/src/main/java/com/healthlens/api/dto/response/AdminChangeSetDetailResponse.java` (new)
- `apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java` (modified — added approval/publish methods and multi-admin logic)
- `apps/api/src/main/java/com/healthlens/api/controller/AdminReferenceDataController.java` (modified — added approval/publish/config endpoints)
- `apps/api/src/test/java/com/healthlens/api/service/ReferenceDataAdminServiceTest.java` (modified — added single/multi admin tests)
- `packages/shared/constants/api.ts` (modified — added change-set API paths)
- `apps/web/src/app/admin/reference-data/approvals/page.tsx` (new — multi-admin aware)
- `apps/web/src/app/admin/reference-data/page.tsx` (modified — dynamically shows Publish or Submit button)
- `apps/web/src/app/admin/layout.tsx` (modified — added Phê duyệt nav item)
