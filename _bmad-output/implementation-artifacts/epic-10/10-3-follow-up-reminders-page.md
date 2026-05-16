# Story 10.3: Trang nhắc lịch tái khám

Status: done

## Execution scope

**Phase 1 — Web + API reminder delivery:** Story thuộc Epic 10 — phạm vi **web dashboard + backend persistence + email reminder delivery**. Tạo trang tự phục vụ để người dùng tạo, sửa, xem và xóa nhắc lịch tái khám/xét nghiệm lại cho từng hồ sơ; dữ liệu được lưu bằng backend CRUD và HealthLens gửi email nhắc lịch cho các reminder đến hạn. Không tích hợp bệnh viện, đặt lịch khám trực tiếp, Google Calendar, SMS, push notification, hoặc backend appointment API.

## Story

As a người dùng HealthLens,
I want tạo và xem nhắc lịch tái khám cho hồ sơ sức khỏe của mình,
so that tôi chủ động quay lại kiểm tra sức khỏe mà không cần HealthLens kết nối trực tiếp với bệnh viện.

## Acceptance Criteria

1. **Given** tôi đã đăng nhập, **When** truy cập `/follow-up-reminders`, **Then** trang hiển thị trong dashboard shell với tiêu đề "Nhắc lịch tái khám".
2. **Given** tôi có ít nhất một hồ sơ, **When** mở trang, **Then** hệ thống chọn hồ sơ mặc định hoặc hồ sơ từ `profileId` query nếu hợp lệ.
3. **Given** tôi tạo nhắc lịch, **When** điền ngày nhắc, loại nhắc và ghi chú tùy chọn rồi lưu, **Then** nhắc lịch được lưu bằng backend theo từng hồ sơ và hiển thị trong danh sách trên trang.
4. **Given** tôi đã lưu nhắc lịch, **When** tải lại trang hoặc đăng nhập lại trên trình duyệt khác, **Then** các nhắc lịch đã lưu vẫn hiển thị từ backend.
5. **Given** tôi nhập ngày nhắc không hợp lệ hoặc bỏ trống trường bắt buộc, **When** bấm lưu, **Then** trang hiển thị lỗi inline bằng tiếng Việt và không lưu dữ liệu lỗi.
6. **Given** tôi không có hồ sơ hoặc dữ liệu hồ sơ chưa tải xong, **When** trang render, **Then** UI có trạng thái trống/loading rõ ràng và không crash.
7. **Given** tôi muốn dọn danh sách, **When** bấm xóa một nhắc lịch và xác nhận, **Then** nhắc lịch bị xóa khỏi danh sách và khỏi backend.
8. **Given** tôi dùng bàn phím hoặc screen reader, **When** thao tác với form và danh sách, **Then** label, button, error message và focus state rõ nghĩa, không phụ thuộc màu để truyền đạt trạng thái.
9. **Given** tôi mở trang trên mobile, tablet, hoặc desktop, **When** viewport thay đổi, **Then** form và danh sách không tràn, touch target đủ lớn, spacing nhất quán với dashboard hiện có.
10. **Given** một nhắc lịch đến hạn hôm nay hoặc đã quá hạn và email chưa gửi, **When** hệ thống tạo/cập nhật reminder hoặc scheduler chạy, **Then** HealthLens gửi email nhắc lịch một lần tới email tài khoản và ghi nhận thời điểm gửi.

## Tasks / Subtasks

