# Story 6.1: Mời thành viên gia đình vào hồ sơ qua email

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Chia sẻ gia đình - HealthLens | `projects/578519912546445367/screens/6c7c4797727240cda249483e8307e4cf` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2U4YWY1NjM2NGFhZDRjZjNhZmM1ODBiYzQ4MzNlZWYwEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhVJVFS4A4qTdHejoumqcAW2zoxle4cvQUebbLe2G98ZHtcs-odgb0C_Htrha74dnqNsirr_cMVdtD7BIg6e76goaKyQ2yZN3J8ulVL-qFg6GWINeCD5d42pIIW-c0cSB9NlEhh_1ui1-RwdZmzwTxnHsk0d01-YLJNXIrihA9zUPlLyIds8YrE0sNgqllVOUGn2RSo67-ch6l1hc8WQTX6aE6I2LT0857A7LPtOZFYo1I5EjiDGPKYGQ) |
| Chưa có thành viên gia đình nào - HealthLens | `projects/578519912546445367/screens/4fa649c74ee4431fb2ad96bc3550ebd8` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzE4MzBmYTJjZmZiNDRmOWM5YTg2ZWY4MGFiY2ExMmRhEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ujLvf17-7gS5u0h-1kjqsPyiy8jn1gN_nfz0VVwf2einujV99jtP6dYIi1PFQtVgxCY8I51RW2MZoiYE0Ah_6g92xP7QhtriTYXBZRhBRxtw_qepk-FTdkYIRR-CrSamuA8Kzem2cSGmvpaREiaJ7c8K1CML89Epo51rXwBhoyzVsB27OJdty9zJqE10t_-FK8VLMYLAhQ4VqB365WoDTos8innjlydf3-TUbU_qmcY0CNluKpHVjcBWA) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a chủ hồ sơ,
I want gửi lời mời chia sẻ qua email,
so that người thân có thể theo dõi kết quả khám.

## Acceptance Criteria

1. **Given** hồ sơ thuộc quyền sở hữu của tôi, **When** nhập email người nhận và gửi lời mời, **Then** hệ thống tạo invitation token và gửi email mời.
2. **Given** email mời, **When** kiểm tra, **Then** lời mời có trạng thái `pending`/`accepted`/`expired`, hết hạn sau 7 ngày.
3. **Given** email người nhận đã có tài khoản, **When** họ click link mời, **Then** được redirect vào dashboard với profile đã granted.
4. **Given** email người nhận chưa có tài khoản, **When** họ click link mời, **Then** được redirect đến trang đăng ký với invitation context.
5. **Given** chủ hồ sơ, **When** xem danh sách invited members, **Then** thấy email, trạng thái, ngày invite.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Profile shares tables + invite endpoint (AC: #1, #2)
  - [ ] Flyway migration `V014__create_profile_shares_tables.sql`
  - [ ] Bảng `profile_invitations(id, profile_id, inviter_id, invitee_email, token, status, expires_at, created_at)`
  - [ ] Bảng `profile_shares(id, profile_id, owner_id, viewer_id, granted_at, revoked_at)`
  - [ ] `POST /api/v1/profiles/{profileId}/invitations` → tạo invitation, gửi email
  - [ ] `GET /api/v1/profiles/{profileId}/invitations` → danh sách invited members
- [ ] Task 2 — Backend: Accept invitation endpoint (AC: #3, #4)
  - [ ] `POST /api/v1/invitations/accept?token={invitationToken}`
  - [ ] Validate token, mark `accepted`, tạo `profile_shares` row
  - [ ] Nếu user chưa đăng nhập: redirect về `/register?inviteToken={token}`
- [ ] Task 3 — Backend: Email mời tiếng Việt (AC: #1)
  - [ ] Subject: "{inviterName} muốn chia sẻ hồ sơ sức khỏe với bạn"
  - [ ] Link: `{frontendUrl}/invitations/accept?token={token}`
- [ ] Task 4 — Web: Invite UI trong profile settings (AC: #1, #5)
  - [ ] Thêm tab "Chia sẻ" trong profile detail page
  - [ ] Form: nhập email, nút "Gửi lời mời"
  - [ ] List invited members với trạng thái (pending/accepted/expired)
- [ ] Task 5 — Tests (AC: #1, #2, #3)
  - [ ] `ProfileShareServiceTest`: invite, accept, duplicate invite, expired token

## Dev Notes

### Database Schema

```sql
-- V014__create_profile_shares_tables.sql
CREATE TABLE profile_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    inviter_id UUID NOT NULL REFERENCES users(id),
    invitee_email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE profile_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES users(id),
    viewer_id UUID NOT NULL REFERENCES users(id),
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP,
    UNIQUE(profile_id, viewer_id)
);
```

### References

- [Source: architecture.md#Mẫu-Phân-Quyền]
- [Source: epics.md#Story-6.1]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
