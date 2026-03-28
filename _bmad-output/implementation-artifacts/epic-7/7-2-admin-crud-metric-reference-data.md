# Story 7.2: CRUD chỉ số y tế và ngưỡng tham chiếu

Status: ready-for-dev

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

- [ ] Task 1 — Backend: Reference data CRUD endpoints (AC: #1, #2, #3, #4)
  - [ ] Thêm cột `status` (`active`/`draft`/`deactivated`) vào `reference_metrics`
  - [ ] Thêm bảng `reference_data_change_sets(id, admin_id, changes_json, status, created_at, approved_at)` (Flyway V016)
  - [ ] `GET /api/v1/admin/reference-data/metrics` — danh sách tất cả metrics (kể cả draft)
  - [ ] `POST /api/v1/admin/reference-data/metrics` — tạo metric draft
  - [ ] `PUT /api/v1/admin/reference-data/metrics/{id}` — update → tạo change set draft
  - [ ] `DELETE /api/v1/admin/reference-data/metrics/{id}` → set status `deactivated`
  - [ ] Validate: minValue < maxValue, không negative cho các chỉ số như Glucose
- [ ] Task 2 — Web: Admin Reference Data page (AC: #1, #2, #3, #4)
  - [ ] Tạo `apps/web/src/app/admin/reference-data/page.tsx`
  - [ ] Table với columns: tên, đơn vị, số ranges, status, actions
  - [ ] "Thêm chỉ số" form, "Sửa" inline hoặc modal, "Xóa" có confirm
  - [ ] Edit range trong expandable row
- [ ] Task 3 — Tests (AC: #1, #2, #4)
  - [ ] `ReferenceDataAdminServiceTest`: CRUD, validation, deactivation

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

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
