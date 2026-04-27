# Bug Ticket: Review Record Page — Metadata Save Blocked & "Edit Results" Toggle Lost After Manual Entry

Status: open
Priority: high (Bug 1) / medium (Bug 2)
Type: bug / UX / state-management
Area: Frontend (Next.js)
Affected file: `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
Related stories: `epic-3/3-3-ocr-extract-and-review-list` (done), `epic-3/3-4-manual-edit-input-ocr-fallback` (done)

---

## Bug 1 — "Save Changes" Button Disabled When Only Metadata Fields Are Edited

### 1) Problem Summary

Khi hồ sơ đã ở trạng thái `done`, người dùng thấy 4 trường metadata vẫn editable:

- Ngày khám (`examDate`)
- Loại phiếu (`recordType`)
- Tên bệnh viện / Phòng khám (`hospitalName`)
- Chẩn đoán / Kết luận của bác sĩ (`diagnosis`)

Người dùng có thể gõ và sửa các trường này, nhưng nút "LƯU CHỈNH SỬA" ở cuối trang luôn ở trạng thái disabled, không phản hồi khi bấm.

Workaround duy nhất hiện nay: bấm "Chỉnh sửa kết quả" để mở chế độ chỉnh sửa bảng metric (dù không định sửa metric nào), sau đó mới có thể bấm Lưu.

### 2) User Impact

- Người dùng không cập nhật được thông tin hành chính của hồ sơ đã lưu (ngày khám, cơ sở khám, loại phiếu, chẩn đoán) nếu phát hiện sai/thiếu sau khi đã xác nhận.
- UX gây hiểu nhầm: input còn cho gõ → người dùng tưởng đang sửa được, bấm Lưu thì không có phản hồi.
- Tạo cảm giác lỗi hệ thống và rủi ro phát sinh ticket hỗ trợ.

### 3) Root Cause

Trong `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`:

- Nút lưu bị khóa theo `editMode` (qua biến phái sinh `showEditableTable`):
  - `disabled={isSaving || !showEditableTable}` (~dòng 741)
  - `showEditableTable = editMode` (~dòng 424)
- 4 input metadata được render **không phụ thuộc** `editMode`, **không có** thuộc tính `disabled` (block từ ~dòng 487 đến ~dòng 528).
- Trang đã có `isDirty` (so sánh snapshot, bao gồm cả 4 trường metadata, ~dòng 194-208) nhưng `isDirty` chỉ dùng cho cảnh báo `beforeunload` (~dòng 210-221), **không** dùng để bật nút lưu.

Backend đã hỗ trợ đầy đủ — `HealthRecordService.confirmRecord` (~dòng 360-401 trong `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`) cập nhật từng trường metadata độc lập, không yêu cầu `metrics` đi kèm. Vậy đây là lỗi FE thuần.

### 4) Proposed Fix

Có thể chọn một trong hai (hoặc kết hợp), khuyến nghị áp dụng cả hai:

1. **Bật nút lưu theo `isDirty`** (vá trực tiếp hành vi):
   - Đổi `disabled={isSaving || !showEditableTable}` → `disabled={isSaving || !isDirty}`.
   - Cho phép lưu mọi thay đổi (metadata hoặc metric) miễn là có khác snapshot ban đầu.

2. **Đồng bộ trải nghiệm input metadata với edit-mode** (vá nhận thức):
   - Khi `!editMode` và `data.status === "done"`: hiển thị metadata ở chế độ readonly/disabled rõ ràng, hoặc:
   - `onChange` bất kỳ trường metadata nào tự động đặt `editMode = true` và hiển thị label "Đang chỉnh sửa".

### 5) Acceptance Criteria

1. Khi hồ sơ ở trạng thái `done`, người dùng chỉ chỉnh sửa 1 hoặc nhiều trường metadata (không đụng metric) → nút "LƯU CHỈNH SỬA" được bật và lưu thành công.
2. Sau khi lưu, dữ liệu trong DB phản ánh đúng các trường metadata mới (kiểm tra `record.exam_date`, `record_type`, `hospital_name`, `diagnosis`).
3. Sau khi lưu, snapshot được cập nhật → `isDirty = false` → nút lưu trở về disabled cho đến khi có thay đổi mới.
4. Khi không có thay đổi nào, nút lưu disabled (không cho gọi API rỗng).
5. Không hồi quy luồng OCR thành công (`review_required`) và luồng manual entry (`ocr_failed` + `?mode=manual`).

### 6) Test Plan

- **Unit / Component**:
  - Mount trang ở trạng thái `done`, không thay đổi gì → nút lưu disabled.
  - Sửa từng trường metadata riêng lẻ → nút lưu enable.
  - Bấm lưu → POST `/health-records/{id}/confirm` với đúng payload.
- **E2E**:
  - Tạo record → confirm → vào lại record (status `done`) → sửa "Ngày khám" → Lưu → reload → xác nhận giá trị mới.
  - Lặp lại cho `recordType`, `hospitalName`, `diagnosis`.
  - Trường hợp sửa cả metadata + metric → vẫn lưu đúng.
- **Regression**:
  - Luồng `review_required`: confirm record bình thường, status chuyển `done`.
  - Luồng `ocr_failed` + manual: nhập metric, lưu, status chuyển `done`.

---

## Bug 2 — "Edit Results" Button Hidden After Reloading Manual-Mode Page Post-Save

### 1) Problem Summary

Khi người dùng đi qua flow:
"Upload → OCR fail → Nhập thủ công → Nhập chỉ số → Lưu" rồi sau đó **F5/refresh** trang (URL vẫn còn `?mode=manual`) — hoặc quay lại record qua link cũ có `?mode=manual` — section "Danh sách chỉ số và ngưỡng tham chiếu" bị ẩn, kéo theo nút "Chỉnh sửa kết quả" không hiển thị, mặc dù record đã ở trạng thái `done`.

Người dùng bị "kẹt" trong bảng chỉnh sửa, không có cách thấy lại view card với ngưỡng tham chiếu và không thấy nút "Chỉnh sửa kết quả" như flow OCR thành công.

> Lưu ý: Ngay sau khi click "Nhập thủ công" lần đầu (trước khi lưu) thì việc ẩn nút "Chỉnh sửa kết quả" là **đúng thiết kế** — người dùng đã ở chế độ chỉnh sửa, không cần nút bật chế độ chỉnh sửa nữa. Bug chỉ xuất hiện sau khi lưu mà còn truy cập lại trang với `?mode=manual`.

### 2) User Impact

- Sau khi lưu manual và refresh, người dùng không thấy view card với ngưỡng tham chiếu để đọc kết quả → mâu thuẫn với flow OCR thành công.
- Tăng nhầm lẫn: nhìn như chưa lưu, tưởng phải bấm "Lưu" lại.
- Khó quay về trạng thái "đã xác nhận" để sau này muốn chỉnh sửa tiếp tục bằng nút "Chỉnh sửa kết quả".

### 3) Root Cause

Trong `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` ~dòng 175-192:

```ts
useEffect(() => {
  if (data && data.status !== "processing" && !initialized.current) {
    ...
    initialized.current = true;
    setEditMode(data.status === "review_required" || manualMode);
  }
}, [data, manualMode]);
```

Trên mỗi lần mount mới (refresh page), `manualMode` lấy từ URL `?mode=manual` vẫn là `true`, bất kể `data.status` đang là `done`. Logic OR trong `setEditMode(...)` ép `editMode = true` ngay cả khi record đã xác nhận, dẫn đến:

- `showMetricCards = !editMode = false` → ẩn section "Danh sách chỉ số và ngưỡng tham chiếu" (chứa nút "Chỉnh sửa kết quả").
- Hiển thị section "Chỉnh sửa danh sách chỉ số" như chưa lưu.

Bên cạnh đó, sau khi `executeSave` thành công ở chế độ manual, code không clear query `?mode=manual` khỏi URL → query string vẫn dính lại sau navigate/reload.

### 4) Proposed Fix

Hai thay đổi nhỏ, nên áp cùng nhau:

1. **Sửa điều kiện `setEditMode`** để chỉ bật chế độ edit khi thật sự cần:
   ```ts
   setEditMode(
     data.status === "review_required" ||
     (data.status === "ocr_failed" && manualMode)
   );
   ```
   - `done` + `manualMode=true` → `editMode = false` → cards view + nút "Chỉnh sửa kết quả" hiển thị bình thường (giống OCR success flow).
   - `ocr_failed` + `manualMode=true` (chưa lưu) → giữ `editMode = true` như hiện tại.

2. **Clear query `?mode=manual` sau khi lưu** trong `executeSave` (success branch):
   ```ts
   router.replace(`/health-records/review/${recordId}`);
   ```
   Tránh kích hoạt lại nhánh manual khi quay lại trang sau này.

### 5) Acceptance Criteria

1. Sau khi lưu thành công ở manual mode, refresh trang → hiển thị view card "Danh sách chỉ số và ngưỡng tham chiếu" + nút "Chỉnh sửa kết quả" (giống flow OCR success).
2. Trước khi lưu (status `ocr_failed` + `?mode=manual`): vẫn hiển thị bảng chỉnh sửa như hiện tại; không hồi quy.
3. Sau khi lưu thành công, URL không còn query `?mode=manual`.
4. Bấm "Chỉnh sửa kết quả" → vào lại edit-mode bình thường, có thể chỉnh sửa metric & lưu lại.
5. Không hồi quy điều kiện hiển thị "OCR Failure Screen" (chỉ hiện khi `ocr_failed && !manualMode`).

### 6) Test Plan

- **E2E (manual recovery)**:
  - Upload ảnh OCR fail → click "Nhập thủ công" → nhập 2-3 metric → bấm Xác nhận và Lưu.
  - Refresh trang → confirm thấy section "Danh sách chỉ số và ngưỡng tham chiếu" có nút "Chỉnh sửa kết quả".
  - Click nút "Chỉnh sửa kết quả" → mở bảng edit, chỉnh 1 metric, lưu lại → DB cập nhật đúng.
- **E2E (OCR success regression)**:
  - Upload ảnh thành công → review → confirm → reload → vẫn thấy nút "Chỉnh sửa kết quả".
- **E2E (OCR fail without manual)**:
  - Upload OCR fail → không click "Nhập thủ công" → vẫn thấy `OcrFailureScreen` đầy đủ 3 lựa chọn.
- **Unit**:
  - `setEditMode` logic: ma trận `(status × manualMode)` → đúng kết quả mong đợi.

---

## Suggested Owner / Phase

- Owner: Web (FE)
- Phase: Sprint 2 hot-fix (cùng đợt với epic 3 vì cả hai bug đều thuộc luồng review/manual của epic 3 đã `done`).
- Effort ước tính: ~0.5 ngày cho cả 2 bug (cùng file, ~5-10 dòng thay đổi + tests).

## Out of Scope

- Refactor toàn bộ state-machine của trang review (đáng làm nhưng để story riêng).
- Thay đổi contract API `/confirm` hoặc thêm endpoint `PATCH metadata` riêng.
- Đổi logic `keepPartial` hay `source_type`.
