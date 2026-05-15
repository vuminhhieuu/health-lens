# Story 7.5: Audit log đầy đủ cho reference data

Status: review

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

- [x] Task 1 — Backend: Audit log table + query endpoint (AC: #1, #2)
  - [x] Flyway migration `V021__create_audit_logs_table.sql`
  - [x] Schema: id, actor_id, action, resource_type, resource_id, old_value_json, new_value_json, ip_address, timestamp
  - [x] `GET /api/v1/admin/audit-logs?resourceType=REFERENCE_DATA&from={date}&to={date}&page=0&limit=50`
  - [x] Spring AOP mở rộng `@Auditable` để ghi audit khi có annotation + unified resource type
- [x] Task 2 — Backend: Export CSV (AC: #4)
  - [x] `GET /api/v1/admin/audit-logs/export?...` → stream CSV download
  - [x] Dùng Apache Commons CSV
- [x] Task 3 — Web: Audit log page (AC: #1, #2, #3, #4)
  - [x] Tạo `apps/web/src/app/admin/audit-log/page.tsx`
  - [x] Date range picker, filter by resource type + metric resourceId + actor email
  - [x] Table: actor email, action, entity, timestamp, IP, [Chi tiết]
  - [x] Detail modal: before/after diff (JSON diff view)
  - [x] "Export CSV" button
- [x] Task 4 — Tests (AC: #1, #2)
  - [x] `AdminAuditLogServiceTest`: query with filters, pagination + date boundary helpers

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

Auto (Cursor)

### Debug Log References

- `./gradlew.bat test --tests "com.healthlens.api.aspect.AuditableAspectTest" --tests "com.healthlens.api.service.admin.AdminAuditLogServiceTest" --no-daemon` ✅
- `pnpm --filter web lint` ✅ (chỉ còn warning cũ ở `src/app/(dashboard)/health-records/page.tsx`, không thuộc story này)

### Completion Notes List

- Added unified admin audit foundation:
  - migration `V021__create_audit_logs_table.sql`
  - `AuditLog` entity + repository + admin DTOs + `AdminAuditLogService`
  - `AdminAuditLogController` list/export endpoints with date filters, resource filters, pagination, and CSV streaming.
- Extended annotation/aspect flow:
  - `@Auditable` supports `unifiedResourceType`
  - `AuditableAspect` now supports both legacy health-record audit and unified `audit_logs` writes using `SecurityContext` actor + request IP/User-Agent.
  - Added `UnifiedAuditSnapshot` thread-local payload for old/new JSON snapshots.
- Added admin reference metric mutation endpoint to generate real `REFERENCE_DATA` audit rows:
  - `PATCH /api/v1/admin/reference-metrics/{metricId}/display`
  - `GET /api/v1/admin/reference-metrics` for web filter options.
- Added web admin UI according to Stitch story:
  - new admin layout guard + role guidance
  - audit log page with date/resource/metric/actor filters, table, detail modal with before/after JSON, CSV export button.
- Security and routes:
  - `ApiRoutes.ADMIN_*` constants
  - `SecurityConfig` now enforces `ROLE_ADMIN` for `/api/v1/admin/**`
  - shared frontend API constants include admin endpoints.

### File List

- `apps/api/src/main/resources/db/migration/V021__create_audit_logs_table.sql`
- `apps/api/build.gradle.kts`
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`
- `apps/api/src/main/java/com/healthlens/api/entity/UserRole.java`
- `apps/api/src/main/java/com/healthlens/api/annotation/Auditable.java`
- `apps/api/src/main/java/com/healthlens/api/aspect/AuditableAspect.java`
- `apps/api/src/main/java/com/healthlens/api/entity/AuditLog.java`
- `apps/api/src/main/java/com/healthlens/api/repository/AuditLogRepository.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditActions.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditResourceTypes.java`
- `apps/api/src/main/java/com/healthlens/api/audit/UnifiedAuditSnapshot.java`
- `apps/api/src/main/java/com/healthlens/api/dto/admin/AuditLogEntryDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/admin/AuditLogPageDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/admin/ReferenceMetricAdminDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/UpdateReferenceMetricDisplayRequest.java`
- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminAuditLogService.java`
- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminReferenceMetricService.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/AdminAuditLogController.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/AdminReferenceMetricController.java`
- `apps/api/src/test/java/com/healthlens/api/aspect/AuditableAspectTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/admin/AdminAuditLogServiceTest.java`
- `apps/web/src/app/admin/layout.tsx`
- `apps/web/src/app/admin/audit-log/page.tsx`
- `packages/shared/constants/api.ts`

### Change Log

- 2026-05-10: Completed Story 7.5 implementation (backend audit infrastructure + admin APIs + web audit page + tests), status moved to `review`.
