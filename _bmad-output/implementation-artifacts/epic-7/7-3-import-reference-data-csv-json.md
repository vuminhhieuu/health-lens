# Story 7.3: Import reference data từ CSV/JSON

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Dữ liệu Tham chiếu - Admin HealthLens | `projects/578519912546445367/screens/bb3084187f0e41d5b4ac0617a52da22c` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzE0YTgwNGNlZDlkNzRiOTJiNmNlNWQwNTVmYWI2NzM3EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uh42o7fYc6nqTeJBKoLQvRB7lNnAYSUfcovOLPdJpl6DwDxqOb2JEVXIBoBepBYWDYZcaoKqLXP8F57mOqzfFN2esILvzK-n4S1qJq79DuhmfknojzMpIbMsrkvUvIPVmlhAyaYCTq8MHyv-ih2wKuv3TXb6SGQQnrKZ2G8OdjR6w_tRi-_qfWUrZnKGopQ86ClNeKONYGWu-kImXgPTpVHGzUaAQRzbdf-a_zu9TlC3oOLxZRwokJ-) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As an admin,
I want import dữ liệu chuẩn hàng loạt,
so that cập nhật danh mục nhanh và nhất quán.

## Acceptance Criteria

1. **Given** file CSV hoặc JSON đúng format, **When** upload, **Then** hệ thống parse, validate và hiển thị preview trước khi áp dụng.
2. **Given** file có dòng lỗi (sai format, giá trị không hợp lệ), **When** parse, **Then** trả về danh sách dòng lỗi với số dòng và lý do lỗi.
3. **Given** preview hợp lệ, **When** admin xác nhận import, **Then** tạo change set draft cho tất cả items (cần approve ở Story 7.4).
4. **Given** file quá 5MB hoặc sai định dạng, **When** upload, **Then** client-side rejection trước khi gửi lên server.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Import parse + validate endpoint (AC: #1, #2)
  - [ ] `POST /api/v1/admin/reference-data/import/preview` (multipart upload)
  - [ ] Support CSV và JSON format
  - [ ] Parse → validate từng row (metricName, unit, min, max, gender, ageMin, ageMax)
  - [ ] Trả về: `{ validRows: [...], errorRows: [{ line, error }] }`
- [ ] Task 2 — Backend: Confirm import (AC: #3)
  - [ ] `POST /api/v1/admin/reference-data/import/confirm` với body `{ importId }`
  - [ ] Tạo change set draft cho mỗi valid row
- [ ] Task 3 — Web: Import UI (AC: #1, #2, #3, #4)
  - [ ] Trang `apps/web/src/app/admin/reference-data/import/page.tsx`
  - [ ] Drag-and-drop file upload hoặc file picker
  - [ ] Preview table: valid rows (xanh) + error rows (đỏ)
  - [ ] "Xác nhận import" button → call confirm endpoint

## Dev Notes

### CSV Format Example

```csv
metricName,displayNameVi,unit,minValue,maxValue,gender,minAge,maxAge
Glucose,Đường huyết lúc đói,mmol/L,3.9,6.1,,18,
Glucose,Đường huyết lúc đói,mmol/L,3.9,5.5,female,18,45
HbA1c,HbA1c,%,0,5.7,,,
```

### JSON Format Example

```json
[
  { "metricName": "Glucose", "unit": "mmol/L", "minValue": 3.9, "maxValue": 6.1 },
  { "metricName": "HbA1c", "unit": "%", "minValue": 0, "maxValue": 5.7 }
]
```

### References

- [Source: epics.md#Story-7.3]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
