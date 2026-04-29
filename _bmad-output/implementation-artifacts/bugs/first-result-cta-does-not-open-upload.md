# Bug Ticket: "Add First Result" CTA Does Not Open Upload Flow

Status: open  
Priority: high  
Type: bug / navigation / upload-flow  
Area: Frontend (Next.js)

## 1) Problem Summary

Tại trang lịch sử hồ sơ, khi hồ sơ chưa có kết quả, CTA **"Thêm kết quả đầu tiên"** điều hướng sang `/health-records?profileId={id}&openUpload=1`.

Tuy nhiên sau khi điều hướng, màn hình chỉ hiển thị hub chọn hồ sơ và không mở file picker để tải lên. Người dùng kỳ vọng CTA này đi thẳng vào thao tác tải tệp đầu tiên.

## 2) User Impact

- Chặn hoặc làm gián đoạn luồng thêm kết quả đầu tiên cho hồ sơ rỗng.
- Tăng số bước thao tác không cần thiết, dễ gây hiểu nhầm là nút không hoạt động.
- Tạo cảm nhận sản phẩm thiếu nhất quán với CTA label.

## 3) Root Cause

- `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` tạo link:
  - `/health-records?profileId=...&openUpload=1`.
- Cờ query `openUpload=1` chỉ được xử lý trong `UploadButton` (`apps/web/src/components/features/upload/UploadButton.tsx`).
- Trang đích `/health-records` (`apps/web/src/app/(dashboard)/health-records/page.tsx`) hiện không render `UploadButton`, nên query flag không có nơi tiêu thụ.

Kết quả: link điều hướng thành công nhưng hành vi auto-open upload không xảy ra.

## 4) Proposed Fix

Chọn một trong hai hướng (hoặc kết hợp):

1. **Route correction**  
   Điều hướng CTA tới màn hình có `UploadButton` thật sự (hoặc màn hình review/upload chuyên biệt) để `openUpload=1` được xử lý.

2. **Hub support for deep-link upload**  
   Bổ sung logic ở `/health-records` để nhận `profileId + openUpload=1` và mở upload flow tương ứng.

3. **Post-action URL cleanup (optional)**  
   Sau khi đã kích hoạt auto-open, xóa query `openUpload=1` để tránh trigger lặp khi refresh.

## 5) Acceptance Criteria

1. Từ empty state của `/profiles/{profileId}/history`, bấm **"Thêm kết quả đầu tiên"** sẽ mở được file picker ngay (hoặc đi tới màn upload tương đương không mất thêm bước chọn lại).
2. `profileId` được giữ đúng, upload request dùng đúng hồ sơ mục tiêu.
3. Không hồi quy CTA upload ở các điểm khác (ví dụ dashboard home).
4. Refresh sau khi đã vào flow không gây auto-open lặp ngoài ý muốn.

## 6) Reproduction Steps

1. Đăng nhập với tài khoản có ít nhất một profile chưa có record.
2. Mở `/profiles/{profileId}/history`.
3. Xác nhận đang ở empty state.
4. Bấm **"Thêm kết quả đầu tiên"**.
5. Quan sát: điều hướng sang `/health-records?...&openUpload=1` nhưng không mở upload picker.

## 7) Test Plan

- **Manual**
  - Kiểm tra flow trên profile rỗng: CTA mở được upload picker.
  - Kiểm tra flow profile đã có record: `UploadButton` hiện hữu vẫn upload bình thường.
- **E2E**
  - Script từ history empty-state -> click CTA -> assert file input dialog trigger path (hoặc modal/upload state active).
  - Assert upload API nhận đúng `profileId`.
- **Regression**
  - CTA "Tải kết quả" ở dashboard home với `openUpload=1` vẫn hoạt động đúng.

## 8) Suggested Owner / Phase

- Owner: Web (FE)  
- Suggested phase: Sprint hot-fix gần nhất (ảnh hưởng entry flow quan trọng của Epic 3/5)
