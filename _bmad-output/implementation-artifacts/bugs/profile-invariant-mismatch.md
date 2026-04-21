# Bug Ticket: Profile Invariant Mismatch (`/users/me` vs `/profiles`)

Status: open  
Priority: high  
Type: bug / consistency / data-model

## 1) Problem Summary

Người dùng thấy mục **"Tôi"** ở trang hồ sơ (dữ liệu từ `GET /api/v1/users/me`) nhưng khi vào luồng upload kết quả khám (dùng `GET /api/v1/profiles`) lại nhận trạng thái **không có hồ sơ**.

Kết quả là UX mâu thuẫn: nhìn như đã có hồ sơ nhưng thực tế không có `profileId` hợp lệ trong bảng `profiles` để upload.

## 2) User Impact

- Chặn luồng chính của Epic 3 (upload xét nghiệm) với user mới/chưa tạo profile thủ công.
- Tăng nhầm lẫn và tạo cảm giác lỗi hệ thống.
- Tăng nguy cơ support ticket về "mất hồ sơ".

## 3) Root Cause

- UI trang hồ sơ đang hiển thị "Tôi" từ `users` domain (account identity), không đảm bảo có bản ghi trong `profiles`.
- Domain invariant chưa được enforce rõ ràng: **mỗi user cần có ít nhất 1 profile** để dùng feature health-record upload.
- Dữ liệu legacy (user cũ) không được backfill profile mặc định.

## 4) Proposed Fix (Best Practice - Hybrid)

Áp dụng mô hình **Hybrid: eager by default, lazy as guardrail**.

1. **Eager create on register**  
   - Khi register thành công, tạo luôn default profile cho user.
2. **Keep lazy ensure as safety net**  
   - Giữ endpoint idempotent `POST /api/v1/profiles/ensure-default` để self-heal user legacy hoặc partial failure.
3. **Backfill migration/task**  
   - Chạy job/script tạo default profile cho user hiện hữu chưa có profile.
4. **DB guardrail**  
   - Ràng buộc để tránh duplicate profile mặc định theo `user_id` (tùy schema policy).
5. **Observability**  
   - Theo dõi metric/log:
     - `profile_created_on_register`
     - `profile_auto_healed_by_ensure`
     - `upload_blocked_no_profile`

## 5) Acceptance Criteria

1. User mới sau register luôn có ít nhất 1 profile hợp lệ trong DB.  
2. User cũ thiếu profile được tự-heal khi vào luồng cần profile (`ensure-default`).  
3. Trang upload không còn hiển thị lỗi "chưa có hồ sơ" trong trường hợp đã có account hợp lệ.  
4. Không tạo duplicate profile khi nhiều request đồng thời.  
5. Dashboard/monitoring có thể quan sát số lượng auto-heal và detect anomaly.

## 6) Test Plan

- **Integration**
  - Register -> verify có profile mặc định.
  - User legacy không profile -> gọi `ensure-default` -> tạo đúng 1 bản ghi.
- **Concurrency**
  - N request song song `ensure-default` cùng user -> vẫn chỉ 1 profile.
- **E2E**
  - User mới login + consent -> vào upload -> có profile để chọn và upload được.
- **Regression**
  - User đã có profile từ trước không bị tạo profile thừa.

## 7) Out of Scope

- Refactor toàn bộ UI profile list.
- Thiết kế lại hoàn toàn domain model user/profile ngoài phạm vi invariant cần thiết.

## 8) Suggested Owner / Phase

- Owner: Backend + Web  
- Sprint: gần nhất sau Story 3.1 (ưu tiên cao, vì ảnh hưởng luồng upload cốt lõi)
