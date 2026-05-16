# Story 10.1: Trang trợ giúp và trang thắc mắc

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 10 — phạm vi **web dashboard**. Triển khai hai trang nội dung hỗ trợ người dùng đã đăng nhập, liên kết từ Home và icon dấu hỏi ở header. Không thêm backend/API trừ khi có yêu cầu nội dung động sau này.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

Không có screen Stitch riêng cho trang trợ giúp/thắc mắc trong snapshot hiện tại. Reuse shell, spacing, iconography và visual language từ Web Dashboard hiện có.

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script liên quan nếu cần.

## Story

As a người dùng HealthLens,
I want mở trang trợ giúp từ Home và trang thắc mắc từ biểu tượng dấu hỏi ở header,
so that tôi biết cách dùng các tính năng chính và tìm câu trả lời nhanh khi gặp vấn đề.

## Acceptance Criteria

1. **Given** tôi đang ở trang Home, **When** bấm tile/nút "Trợ giúp" hoặc CTA "Đọc hướng dẫn", **Then** hệ thống điều hướng đến trang `/help` trong dashboard shell.
2. **Given** tôi đang ở bất kỳ trang dashboard nào có header, **When** bấm biểu tượng `?`/`HelpCircle`, **Then** hệ thống điều hướng đến trang `/questions`.
3. **Given** trang `/help` được mở, **When** nội dung hiển thị, **Then** trang có các nhóm hướng dẫn tối thiểu: tải kết quả khám, xem kết quả/giải thích chỉ số, quản lý hồ sơ, chia sẻ cho người thân, tải PDF kết quả, và quyền riêng tư dữ liệu.
4. **Given** trang `/questions` được mở, **When** nội dung hiển thị, **Then** trang có danh sách thắc mắc thường gặp tối thiểu về OCR không đọc được, chỉ số bất thường, thông tin chỉ mang tính tham khảo, chia sẻ quyền xem, xóa dữ liệu, và lỗi đăng nhập/tải file.
5. **Given** người dùng đọc nội dung y tế trên hai trang, **When** nội dung nhắc đến giải thích/chỉ số sức khỏe, **Then** phải có disclaimer rõ ràng: HealthLens chỉ hỗ trợ tham khảo, không thay thế tư vấn y tế chuyên môn.
6. **Given** người dùng dùng bàn phím hoặc screen reader, **When** tab qua các liên kết và phần FAQ, **Then** các control có `aria-label`/text rõ nghĩa, focus visible, heading hierarchy hợp lý, và không phụ thuộc màu để truyền đạt trạng thái.
7. **Given** người dùng mở trên mobile, tablet, hoặc desktop, **When** viewport thay đổi, **Then** nội dung không bị tràn, header/nav không che nội dung, các card/section giữ khoảng cách dễ đọc theo dashboard hiện có.
8. **Given** tôi chưa đăng nhập và truy cập trực tiếp `/help` hoặc `/questions`, **When** dashboard layout kiểm tra auth, **Then** hành vi redirect về `/login?returnUrl=...` giữ nguyên như các dashboard route khác.

## Tasks / Subtasks

