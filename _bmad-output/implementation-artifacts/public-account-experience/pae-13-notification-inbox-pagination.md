# Story 13: Notification Inbox Pagination

Status: ready-for-dev

<!-- Note: Validation is optional. Run validate-create-story for quality check before dev-story. -->

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P2 | **Depends on:** `pae-10`, `pae-11`, `pae-12`

## Story

Là người dùng đã đăng nhập,  
tôi muốn duyệt thông báo theo từng trang trên màn hình **Cài đặt → Thông báo**,  
để danh sách dài (lời mời, nhắc tái khám, mục đã đọc lưu snapshot) không bị tràn một khối và vẫn dùng được trên mobile.

## Acceptance Criteria

1. **Given** người dùng mở `/settings/notifications`, **When** có nhiều hơn `limit` mục inbox, **Then** chỉ hiển thị một trang; có điều khiển phân trang tiếng Việt (Trang đầu / Trước / Sau / Trang cuối) và dòng tóm tắt dạng «Đang hiển thị X trên Y thông báo» — pattern giống `AuditLogTableSection`.
2. **Given** API inbox, **When** gọi `GET /api/v1/notifications/inbox?page=&limit=`, **Then** response theo envelope chuẩn dự án: `data` (mảng trang hiện tại), `pagination` (`page`, `limit`, `total`, `totalPages`), `meta` (giữ `timestamp`, `requestId`); `page` mặc định `0`, `limit` mặc định `10`, `limit` tối đa `50`.
3. **Given** bất kỳ trang nào, **When** response inbox trả về, **Then** `meta.unreadCount` là tổng số mục `read: false` trên **toàn bộ** inbox (trước khi cắt trang) — dùng cho badge chuông và toolbar «Đánh dấu tất cả đã đọc».
4. **Given** chuông header + dropdown, **When** mở dropdown, **Then** vẫn chỉ preview tối đa 5 mục mới nhất (`page=0`, `limit=5`); link «Xem tất cả» → `/settings/notifications`; **không** thêm phân trang trong dropdown.
5. **Given** `POST .../inbox/read` và `.../read-all`, **When** thao tác thành công, **Then** hành vi giữ nguyên (optimistic UI + invalidate/refetch); sau mark-read, trang hiện tại và `unreadCount` cập nhật đúng.
6. **Given** `page` vượt `totalPages`, **When** request inbox, **Then** trả `data: []` và `pagination` nhất quán (không 500).
7. **Given** backend tests, **When** chạy `./gradlew test`, **Then** có test controller/service cho phân trang, `unreadCount`, và giới hạn `limit`; không leak dữ liệu user khác.

## Tasks / Subtasks

- [ ] **API** — `NotificationController.listInbox`: thêm `@RequestParam page`, `limit`; map `PaginationResponse`; thêm `meta.unreadCount`.
- [ ] **Service** — `NotificationInboxService.listInbox(userId, page, limit)`: merge + sort như hiện tại, tính `total` / `unreadCount` trước slice; giữ `MAX_ITEMS` aggregate (50) là trần an toàn — phân trang áp dụng trên danh sách đã merge (tối đa 50 mục, tức tối đa 5 trang với `limit=10`).
- [ ] **Shared** — (tuỳ chọn) type `PaginationMeta` / mở rộng response type nếu pattern shared đã có; đồng bộ `ApiPaths` nếu thêm query documented.
- [ ] **Web hook** — Tách hoặc mở rộng `useNotificationInbox`:
  - Mode **summary** (bell): `page=0`, `limit=5`, dùng `meta.unreadCount`.
  - Mode **paged** (settings): state `page` + `limit=10`, query key gồm page.
- [ ] **UI** — `NotificationInboxList` variant `full`: nhận `pagination`, `onPageChange`, render footer phân trang (reuse UX audit log: nút disabled, `aria-label` tiếng Việt).
- [ ] **UI** — `/settings/notifications/page.tsx`: wire phân trang; giữ email preferences section không đổi.
- [ ] **Tests** — Cập nhật `NotificationControllerTest`, `NotificationInboxServiceTest` (pagination bounds, unreadCount, empty page).

## Dev Notes

### Bối cảnh hiện tại (đừng làm lại)

