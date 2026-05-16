# Story 10.4: Trang tóm tắt đi khám

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 10 — phạm vi **web dashboard**. Tạo trang tổng hợp thông tin giúp người dùng chuẩn bị khi gặp bác sĩ ngoài đời. Không thêm bác sĩ trực tuyến, chat y tế, chẩn đoán AI, hay backend mới nếu dữ liệu hiện có đủ.

## Story

As a người dùng HealthLens,
I want xem một bản tóm tắt gọn để mang theo khi đi khám,
so that tôi có thể trao đổi với bác sĩ rõ ràng hơn mà không hiểu nhầm HealthLens là dịch vụ tư vấn y tế.

## Acceptance Criteria

1. **Given** tôi đã đăng nhập, **When** truy cập `/visit-summary`, **Then** trang hiển thị trong dashboard shell với tiêu đề "Tóm tắt đi khám".
2. **Given** tôi có ít nhất một hồ sơ, **When** mở trang, **Then** hệ thống chọn hồ sơ mặc định hoặc hồ sơ từ `profileId` query nếu hợp lệ.
3. **Given** hồ sơ có kết quả khám gần đây, **When** trang render, **Then** hiển thị kết quả gần nhất gồm loại xét nghiệm/phiếu khám, ngày thực hiện, trạng thái tổng quan, và số chỉ số bất thường nếu có.
4. **Given** hồ sơ chưa có kết quả khám, **When** trang render, **Then** hiển thị empty state hữu ích và CTA/link đến flow tải kết quả, không hiển thị dữ liệu giả.
5. **Given** người dùng chuẩn bị đi khám, **When** xem trang, **Then** trang có danh sách câu hỏi gợi ý để hỏi bác sĩ ngoài đời, tối thiểu 5 câu và không mang tính chẩn đoán.
6. **Given** trang hiển thị thông tin sức khỏe, **When** nội dung render, **Then** có disclaimer rõ rằng HealthLens chỉ hỗ trợ chuẩn bị thông tin tham khảo, không thay thế tư vấn, chẩn đoán, hoặc điều trị từ bác sĩ.
7. **Given** tôi cần mang thông tin ra ngoài, **When** bấm hành động in hoặc sao chép, **Then** trang hỗ trợ ít nhất một cách xuất thông tin đơn giản: `window.print()` hoặc copy nội dung tóm tắt vào clipboard với phản hồi thành công/thất bại.
8. **Given** API hồ sơ/kết quả lỗi hoặc đang tải, **When** trang render, **Then** UI có loading/error state rõ ràng và không crash.
9. **Given** tôi dùng bàn phím hoặc screen reader, **When** thao tác với trang, **Then** heading hierarchy, link/button labels, focus states và nội dung disclaimer đều rõ nghĩa.
10. **Given** tôi mở trang trên mobile, tablet, hoặc desktop, **When** viewport thay đổi, **Then** nội dung tóm tắt không tràn, dễ đọc, và giữ visual language dashboard hiện có.

## Tasks / Subtasks

