# Story 3.5: OCR failure recovery flow có hướng dẫn rõ ràng

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)


| Tiêu đề (Stitch)                  | Resource                                                               | HTML prototype                                                                                                                                                                                                                                                              | Screenshot                                                                                                                                                                                                                                                                                                 |
| --------------------------------- | ---------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| OCR Failure Recovery - HealthLens | `projects/578519912546445367/screens/ace87425151148448083728c199470c7` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzIyYTVmYmUyZDQwNzQ0NmU4ZTIyOTJkYjQ3OWI3ZTY0EgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhkPWvUwzdykduZ7ucmjcKEFDsPCFSzxEQ9MGlpdbZR5lW8ut4ifGjrA4Ro2rO8PZis9k2MksMIE7llMNDnNZ5EEMGtAWW-hpwB-Q0wtOh7WNGyPHd4d3YEIM9h7A2GL99mCj4vFmGeh25t-md9CcnFHMGwakpieXT74z5Jcma7jRcA0ADtlVbscGkRRT62NkAKXyA6-17xvJfkoGOQyb2C5gEzhYbED8hp_hIedB22Fv_Y1OgmhJ6-) |


*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want nhận hướng dẫn cụ thể khi OCR thất bại,
so that tôi biết bước tiếp theo thay vì bị kẹt.

## Acceptance Criteria

1. **Given** OCR confidence < 50% hoặc OCR timeout (>15s), **When** hệ thống trả về trạng thái thất bại, **Then** hiển thị màn hình recovery với 3 lựa chọn: "Chụp lại rõ hơn", "Nhập thủ công", "Giữ những gì có".
2. **Given** OCR confidence 50-84% (partial), **When** hiển thị partial result screen, **Then** chỉ số confidence thấp được highlight vàng "Vui lòng kiểm tra".
3. **Given** user chọn "Chụp lại rõ hơn", **When** navigate, **Then** quay lại camera screen với tips camera hiển thị.
4. **Given** user chọn "Nhập thủ công", **When** navigate, **Then** mở màn hình nhập tay (Story 3.4 flow).
5. **Given** user chọn "Giữ những gì có", **When** confirm, **Then** lưu partial metrics với source_type phù hợp.
6. **Given** camera retry screen, **When** hiển thị tips, **Then** tips phải bao gồm: "Đặt giấy phẳng", "Ánh sáng đều", "Tránh bóng đổ", "Xoay ngang nếu cần".

## Tasks / Subtasks

