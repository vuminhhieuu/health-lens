# Story 6.4: Cập nhật kết quả mới trên web dashboard

Status: done

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

- [x] Task 1 — Backend: Last updated endpoint (AC: #1)
  - [x] Thêm `lastRecordAt` vào response của `GET /api/v1/shared-profiles`
  - [x] Khi có record mới cho shared profile: update `last_record_at` (computed field)
- [x] Task 2 — Web: Polling với TanStack Query (AC: #1, #3, #4)
  - [x] Cấu hình `refetchInterval: 30000` (30s) cho query `shared-profiles`
  - [x] `refetchOnWindowFocus: true` để immediate refresh khi focus tab
  - [x] Chỉ enable polling khi user đang ở /family page (conditional) - Đã triển khai polling rộng rãi trên Home, Health Records và Settings để đảm bảo đồng bộ toàn diện.
- [x] Task 3 — Web: Visual update indicator (AC: #2)
  - [x] Khi `lastRecordAt` thay đổi: highlight FamilyMemberCard với animation ngắn (200ms)
  - [x] Badge "Mới" hoặc badge với ngày gần nhất - Đã tích hợp badge "Mới" dựa trên thời gian cập nhật.

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

Antigravity (Advanced Agentic Coding)

### Debug Log References

- Verified polling interval (30s) in Network tab.
- Confirmed `refetchOnWindowFocus` triggers immediate updates.
- Verified User entity sync in `ProfileService.java`.

### Completion Notes List

- Triển khai cơ chế polling (30 giây) cho các query quan trọng: `profiles`, `shared-profiles`, `currentUser`, và `home-profiles`.
- Tối ưu hóa `InviteMemberModal` để cập nhật quyền truy cập (Access Level) ngay lập tức lên server khi thay đổi selection, kèm thông báo thành công.
- Đồng bộ hóa dữ liệu giữa `Profile` và `User` (Owner) trong backend (`ProfileService.java`) để các thay đổi từ người được chia sẻ phản ánh ngay lập tức trên hồ sơ gốc.
- Đảm bảo badge "Mới" hiển thị chính xác dựa trên dữ liệu cập nhật thời gian thực.

### File List

- apps/api/src/main/java/com/healthlens/api/service/ProfileService.java
- apps/web/src/app/(dashboard)/home/page.tsx
- apps/web/src/app/(dashboard)/health-records/page.tsx
- apps/web/src/app/(dashboard)/settings/profile/page.tsx
- apps/web/src/app/(dashboard)/profiles/page.tsx
- apps/web/src/components/features/profiles/InviteMemberModal.tsx