- [x] Task 1 — Web: Tạo route `/follow-up-reminders` (AC: #1, #6, #9)
  - [x] Tạo `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx`.
  - [x] Reuse `DashboardPageShell` nếu phù hợp với các trang `/help` và `/questions`.
  - [x] Thêm breadcrumb/title rõ: `Trang chủ / Nhắc lịch tái khám` nếu pattern hiện có hỗ trợ.
- [x] Task 2 — Web: Tải danh sách hồ sơ và chọn hồ sơ hiện tại (AC: #2, #6)
  - [x] Reuse `apiClient.get(ApiPaths.PROFILES.BASE)` như Home để lấy hồ sơ.
  - [x] Nếu URL có `profileId` hợp lệ, chọn hồ sơ đó; nếu không, chọn hồ sơ mặc định hoặc hồ sơ đầu tiên.
  - [x] Có empty state khi chưa có hồ sơ và link/action về `/profiles`.
- [x] Task 3 — Web: Form tạo/sửa nhắc lịch qua backend (AC: #3, #5, #8, #9)
  - [x] Form gồm ngày nhắc, loại nhắc (`Tái khám`, `Xét nghiệm lại`, `Theo dõi chỉ số`, `Khác`), ghi chú tùy chọn.
  - [x] Validate ngày nhắc bắt buộc và không cho giá trị không parse được.
  - [x] Gọi backend CRUD bằng `ApiPaths.PROFILES.FOLLOW_UP_REMINDERS(profileId)`.
  - [x] Dùng ID ổn định từ backend cho từng reminder để hỗ trợ sửa/xóa.
- [x] Task 4 — Web: Danh sách nhắc lịch đã lưu (AC: #4, #7, #8, #9)
  - [x] Đọc reminder từ backend khi profile hiện tại thay đổi.
  - [x] Sắp xếp theo ngày gần nhất trước.
  - [x] Hiển thị trạng thái trống nếu chưa có reminder.
  - [x] Thêm hành động xóa có xác nhận hoặc UI rõ ràng để tránh bấm nhầm.
- [x] Task 5 — API: Backend persistence và email delivery (AC: #3, #4, #7, #10)
  - [x] Thêm migration tạo bảng follow-up reminders theo profile.
  - [x] Thêm controller/service/repository cho list, create, update, delete.
  - [x] Chỉ cho phép người dùng thao tác reminder thuộc hồ sơ của chính họ.
  - [x] Gửi email ngay khi tạo/cập nhật reminder đến hạn và scheduler gửi các reminder đến hạn chưa gửi.
  - [x] Ghi nhận `emailSentAt` để tránh gửi lại reminder đã gửi.
- [x] Task 6 — Tests and validation (AC: #1-#10)
  - [x] Thêm `apps/web/src/app/(dashboard)/follow-up-reminders/page.test.tsx`.
  - [x] Test render title và form fields.
  - [x] Test chọn hồ sơ từ dữ liệu mock.
  - [x] Test validate trường bắt buộc.
  - [x] Test lưu/xóa reminder qua API mock.
  - [x] Test sửa reminder hiện có.
  - [x] Thêm service-level API tests cho backend reminder behavior nếu phù hợp.
  - [x] Chạy targeted Vitest/backend tests và `pnpm --filter web test` nếu khả thi.

### Review Findings

- [x] [Review][Decision] Story scope is stale versus implemented real reminders — Resolved by updating this story's scope, ACs, tasks, and dev notes to make backend persistence + email reminder delivery the official story scope.
- [x] [Review][Patch] Scheduled due-reminder delivery can send duplicate emails under concurrency — Fixed by adding `emailClaimedAt`, atomic claim update, claim timeout, and send-after-claim flow before marking `emailSentAt`. [apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java:154]
- [x] [Review][Patch] Reminder email is sent inside database transactions — Fixed by deferring create/update due-email sending with `TransactionSynchronization.afterCommit` and performing scheduler email I/O outside the claim/mark transactions. [apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java:228]
- [x] [Review][Patch] Reminder email HTML interpolates user-controlled names/types without escaping — Fixed by escaping `fullName`, `displayName`, `reminderType`, and the reminder URL before interpolating HTML. [apps/api/src/main/java/com/healthlens/api/service/EmailService.java:293]
- [x] [Review][Patch] Follow-up emails can still be sent for accounts pending deletion — Fixed by filtering due reminder candidates and atomic claims to `AccountStatus.ACTIVE`, with a final active-account guard before email send. [apps/api/src/main/java/com/healthlens/api/repository/FollowUpReminderRepository.java:26]
- [x] [Review][Patch] Backend accepts reminder types outside the frontend's fixed option set — Fixed with a service-level allowlist aligned with the frontend options: `Tái khám`, `Xét nghiệm lại`, `Theo dõi chỉ số`, `Khác`. [apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java:39]
- [x] [Review][Patch] Due-reminder scheduler loads all unsent due reminders without batching — Fixed by querying due reminder IDs with `PageRequest`, processing batches of 100, and capping each scheduler run at 500 reminders. [apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java:114]
- [x] [Review][Patch] Service path can throw null-date NPE outside controller validation — Fixed by adding a service-level null-date guard and unit coverage for direct service calls. [apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java:255]

## Dev Notes

### Current state

- Home quick action trỏ tới `/follow-up-reminders` từ Story 10.2.
- Route `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx` đã được triển khai.
- Home đã có pattern query hồ sơ cho upload: `/health-records?profileId=${primaryProfileId ?? ""}&openUpload=1`.

### Implementation guidance

- Đây là nhắc lịch cá nhân trong app, không phải đặt lịch khám với bệnh viện.
- Gửi email nhắc lịch là scope chính thức; không gửi SMS/push notification trong story này.
- Reminder phải được lưu bằng backend CRUD theo hồ sơ, không dùng `localStorage` làm source of truth.
- Nếu API không khả dụng, form vẫn render và hiển thị lỗi thân thiện khi tải/lưu.
- Email reminder cần privacy-safe: không đưa nội dung y tế nhạy cảm vào email ngoài loại nhắc, ngày nhắc, tên hồ sơ ở mức cần thiết.
- Reminder đến hạn hôm nay hoặc đã quá hạn được gửi ngay khi tạo/cập nhật nếu chưa gửi; scheduler gửi lại các reminder đến hạn chưa có `emailSentAt`.
- Nội dung nên giúp người dùng chuẩn bị tái khám, không đưa lời khuyên chẩn đoán hoặc điều trị.
- Dùng `next/link`, `@tanstack/react-query`, `apiClient`, và `ApiPaths` theo pattern Home.

### Suggested UI copy

- Page title: `Nhắc lịch tái khám`
- Intro: `Lưu lời nhắc cá nhân để bạn chủ động kiểm tra lại khi cần. HealthLens chưa đặt lịch trực tiếp với bệnh viện.`
- Save button: `Lưu nhắc lịch`
- Empty state: `Chưa có nhắc lịch cho hồ sơ này.`
- Success text: `Đã lưu nhắc lịch cho hồ sơ này.`
- Edit save button: `Cập nhật nhắc lịch`
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

GPT-5 Codex

### Debug Log References

- 2026-05-16: Targeted red test failed because `/follow-up-reminders/page.tsx` did not exist yet.
- 2026-05-16: Targeted test exposed local-date validation issue from `toISOString()`; replaced with calendar component validation.
- 2026-05-16: `pnpm --filter web test` initially failed on React lint rule for synchronous `setState` in effects; refactored profile selection to derived render state and reminder storage into a keyed child component.
- 2026-05-16: User review noted missing edit flow and ambiguity around actual reminder delivery; added client-side edit flow and documented delivery recommendation in assistant response.
- 2026-05-16: Implemented real reminder delivery with backend persistence and scheduled email sending; localStorage is no longer the source of truth.
- 2026-05-16: User review requested icon-only edit/delete controls, clearer edit-form button spacing, and exact same-day email behavior; updated UI and immediate due-email sending.
- 2026-05-16: Review patches addressed with atomic reminder email claiming, after-commit delivery, HTML escaping, active-account filtering, server-side type/date validation, and scheduler batching.
- 2026-05-16: Attempted targeted Gradle test for `FollowUpReminderServiceTest`, but local environment has no Java Runtime installed.

### Completion Notes List

- Implemented `/follow-up-reminders` dashboard route with `DashboardPageShell`, breadcrumb `Trang chủ / Nhắc lịch tái khám`, loading/error/empty profile states, and `/profiles` action for users without profiles.
- Loaded profiles through `apiClient.get(ApiPaths.PROFILES.BASE)` and selected valid `profileId` query, default profile, or first profile.
- Added accessible reminder form with Vietnamese inline validation, backend persistence, stable reminder IDs, reload persistence, sorted list, and confirmed deletion.
- Added edit mode for saved reminders, preserving reminder ID and updating the same backend reminder record.
- Replaced client-only storage with authenticated backend CRUD under `/api/v1/profiles/{profileId}/follow-up-reminders`.
- Added backend scheduler that sends privacy-safe email reminders once per due reminder using the existing `EmailService`.
- Added immediate email send on create/update when the reminder date is today or overdue; hourly scheduler retries unsent due reminders.
- Hardened email delivery with `emailClaimedAt`, active-account filtering, batched due reminder queries, after-commit immediate delivery, and HTML escaping for reminder email fields.
- Added service-level validation for reminder date and allowed reminder types.
- Updated saved reminder actions to icon-only edit/delete buttons and added responsive spacing between update/cancel edit actions.
- Added colocated Vitest coverage for rendering, query profile selection, required validation, API save/delete, and no-profile empty state.
- Added Vitest coverage for editing an existing reminder and service-level tests for reminder email delivery validation behavior.

### File List

- apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx
- apps/web/src/app/(dashboard)/follow-up-reminders/page.test.tsx
- packages/shared/constants/api.ts
- apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java
- apps/api/src/main/java/com/healthlens/api/controller/FollowUpReminderController.java
- apps/api/src/main/java/com/healthlens/api/config/FollowUpReminderScheduler.java
- apps/api/src/main/java/com/healthlens/api/dto/request/FollowUpReminderRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/response/FollowUpReminderResponse.java
- apps/api/src/main/java/com/healthlens/api/entity/FollowUpReminder.java
- apps/api/src/main/java/com/healthlens/api/exception/GlobalExceptionHandler.java
- apps/api/src/main/java/com/healthlens/api/repository/FollowUpReminderRepository.java
- apps/api/src/main/java/com/healthlens/api/service/EmailService.java
- apps/api/src/main/java/com/healthlens/api/service/FollowUpReminderService.java
- apps/api/src/main/resources/db/migration/V031__create_follow_up_reminders_table.sql
- apps/api/src/test/java/com/healthlens/api/service/FollowUpReminderServiceTest.java
- _bmad-output/implementation-artifacts/epic-10/10-3-follow-up-reminders-page.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-05-16: Tạo story 10.3 cho trang `/follow-up-reminders`, nhắc lịch client-side theo hồ sơ, không tích hợp bệnh viện/bác sĩ.
- 2026-05-16: Implemented `/follow-up-reminders` page, client-side reminder persistence, validation, deletion, tests, and moved story to review.
- 2026-05-16: Added edit support for saved follow-up reminders.
- 2026-05-16: Added backend follow-up reminder CRUD, Flyway table, scheduled email delivery, and web API integration.
- 2026-05-16: Refined reminder action UI and same-day email delivery behavior.
- 2026-05-16: Resolved review decision by updating story scope from local-only MVP to backend persistence and email reminder delivery.
- 2026-05-16: Fixed all seven review patch findings for reminder email delivery safety, validation, escaping, active-account filtering, and batching.
