# Story 2.2: Tạo hồ sơ sức khỏe mới trong tài khoản

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Hồ sơ của tôi - HealthLens | `projects/578519912546445367/screens/68716ba836ae433e82006a112d724e12` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2FkMzBlOTA1YWFjZjRjMjU4NzdiODY3YTY0ZmIyZTJmEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhK4JDXrU2o63v7yUUyQN0nSYvKbp93o62VlefncqjtFgIDOnq6pKrVGa_CEcYrwPKfpd7NFBLwiJKIrIoP-oKMc7lVZZLfzR-pWzhiA8ANGkw4ZEiZm4-xRgywCR_ZY6dtWB00Bbi1voBtbEUqGUZVatREMA2mCNau0y_zfISWZ6ovajXGSf78KkFnGEmMmof9UJzsyiDxVVdGlf3Ezo3_hu2h_9j6JwujqCiNEJvTRpQmAFwFkPoU) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want tạo nhiều hồ sơ sức khỏe,
so that tôi quản lý dữ liệu theo từng người.

## Acceptance Criteria

1. **Given** người dùng ở màn hình Profiles, **When** chọn "Tạo hồ sơ" và nhập displayName (bắt buộc), **Then** hệ thống tạo hồ sơ mới gắn đúng ownership của tài khoản.
2. **Given** hồ sơ được tạo, **When** kiểm tra danh sách, **Then** hồ sơ mới xuất hiện ngay trong profile cards.
3. **Given** một tài khoản, **When** đã có 10 hồ sơ, **Then** nút "Tạo hồ sơ" bị vô hiệu hóa và hiển thị thông báo giới hạn.
4. **Given** displayName trống, **When** submit, **Then** validation error.
5. **Given** hồ sơ được tạo, **When** kiểm tra ownership, **Then** profile.userId phải khớp với user đang đăng nhập — không thể tạo profile cho user khác.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Profile entity + migration (AC: #1, #5)
  - [ ] Tạo entity `Profile.java`: id (UUID), userId (FK → users), displayName, notes, dateOfBirth, gender, createdAt, updatedAt
  - [ ] Flyway migration `V009__create_profiles_table.sql`
  - [ ] `POST /api/v1/profiles` — tạo profile mới, gán userId từ JWT context (không từ request body)
  - [ ] `GET /api/v1/profiles` — trả về danh sách profiles của user hiện tại
  - [ ] Validate max 10 profiles per user (AC: #3)
- [ ] Task 2 — Web: Profiles page + create flow (AC: #1, #2, #3)
  - [ ] Tạo `apps/web/src/app/(dashboard)/profiles/page.tsx`
  - [ ] Grid layout ProfileCards (UX-DR2)
  - [ ] Button "Tạo hồ sơ mới" → Dialog/modal tạo profile
  - [ ] Form: displayName (required, max 50 chars), dateOfBirth (optional), gender (optional)
  - [ ] Sau tạo: invalidate query `['profiles']`, hiển thị card mới ngay (optimistic update)
- [ ] Task 3 — Mobile (Phase 2): Profiles tab (AC: #1, #2)
  - [ ] Tạo `apps/mobile/app/(tabs)/profiles.tsx`
  - [ ] ProfileCard component cho mỗi hồ sơ
  - [ ] FAB "+" để tạo profile mới (theo UX-DR7 — Bottom tabs + FAB)
- [ ] Task 4 — Tests (AC: #1, #3, #5)
  - [ ] `ProfileServiceTest`: tạo profile, vượt limit, ownership check

## Dev Notes

### Database Schema

```sql
-- V009__create_profiles_table.sql
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100) NOT NULL,
    notes TEXT,
    date_of_birth DATE,
    gender VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_profiles_user_id ON profiles(user_id);
CONSTRAINT profiles_fk_user_id FOREIGN KEY (user_id) REFERENCES users(id)
```

### API

```
POST /api/v1/profiles
Body: { "displayName": "Ba", "dateOfBirth": "1960-05-01", "gender": "male" }
Response 201: { "data": { "id": "uuid", "displayName": "Ba", ... } }
Response 409: { "title": "Đã đạt giới hạn 10 hồ sơ" }

GET /api/v1/profiles
Response 200: { "data": [...profiles], "meta": {...} }
```

### ProfileCard Component (UX-DR2)

```typescript
interface ProfileCardProps {
  profile: { id: string; displayName: string; notes?: string; latestStatus?: string; lastUpdated?: string };
}
```

### Security

- userId trong profile **luôn** được lấy từ JWT context (`@AuthenticationPrincipal`)
- Không accept userId trong request body — phòng chống privilege escalation

### References

- [Source: architecture.md#Mẫu-Phân-Quyền]
- [Source: ux-design-specification.md#UX-DR2]
- [Source: epics.md#Story-2.2]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
