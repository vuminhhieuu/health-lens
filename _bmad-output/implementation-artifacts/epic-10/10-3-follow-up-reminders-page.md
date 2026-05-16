# Story 10.3: Trang nhắc lịch tái khám

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 10 — phạm vi **web dashboard**. Tạo trang tự phục vụ để người dùng lưu nhắc lịch tái khám/xét nghiệm lại cho từng hồ sơ trong trình duyệt. Không tích hợp bệnh viện, Google Calendar, email/SMS/push notification, hoặc backend appointment API.

## Story

As a người dùng HealthLens,
I want tạo và xem nhắc lịch tái khám cho hồ sơ sức khỏe của mình,
so that tôi chủ động quay lại kiểm tra sức khỏe mà không cần HealthLens kết nối trực tiếp với bệnh viện.

## Acceptance Criteria

1. **Given** tôi đã đăng nhập, **When** truy cập `/follow-up-reminders`, **Then** trang hiển thị trong dashboard shell với tiêu đề "Nhắc lịch tái khám".
2. **Given** tôi có ít nhất một hồ sơ, **When** mở trang, **Then** hệ thống chọn hồ sơ mặc định hoặc hồ sơ từ `profileId` query nếu hợp lệ.
3. **Given** tôi tạo nhắc lịch, **When** điền ngày nhắc, loại nhắc và ghi chú tùy chọn rồi lưu, **Then** nhắc lịch được lưu ở client theo từng hồ sơ và hiển thị trong danh sách trên trang.
4. **Given** tôi đã lưu nhắc lịch, **When** tải lại trang trên cùng trình duyệt, **Then** các nhắc lịch đã lưu vẫn hiển thị.
5. **Given** tôi nhập ngày nhắc không hợp lệ hoặc bỏ trống trường bắt buộc, **When** bấm lưu, **Then** trang hiển thị lỗi inline bằng tiếng Việt và không lưu dữ liệu lỗi.
6. **Given** tôi không có hồ sơ hoặc dữ liệu hồ sơ chưa tải xong, **When** trang render, **Then** UI có trạng thái trống/loading rõ ràng và không crash.
7. **Given** tôi muốn dọn danh sách, **When** bấm xóa một nhắc lịch và xác nhận, **Then** nhắc lịch bị xóa khỏi danh sách và khỏi storage.
8. **Given** tôi dùng bàn phím hoặc screen reader, **When** thao tác với form và danh sách, **Then** label, button, error message và focus state rõ nghĩa, không phụ thuộc màu để truyền đạt trạng thái.
9. **Given** tôi mở trang trên mobile, tablet, hoặc desktop, **When** viewport thay đổi, **Then** form và danh sách không tràn, touch target đủ lớn, spacing nhất quán với dashboard hiện có.

## Tasks / Subtasks

