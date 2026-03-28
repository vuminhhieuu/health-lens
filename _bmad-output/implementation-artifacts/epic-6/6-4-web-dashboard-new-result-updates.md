# Story 6.4: Cập nhật kết quả mới trên web dashboard

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Dashboard Home Page - HealthLens | `projects/578519912546445367/screens/010095343e6c46b4969b42c4ab93165a` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzBkNTk5MTY2NzIxNzQ0NDk5YjJlMjQxMWM1N2U3ZGRkEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ujIG2rNbWjcWsO9G1za9-KcM9gHUwj2X237xGhShjEjCWwi29NS6udE0Flhdtdop0NdfoD_Q2MWeNXsYPv9ycxE1WW7o8Q-d1R0yleCrW7kDtJKwmG3PSp2i5ElEnFX2PBVJRnaMV1LyvBv2tceKjwgi32axKgQD_qspcqMvh2sOPytIhDH-xmzLB10vKA25LIp5_lTgyqaRkKrSQ3LXCMp6nLt8BGuRGlAxnkrlkV-A5GUZWmIVjBIVg) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a thành viên gia đình,
I want nhận cập nhật khi có kết quả mới,
so that tôi nắm bắt thay đổi sức khỏe nhanh chóng.

## Acceptance Criteria

1. **Given** hồ sơ chia sẻ có bản ghi mới, **When** người nhận mở web dashboard, **Then** hệ thống hiển thị cập nhật trong ≤30 giây theo cơ chế polling.
2. **Given** family dashboard, **When** có record mới, **Then** FamilyMemberCard cập nhật badge trạng thái mới nhất.
3. **Given** polling interval, **When** kiểm tra network traffic, **Then** request mỗi 30 giây (không liên tục gọi).
4. **Given** tab web không active (background), **When** quay lại, **Then** refetch ngay lập tức để có data mới nhất.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Last updated endpoint (AC: #1)
  - [ ] Thêm `lastRecordAt` vào response của `GET /api/v1/shared-profiles`
  - [ ] Khi có record mới cho shared profile: update `last_record_at` (computed field)
- [ ] Task 2 — Web: Polling với TanStack Query (AC: #1, #3, #4)
  - [ ] Cấu hình `refetchInterval: 30000` (30s) cho query `shared-profiles`
  - [ ] `refetchOnWindowFocus: true` để immediate refresh khi focus tab
  - [ ] Chỉ enable polling khi user đang ở /family page (conditional)
- [ ] Task 3 — Web: Visual update indicator (AC: #2)
  - [ ] Khi `lastRecordAt` thay đổi: highlight FamilyMemberCard với animation ngắn (200ms)
  - [ ] Badge "Mới" hoặc badge với ngày gần nhất

## Dev Notes

### TanStack Query Polling

```typescript
const { data } = useQuery({
  queryKey: ['shared-profiles'],
  queryFn: fetchSharedProfiles,
  refetchInterval: 30_000,         // Poll mỗi 30s (NFR-P4)
  refetchOnWindowFocus: true,      // Immediate khi user quay lại tab
  enabled: isOnFamilyPage,         // Chỉ poll khi ở /family route
});
```

### NFR-P4 Compliance

> Web dashboard cập nhật kết quả mới trong ≤30 giây (polling) kể từ khi upload thành công.

Polling 30s đảm bảo đáp ứng yêu cầu này. WebSocket có thể thay thế sau trong P2.

### References

- [Source: epics.md#Story-6.4]
- [Source: architecture.md#Quản-Lý-State]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
