# Story 7.4: Phê duyệt thay đổi trước khi có hiệu lực

Status: ready-for-dev

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

- [ ] Task 1 — Backend: Approval workflow endpoints (AC: #1, #2, #3)
  - [ ] `GET /api/v1/admin/change-sets?status=pending` — danh sách change sets chờ duyệt
  - [ ] `POST /api/v1/admin/change-sets/{id}/approve` — approve, apply changes to production data
  - [ ] `POST /api/v1/admin/change-sets/{id}/reject` với body `{ reason }` — reject
  - [ ] Khi approve: apply changes_json vào bảng reference data thực
  - [ ] Ghi audit log cho mỗi approve/reject action
- [ ] Task 2 — Web: Approval queue page (AC: #1, #2)
  - [ ] Tạo `apps/web/src/app/admin/reference-data/approvals/page.tsx`
  - [ ] Table: entity type, operation, changes summary, created by, created at, actions
  - [ ] "Chi tiết" expand để xem diff của changes_json
  - [ ] Nút "Phê duyệt" và "Từ chối" (reject form với reason field)
- [ ] Task 3 — Tests (AC: #1, #2, #3)
  - [ ] `ChangeSetServiceTest`: approve applies data, reject does not, audit logging

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

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
