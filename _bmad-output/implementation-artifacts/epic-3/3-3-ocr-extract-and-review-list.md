# Story 3.3: OCR trích xuất chỉ số và hiển thị danh sách xác nhận

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| OCR Processing State - HealthLens | `projects/578519912546445367/screens/661b65268f614bacab4dee0690eb1790` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzljNGUxYzg4ODEzNTRjZDQ5N2FkMzhhY2E4ZTVlNzY5EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhI6jWzI4qJXPiXGiPSXsYzTacr9pa-LCo-1pBav8Vk_a45C5Z5RVpM1RjrsGcYSg3A9PG0XwlnAnHU8p-rjONaZqZXUQchaOMHV1wXv4QolnoCl-6vu9_OgnZ0jJ0ezBgLM3r1jRqF7Om9B3ad4kHbJCWY9Xd-0nK6sQL0V1vOe4oJmbrFcsNGhipUro0GZr0WGbZmUld9wGikDCEq34qVlzLetH_dPQvZhTxzdw73Pdl0GX_-q9axvw) |
| Kiểm tra & Xác nhận OCR - HealthLens | `projects/578519912546445367/screens/a9f6ae144fa7402bac66d0c56b36f4c0` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzQwMjExNDBiNGNiMDQ3YzNiZjA1NWVmYTdlYzlmNTE5EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uguG_cHKCFxywAlpRMKgpRT5xbvhwyNE8dLLYx0B6_5b0Co9i0C3X9PIhUxJZoYtgHL2DlykefHaQGMQCHGT1pcSAjOLz45gjiKsbKIcOxgxgGs_MNsQ2G5FD-lFmq_7cWc4AbJ0Qth9Sna7BrsFOQ_6Af-WoXwJhMqQUNHW20i9609iKSEcIS8h3dq4-cA669XNVgpqbONYqXOMU1GhlpFfD8KQRH2HhRyQY1HwLOfD2nR-ZGD77CbKw) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want xem danh sách chỉ số OCR trước khi lưu,
so that tôi kiểm tra tính đúng đắn của kết quả trích xuất.

## Acceptance Criteria

1. **Given** OCR job hoàn tất trong timeout 15 giây, **When** mở review screen, **Then** hệ thống hiển thị danh sách chỉ số với metric name, value, unit, và confidence.
2. **Given** danh sách chỉ số đã hiển thị, **When** người dùng xác nhận và lưu, **Then** health record được persist với status `done` và gắn đúng profile.
3. **Given** OCR confidence ≥ 85%, **When** hiển thị, **Then** chỉ số được đánh dấu "Đã xác minh" (không cần highlight).
4. **Given** OCR confidence 50-84%, **When** hiển thị, **Then** chỉ số được highlight "Vui lòng kiểm tra" (theo UX-DR4 partial result).
5. **Given** review screen, **When** user chưa xác nhận mà thoát, **Then** hiển thị dialog confirm "Bỏ kết quả này?" để tránh mất dữ liệu.

## Tasks / Subtasks

- [ ] Task 1 — Backend: OCR result parsing (AC: #1, #3, #4)
  - [ ] `OcrService.java`: call OCR API, parse JSON response thành list metrics
  - [ ] Metric schema: `{ name, value, unit, confidence, source: "ocr" }`
  - [ ] Classify confidence: ≥85% → `high`, 50-84% → `medium`, <50% → `low`
  - [ ] Lưu parsed metrics vào `health_records.metrics` JSONB, update status → `review_required`
  - [ ] Update `health_records.raw_ocr_result` với raw OCR response (cho audit)
- [ ] Task 2 — Backend: Confirm save endpoint (AC: #2)
  - [ ] `POST /api/v1/health-records/{recordId}/confirm` với optional body `{ examDate }`
  - [ ] Update status từ `review_required` → `done`
  - [ ] Set `exam_date` nếu không có từ OCR
- [ ] Task 3 — Backend: Poll status endpoint (AC: #1)
  - [ ] `GET /api/v1/health-records/{recordId}/status` → `{ status, metrics? }`
  - [ ] Caching với Redis để giảm DB load khi polling
- [ ] Task 4 — Web: OCR Review screen (AC: #1, #3, #4, #5)
  - [ ] Tạo `apps/web/src/app/(dashboard)/upload/review/[recordId]/page.tsx`
  - [ ] List metrics: tên chỉ số, giá trị, đơn vị, confidence badge
  - [ ] Confidence badge: xanh "Đã xác minh" (≥85%), vàng "Kiểm tra" (50-84%)
  - [ ] Nút "Xác nhận và Lưu" → gọi confirm endpoint
  - [ ] Confirm dialog khi thoát mà chưa lưu
- [ ] Task 5 — Mobile (Phase 2): OCR Review screen (AC: #1, #4, #5)
  - [ ] Tạo `apps/mobile/app/upload/review.tsx`
  - [ ] FlatList để scroll qua nhiều chỉ số
  - [ ] Alert dialog khi backtrack mà chưa lưu
- [ ] Task 6 — Tests (AC: #1, #2, #3)
  - [ ] `OcrServiceTest`: parse OCR response, classify confidence levels

## Dev Notes

### OCR Metric JSON Schema

```json
{
  "metrics": [
    {
      "name": "Glucose",
      "value": "5.4",
      "unit": "mmol/L",
      "confidence": 0.92,
      "source": "ocr"
    },
    {
      "name": "HbA1c",
      "value": "6.1",
      "unit": "%",
      "confidence": 0.67,
      "source": "ocr"
    }
  ]
}
```

### API

```
GET /api/v1/health-records/{recordId}/status
Response: { "data": { "status": "review_required", "metrics": [...], "confidence": 0.85 }, "meta": {...} }

POST /api/v1/health-records/{recordId}/confirm
Body: { "examDate": "2026-03-17" }
Response 200: { "data": { "id": "...", "status": "done" }, "meta": {...} }
```

### Confidence Thresholds (packages/shared/constants)

```typescript
export const OCR_CONFIDENCE = {
  HIGH: 0.85,    // ≥85% → xanh
  MEDIUM: 0.50,  // 50-84% → vàng
  LOW: 0,        // <50% → đỏ / recovery flow (Story 3.5)
} as const;
```

### HealthStatusBadge Component (UX-DR2)

```typescript
// apps/web/src/components/ui/HealthStatusBadge.tsx
interface HealthStatusBadgeProps {
  status: 'normal' | 'attention' | 'abnormal';
  label: string;
}
// Triple redundancy: color + icon + text label (WCAG)
```

### References

- [Source: ux-design-specification.md#UX-DR4]
- [Source: architecture.md#Kiến-Trúc-Dữ-Liệu]
- [Source: epics.md#Story-3.3]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
