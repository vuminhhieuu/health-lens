# Story 7.2: CRUD chỉ số y tế và ngưỡng tham chiếu

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Dữ liệu Tham chiếu - Admin HealthLens | `projects/578519912546445367/screens/bb3084187f0e41d5b4ac0617a52da22c` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzE0YTgwNGNlZDlkNzRiOTJiNmNlNWQwNTVmYWI2NzM3EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uh42o7fYc6nqTeJBKoLQvRB7lNnAYSUfcovOLPdJpl6DwDxqOb2JEVXIBoBepBYWDYZcaoKqLXP8F57mOqzfFN2esILvzK-n4S1qJq79DuhmfknojzMpIbMsrkvUvIPVmlhAyaYCTq8MHyv-ih2wKuv3TXb6SGQQnrKZ2G8OdjR6w_tRi-_qfWUrZnKGopQ86ClNeKONYGWu-kImXgPTpVHGzUaAQRzbdf-a_zu9TlC3oOLxZRwokJ-) |
| Chỉnh sửa Chỉ số - Admin HealthLens | `projects/578519912546445367/screens/7c23ef03075048afb2fddab20e12afd6` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzhiMmJjZTQzOTQxZjQxOWI5NjQ2MzEyNDliM2FjOTQ2EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uiOEVlTV-RYlrrZTzyHTLjzUTLYAtTByji201yMfxskYSzJzpELN9J9GS25lgSQEn8u768pN6XqrgXulqoZ8E9qQ9qWjhQWSuy4fTRZxqlM1roPT_vONpJ03T76HHGLUYHUonw4BVmGrC8fD25ToLxv16VP_NtPZlRVu4G27_Pe-wswbYqDIj5GlMNAFFvAGDLq0sNiLp12j_I0g4j2FCRm69g3cPl_fOtv3RfeqEvSkQhTjjVFmBOEUg) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As an admin,
I want quản lý danh mục chỉ số và reference ranges,
so that hệ thống diễn giải kết quả đúng chuẩn.

## Acceptance Criteria

1. **Given** admin đã đăng nhập hợp lệ, **When** tạo chỉ số mới, **Then** chỉ số được lưu với trạng thái `draft` cho đến khi được approve (Story 7.4).
2. **Given** admin sửa ngưỡng tham chiếu, **When** save, **Then** tạo change set `draft`, không ảnh hưởng production ngay.
3. **Given** admin xóa chỉ số, **When** có health records đang dùng chỉ số đó, **Then** soft delete (deactivate) thay vì hard delete.
4. **Given** nhập giá trị không hợp lệ (min > max, negative range cho glucose), **When** submit, **Then** validation error rõ.

## Tasks / Subtasks