- [x] Task 1 — Web: Tạo route `/visit-summary` (AC: #1, #8, #10)
  - [x] Tạo `apps/web/src/app/(dashboard)/visit-summary/page.tsx`.
  - [x] Reuse `DashboardPageShell` nếu phù hợp với `/help` và `/questions`.
  - [x] Thêm title rõ: `Tóm tắt đi khám`.
- [x] Task 2 — Web: Tải hồ sơ và kết quả gần nhất (AC: #2, #3, #4, #8)
  - [x] Reuse `apiClient.get(ApiPaths.PROFILES.BASE)` để lấy hồ sơ.
  - [x] Chọn profile từ query `profileId`, default profile, hoặc profile đầu tiên.
  - [x] Reuse endpoint `ApiPaths.PROFILES.HEALTH_RECORDS(profileId)` với `page: 0, limit: 1` để lấy kết quả gần nhất.
  - [x] Có loading, error, và empty state rõ ràng.
- [x] Task 3 — Web: Hiển thị bản tóm tắt và câu hỏi gợi ý (AC: #3, #4, #5, #6, #10)
  - [x] Hiển thị tên hồ sơ, loại kết quả, ngày thực hiện, trạng thái, số chỉ số bất thường.
  - [x] Dùng `recordStatusLabel` hoặc helper tương đương; tránh duplicate quá mức nếu có thể trích helper dùng chung.
  - [x] Thêm tối thiểu 5 câu hỏi gợi ý không chẩn đoán.
  - [x] Thêm disclaimer y tế nổi bật nhưng không gây hoảng sợ.
- [x] Task 4 — Web: Hỗ trợ mang thông tin ra ngoài (AC: #7, #9)
  - [x] Thêm nút `In tóm tắt` dùng `window.print()` hoặc nút `Sao chép tóm tắt`.
  - [x] Nếu dùng clipboard, xử lý success/error bằng text state hoặc alert thân thiện.
  - [x] Đảm bảo button có accessible name và focus state.
- [x] Task 5 — Tests and validation (AC: #1-#10)
  - [x] Thêm `apps/web/src/app/(dashboard)/visit-summary/page.test.tsx`.
  - [x] Test render title, disclaimer, câu hỏi gợi ý.
  - [x] Test empty state khi không có result.
  - [x] Test hiển thị result gần nhất từ mock query.
  - [x] Test print/copy action bằng mock `window.print` hoặc `navigator.clipboard`.
  - [x] Chạy targeted Vitest cho page mới và `pnpm --filter web test` nếu khả thi.

## Dev Notes

### Current state

- Home quick action sẽ trỏ tới `/visit-summary` từ Story 10.2.
- Hiện chưa có route `apps/web/src/app/(dashboard)/visit-summary/page.tsx`.
- Home đã có logic lấy hồ sơ và recent records:
  - `ApiPaths.PROFILES.BASE`
  - `ApiPaths.PROFILES.HEALTH_RECORDS(primaryProfileId)`
  - params `{ page: 0, limit: 3 }`

### Implementation guidance

- Trang này giúp người dùng chuẩn bị cuộc hẹn ngoài đời, không phải liên hệ bác sĩ trong HealthLens.
- Không tạo dữ liệu bác sĩ/bệnh viện giả.
- Không dùng ngôn ngữ chẩn đoán, kê toa, hoặc khẳng định bệnh.
- Nếu chưa có dữ liệu result, ưu tiên CTA tải kết quả: `/health-records?profileId=${profileId}&openUpload=1`.
- Có thể tạo helper nhỏ cục bộ cho format status/date; chỉ trích ra shared helper nếu tránh duplicate thực sự đáng kể.
- Nếu dùng `window.print()`, đảm bảo nút không xuất hiện vô nghĩa trong test SSR; page là client component nếu cần query hooks.

### Suggested UI copy

- Page title: `Tóm tắt đi khám`
- Intro: `Chuẩn bị thông tin chính để trao đổi với bác sĩ khi bạn đi khám trực tiếp.`
- Empty state: `Chưa có kết quả nào để tóm tắt cho hồ sơ này.`
- CTA: `Tải kết quả khám`
- Questions section: `Câu hỏi nên trao đổi với bác sĩ`
- Disclaimer: `HealthLens giúp bạn chuẩn bị thông tin tham khảo. Nội dung này không thay thế tư vấn, chẩn đoán hoặc điều trị từ bác sĩ.`
- Print/copy button: `In tóm tắt` hoặc `Sao chép tóm tắt`

### Project Structure Notes

- Frontend app router: `apps/web/src/app/(dashboard)/visit-summary/page.tsx`.
- Test colocated: `apps/web/src/app/(dashboard)/visit-summary/page.test.tsx`.
- Reuse dashboard visual language: teal primary, white cards, clear sections, generous spacing.
- Use `lucide-react` icons already installed; no new dependency.

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Cấu-Trúc-Frontend-Next.js-Web]
- [Source: _bmad-output/planning-artifacts/architecture.md#Quy-Ước-Vị-Trí-Test]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md#Layout-Foundation]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md#Error-Messages]
- [Previous: _bmad-output/implementation-artifacts/epic-10/10-2-home-quick-actions-self-service.md]
- [Current: apps/web/src/app/(dashboard)/home/page.tsx]
- [Current: apps/web/src/app/(dashboard)/help/page.tsx]

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-05-16: Targeted red test confirmed route missing before implementation: `pnpm --filter web exec vitest run 'src/app/(dashboard)/visit-summary/page.test.tsx'`.
- 2026-05-16: Targeted page test passed after implementation: `pnpm --filter web exec vitest run 'src/app/(dashboard)/visit-summary/page.test.tsx'` — 6 tests passed.
- 2026-05-16: Full web validation passed: `pnpm --filter web test` — lint passed, 11 test files / 24 tests passed.
- 2026-05-16: Code review targeted validation passed: `pnpm --filter web exec vitest run 'src/app/(dashboard)/visit-summary/page.test.tsx'` — 6 tests passed.
- 2026-05-16: PDF/print enhancement targeted validation passed: `pnpm --filter web exec vitest run 'src/app/(dashboard)/visit-summary/page.test.tsx'` — 7 tests passed.
- 2026-05-16: PDF/print enhancement full web validation passed: `pnpm --filter web test` — lint passed, 11 test files / 25 tests passed.
- 2026-05-16: Dashboard print chrome fix validation passed: `pnpm --filter web exec vitest run 'src/app/(dashboard)/layout.test.tsx' 'src/app/(dashboard)/visit-summary/page.test.tsx'` — 8 tests passed.
- 2026-05-16: Dashboard print chrome fix full web validation passed: `pnpm --filter web test` — lint passed, 11 test files / 25 tests passed.
- 2026-05-16: Print content trim validation passed: `pnpm --filter web exec vitest run 'src/app/(dashboard)/visit-summary/page.test.tsx'` — 7 tests passed.
- 2026-05-16: Suspense boundary fix and frontend test-file removal validation passed: `pnpm --filter web test` — lint passed, no frontend test files found, Vitest exited with code 0 via `--passWithNoTests`.
- 2026-05-16: Suspense boundary TypeScript validation passed: `pnpm --filter web exec tsc --noEmit`.
- 2026-05-16: Production build check attempted with `pnpm --filter web build`, blocked by local Node.js 18.16.0 because Next.js requires `>=20.9.0`.

### Completion Notes List

- Implemented `/visit-summary` as a dashboard client page with profile selection, default/query profile resolution, latest health record loading via existing `ApiPaths`, and responsive dashboard visual language.
- Added loading, profile error, record error, no-profile, and no-record states without showing fake health data.
- Added concise visit summary content, five non-diagnostic doctor discussion questions, clear medical disclaimer, and `window.print()` export action.
- Added colocated tests covering title/disclaimer/questions, query profile selection, latest result display, empty state CTA, error state, and print behavior.
- Enhanced the print/PDF output with an A4-oriented summary sheet for doctor visits, including self-fill sections for visit goals, concerns, medicines/supplements, allergies, latest record summary, suggested questions, and a stronger source/disclaimer note.
- Fixed dashboard print chrome by hiding the top navigation, sidebar, and mobile nav in print mode and removing the dashboard left margin for printed content.
- Removed screen-only profile selector and on-screen latest-result card from print/PDF output so the PDF shows only the doctor-facing summary sheet.
- Wrapped the `useSearchParams()` implementation in a Suspense-backed inner component to avoid App Router production prerender/build issues.
- Removed frontend test files under `apps/web/src` per request and updated the web test script to pass cleanly when no test files remain.

### File List

- `_bmad-output/implementation-artifacts/epic-10/10-4-visit-summary-page.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `apps/web/package.json`
- `apps/web/src/app/(dashboard)/layout.tsx`
- `apps/web/src/app/(dashboard)/visit-summary/page.tsx`

### Change Log

- 2026-05-16: Tạo story 10.4 cho trang `/visit-summary`, tóm tắt thông tin đi khám và disclaimer y tế rõ ràng, không kết nối bác sĩ trực tuyến.
- 2026-05-16: Implemented story 10.4 `/visit-summary` page, tests, and validation; moved story to review.
- 2026-05-16: Code review clean; moved story to done.
- 2026-05-16: Enhanced `In / lưu PDF` output with doctor-facing print layout and validation coverage.
- 2026-05-16: Fixed print mode so dashboard nav, buttons, and sidebar are hidden from the saved PDF.
- 2026-05-16: Removed the screen profile selector and latest-result card from PDF output.
- 2026-05-16: Fixed Copilot Suspense warning for `useSearchParams()` and removed frontend test files.
