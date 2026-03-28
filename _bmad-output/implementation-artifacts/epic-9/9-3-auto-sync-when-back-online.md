# Story 9.3: Tự động đồng bộ khi kết nối được khôi phục

Status: backlog

## Execution scope

**Phase 2 — Mobile:** Chỉ triển khai **sau** khi Web MVP (Epic 1–8) hoàn thành. Phụ thuộc API và luồng web/upload ổn định từ Phase 1.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

**Lưu ý:** Epic 9 (mobile) chưa có screen trong project Stitch Web MVP; dùng cùng luồng auth/upload ở web như tham chiếu UX hoặc bổ sung project Stitch riêng sau.

Không có screen Stitch tương ứng trong project Web MVP này (story kỹ thuật / mobile phase 2).

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want dữ liệu tự sync lại khi có mạng,
so that lịch sử luôn cập nhật mà không cần thao tác thủ công.

## Acceptance Criteria

1. **Given** app đang offline rồi chuyển online, **When** mạng ổn định trở lại, **Then** app tự trigger đồng bộ dữ liệu nền.
2. **Given** đang sync, **When** theo dõi status, **Then** UI hiển thị indicator "Đang đồng bộ..." (subtle, không blocking).
3. **Given** sync thành công, **When** hoàn tất, **Then** UI cập nhật `lastSyncAt` và OfflineBanner biến mất.
4. **Given** sync thất bại (network unstable), **When** lỗi xảy ra, **Then** thử lại tối đa 3 lần với exponential backoff, hiển thị "Đồng bộ thất bại. Thử lại sau".

## Tasks / Subtasks

- [ ] Task 1 — Mobile (Phase 2): Auto-sync trigger (AC: #1, #2)
  - [ ] Listen `NetInfo` event `isConnected: false → true`
  - [ ] Trigger `syncData()` function khi online
  - [ ] Show subtle sync indicator (ActivityIndicator nhỏ trên OfflineBanner)
- [ ] Task 2 — Mobile (Phase 2): Sync function (AC: #1, #3, #4)
  - [ ] `syncData()`: fetch `/api/v1/profiles`, fetch records mỗi profile → upsert vào SQLite
  - [ ] Retry logic: 3 lần, backoff 1s/2s/4s
  - [ ] Update `lastSyncAt` trong netStore sau khi thành công
- [ ] Task 3 — Mobile (Phase 2): Sync status UI (AC: #2, #3, #4)
  - [ ] OfflineBanner component cập nhật state: offline → syncing → online
  - [ ] Sau khi online và sync: banner ẩn sau 2 giây
  - [ ] Error state: banner đổi thành "Đồng bộ thất bại. [Thử lại]"

## Dev Notes

### Auto-Sync Flow

```typescript
useEffect(() => {
  const unsubscribe = NetInfo.addEventListener(state => {
    const wasOffline = !useNetworkStore.getState().isOnline;
    const isNowOnline = state.isConnected;
    if (wasOffline && isNowOnline) {
      syncData();  // trigger background sync
    }
    useNetworkStore.setState({ isOnline: isNowOnline });
  });
  return () => unsubscribe();
}, []);
```

### Retry with Backoff

```typescript
const syncWithRetry = async (retries = 3, delay = 1000) => {
  try {
    await syncData();
  } catch (e) {
    if (retries > 0) {
      await new Promise(r => setTimeout(r, delay));
      return syncWithRetry(retries - 1, delay * 2);
    }
    throw e;
  }
};
```

### References

- [Source: architecture.md#Chiến-Lược-Offline-Mobile]
- [Source: epics.md#Story-5.5]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