- [x] Task 1 — Backend: Reference data CRUD endpoints (AC: #1, #2, #3, #4)
  - [x] Thêm cột `status` (`active`/`draft`/`deactivated`) vào `reference_metrics`
  - [x] Thêm bảng `reference_data_change_sets(id, admin_id, changes_json, status, created_at, approved_at)` (Flyway V016)
  - [x] `GET /api/v1/admin/reference-data/metrics` — danh sách tất cả metrics (kể cả draft)
  - [x] `POST /api/v1/admin/reference-data/metrics` — tạo metric draft
  - [x] `PUT /api/v1/admin/reference-data/metrics/{id}` — update → tạo change set draft
  - [x] `DELETE /api/v1/admin/reference-data/metrics/{id}` → set status `deactivated`
  - [x] Validate: minValue < maxValue, không negative cho các chỉ số như Glucose
- [x] Task 2 — Web: Admin Reference Data page (AC: #1, #2, #3, #4)
  - [x] Tạo `apps/web/src/app/admin/reference-data/page.tsx`
  - [x] Table với columns: tên, đơn vị, số ranges, status, actions
  - [x] "Thêm chỉ số" form, "Sửa" inline hoặc modal, "Xóa" có confirm
  - [x] Edit range trong expandable row
- [x] Task 3 — Tests (AC: #1, #2, #4)
  - [x] `ReferenceDataAdminServiceTest`: CRUD, validation, deactivation

## Dev Notes

### Database

```sql
-- V016__create_reference_data_change_sets.sql
CREATE TABLE reference_data_change_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES users(id),
    entity_type VARCHAR(50) NOT NULL,  -- 'METRIC' or 'RANGE'
    entity_id UUID,
    operation VARCHAR(20) NOT NULL,    -- 'CREATE' 'UPDATE' 'DELETE'
    changes_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',  -- pending/approved/rejected
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMP,
    reviewer_id UUID REFERENCES users(id)
);
```

### Validation Rules (Backend)

```java
void validateRange(ReferenceRange range) {
    if (range.getMinValue() >= range.getMaxValue())
        throw new ValidationException("min phải nhỏ hơn max");
    if (range.getMinValue() < 0 && !isNegativeAllowed(range.getMetricName()))
        throw new ValidationException("Không cho phép ngưỡng âm cho chỉ số này");
}
```

### References

- [Source: epics.md#Story-7.2]
- [Source: architecture.md#Chiến-Lược-Audit-Logging]

## Dev Agent Record

### Agent Model Used

GPT-5

### Debug Log References

- `./gradlew test --tests com.healthlens.api.service.ReferenceDataAdminServiceTest --tests com.healthlens.api.service.ReferenceDataServiceTest`
- `pnpm lint src/app/admin/reference-data/page.tsx`
- `pnpm exec eslint src/app/admin/reference-data/page.tsx src/lib/api/adminApiClient.ts`

### Completion Notes List

- Thêm migration `V024__admin_reference_data_drafts.sql` để mở rộng `reference_metrics.status` và tạo bảng `reference_data_change_sets`. Dùng `V024` thay cho `V016` vì project đã có migration đến `V022`.
- Bổ sung `ReferenceDataAdminService` và `AdminReferenceDataController` cho flow list/create/update draft/deactivate metric dành cho admin.
- Cập nhật `ReferenceDataService` để loại trừ metric draft khỏi luồng reference-data public nhưng vẫn giữ tương thích với test/mock cũ.
- Sửa mapping `jsonb` cho `reference_data_change_sets.changes_json` để tránh lỗi runtime với PostgreSQL khi create/update change set.
- Hoàn thiện flow admin: metric draft được sửa trực tiếp, metric deactivated có thể kích hoạt lại, và màn hình admin tách riêng admin API client để redirect đúng về `/admin/login`.
- Thay placeholder admin page bằng màn hình quản lý metric hoàn chỉnh: modal tạo/sửa, hỗ trợ nhiều ranges, popup xác nhận thay cho `window.confirm`, Việt hóa UI và nhóm chỉ số theo trạng thái.

### File List

- apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java
- apps/api/src/main/java/com/healthlens/api/controller/AdminReferenceDataController.java
- apps/api/src/main/java/com/healthlens/api/dto/request/AdminReferenceMetricRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/request/AdminReferenceRangeRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/response/AdminReferenceChangeSetResponse.java
- apps/api/src/main/java/com/healthlens/api/dto/response/AdminReferenceMetricResponse.java
- apps/api/src/main/java/com/healthlens/api/dto/response/AdminReferenceRangeResponse.java
- apps/api/src/main/java/com/healthlens/api/entity/ReferenceDataChangeSet.java
- apps/api/src/main/java/com/healthlens/api/entity/ReferenceMetric.java
- apps/api/src/main/java/com/healthlens/api/repository/ReferenceDataChangeSetRepository.java
- apps/api/src/main/java/com/healthlens/api/repository/ReferenceMetricAliasRepository.java
- apps/api/src/main/java/com/healthlens/api/repository/ReferenceMetricRepository.java
- apps/api/src/main/java/com/healthlens/api/repository/ReferenceRangeRepository.java
- apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java
- apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java
- apps/api/src/main/resources/db/migration/V023__admin_reference_data_drafts.sql
- apps/api/src/test/java/com/healthlens/api/service/ReferenceDataAdminServiceTest.java
- apps/web/src/app/admin/reference-data/page.tsx
- apps/web/src/lib/api/adminApiClient.ts
- apps/web/src/lib/api/routes.ts
- packages/shared/constants/api.ts

### Change Log

- 2026-05-09: Implemented Story 7.2 admin reference-data CRUD, draft change sets, deactivation flow, and admin UI integration.
- 2026-05-10: Completed Story 7.2 flow hardening with JSONB persistence fix, multi-range admin editing, admin-specific API client, confirmation popup, and metric reactivation flow.
