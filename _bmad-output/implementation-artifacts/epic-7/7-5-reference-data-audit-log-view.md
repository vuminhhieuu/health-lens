# Story 7.5: Audit log đầy đủ cho reference data

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

**Phạm vi triển khai (mở rộng):** UI có tab **Dữ liệu tham chiếu** (mặc định, FR37) và **Toàn hệ thống** (mọi `resource_type` ghi vào `audit_logs`). Route web: `/admin/audit-log` (`?view=all` cho tab toàn hệ thống; mặc định hoặc `?view=reference` cho tab reference). Liên kết từ `/admin/reference-data` → `/admin/audit-log?view=reference`.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Nhật ký Hoạt động - Admin HealthLens | `projects/578519912546445367/screens/d0690e9e60b74787b60020aaadd6c0e4` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzkzNDljMWNhMmM2YTQ0YTFiYTI2NzVmZjgzYTI0M2U4EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uiGZFYKa1dEEgBAJSLmkHMv8W8aCh1AumtZchMO1pen2DpoqwLot13JWFtf25KngUGIhhgdMzOvt1-JHQs1nmi8jgs2Z5hPiz00TyX2eA4-z4MRip1QSH_IN35xS4kfoZ5q4xZWrnujXdC01TEPuAlh4ZncstpBREWxKZ6A63QGBZo8Mn32ZeHJ66rnzmWuMOBtI8CP8lCfCTHx2O0H9T8wD947myDhj_Badem6fNxTWFLs07sG9lem4A) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As an admin,
I want xem lịch sử thay đổi reference data (và tuỳ chọn toàn bộ hoạt động hệ thống),
so that tôi truy vết ai đã sửa gì và khi nào.

## Acceptance Criteria

1. **Given** có thao tác quản trị, **When** mở trang audit log, **Then** bảng hiển thị: **Thời gian**, **Người dùng** (actor), **Hành động**, **Trạng thái** (Thành công / Thất bại từ `outcome` API), **Tóm tắt**, **IP**; link **Xem chi tiết** mở modal với đối tượng (`entityLabel`), metadata và diff JSON before/after.
2. **Given** audit log page, **When** filter theo thời gian, chỉ số (tab reference), loại tài nguyên (tab toàn hệ thống), người thực hiện, loại hành động — **Then** danh sách được lọc đúng (tab reference luôn gửi `resourceType=REFERENCE_DATA`; dropdown hành động trên tab reference chỉ gồm nhóm dữ liệu tham chiếu).
3. **Given** audit log entry, **When** click "Xem chi tiết", **Then** xem before/after JSON và metadata (thời gian, người thực hiện, hành động, đối tượng, IP).
4. **Given** audit log, **When** export CSV, **Then** download `system-audit-logs.csv` với các cột API export: `id`, `actorEmail`, `action`, `resourceType`, `resourceId`, `entityLabel`, `detailSummary`, `outcome`, `oldValueJson`, `newValueJson`, `ipAddress`, `createdAt`.

## Tasks / Subtasks