- [ ] Task 1 — Web: Tạo route `/follow-up-reminders` (AC: #1, #6, #9)
  - [ ] Tạo `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx`.
  - [ ] Reuse `DashboardPageShell` nếu phù hợp với các trang `/help` và `/questions`.
  - [ ] Thêm breadcrumb/title rõ: `Trang chủ / Nhắc lịch tái khám` nếu pattern hiện có hỗ trợ.
- [ ] Task 2 — Web: Tải danh sách hồ sơ và chọn hồ sơ hiện tại (AC: #2, #6)
  - [ ] Reuse `apiClient.get(ApiPaths.PROFILES.BASE)` như Home để lấy hồ sơ.
  - [ ] Nếu URL có `profileId` hợp lệ, chọn hồ sơ đó; nếu không, chọn hồ sơ mặc định hoặc hồ sơ đầu tiên.
  - [ ] Có empty state khi chưa có hồ sơ và link/action về `/profiles`.
- [ ] Task 3 — Web: Form tạo nhắc lịch client-side (AC: #3, #5, #8, #9)
  - [ ] Form gồm ngày nhắc, loại nhắc (`Tái khám`, `Xét nghiệm lại`, `Theo dõi chỉ số`, `Khác`), ghi chú tùy chọn.
  - [ ] Validate ngày nhắc bắt buộc và không cho giá trị không parse được.
  - [ ] Lưu vào `localStorage` bằng key có profile id, ví dụ `healthlens.followUpReminders.${profileId}`.
  - [ ] Dùng ID ổn định cho từng reminder để hỗ trợ xóa.
- [ ] Task 4 — Web: Danh sách nhắc lịch đã lưu (AC: #4, #7, #8, #9)
  - [ ] Đọc reminder từ `localStorage` khi profile hiện tại thay đổi.
  - [ ] Sắp xếp theo ngày gần nhất trước.
  - [ ] Hiển thị trạng thái trống nếu chưa có reminder.
  - [ ] Thêm hành động xóa có xác nhận hoặc UI rõ ràng để tránh bấm nhầm.
- [ ] Task 5 — Tests and validation (AC: #1-#9)
  - [ ] Thêm `apps/web/src/app/(dashboard)/follow-up-reminders/page.test.tsx`.
  - [ ] Test render title và form fields.
  - [ ] Test chọn hồ sơ từ dữ liệu mock.
  - [ ] Test validate trường bắt buộc.
  - [ ] Test lưu, reload bằng cách mock `localStorage`, và xóa reminder.
  - [ ] Chạy targeted Vitest cho page mới và `pnpm --filter web test` nếu khả thi.

## Dev Notes

### Current state

- Home quick action sẽ trỏ tới `/follow-up-reminders` từ Story 10.2.
- Hiện chưa có route `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx`.
- Home đã có pattern query hồ sơ cho upload: `/health-records?profileId=${primaryProfileId ?? ""}&openUpload=1`.

### Implementation guidance

- Đây là nhắc lịch cá nhân trong app, không phải đặt lịch khám với bệnh viện.
- Không gửi email/SMS/push notification trong story này.
- Không gọi backend mới. Lưu client-side là chấp nhận được cho MVP.
- Nếu `localStorage` không khả dụng, form vẫn render và hiển thị lỗi thân thiện khi lưu.
- Nội dung nên giúp người dùng chuẩn bị tái khám, không đưa lời khuyên chẩn đoán hoặc điều trị.
- Dùng `next/link`, `@tanstack/react-query`, `apiClient`, và `ApiPaths` theo pattern Home.

### Suggested UI copy

- Page title: `Nhắc lịch tái khám`
- Intro: `Lưu lời nhắc cá nhân để bạn chủ động kiểm tra lại khi cần. HealthLens chưa đặt lịch trực tiếp với bệnh viện.`
- Save button: `Lưu nhắc lịch`
- Empty state: `Chưa có nhắc lịch cho hồ sơ này.`
- Success text: `Đã lưu nhắc lịch cho hồ sơ này.`
- Delete button: `Xóa nhắc lịch`

### Project Structure Notes

- Frontend app router: `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx`.
- Test colocated: `apps/web/src/app/(dashboard)/follow-up-reminders/page.test.tsx`.
- Reuse dashboard visual language: teal primary, white cards, clear icons, generous spacing.
- Use `lucide-react` icons already installed; no new dependency for date picker.

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Cấu-Trúc-Frontend-Next.js-Web]
- [Source: _bmad-output/planning-artifacts/architecture.md#Quy-Ước-Vị-Trí-Test]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md#Layout-Foundation]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md#Journey-Patterns]
- [Previous: _bmad-output/implementation-artifacts/epic-10/10-2-home-quick-actions-self-service.md]
- [Current: apps/web/src/app/(dashboard)/home/page.tsx]
- [Current: apps/web/src/app/(dashboard)/help/page.tsx]

## Dev Agent Record

### Agent Model Used

TBD by dev agent

### Debug Log References

### Completion Notes List

### File List

### Change Log

- 2026-05-16: Tạo story 10.3 cho trang `/follow-up-reminders`, nhắc lịch client-side theo hồ sơ, không tích hợp bệnh viện/bác sĩ.