- Task 1 — Backend: OCR failure states (AC: #1, #2)
  - Cập nhật OCR worker: khi confidence < 50% → update record status `ocr_failed`
  - Khi confidence 50-84% → status `review_required` với flag `hasLowConfidenceMetrics: true`
  - OCR timeout (>15s) → status `ocr_failed`, reason `timeout`
- Task 2 — Web: OCR failure recovery screen (AC: #1, #3, #4, #5)
  - Tạo `apps/web/src/components/features/upload/OcrFailureScreen.tsx`
  - 3 option cards: "Chụp lại", "Nhập thủ công", "Giữ những gì có" (nếu có partial)
  - Option "Giữ những gì có" chỉ hiện khi có partial metrics
- Task 3 — Mobile (Phase 2): OCR failure recovery screen (AC: #1, #3, #4, #5)
  - Tạo `apps/mobile/app/upload/ocr-failure.tsx`
  - 3 nút action với icon và mô tả ngắn
  - Navigate tương ứng theo lựa chọn
- Task 4 — Mobile (Phase 2): Camera retry screen với tips (AC: #3, #6)
  - Tạo `apps/mobile/app/upload/camera-retry.tsx`
  - Hiển thị 4 tips với icon ở đầu màn hình trước khi mở camera
  - Nút "Chụp lại" → camera screen (Story 3.2)
- Task 5 — Tests (AC: #1, #2)
  - `OcrServiceTest`: timeout handling, partial confidence classification

## Dev Notes

### Health Record Status Flow

```
uploading → processing → review_required (OCR ok / partial)
                       ↘ ocr_failed (confidence <50% or timeout)
           
review_required → done (sau khi user confirm)
ocr_failed → [user action: retry, manual_input, keep_partial]
```

### OCR Failure Screen (UX-DR4)

```
┌─────────────────────────────────────┐
│ ⚠️ OCR không đọc được đầy đủ         │
│                                     │
│ [📷 Chụp lại rõ hơn]                │
│ Ảnh rõ, phẳng, ánh sáng đều        │
│                                     │
│ [✏️ Nhập thủ công]                   │
│ Tự nhập từng chỉ số                 │
│                                     │
│ [✅ Giữ những gì có] (nếu có)        │
│ Lưu {N} chỉ số đã đọc được          │
└─────────────────────────────────────┘
```

### Camera Tips (UX-DR4)

```typescript
const CAMERA_TIPS = [
  { icon: '📄', text: 'Đặt giấy phẳng trên bề mặt cứng' },
  { icon: '💡', text: 'Đảm bảo ánh sáng đều, tránh bóng đổ' },
  { icon: '📐', text: 'Căn chỉnh giấy vào khung hướng dẫn' },
  { icon: '↔️', text: 'Thử xoay ngang nếu kết quả dài' },
];
```

### OCR Failure Flow (Option B+ - Updated 2026-04-01)

```
┌─────────────────────────────────────────────────────────────┐
│                    OCR Processing Pipeline (Option B+)       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Image Upload ──► EasyOCR Service (localhost:8001)          │
│                          │                                   │
│                    ┌──────┴──────┐                           │
│                    │ Success?    │                           │
│                    │ Timeout?     │                           │
│                    └──────┬──────┘                           │
│                     Yes    │ No/Timeout                      │
│                      │     │                                │
│                      ▼     ▼                                │
│                 Return    AWS Textract (Fallback)           │
│                   OCR         │                             │
│                   Result      │                             │
│                                 │                            │
│                          ┌─────┴─────┐                       │
│                          │ Success?  │                       │
│                          └─────┬─────┘                       │
│                           Yes   │ No                         │
│                            │    │                            │
│                            ▼    ▼                            │
│                       Return   Recovery Screen              │
│                         OCR    (Story 3.5)                  │
│                        Result                              │
└─────────────────────────────────────────────────────────────┘
```

**EasyOCR Details:**

- Vietnamese accuracy: ~90%
- Processing time: 3-8s on CPU
- Timeout: 10 seconds (configurable)

**Fallback Chain:**

1. EasyOCR Service (local Python microservice)
2. AWS Textract (cloud, $1.50/1K pages)
3. Recovery Screen (Story 3.5) - Manual input option

### References

- [Source: ux-design-specification.md#UX-DR4]
- [Source: architecture.md#Xử-Lý-Lỗi-&-Khả-Năng-Phục-Hồi]
- [Source: architecture.md#ADR-001-Local-First-AI]
- [Source: epics.md#Story-3.5]

## Dev Agent Record

### Agent Model Used

- Codex 5.3

### Debug Log References

- `apps/api`: `./gradlew test --tests "com.healthlens.api.service.OcrServiceTest" --tests "com.healthlens.api.service.HealthRecordServiceTest"` (pass)
- `apps/web`: `pnpm test` (pass)

### Completion Notes List

- Task 1: Cập nhật OCR pipeline để phân nhánh `ocr_failed` khi timeout/low confidence (<50%), giữ `review_required` khi partial confidence và trả thêm cờ `hasLowConfidenceMetrics` + `ocrFailureReason` qua status API.
- Task 2: Tạo màn hình recovery `OcrFailureScreen` với 3 lựa chọn; nối flow retry (kèm tips), manual input (vào review manual mode), keep partial (xác nhận lưu partial).
- Task 5: Cập nhật và bổ sung test cho timeout handling + confidence boundary + source_type khi xác nhận partial metrics.

### File List

- `apps/api/src/main/java/com/healthlens/api/dto/response/HealthRecordStatusResponse.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `apps/web/src/components/features/upload/OcrFailureScreen.tsx`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/page.tsx`

### Review Findings

- [Review][Decision] Timeout OCR fallback chain — đã chốt hướng: timeout vẫn fallback Textract; chỉ fail khi toàn bộ provider thất bại.
- [Review][Patch] Flow "Nhập thủ công" bị kẹt vì `ocr_failed` luôn render `OcrFailureScreen`, nên `?mode=manual` không mở được màn nhập tay [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx]
- [Review][Patch] Flow "Giữ những gì có" không lưu được từ trạng thái `ocr_failed` vì `confirmRecord` chặn status khác `review_required|done` [apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java]
- [Review][Patch] `handleKeepPartial` luôn redirect về danh sách kể cả khi API save lỗi, gây false-success UX [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx]
- [Review][Patch] Keep-partial đang tự nâng mọi metric lên `confidence=1.0/high`, làm sai provenance và có thể ghi `sourceType="ocr"` thay vì partial [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx]
- [Review][Patch] `sourceType` đang suy từ payload client trong `confirmRecord`, cho phép client tác động phân loại dữ liệu lưu trữ [apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java]
- [Review][Patch] Option "Giữ những gì có" phụ thuộc `data.metrics` nhưng `getStatus` không trả metrics cho `ocr_failed`, khiến nhánh recovery này thực tế bị ẩn [apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java]
- [Review][Patch] Nút "Chụp lại rõ hơn" chỉ điều hướng về `/health-records?retry=1` (banner tips), chưa vào flow camera/capture như AC #3 mong đợi [apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx]
- [Review][Patch] Thiếu test cho branching tại `OcrJobConsumer` ở ngưỡng 0.49/0.50/0.84/0.85 và reason propagation (`timeout`, `low_confidence`) [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java]

