# Story 3.4: Chỉnh sửa thủ công khi OCR thiếu hoặc sai

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Nhập dữ liệu thủ công - HealthLens | `projects/578519912546445367/screens/1ba2a148df7d427d89fdaaf223deaafd` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzAwNWFjMTAyYjQ4MzQxZWZiNjViZWZiODUyY2JkZjg5EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugHKI235OvMHrVJ-VTeZzIUbdNdz-8WxK5BmbfNZfMI0cV4nhMsiL-XyBJ-yBAQROAFd_Y0H7gSSZDpp45hhGuI1zm6MtNoXY19tzOfGc0ChsObDfdnrrr4U-AAFhPlWN1NNjFPLbRQhH-XOM58FxZ-g0c6DAqO-8FNkR0b2tFvQJQ4Un1xbZjLrEsBcypc4At1-fYP9C7dIHdRl0-xqg038fnhSe5VAVf25T9D_ahK8l1D4qQG2NL4dA) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want sửa chỉ số OCR hoặc nhập tay chỉ số bị thiếu,
so that bản ghi cuối cùng phản ánh đúng kết quả khám.

## Acceptance Criteria

1. **Given** màn hình review OCR có dữ liệu thiếu hoặc sai, **When** người dùng tap vào chỉ số để chỉnh sửa, **Then** inline edit field xuất hiện với giá trị hiện tại.
2. **Given** edit field, **When** người dùng cập nhật giá trị và xác nhận, **Then** giá trị được cập nhật trong UI và nguồn chỉ số trở thành `manual`.
3. **Given** cần thêm chỉ số chưa có trong danh sách OCR, **When** nhấn "Thêm chỉ số", **Then** form với dropdown tên chỉ số (từ reference data) và number input.
4. **Given** dữ liệu nhập không hợp lệ (text thay vì số), **When** submit, **Then** validation error rõ ràng.
5. **Given** confirm lưu bản ghi đã chỉnh sửa, **When** kiểm tra DB, **Then** `source_type = 'mixed'` nếu có cả OCR lẫn manual, từng metric có `source` tag đúng.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Update metrics endpoint (AC: #1, #2, #5)
  - [ ] `PUT /api/v1/health-records/{recordId}/metrics` với body `{ metrics: [...] }`
  - [ ] Mỗi metric: `{ name, value, unit, source: "ocr"|"manual" }`
  - [ ] Tính toán lại `source_type`: tất cả ocr → `ocr`, tất cả manual → `manual`, mixed → `mixed`
  - [ ] Lưu updated metrics vào JSONB
- [ ] Task 2 — Backend: Reference metric names API (AC: #3)
  - [ ] `GET /api/v1/reference-data/metrics` → danh sách metric names chuẩn (từ bảng reference_metrics)
  - [ ] Có thể triển khai đơn giản với hardcoded list trước khi có Admin panel (Epic 7)
- [ ] Task 3 — Web: Inline edit + add metric form (AC: #1, #2, #3, #4)
  - [ ] MetricEditRow component: click để edit, input number, confirm/cancel
  - [ ] "Thêm chỉ số" button → Dialog với: dropdown metric name (từ reference API), text input value, text input unit
  - [ ] Zod validation: value phải là số hợp lệ, name không được trống
  - [ ] Source badge: "OCR" vs "Nhập tay" icon
- [ ] Task 4 — Mobile (Phase 2): Edit metric (AC: #1, #2, #3)
  - [ ] Tương tự web, dùng Modal thay Dialog
  - [ ] Keyboard type: `numeric` cho value field
- [ ] Task 5 — Tests (AC: #2, #5)
  - [ ] `HealthRecordServiceTest`: update metrics, source_type calculation

## Dev Notes

### Metric Schema (JSONB)

```json
{
  "metrics": [
    { "name": "Glucose", "value": "5.4", "unit": "mmol/L", "confidence": 0.92, "source": "ocr" },
    { "name": "HbA1c", "value": "6.1", "unit": "%", "confidence": null, "source": "manual" }
  ]
}
```

### Source Tag Values (packages/shared/constants)

```typescript
export const METRIC_SOURCE = {
  OCR: 'ocr',
  MANUAL: 'manual',
} as const;
```

### Source Type Logic (Backend)

```java
String sourceType = metrics.stream().allMatch(m -> "ocr".equals(m.source))   ? "ocr"
                  : metrics.stream().allMatch(m -> "manual".equals(m.source)) ? "manual"
                  : "mixed";
```

### Validation

```typescript
const metricSchema = z.object({
  name: z.string().min(1),
  value: z.string().regex(/^\d+\.?\d*$/, 'Giá trị phải là số'),
  unit: z.string().min(1),
  source: z.enum(['ocr', 'manual']),
});
```

### References

- [Source: ux-design-specification.md#UX-DR4]
- [Source: epics.md#Story-3.4]
- [Source: architecture.md#Kiến-Trúc-Dữ-Liệu]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