- [x] Task 1 — Web: Tạo route trang trợ giúp `/help` (AC: #1, #3, #5, #6, #7, #8)
  - [x] Tạo `apps/web/src/app/(dashboard)/help/page.tsx`.
  - [x] Reuse `DashboardPageShell` để giữ layout, breadcrumb nếu cần: `Trang chủ / Trợ giúp`.
  - [x] Thêm các section hướng dẫn theo nhóm: tải kết quả, xem giải thích, hồ sơ, chia sẻ, PDF, quyền riêng tư.
  - [x] Thêm CTA nội bộ đến các route hiện có: `/health-records`, `/profiles`, `/settings/profile`, và Home nếu phù hợp.
  - [x] Nội dung tiếng Việt đơn giản, tone "Bác sĩ gia đình thân thiện"; tránh jargon y tế.
- [x] Task 2 — Web: Tạo route trang thắc mắc `/questions` (AC: #2, #4, #5, #6, #7, #8)
  - [x] Tạo `apps/web/src/app/(dashboard)/questions/page.tsx`.
  - [x] Reuse `DashboardPageShell` với breadcrumb hoặc title rõ ràng: "Thắc mắc thường gặp".
  - [x] Hiển thị FAQ dạng các mục dễ scan; có thể dùng native `<details><summary>` để có accessible expand/collapse mà không cần thư viện mới.
  - [x] Bao gồm các FAQ tối thiểu từ AC #4 và disclaimer y tế ở cuối hoặc trong section riêng.
- [x] Task 3 — Web: Kết nối entry points hiện có (AC: #1, #2)
  - [x] Trong `apps/web/src/app/(dashboard)/home/page.tsx`, đổi tile "Trợ giúp" từ disabled sang `href="/help"`.
  - [x] Trong card "Chăm sóc sức khỏe chủ động", đổi button "Đọc hướng dẫn" thành `Link` đến `/help`.
  - [x] Trong `apps/web/src/app/(dashboard)/layout.tsx`, đổi button icon `HelpCircle` ở header thành `Link href="/questions"` hoặc button dùng router push.
  - [x] Thêm `title` và `aria-label="Mở trang thắc mắc"` cho icon dấu hỏi.
- [x] Task 4 — UI/accessibility hardening (AC: #6, #7)
  - [x] Dùng `lucide-react` icons đã có; không thêm icon library mới.
  - [x] Giữ touch target tối thiểu khoảng 48px cho các action chính.
  - [x] Đảm bảo focus state không bị mất với Tailwind classes hiện có.
  - [x] Không tạo nested cards phức tạp; dùng section/card đơn giản theo dashboard hiện tại.
- [x] Task 5 — Tests and validation (AC: #1, #2, #6, #7)
  - [x] Thêm hoặc cập nhật test co-located cho Home để tile "Trợ giúp" có `href="/help"`.
  - [x] Thêm test cho dashboard layout hoặc route shell để icon `HelpCircle` trỏ đến `/questions`.
  - [x] Chạy `pnpm --filter web test` hoặc tối thiểu `pnpm --filter web lint` nếu test suite quá rộng.

### Review Findings

- [x] [Review][Patch] Quyền riêng tư CTA trỏ tới route `/settings` không tồn tại [apps/web/src/app/(dashboard)/help/page.tsx:60]

## Dev Notes

### Current state

- `apps/web/src/app/(dashboard)/home/page.tsx` đã có tile:

```tsx
<ActionTile
  icon={<ShieldPlus className="h-6 w-6" />}
  label="Trợ giúp"
  disabled
/>
```

- Cùng trang có CTA "Đọc hướng dẫn" trong card "Chăm sóc sức khỏe chủ động", hiện là `<button>` không điều hướng.
- `apps/web/src/app/(dashboard)/layout.tsx` đã import `HelpCircle` và render icon trong header như button không có handler:

```tsx
<button className="p-2 text-[#3d4947] hover:bg-[#e9f6f3] transition-colors rounded-full">
  <HelpCircle className="w-5 h-5" />
</button>
```

### Implementation guidance

- Đặt cả hai page trong route group `(dashboard)` để tự động kế thừa auth guard, top header, sidebar và mobile nav.
- Story này nên là static-content-first. Không gọi API mới, không tạo bảng DB, không thêm admin CMS.
- Nếu cần component dùng chung, tạo cục bộ trong page hoặc `apps/web/src/components/features/help/` chỉ khi tránh được duplication đáng kể.
- Nội dung nên tập trung vào thao tác trong app, không đưa lời khuyên chẩn đoán y khoa cụ thể.
- Các link nội bộ nên dùng `next/link`, không dùng `window.location`.
- Không sửa logic auth/bootstrap trong `DashboardLayout`; AC #8 dựa vào behavior hiện có.

### Suggested content outline

Trang `/help`:

1. Bắt đầu nhanh: upload kết quả khám.
2. Đọc kết quả: trạng thái bình thường/cần chú ý/bất thường và giải thích chỉ số.
3. Quản lý hồ sơ: hồ sơ cá nhân và hồ sơ gia đình.
4. Chia sẻ an toàn: quyền xem, thu hồi quyền.
5. Lưu trữ: xem lịch sử và tải PDF.
6. Quyền riêng tư: consent, bảo vệ dữ liệu, xóa tài khoản/dữ liệu.

Trang `/questions`:

1. "Nếu OCR không đọc được thì làm gì?"
2. "Chỉ số bất thường có nghĩa là tôi đang bệnh nặng không?"
3. "HealthLens có thay thế bác sĩ không?"
4. "Người thân được xem những gì khi tôi chia sẻ?"
5. "Tôi có thể thu hồi quyền chia sẻ không?"
6. "Tôi có thể xóa dữ liệu sức khỏe không?"
7. "Tại sao tôi không đăng nhập hoặc tải file được?"

### References

- [Source: architecture.md#Cấu-Trúc-Frontend-Next.js-Web]
- [Source: architecture.md#Quy-Ước-Vị-Trí-Test]
- [Source: epics.md#Story-10.1]
- [Source: ux-design-specification.md#UX-DR7-Navigation-Pattern]
- [Source: ux-design-specification.md#UX-DR8-Accessibility-Implementation]
- [Source: ux-design-specification.md#Visual-Direction-Calm-Healthcare]
- [Current: apps/web/src/app/(dashboard)/home/page.tsx]
- [Current: apps/web/src/app/(dashboard)/layout.tsx]
- [Current: apps/web/src/components/layout/DashboardPageShell.tsx]

## Dev Agent Record

### Agent Model Used

Codex GPT-5

### Debug Log References

- 2026-05-15: Started Story 10.1 implementation; marked sprint/story status `in-progress`.
- `pnpm --filter web exec vitest run 'src/app/(dashboard)/help/page.test.tsx' 'src/app/(dashboard)/questions/page.test.tsx' 'src/app/(dashboard)/home/page.test.tsx' 'src/app/(dashboard)/layout.test.tsx'` — failed before implementation as expected (missing routes/links).
- `pnpm --filter web exec vitest run 'src/app/(dashboard)/help/page.test.tsx' 'src/app/(dashboard)/questions/page.test.tsx' 'src/app/(dashboard)/home/page.test.tsx' 'src/app/(dashboard)/layout.test.tsx'` — pass after implementation.
- `pnpm --filter web test` — pass (`eslint` + 9 test files / 11 tests).
- 2026-05-15 code review patch: `pnpm --filter web test` — pass (`eslint` + 9 test files / 11 tests).

### Completion Notes List

- Added `/help` dashboard page with static Vietnamese guidance sections for upload, result explanation, profile management, sharing, PDF download, and data privacy.
- Added `/questions` dashboard page with accessible native FAQ disclosure items covering OCR failure, abnormal metrics, medical disclaimer, sharing scope, revocation, data deletion, and login/file issues.
- Wired Home "Trợ giúp" tile and "Đọc hướng dẫn" CTA to `/help`.
- Wired dashboard header `HelpCircle` icon to `/questions` with `title`, `aria-label`, focus state, and 48px target.
- Added focused web tests for the new pages and navigation entry points; full web lint/test suite passes.
- Resolved code review finding: changed the privacy/data deletion CTA from missing `/settings` route to existing `/settings/delete-account`, with test coverage.

### File List

- apps/web/src/app/(dashboard)/help/page.tsx
- apps/web/src/app/(dashboard)/help/page.test.tsx
- apps/web/src/app/(dashboard)/questions/page.tsx
- apps/web/src/app/(dashboard)/questions/page.test.tsx
- apps/web/src/app/(dashboard)/home/page.tsx
- apps/web/src/app/(dashboard)/home/page.test.tsx
- apps/web/src/app/(dashboard)/layout.tsx
- apps/web/src/app/(dashboard)/layout.test.tsx
- _bmad-output/implementation-artifacts/epic-10/10-1-help-and-questions-pages.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-05-15: Tạo story 10.1 cho trang trợ giúp từ Home và trang thắc mắc từ icon dấu hỏi header.
- 2026-05-15: Implemented Story 10.1 web help/questions pages, entry-point navigation, accessibility polish, and tests; marked ready for review.
- 2026-05-15: Addressed code review finding for invalid privacy CTA route and updated tests.
