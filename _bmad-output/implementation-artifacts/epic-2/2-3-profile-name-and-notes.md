# Story 2.3: Đặt tên và ghi chú cho từng hồ sơ

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
I want chỉnh tên hiển thị và ghi chú từng hồ sơ,
so that tôi phân biệt nhanh giữa nhiều hồ sơ.

## Acceptance Criteria

1. **Given** hồ sơ đã tồn tại, **When** người dùng cập nhật displayName hoặc notes, **Then** hệ thống lưu thay đổi và hiển thị ngay ở profile list/detail.
2. **Given** notes quá 500 ký tự, **When** submit, **Then** validation error.
3. **Given** displayName trống, **When** submit, **Then** validation error.
4. **Given** cập nhật thành công, **When** mở profile trên thiết bị khác, **Then** dữ liệu mới hiển thị.
5. **Given** user không phải owner của profile, **When** gọi API update, **Then** 403 Forbidden.

## Tasks / Subtasks

- [ ] Task 1 — Backend: Update profile endpoint (AC: #1, #2, #3, #5)
  - [ ] `PUT /api/v1/profiles/{profileId}` với DTO: `{ displayName, notes, dateOfBirth, gender }`
  - [ ] Ownership check: profile.userId phải == JWT user ID, nếu không → 403
  - [ ] Validate: displayName not blank, max 100 chars; notes max 500 chars
- [ ] Task 2 — Web: Edit profile flow (AC: #1, #2, #3)
  - [ ] Từ ProfileCard hoặc profile detail page: nút "Chỉnh sửa"
  - [ ] Modal hoặc inline edit với React Hook Form
  - [ ] Optimistic update với TanStack Query sau khi lưu thành công
- [ ] Task 3 — Mobile (Phase 2): Edit profile screen (AC: #1, #2)
  - [ ] `apps/mobile/app/profiles/[id]/edit.tsx`
  - [ ] Tương tự web nhưng native TextInput cho notes (multiline)
- [ ] Task 4 — Tests (AC: #1, #2, #5)
  - [ ] `ProfileServiceTest`: update thành công, validate, ownership check

## Dev Notes

### API

```
PUT /api/v1/profiles/{profileId}
Authorization: Bearer ...
Body: { "displayName": "Mẹ", "notes": "Tiểu đường type 2", "gender": "female" }
Response 200: { "data": { updated profile }, "meta": {...} }
Response 403: { "title": "Không có quyền truy cập hồ sơ này" }
```

### Zod Schema (packages/shared/schemas/profile.ts)

```typescript
export const updateProfileSchema = z.object({
  displayName: z.string().min(1, 'Tên không được để trống').max(100),
  notes: z.string().max(500, 'Ghi chú tối đa 500 ký tự').optional(),
  dateOfBirth: z.string().optional(),
  gender: z.enum(['male', 'female', 'other']).optional(),
});
```

### TanStack Query Invalidation

```typescript
// Sau khi mutation thành công:
queryClient.invalidateQueries({ queryKey: ['profiles'] });
queryClient.invalidateQueries({ queryKey: ['profile', profileId] });
```

### References

- [Source: architecture.md#Mẫu-Phân-Quyền]
- [Source: epics.md#Story-2.3]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
