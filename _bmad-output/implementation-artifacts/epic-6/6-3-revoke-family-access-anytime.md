# Story 6.3: Thu hồi quyền truy cập bất cứ lúc nào

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — phạm vi **web** (và backend/API nếu liệt kê).

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Chia sẻ gia đình - HealthLens | `projects/578519912546445367/screens/6c7c4797727240cda249483e8307e4cf` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sX2U4YWY1NjM2NGFhZDRjZjNhZmM1ODBiYzQ4MzNlZWYwEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uhVJVFS4A4qTdHejoumqcAW2zoxle4cvQUebbLe2G98ZHtcs-odgb0C_Htrha74dnqNsirr_cMVdtD7BIg6e76goaKyQ2yZN3J8ulVL-qFg6GWINeCD5d42pIIW-c0cSB9NlEhh_1ui1-RwdZmzwTxnHsk0d01-YLJNXIrihA9zUPlLyIds8YrE0sNgqllVOUGn2RSo67-ch6l1hc8WQTX6aE6I2LT0857A7LPtOZFYo1I5EjiDGPKYGQ) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a chủ hồ sơ,
I want thu hồi quyền truy cập của thành viên,
so that tôi kiểm soát chia sẻ dữ liệu linh hoạt.

## Acceptance Criteria

1. **Given** thành viên đang có quyền xem hồ sơ, **When** chủ hồ sơ chọn revoke, **Then** quyền truy cập bị vô hiệu hóa ngay lập tức.
2. **Given** quyền đã bị thu hồi, **When** family member thử truy cập profile hoặc records, **Then** 403 Forbidden.
3. **Given** revoke action, **When** kiểm tra audit log, **Then** ghi nhận actor, timestamp, resource.
4. **Given** family member đang online khi bị revoke, **When** thực hiện API call tiếp theo, **Then** nhận 403 và được redirect về dashboard.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Revoke share endpoint (AC: #1, #2, #3)
  - [ ] `DELETE /api/v1/profiles/{profileId}/shares/{viewerId}`
  - [ ] Set `revoked_at = now()` trong `profile_shares`
  - [ ] Ownership check: chỉ profile owner mới được revoke
  - [ ] Ghi audit log: "REVOKE_PROFILE_SHARE"
- [ ] Task 2 — Web: Revoke UI trong invite list (AC: #1)
  - [ ] Trong invite list (Story 6.1): mỗi accepted member có nút "Thu hồi" (icon X)
  - [ ] Confirm dialog: "Thu hồi quyền truy cập của {email}?"
  - [ ] Sau revoke: refresh danh sách members
- [ ] Task 3 — Tests (AC: #1, #2)
  - [ ] `ProfileShareServiceTest`: revoke thành công, access blocked sau revoke, non-owner cannot revoke

## Dev Notes

### API

```
DELETE /api/v1/profiles/{profileId}/shares/{viewerId}
Authorization: Bearer ...
Response 204 No Content (thành công)
Response 403: { "title": "Chỉ chủ hồ sơ mới có thể thu hồi quyền truy cập" }
```

### Immediate Effect

Sau khi revoke, mọi API call từ viewer:
1. Backend check `profile_shares` với `revoked_at IS NULL`
2. Không tìm thấy active share → 403

Không cần invalidate token — 403 xảy ra tức thì khi API gọi tiếp theo.

### References

- [Source: architecture.md#Mẫu-Phân-Quyền]
- [Source: epics.md#Story-6.3]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
