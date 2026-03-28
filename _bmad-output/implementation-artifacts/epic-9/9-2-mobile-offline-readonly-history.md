# Story 9.2: Offline read-only cho lịch sử trên mobile

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

As a người dùng mobile,
I want xem dữ liệu lịch sử khi không có mạng,
so that tôi vẫn tra cứu thông tin đã đồng bộ trước đó.

## Acceptance Criteria

1. **Given** app đã cache dữ liệu trên thiết bị, **When** thiết bị mất kết nối mạng, **Then** timeline và chi tiết vẫn xem được ở chế độ read-only.
2. **Given** chế độ offline, **When** user xem history, **Then** UI hiển thị banner "Đang offline - Dữ liệu từ {lastSyncTime}".
3. **Given** chế độ offline, **When** user thử upload hoặc xóa record, **Then** action bị disable với tooltip "Cần kết nối mạng".
4. **Given** offline cache, **When** kiểm tra, **Then** profiles và health records (metrics, không có files gốc) được lưu trong Expo SQLite.

## Tasks / Subtasks

- [ ] Task 1 — Mobile (Phase 2): SQLite schema setup (AC: #4)
  - [ ] Tạo `apps/mobile/lib/database/schema.ts` với Expo SQLite
  - [ ] Bảng `cached_profiles(id, userId, displayName, notes, gender, dateOfBirth, syncedAt)`
  - [ ] Bảng `cached_health_records(id, profileId, examDate, overallStatus, metrics_json, createdAt, syncedAt)`
  - [ ] Helper functions: `upsertProfile()`, `upsertHealthRecord()`, `getProfileRecords()`
- [ ] Task 2 — Mobile (Phase 2): Network status detection (AC: #2, #3)
  - [ ] Dùng `@react-native-community/netinfo` hoặc `expo-network`
  - [ ] Zustand store: `useNetworkStore` với `isOnline: boolean`, `lastSyncAt: Date`
  - [ ] Banner component `OfflineBanner` — sticky ở top khi offline
- [ ] Task 3 — Mobile (Phase 2): Offline-aware UI (AC: #1, #3)
  - [ ] Khi offline: render data từ SQLite cache thay vì API
  - [ ] Disable: Upload button, Delete button, Edit button
  - [ ] Tooltip khi tap disabled action: "Cần kết nối mạng để thực hiện"
- [ ] Task 4 — Mobile (Phase 2): Cache on sync (AC: #4)
  - [ ] Sau mỗi successful API call cho profiles/records: upsert vào SQLite
  - [ ] Using `useMutation` onSuccess callback

## Dev Notes

### Expo SQLite (SDK 55)

```typescript
import * as SQLite from 'expo-sqlite';

const db = SQLite.openDatabaseSync('healthlens.db');

// Create tables
db.execSync(`
  CREATE TABLE IF NOT EXISTS cached_health_records (
    id TEXT PRIMARY KEY,
    profile_id TEXT NOT NULL,
    exam_date TEXT,
    overall_status TEXT,
    metrics_json TEXT NOT NULL,
    created_at TEXT,
    synced_at TEXT NOT NULL
  );
`);

// Upsert record
const upsertRecord = (record) => {
  db.runSync(
    `INSERT OR REPLACE INTO cached_health_records VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [record.id, record.profileId, record.examDate, record.overallStatus,
     JSON.stringify(record.metrics), record.createdAt, new Date().toISOString()]
  );
};
```

### Network Detection

```typescript
import NetInfo from '@react-native-community/netinfo';

NetInfo.addEventListener(state => {
  useNetworkStore.setState({ isOnline: state.isConnected });
});
```

### Conflict Resolution

- Offline chỉ read-only → không có conflicts để resolve
- Khi online trở lại: fetch từ server (Story 5.5) overwrite local cache

### References

- [Source: architecture.md#Chiến-Lược-Offline-Mobile]
- [Source: epics.md#Story-5.4]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
