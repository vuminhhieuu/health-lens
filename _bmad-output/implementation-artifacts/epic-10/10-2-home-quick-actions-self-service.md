# Story 10.2: Thay thế thao tác nhanh chưa khả dụng bằng hành động tự phục vụ

Status: review

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 10 — phạm vi **web dashboard Home**. Thay hai quick action đang disabled vì chưa có tích hợp bệnh viện/bác sĩ bằng các hành động người dùng có thể thực hiện ngay trong HealthLens. Không thêm backend đặt lịch bệnh viện, không thêm tính năng chat/liên hệ bác sĩ thật, không hứa có tư vấn y tế chuyên môn.

## Story

As a người dùng HealthLens,
I want các ô "Thao tác nhanh" trên Home đều là hành động có thể dùng ngay,
so that tôi không bị hụt kỳ vọng bởi tính năng chưa sẵn sàng và vẫn có cách chuẩn bị cho lần tái khám ngoài đời.

## Acceptance Criteria

1. **Given** tôi đang ở trang Home, **When** nhìn vào nhóm "Thao tác nhanh", **Then** không còn ô disabled "Đặt lịch khám" hoặc "Liên hệ bác sĩ".
2. **Given** hệ thống chưa tích hợp bệnh viện/phòng khám, **When** quick action liên quan đến lịch khám hiển thị, **Then** label phải là "Nhắc lịch tái khám" và không được dùng ngôn ngữ ám chỉ đặt lịch trực tiếp với cơ sở y tế.
3. **Given** tôi bấm "Nhắc lịch tái khám", **When** action được kích hoạt, **Then** hệ thống điều hướng đến trang `/follow-up-reminders` trong dashboard shell.
4. **Given** trang Home hiển thị quick actions, **When** tile "Nhắc lịch tái khám" render, **Then** tile là link hợp lệ đến `/follow-up-reminders`, không phải button disabled hoặc link rỗng.
5. **Given** hệ thống chưa có bác sĩ trực tuyến, **When** quick action thay cho "Liên hệ bác sĩ" hiển thị, **Then** label phải là "Tóm tắt đi khám" và không được tạo kỳ vọng rằng HealthLens sẽ kết nối trực tiếp với bác sĩ.
6. **Given** tôi bấm "Tóm tắt đi khám", **When** action được kích hoạt, **Then** hệ thống điều hướng đến trang `/visit-summary` trong dashboard shell.
7. **Given** trang Home hiển thị quick actions, **When** tile "Tóm tắt đi khám" render, **Then** tile là link hợp lệ đến `/visit-summary`, không phải button disabled hoặc link rỗng.
8. **Given** tôi dùng bàn phím hoặc screen reader, **When** thao tác với hai action mới, **Then** link có accessible name rõ nghĩa, focus visible, và không phụ thuộc màu để truyền đạt trạng thái.
9. **Given** tôi mở Home trên mobile, tablet, hoặc desktop, **When** grid "Thao tác nhanh" render, **Then** các label mới không tràn khỏi tile, touch target vẫn đủ lớn, spacing giữ cùng visual language dashboard hiện có.

## Tasks / Subtasks

