# Story 7.5: Audit log đầy đủ cho reference data

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Nhật ký Hoạt động - Admin HealthLens | `projects/578519912546445367/screens/d0690e9e60b74787b60020aaadd6c0e4` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzkzNDljMWNhMmM2YTQ0YTFiYTI2NzVmZjgzYTI0M2U4EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uiGZFYKa1dEEgBAJSLmkHMv8W8aCh1AumtZchMO1pen2DpoqwLot13JWFtf25KngUGIhhgdMzOvt1-JHQs1nmi8jgs2Z5hPiz00TyX2eA4-z4MRip1QSH_IN35xS4kfoZ5q4xZWrnujXdC01TEPuAlh4ZncstpBREWxKZ6A63QGBZo8Mn32ZeHJ66rnzmWuMOBtI8CP8lCfCTHx2O0H9T8wD947myDhj_Badem6fNxTWFLs07sG9lem4A) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As an admin,
I want xem lịch sử thay đổi reference data,
so that tôi truy vết ai đã sửa gì và khi nào.

## Acceptance Criteria

1. **Given** có thao tác quản trị trên reference data, **When** mở trang audit log, **Then** hệ thống hiển thị actor, timestamp, operation, entity, diff, source IP.
2. **Given** audit log page, **When** filter theo thời gian và chỉ số, **Then** danh sách được lọc đúng.
3. **Given** audit log entry, **When** click "Chi tiết", **Then** xem before/after diff của thay đổi.
4. **Given** audit log, **When** export CSV, **Then** download file với đầy đủ columns.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Audit log table + query endpoint (AC: #1, #2)
  - [ ] Flyway migration `V017__create_audit_logs_table.sql`
  - [ ] Schema: id, actor_id, action, resource_type, resource_id, old_value_json, new_value_json, ip_address, timestamp
  - [ ] `GET /api/v1/admin/audit-logs?resourceType=REFERENCE_DATA&from={date}&to={date}&page=0&limit=50`
  - [ ] Spring AOP nghiêng `@Auditable` tự động ghi audit khi có annotation
- [ ] Task 2 — Backend: Export CSV (AC: #4)
  - [ ] `GET /api/v1/admin/audit-logs/export?...` → stream CSV download
  - [ ] Dùng Apache Commons CSV hoặc OpenCSV
- [ ] Task 3 — Web: Audit log page (AC: #1, #2, #3, #4)
  - [ ] Tạo `apps/web/src/app/admin/audit-log/page.tsx`
  - [ ] Date range picker, filter by resource type
  - [ ] Table: actor email, action, entity, timestamp, IP, [Chi tiết]
  - [ ] Detail modal: before/after diff (JSON diff view)
  - [ ] "Export CSV" button
- [ ] Task 4 — Tests (AC: #1, #2)
  - [ ] `AuditLogServiceTest`: query with filters, pagination

## Dev Notes

### Audit Log Table

```sql
-- V017__create_audit_logs_table.sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,         -- e.g., 'APPROVE_CHANGE_SET', 'DELETE_METRIC'
    resource_type VARCHAR(50) NOT NULL,   -- 'REFERENCE_DATA', 'HEALTH_RECORD', etc.
    resource_id UUID,
    old_value_json JSONB,
    new_value_json JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
```

### Spring AOP Audit

```java
@Aspect
@Component
public class AuditAspect {
    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        // Extract user from SecurityContext, method args, IP from request
        // Execute method
        // Log to audit_logs table
    }
}
```

### References

- [Source: architecture.md#Chiến-Lược-Audit-Logging]
- [Source: epics.md#Story-7.5]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
