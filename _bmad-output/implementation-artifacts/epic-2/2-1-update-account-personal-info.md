# Story 2.1: Cập nhật thông tin cá nhân tài khoản

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

| Tiêu đề (Stitch) | Resource | HTML prototype | Screenshot |
|---|---|---|---|
| Bảo mật & MFA - Admin HealthLens | `projects/578519912546445367/screens/fc0abf8742984269b5a25a0e0ab20782` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzgwODQwM2RhZmU1ZjQxYzVhZjlkMmM3M2ZhYjZlNTRlEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0ugyW7fgyBR0_jEdEXcjJfxOc-6CikrdsiczJhlmYeYx06neSyys78DHloUAPnRGIPR0XUM-FfdkIQfkCuMiyyGWP_D809TnwtZI6UiT2Qva9syxUZw3-VLdJMq9hOZ32beEUYcHZMV6u172JDnvzcJxBToc3yjtRX4QALmrM65BksPWyZEsOBvzll_uAk6GD_dAfZznnXv_RjbRu7Tp-y3KzwEr9BlywF2XpRJDYYsGDS-Z2hXU5biO9g) |
| Cài đặt hệ thống - Admin HealthLens | `projects/578519912546445367/screens/f6198b62522b49d88bd34fad6c9cee84` | [Mở](https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ6Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpZCiVodG1sXzExNDJmMmIzZGJjMzQ5NDRiNzE2ZTJiYTExMThmZjAwEgsSBxCr9e3nmh0YAZIBIgoKcHJvamVjdF9pZBIUQhI1Nzg1MTk5MTI1NDY0NDUzNjc&filename=&opi=96797242) | [Xem](https://lh3.googleusercontent.com/aida/ADBb0uguwdmxJMCbxtT_MJxyXkaWRm-i6TXm49DBhdMUFgh18aER7jtytjHXhxWAnlOp9FKlKGCxCFYFDV_JWI5YRAHjKFc9B8mrzq3vp41YbW5E8pJmRrxjiLvQHfIjrQWk4f_zZ3CWPJKYYqbHylwy3JU4SNYAdPzJNRlYwSSfa0pBD0qh8mClBoKDNPYP6wERtt-Gu6gtm6ngH9X0Owpgk_lpM2Ojv4ZPCd-oPlnLsQV41vYcgn2ANad_jA) |

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a người dùng,
I want cập nhật tên, ngày sinh, giới tính,
so that hệ thống cá nhân hóa hiển thị và diễn giải phù hợp.

## Acceptance Criteria

1. **Given** người dùng đã đăng nhập, **When** chỉnh sửa tên, ngày sinh, giới tính và lưu, **Then** dữ liệu được cập nhật và hiển thị nhất quán trên **web** (Phase 1); khi có app mobile (Phase 2), cùng nguồn API đảm bảo hiển thị đồng bộ.
2. **Given** ngày sinh nhập sai định dạng hoặc không hợp lệ, **When** submit, **Then** validation error RFC 7807 chỉ rõ field lỗi.
3. **Given** giới tính nhập giá trị ngoài enum (`male`, `female`, `other`), **When** submit, **Then** validation error.
4. **Given** cập nhật thành công, **When** mở profile trên thiết bị khác, **Then** dữ liệu mới hiển thị (không cache stale).

## Tasks / Subtasks

- [ ] Task 1 — Backend: Account info endpoints (AC: #1, #2, #3)
  - [ ] Thêm cột vào bảng `users`: `full_name`, `date_of_birth` (DATE), `gender` (VARCHAR 10)
  - [ ] Flyway migration `V008__add_profile_fields_to_users.sql`
  - [ ] `GET /api/v1/users/me` — trả về thông tin hiện tại của user
  - [ ] `PUT /api/v1/users/me` với DTO: `{ fullName, dateOfBirth, gender }`
  - [ ] Validation: dateOfBirth phải là ngày hợp lệ, gender chỉ `["male", "female", "other"]`
- [ ] Task 2 — Web: Profile settings page (AC: #1, #2)
  - [ ] Tạo `apps/web/src/app/(dashboard)/settings/profile/page.tsx`
  - [ ] Form với React Hook Form + Zod: fullName (required), dateOfBirth (date picker), gender (select)
  - [ ] Pre-populate với dữ liệu hiện tại từ `GET /api/v1/users/me`
  - [ ] Toast success sau khi lưu
- [ ] Task 3 — Mobile (Phase 2): Profile settings screen (AC: #1, #2)
  - [ ] Tạo `apps/mobile/app/(tabs)/settings/profile.tsx`
  - [ ] Form tương tự, dùng native DateTimePicker cho date
- [ ] Task 4 — Tests (AC: #1, #2, #3)
  - [ ] `UserServiceTest`: update thành công, validate ngày, validate gender

## Dev Notes

### API

```
GET /api/v1/users/me → { data: { id, email, fullName, dateOfBirth, gender, emailVerified, consentGiven }, meta }
PUT /api/v1/users/me → { data: { id, email, fullName, dateOfBirth, gender }, meta }
Body: { "fullName": "...", "dateOfBirth": "1990-01-15", "gender": "male" }
```

### Gender Enum (packages/shared/constants)

```typescript
export const GENDER_OPTIONS = ['male', 'female', 'other'] as const;
export type Gender = typeof GENDER_OPTIONS[number];
```

### Zod Schema

```typescript
// packages/shared/schemas/user.ts
export const updateProfileSchema = z.object({
  fullName: z.string().min(1).max(100),
  dateOfBirth: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Định dạng YYYY-MM-DD').optional(),
  gender: z.enum(['male', 'female', 'other']).optional(),
});
```

### TanStack Query

```typescript
// Invalidate cache sau update
queryClient.invalidateQueries({ queryKey: ['currentUser'] });
```

### References

- [Source: architecture.md#Mẫu-Cấu-Trúc]
- [Source: epics.md#Story-2.1]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
