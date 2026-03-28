# Story 3.5: OCR failure recovery flow có hướng dẫn rõ ràng

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
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

- [ ] Task 1 — Backend: OCR failure states (AC: #1, #2)
  - [ ] Cập nhật OCR worker: khi confidence < 50% → update record status `ocr_failed`
  - [ ] Khi confidence 50-84% → status `review_required` với flag `hasLowConfidenceMetrics: true`
  - [ ] OCR timeout (>15s) → status `ocr_failed`, reason `timeout`
- [ ] Task 2 — Web: OCR failure recovery screen (AC: #1, #3, #4, #5)
  - [ ] Tạo `apps/web/src/components/features/upload/OcrFailureScreen.tsx`
  - [ ] 3 option cards: "Chụp lại", "Nhập thủ công", "Giữ những gì có" (nếu có partial)
  - [ ] Option "Giữ những gì có" chỉ hiện khi có partial metrics
- [ ] Task 3 — Mobile (Phase 2): OCR failure recovery screen (AC: #1, #3, #4, #5)
  - [ ] Tạo `apps/mobile/app/upload/ocr-failure.tsx`
  - [ ] 3 nút action với icon và mô tả ngắn
  - [ ] Navigate tương ứng theo lựa chọn
- [ ] Task 4 — Mobile (Phase 2): Camera retry screen với tips (AC: #3, #6)
  - [ ] Tạo `apps/mobile/app/upload/camera-retry.tsx`
  - [ ] Hiển thị 4 tips với icon ở đầu màn hình trước khi mở camera
  - [ ] Nút "Chụp lại" → camera screen (Story 3.2)
- [ ] Task 5 — Tests (AC: #1, #2)
  - [ ] `OcrServiceTest`: timeout handling, partial confidence classification

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

### References

- [Source: ux-design-specification.md#UX-DR4]
- [Source: architecture.md#Xử-Lý-Lỗi-&-Khả-Năng-Phục-Hồi]
- [Source: epics.md#Story-3.5]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