- [x] Task 1 — Web: Thay quick action disabled bằng action tự phục vụ (AC: #1, #2, #5, #9)
  - [x] Trong `apps/web/src/app/(dashboard)/home/page.tsx`, đổi tile "Đặt lịch khám" thành "Nhắc lịch tái khám".
  - [x] Đổi tile "Liên hệ bác sĩ" thành "Tóm tắt đi khám".
  - [x] Dùng icon `Bell`, `CalendarClock`, `ClipboardList`, hoặc `FileText` từ `lucide-react`; không thêm icon library mới.
  - [x] Đảm bảo hai tile mới là `Link` enabled: `/follow-up-reminders` và `/visit-summary`, không phải link rỗng hoặc button disabled.
- [x] Task 2 — Web: Kết nối đến trang "Nhắc lịch tái khám" (AC: #2, #3, #4, #8, #9)
  - [x] Tile "Nhắc lịch tái khám" dùng `href="/follow-up-reminders"`.
  - [x] Nếu cần truyền hồ sơ hiện tại, dùng query `profileId=${primaryProfileId}` theo pattern upload hiện có.
  - [x] Không implement form nhắc lịch trong Home; nội dung trang riêng thuộc Story 10.3.
- [x] Task 3 — Web: Kết nối đến trang "Tóm tắt đi khám" (AC: #5, #6, #7, #8, #9)
  - [x] Tile "Tóm tắt đi khám" dùng `href="/visit-summary"`.
  - [x] Nếu cần truyền hồ sơ hiện tại, dùng query `profileId=${primaryProfileId}` theo pattern upload hiện có.
  - [x] Không implement bản tóm tắt trong Home; nội dung trang riêng thuộc Story 10.4.
- [x] Task 4 — UX/accessibility hardening (AC: #8, #9)
  - [x] Link có accessible name rõ, focus state không bị mất.
  - [x] Nội dung y tế tránh từ ngữ chẩn đoán, kê toa, hoặc đảm bảo kết quả.
  - [x] Giữ thiết kế calm healthcare hiện có: teal primary, white cards, rounded corners vừa phải, shadow nhẹ.
  - [x] Không tạo card lồng card phức tạp trong tile grid.
- [x] Task 5 — Tests and validation (AC: #1-#9)
  - [x] Cập nhật `apps/web/src/app/(dashboard)/home/page.test.tsx` để assert không còn "Đặt lịch khám" và "Liên hệ bác sĩ".
  - [x] Test hai label mới "Nhắc lịch tái khám" và "Tóm tắt đi khám" render dưới dạng link enabled.
  - [x] Test href lần lượt trỏ đến `/follow-up-reminders` và `/visit-summary`.
  - [x] Chạy `pnpm --filter web test` hoặc tối thiểu targeted Vitest cho Home nếu full suite quá rộng.

## Dev Notes

### Current state

- `apps/web/src/app/(dashboard)/home/page.tsx` đang render nhóm "Thao tác nhanh" gồm 6 `ActionTile`.
- Hai tile chưa khả dụng hiện tại:

```tsx
<ActionTile
  icon={<CalendarDays className="h-6 w-6" />}
  label="Đặt lịch khám"
  disabled
/>

<ActionTile
  icon={<HelpCircle className="h-6 w-6" />}
  label="Liên hệ bác sĩ"
  disabled
/>
```

- `ActionTile` đã hỗ trợ `Link` khi truyền `href`. Với story này nên dùng link vì hai chức năng có trang riêng.
- Home đã có `profiles`, `primaryProfileId`, `recentRecords`, `historyHref`, và upload href: `/health-records?profileId=${primaryProfileId ?? ""}&openUpload=1`.

### Implementation guidance

- Story này là **trust repair** cho Home: không để quick action chính là dead-end.
- Không implement đặt lịch thật, calendar provider, hospital booking, doctor chat, telemedicine, hoặc backend appointment API.
- Không tạo dữ liệu bác sĩ/bệnh viện giả.
- "Nhắc lịch tái khám" nên trỏ đến trang riêng `/follow-up-reminders`; chi tiết trang thuộc Story 10.3.
- "Tóm tắt đi khám" nên trỏ đến trang riêng `/visit-summary`; chi tiết trang thuộc Story 10.4.
- Giữ nội dung tiếng Việt đơn giản, thân thiện, không gây lo lắng.
- Có thể dùng `window.localStorage` bên trong handler/effect client-side vì Home đang là `"use client"`.
- Nếu thêm component con, ưu tiên colocate trong `home/page.tsx` hoặc `apps/web/src/components/features/home/` chỉ khi giúp giữ Home dễ đọc.

### Suggested UI copy

- Tile label: `Nhắc lịch tái khám`
- Route: `/follow-up-reminders`
- Tile label: `Tóm tắt đi khám`
- Route: `/visit-summary`

### Previous story intelligence

- Story 10.1 đã thêm `/help` và `/questions`, nối Home "Trợ giúp" đến `/help`, và header `HelpCircle` đến `/questions`.
- 10.1 dùng static-content-first, không thêm backend/API. Story 10.2 nên giữ cùng nguyên tắc.
- Tests hiện có cho Home đang mock `@tanstack/react-query` và `apiClient`; mở rộng test này thay vì dựng setup mới.
- Review 10.1 đã phát hiện một CTA trỏ sai route `/settings`; với story này phải kiểm tra mọi href mới trỏ đến route thật hoặc dùng button không href.

### Project Structure Notes

- Frontend app router nằm tại `apps/web/src/app/(dashboard)/`.
- Test colocated theo route, ví dụ `apps/web/src/app/(dashboard)/home/page.test.tsx`.
- Dùng `lucide-react` đã có trong project.
- Không thêm dependency mới cho thay đổi điều hướng đơn giản này.

### References

- [Source: _bmad-output/planning-artifacts/epics.md#Epic-10-Trợ-Giúp-và-Thắc-Mắc-Người-Dùng]
- [Source: _bmad-output/planning-artifacts/architecture.md#Cấu-Trúc-Frontend-Next.js-Web]
- [Source: _bmad-output/planning-artifacts/architecture.md#Quy-Ước-Vị-Trí-Test]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md#Layout-Foundation]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md#Iconography]
- [Source: _bmad-output/planning-artifacts/ux-design-specification.md#Journey-Patterns]
- [Previous: _bmad-output/implementation-artifacts/epic-10/10-1-help-and-questions-pages.md]
- [Current: apps/web/src/app/(dashboard)/home/page.tsx]
- [Current: apps/web/src/app/(dashboard)/home/page.test.tsx]

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `pnpm --filter web exec vitest run 'src/app/(dashboard)/home/page.test.tsx'` — RED failed as expected before implementation because "Đặt lịch khám" still rendered.
- `pnpm --filter web exec vitest run 'src/app/(dashboard)/home/page.test.tsx'` — GREEN passed after replacing the quick actions.
- `pnpm --filter web test` — passed lint and full web Vitest suite.

### Completion Notes List

- Replaced disabled Home quick actions with enabled self-service links: "Nhắc lịch tái khám" -> `/follow-up-reminders` and "Tóm tắt đi khám" -> `/visit-summary`.
- Used existing `ActionTile` link path with `Bell` and `FileText` icons from `lucide-react`; no dependencies or backend changes added.
- Added focus-visible styling and tighter label wrapping on action tiles to support keyboard users and responsive labels.
- Expanded Home tests to assert old unavailable labels are absent and new actions render as enabled links with correct hrefs.

### File List

- `apps/web/src/app/(dashboard)/home/page.tsx`
- `apps/web/src/app/(dashboard)/home/page.test.tsx`
- `_bmad-output/implementation-artifacts/epic-10/10-2-home-quick-actions-self-service.md`
- `_bmad-output/implementation-artifacts/sprint-status.yaml`

### Change Log

- 2026-05-16: Tạo story 10.2 để thay hai quick action disabled trên Home bằng "Nhắc lịch tái khám" và "Tóm tắt đi khám" trong phạm vi web MVP, không cần tích hợp bệnh viện/bác sĩ.
- 2026-05-16: Implemented Home self-service quick actions, added route assertions, and moved story to review.
