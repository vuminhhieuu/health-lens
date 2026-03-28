# Story 6.2: Người được mời xem dữ liệu hồ sơ chia sẻ trên web

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Chi tiết Sức khỏe - Lê Thị Hồng | `projects/578519912546445367/screens/32cf2c3670374324b90b464766cf8872` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzE0Nzc3OTEyZGVmYjQzNzJhZjYwMGRmZTgyNDhlMzYyEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhMwyiZT_MRHSlYbtyf85AIk1RgbCKapJ8e7T9zWlBXlnM7ZKtZfzm9Fn8Luw5_b_-szpmhDdjCSq1hZnkeDp8GUgLEBl0TD1qu3iDvwSo8HaF9SkykEArdOHAktsHhyk38ugKIIoDu9E_BUVIiZVqeo_gRhR7xCc-Yx6yzGPSZydtM3EfWJnTrZ1gxkL2TQPuzhJRM7N4NMf6ZDzg7mTe-NEx-4AV9-L0oVj6YfAougf5GLQuCU2hK0Q) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a thành viên gia đình được mời,
I want xem lịch sử và kết quả hồ sơ được chia sẻ,
so that tôi theo dõi sức khỏe người thân kịp thời.

## Acceptance Criteria

1. **Given** lời mời đã được chấp nhận, **When** người nhận đăng nhập web dashboard, **Then** họ xem được hồ sơ và health records được granted.
2. **Given** viewer truy cập API profile không được share, **When** backend check, **Then** 403 Forbidden.
3. **Given** family dashboard web, **When** hiển thị, **Then** glanceable cards theo UX-DR6 với màu status và tên profile.
4. **Given** viewer xem record detail, **When** hiển thị, **Then** đầy đủ thông tin nhưng không có action xóa/edit.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Shared profiles endpoint (AC: #1, #2)
  - [ ] `GET /api/v1/shared-profiles` → danh sách profiles được share đến current user
  - [ ] RBAC check: trả về profiles từ `profile_shares` WHERE `viewer_id = userId AND revoked_at IS NULL`
  - [ ] Tất cả health record endpoints: check ownership OR share access
- [ ] Task 2 — Web: Family dashboard (AC: #1, #3, #4)
  - [ ] Tạo `apps/web/src/app/(dashboard)/family/page.tsx`
  - [ ] Grid layout FamilyMemberCard (UX-DR6): profile name, latest status, date
  - [ ] Click card → history của profile đó (read-only mode)
  - [ ] Thêm route group `/family/` với shared profiles context
- [ ] Task 3 — Tests (AC: #1, #2)
  - [ ] `ProfileShareServiceTest`: viewer can access, non-viewer blocked, revoked share blocked

## Dev Notes

### FamilyMemberCard (UX-DR6)

```typescript
interface FamilyMemberCardProps {
  profile: { displayName: string; overallStatus: string; lastUpdated: string; };
  onClick: () => void;
}
// Color-coded status: border-color theo health status color
// 1-click depth: click → profile records
```

### API — isOwner Flag

Tất cả record endpoints trả về `isOwner: boolean`:
```json
{ "data": { "id": "...", "isOwner": false, "metrics": [...] } }
```
Frontend dùng flag này để ẩn/hiện action buttons.

### References

- [Source: ux-design-specification.md#UX-DR6]
- [Source: architecture.md#Mẫu-Phân-Quyền]
- [Source: epics.md#Story-6.2]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