- [x] Task 1 — Backend: Audit log table + query endpoint (AC: #1, #2)
  - [x] Flyway migration `V034__create_audit_logs_table.sql`
  - [x] Schema: id, actor_id, action, resource_type, resource_id, old_value_json, new_value_json, ip_address, user_agent, created_at
  - [x] `GET /api/v1/admin/audit-logs` — query: `resourceType`, `resourceId`, `actorEmail`, `action`, `from`, `to`, `page` (default 0), `limit` (default 20, max 200)
  - [x] `AuditLogEntryDto` gồm `entityLabel`, `detailSummary`, `outcome` (`SUCCESS` / `FAILURE` qua `AuditOutcome.fromAction`)
  - [x] `AuditEventRecorder` + mở rộng `@Auditable` / `AuditableAspect` cho unified `audit_logs`
- [x] Task 2 — Backend: Export CSV (AC: #4)
  - [x] `GET /api/v1/admin/audit-logs/export?...` → stream CSV (UTF-8 BOM), `maxRows` default 10000 max 50000
  - [x] Dùng Apache Commons CSV; header khớp DTO (có `outcome`)
- [x] Task 3 — Web: Audit log page (AC: #1–#4)
  - [x] `apps/web/src/app/admin/audit-log/page.tsx`
  - [x] Tab phạm vi: Dữ liệu tham chiếu / Toàn hệ thống
  - [x] Bộ lọc: từ/đến ngày, chỉ số, loại tài nguyên (tab all), người thực hiện, loại hành động; chip + Đặt lại / Áp dụng
  - [x] Bảng + modal Chi tiết + Xuất CSV + reload (icon only) + phân trang (`page` / `limit`)
  - [x] Select filter có mũi tên dropdown; header bảng `whitespace-nowrap`
  - [x] Filter chỉ số: `GET /api/v1/admin/reference-metrics` (dropdown)
- [x] Task 4 — Tests (AC: #1, #2, #4)
  - [x] `AdminAuditLogServiceTest` (query, labels, `outcome`, UTC boundaries, CSV header/row)
  - [x] `AuditableAspectTest`, `AuditEventRecorderTest`

## Dev Notes

### Audit Log Table

```sql
-- V034__create_audit_logs_table.sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
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

### Trạng thái xử lý (`outcome`)

- Không có cột DB riêng; suy ra từ `action` khi map DTO.
- **FAILURE:** `LOGIN_FAILED` hoặc action kết thúc `_FAILED` (sự kiện thất bại được ghi chủ động, ví dụ đăng nhập sai).
- **SUCCESS:** mọi bản ghi audit còn lại (gồm `REJECT_CHANGE_SET` — từ chối phê duyệt là thao tác thành công, không phải lỗi hệ thống).

### UI bảng vs modal vs CSV

| Nơi hiển thị | Cột / field |
|---|---|
| Bảng danh sách | Thời gian, Người dùng, Hành động, Trạng thái, Tóm tắt (+ Xem chi tiết), IP |
| Modal Chi tiết | Thời gian, Người thực hiện, Hành động, Đối tượng, IP, JSON before/after (không lặp cột Trạng thái) |
| CSV export | Đầy đủ DTO kể cả `entityLabel`, `outcome` |

### References

- [Source: architecture.md#Chiến-Lược-Audit-Logging]
- [Source: epics.md#Story-7.5]

## Dev Agent Record

### Agent Model Used

Auto (Cursor)

### Debug Log References

- `./gradlew.bat test --tests "com.healthlens.api.aspect.AuditableAspectTest" --tests "com.healthlens.api.audit.AuditEventRecorderTest" --tests "com.healthlens.api.service.admin.AdminAuditLogServiceTest" --no-daemon`
- `pnpm --filter web lint`

### Completion Notes List

- Unified `audit_logs` (migration **V034**), `AdminAuditLogService` list/export, `AuditEventRecorder` ghi từ nhiều domain service.
- API trả `outcome` (`AuditOutcome`); web hiển thị **Thành công** / **Thất bại** trên bảng.
- Web `/admin/audit-log`: tab reference / toàn hệ thống, lọc đầy đủ, bảng không có cột Đối tượng (chỉ trong modal + CSV), modal diff JSON, export CSV.
- UI admin slate/teal; select có chevron; nút tải lại chỉ icon.

### File List

- `apps/api/src/main/resources/db/migration/V034__create_audit_logs_table.sql`
- `apps/api/build.gradle.kts`
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`
- `apps/api/src/main/java/com/healthlens/api/annotation/Auditable.java`
- `apps/api/src/main/java/com/healthlens/api/aspect/AuditableAspect.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditActions.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditOutcome.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditResourceTypes.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditEventRecorder.java`
- `apps/api/src/main/java/com/healthlens/api/audit/UnifiedAuditLogWriter.java`
- `apps/api/src/main/java/com/healthlens/api/audit/UnifiedAuditSnapshot.java`
- `apps/api/src/main/java/com/healthlens/api/entity/AuditLog.java`
- `apps/api/src/main/java/com/healthlens/api/repository/AuditLogRepository.java`
- `apps/api/src/main/java/com/healthlens/api/dto/admin/AuditLogEntryDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/admin/AuditLogPageDto.java`
- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminAuditLogService.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/AdminAuditLogController.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/AdminReferenceMetricController.java` (filter chỉ số trên UI)
- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminReferenceMetricService.java` (filter chỉ số trên UI)
- `apps/api/src/test/java/com/healthlens/api/aspect/AuditableAspectTest.java`
- `apps/api/src/test/java/com/healthlens/api/audit/AuditEventRecorderTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/admin/AdminAuditLogServiceTest.java`
- `apps/web/src/app/admin/layout.tsx`
- `apps/web/src/app/admin/audit-log/page.tsx`
- `packages/shared/constants/api.ts`

### Change Log

- 2026-05-10: Hoàn thành implementation Story 7.5.
- 2026-05-17: Đồng bộ tài liệu với code — `outcome` API/CSV, cột Trạng thái bảng, bỏ cột Đối tượng khỏi bảng, UI filter/reload, test CSV/outcome.
- 2026-05-17: Rà soát đồng bộ — bổ sung phân trang, filter hành động tab reference, link reference-data, ghi chú modal.