- Inbox aggregate: `NotificationInboxService` gộp profile invite + health-record invite + reminder 90 ngày + archived read snapshots; sort `createdAt` desc; **`MAX_ITEMS = 50`** sau merge ([`NotificationInboxService.java`](../../../apps/api/src/main/java/com/healthlens/api/service/NotificationInboxService.java)).
- API hiện trả **toàn bộ** mảng trong `data`, không có `pagination` ([`NotificationController.java`](../../../apps/api/src/main/java/com/healthlens/api/controller/NotificationController.java)).
- Web: `useNotificationInbox` fetch một lần, `unreadCount = items.filter(!read).length` ([`useNotificationInbox.ts`](../../../apps/web/src/hooks/useNotificationInbox.ts)).
- Dropdown: `items.slice(0, 5)` client-side ([`NotificationDropdown.tsx`](../../../apps/web/src/components/features/notifications/NotificationDropdown.tsx)).
- Trang settings: `NotificationInboxList` variant `full` render hết `items` ([`settings/notifications/page.tsx`](../../../apps/web/src/app/(dashboard)/settings/notifications/page.tsx)).

### Quyết định kỹ thuật (bắt buộc)

| Chủ đề | Quyết định |
|--------|------------|
| Trang áp dụng phân trang | Chỉ **settings full list**; dropdown giữ preview 5 |
| `unreadCount` | **Server-side** trong `meta` — client không đếm từ một trang |
| `markAllAsRead` | Vẫn đánh dấu **tất cả** mục active trong inbox, không chỉ trang hiện tại |
| Envelope API | Khớp [`architecture.md`](../../planning-artifacts/architecture.md) — `pagination` + `meta` |
| Page index | **0-based** (`page=0` trang đầu), giống `ProfileController.getProfileHealthRecords` |
| Default `limit` | `10` (settings); dropdown gọi `limit=5` |
| Polling 30s | Giữ cho bell summary query; paged settings query refetch khi đổi trang + sau mutation |

### Mẫu code tham chiếu

- Pagination DTO: [`PaginationResponse.java`](../../../apps/api/src/main/java/com/healthlens/api/dto/response/PaginationResponse.java)
- Controller query params: [`ProfileController.java`](../../../apps/api/src/main/java/com/healthlens/api/controller/ProfileController.java) (`page`, `limit` defaults)
- UI phân trang: [`AuditLogTableSection.tsx`](../../../apps/web/src/components/admin/audit-log/AuditLogTableSection.tsx) (dòng ~200–250)
- Story trước: [`pae-11-notification-center-ui-and-bell-wiring.md`](./pae-11-notification-center-ui-and-bell-wiring.md), [`pae-10-notification-inbox-aggregate-api.md`](./pae-10-notification-inbox-aggregate-api.md)

### Không nằm trong scope

- Nâng `MAX_ITEMS` > 50 hoặc inbox persistent DB feed (push/FCM) — Phase 2
- Phân trang email preferences (`pae-12`)
- Infinite scroll thay cho nút trang
- Vitest frontend mới (project chưa bắt buộc cho story notification trước đây) — smoke thủ công theo AC

### Rủi ro / edge case

- User có đúng 50 mục: trang cuối đầy đủ; badge = 0 khi tất cả đã đọc.
- Đổi trang khi đang `isMarkingAllRead`: disable nút trang hoặc chờ mutation xong.
- Refetch sau mark-read: giữ `page` hiện tại trừ khi trang trống → lùi về `page - 1` tối thiểu 0.
- Breaking change: client cũ expect `data` là full list — **cập nhật đồng bộ** web + shared consumers trong cùng PR.

### Project Structure Notes

- API logic trong `NotificationInboxService`, không controller fat.
- Route constants: `ApiRoutes.java` + `packages/shared/constants/api.ts` (query params không cần path mới).
- Tuân [`docs/project-context.md`](../../../docs/project-context.md): đồng bộ route, Flyway không đổi schema cho story này.

### Testing Requirements

- `./gradlew test` — `NotificationInboxServiceTest`, `NotificationControllerTest`, integration nếu có fixture inbox.
- Smoke thủ công: ≥11 thông báo (hoặc seed test), chuyển trang settings, badge chuông khớp `meta.unreadCount`, dropdown 5 mục, mark one / mark all, reload.

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Paginated Response]
- [Source: _bmad-output/implementation-artifacts/public-account-experience/pae-10-notification-inbox-aggregate-api.md]
- [Source: _bmad-output/implementation-artifacts/public-account-experience/pae-11-notification-center-ui-and-bell-wiring.md]
- [Source: docs/project-context.md]

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List

### Change Log

- 2026-05-25: Story created — pagination cho trang Cài đặt → Thông báo (PAE-13).
