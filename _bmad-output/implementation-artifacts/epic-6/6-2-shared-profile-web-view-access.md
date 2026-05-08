# Story 6.2: Người được mời xem dữ liệu hồ sơ chia sẻ trên web

Status: done

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

- [x] Task 1 — Backend: Shared profiles endpoint (AC: #1, #2)
  - [x] `GET /api/v1/shared-profiles` → danh sách profiles được share đến current user
  - [x] RBAC check: trả về profiles từ `profile_shares` WHERE `viewer_id = userId AND revoked_at IS NULL`
  - [x] Tất cả health record endpoints đọc dữ liệu: check ownership OR share access
- [x] Task 2 — Web: Family dashboard (AC: #1, #3, #4)
  - [x] Tạo `apps/web/src/app/(dashboard)/family/page.tsx`
  - [x] Grid layout FamilyMemberCard (UX-DR6): profile name, latest status, date
  - [x] Click card → history của profile đó (read-only mode)
  - [x] Thêm route group `/family/` với shared profiles context
- [x] Task 3 — Tests (AC: #1, #2)
  - [x] Cập nhật `HealthRecordServiceTest` để đảm bảo luồng owner/shared không bị regression

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

Codex 5.3

### Debug Log References

- `pnpm --filter web lint`
- `./gradlew.bat test --tests com.healthlens.api.service.HealthRecordServiceTest`

### Completion Notes List

- Thêm endpoint `GET /api/v1/shared-profiles` để trả về hồ sơ được chia sẻ cho người dùng hiện tại.
- Mở quyền đọc health record cho viewer được share (status/detail/metrics explanation/recommendations/list-by-profile), đồng thời vẫn giữ các action ghi/xóa chỉ cho owner.
- Bổ sung `isOwner` trong payload status/detail để frontend ẩn hiện action đúng vai trò.
- Tạo trang dashboard `/family` với cards màu theo health status và điều hướng 1-click sang lịch sử hồ sơ được chia sẻ.

### File List

- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/controller/SharedProfileController.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/HealthRecordDetailResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/HealthRecordStatusResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/SharedProfileResponse.java`
- `apps/api/src/main/java/com/healthlens/api/repository/HealthRecordRepository.java`
- `apps/api/src/main/java/com/healthlens/api/repository/ProfileShareRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/ProfileService.java`
- `apps/web/src/app/(dashboard)/family/page.tsx`
- `apps/web/src/app/(dashboard)/layout.tsx`
- `packages/shared/constants/api.ts`
