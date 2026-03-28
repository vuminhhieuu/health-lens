# Story 4.1: Hiển thị metric card với trạng thái và ngưỡng tham chiếu

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Health Record Detail Page | `projects/578519912546445367/screens/3c9f3f9c951b4e45aa713c6da552be29` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzc2ZDgwMGI2MWVjZjRjZjBhNjZlYmFhMGM1MThjMTQyEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhzldktcjBvfyA80T-sCTf9VYf7EEA0b8kZJqSNh2gb2l2nyuCMywxSBNQM_QjkdzH-eQvQMehBqSForVsETegaE_zrHY-5HTDbo7tHtOdGCCrnJPfo84-vpSeeO6ecq8xYdwkq5DqHc-zEg0abPd7o_U2M9-LTR7Coxv_9sKGYfag9-O6z0u1vdbo67DJ8abgHQa-7OSDXTdrfLdy-vcamk0AbOBtN-HT_uxCGuCfTJsUkiJ8HqlJRSQ) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want xem từng chỉ số kèm ngưỡng chuẩn và trạng thái,
so that tôi hiểu ngay chỉ số nào cần chú ý.

## Acceptance Criteria

1. **Given** bản ghi khám đã lưu, **When** mở màn hình chi tiết kết quả, **Then** hiển thị HealthMetricCard gồm tên chỉ số, giá trị, đơn vị, reference range, và status badge.
2. **Given** mỗi chỉ số, **When** hiển thị status, **Then** sử dụng ba mức: "Bình thường" (normal), "Cần chú ý" (attention), "Bất thường" (abnormal).
3. **Given** status badge, **When** hiển thị, **Then** dùng triple redundancy: màu + icon + text label (WCAG 2.1 AA — NFR-A3).
4. **Given** chỉ số không có trong reference data, **When** hiển thị, **Then** status = "Không có dữ liệu tham chiếu" và không phân loại.
5. **Given** màn hình detail, **When** tap vào một MetricCard, **Then** expand để xem thêm thông tin (progressive disclosure — UX-DR5).

## Tasks / Subtasks

- [ ] Task 1 — Backend: Reference data tables (AC: #1, #4)
  - [ ] Flyway migration `V011__create_reference_data_tables.sql`
  - [ ] Bảng `reference_metrics`: id, name, display_name_vi, unit, created_at
  - [ ] Bảng `reference_ranges`: id, metric_id, min_value, max_value, attention_min, attention_max, gender (nullable), min_age (nullable), max_age (nullable), status (active/draft)
  - [ ] Seed data cơ bản: Glucose, HbA1c, Cholesterol, Triglycerides, HDL, LDL, Hemoglobin, WBC
  - [ ] `GET /api/v1/reference-data/ranges?metricName={name}&age={age}&gender={gender}`
- [ ] Task 2 — Backend: Classify metric status (AC: #2, #4)
  - [ ] `ReferenceDataService.classifyMetric(name, value, age, gender)` → `{ status, referenceRange }`
  - [ ] Logic: value < attention_min hoặc value > attention_max → `abnormal`; value gần ngưỡng → `attention`; trong range → `normal`
  - [ ] Khi không tìm thấy reference → `no_data`
- [ ] Task 3 — Backend: Health record detail endpoint (AC: #1)
  - [ ] `GET /api/v1/health-records/{recordId}` → trả về metrics kèm reference ranges và status classification
  - [ ] Enrich metrics với reference data ở backend trước khi trả về
- [ ] Task 4 — Web: HealthMetricCard component (AC: #1, #2, #3, #5)
  - [ ] Tạo `apps/web/src/components/ui/HealthMetricCard.tsx`
  - [ ] Props: metricName, value, unit, referenceRange, status, explanation (optional)
  - [ ] HealthStatusBadge: màu (#10B981 normal, #F59E0B attention, #EF4444 abnormal), icon (✅⚠️❌), text label
  - [ ] Expandable: collapsed → chỉ tên + value + badge; expanded → thêm reference range, explanation
  - [ ] CSS transition cho expand/collapse (200ms — UX-DR10)
- [ ] Task 5 — Mobile (Phase 2): HealthMetricCard component (AC: #1, #2, #3, #5)
  - [ ] React Native tương đương với Animated API cho expand
- [ ] Task 6 — Tests (AC: #2, #4)
  - [ ] `ReferenceDataServiceTest`: classify normal, attention, abnormal, no data

## Dev Notes

### Reference Data Seed (Flyway)

```sql
-- V012__seed_reference_metrics.sql
INSERT INTO reference_metrics (id, name, display_name_vi, unit) VALUES
  (gen_random_uuid(), 'Glucose', 'Đường huyết', 'mmol/L'),
  (gen_random_uuid(), 'HbA1c', 'HbA1c', '%'),
  (gen_random_uuid(), 'Cholesterol', 'Cholesterol toàn phần', 'mmol/L'),
  (gen_random_uuid(), 'Triglycerides', 'Triglycerides', 'mmol/L'),
  (gen_random_uuid(), 'HDL', 'HDL Cholesterol', 'mmol/L'),
  (gen_random_uuid(), 'LDL', 'LDL Cholesterol', 'mmol/L');
```

### Health Status Colors (UX-DR1)

```typescript
// packages/shared/constants/index.ts
export const HEALTH_STATUS_COLORS = {
  normal:    { hex: '#10B981', label: 'Bình thường', icon: '✅' },
  attention: { hex: '#F59E0B', label: 'Cần chú ý',   icon: '⚠️' },
  abnormal:  { hex: '#EF4444', label: 'Bất thường',  icon: '❌' },
  no_data:   { hex: '#6B7280', label: 'Không có dữ liệu', icon: 'ℹ️' },
} as const;
```

### HealthMetricCard Props

```typescript
interface HealthMetricCardProps {
  metricName: string;
  displayNameVi: string;
  value: string;
  unit: string;
  referenceRange?: { min: number; max: number };
  status: 'normal' | 'attention' | 'abnormal' | 'no_data';
  source: 'ocr' | 'manual';
  explanation?: string;  // từ LLM — Story 4.3
  recommendation?: string;  // từ LLM — Story 4.4
}
```

### Progressive Disclosure (UX-DR5)

- Mặc định: chỉ metric name + value + status badge
- Expanded: reference range, explanation text, recommendation nếu có
- Trigger: click/tap vào card

### References

- [Source: ux-design-specification.md#UX-DR1]
- [Source: ux-design-specification.md#UX-DR2]
- [Source: ux-design-specification.md#UX-DR5]
- [Source: architecture.md#Kiến-Trúc-Dữ-Liệu]
- [Source: epics.md#Story-4.1]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
