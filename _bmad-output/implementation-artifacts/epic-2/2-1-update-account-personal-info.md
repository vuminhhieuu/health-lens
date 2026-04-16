# Story 2.1: Cập nhật thông tin cá nhân tài khoản

Status: review

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

- [x] Task 1 — Backend: Account info endpoints (AC: #1, #2, #3)
  - [x] Thêm cột vào bảng `users`: `full_name`, `date_of_birth` (DATE), `gender` (VARCHAR 10)
  - [x] Flyway migration `V008__add_profile_fields_to_users.sql`
  - [x] `GET /api/v1/users/me` — trả về thông tin hiện tại của user
  - [x] `PUT /api/v1/users/me` với DTO: `{ fullName, birthDate, gender }`
  - [x] Validation: birthDate phải là ngày hợp lệ, gender chỉ `["male", "female", "other"]`
- [x] Task 2 — Web: Profile settings page (AC: #1, #2)
  - [x] Tạo `apps/web/src/app/(dashboard)/settings/profile/page.tsx`
  - [x] Form với React Hook Form + Zod: fullName (required), birthDate (date picker), gender (select)
  - [x] Pre-populate với dữ liệu hiện tại từ `GET /api/v1/users/me`
  - [x] Toast success sau khi lưu
- [ ] Task 3 — Mobile (Phase 2): Profile settings screen (AC: #1, #2)
  - [ ] Tạo `apps/mobile/app/(tabs)/settings/profile.tsx`
  - [ ] Form tương tự, dùng native DateTimePicker cho date
- [x] Task 4 — Tests (AC: #1, #2, #3)
  - [x] `UserServiceTest`: update thành công, validate ngày, validate gender

## Dev Notes

### API

```
GET /api/v1/users/me → { data: { id, email, fullName, birthDate, gender, emailVerified, consentGiven }, meta }
PUT /api/v1/users/me → { data: { id, email, fullName, birthDate, gender }, meta }
Body: { "fullName": "...", "birthDate": "1990-01-15", "gender": "male" }
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
  birthDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Định dạng YYYY-MM-DD').optional(),
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

Gemini 3.1 Pro (High)

### Debug Log References

- Code implemented and tested with standard Spring Boot standards and React Hook Form + Zod.
- TanStack Query query invalidation properly implemented.
- Replaced `birthDate` with `birthDate` everywhere on API to align with DTO requests without breaking mapping.

### Completion Notes List

- Cập nhật Flyway V008 
- Thêm UserService, UserController, test cases.
- Cập nhật schemas, package `@healthlens/shared` cho schemas và constants (gender).
- Implement màn hình profile trên Web hoàn chỉnh với `@radix-ui/themes`.
- Phase 2 (Mobile) là out of scope cho phase này theo Requirement.

### File List
- `apps/api/src/main/resources/db/migration/V008__add_profile_fields_to_users.sql` (NEW)
- `apps/api/src/main/java/com/healthlens/api/entity/User.java` (MODIFIED)
- `apps/api/src/main/java/com/healthlens/api/dto/request/RegisterRequest.java` (MODIFIED)
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java` (MODIFIED)
- `apps/api/src/test/java/com/healthlens/api/controller/AuthControllerTest.java` (MODIFIED)
- `apps/api/src/main/java/com/healthlens/api/dto/response/UserResponse.java` (NEW)
- `apps/api/src/main/java/com/healthlens/api/dto/request/UpdateUserRequest.java` (NEW)
- `apps/api/src/main/java/com/healthlens/api/service/UserService.java` (NEW)
- `apps/api/src/main/java/com/healthlens/api/controller/UserController.java` (NEW)
- `apps/api/src/test/java/com/healthlens/api/service/UserServiceTest.java` (NEW)
- `apps/api/src/main/java/com/healthlens/api/common/ApiRoutes.java` (MODIFIED)
- `packages/shared/constants/index.ts` (MODIFIED)
- `packages/shared/schemas/user.ts` (NEW)
- `packages/shared/schemas/index.ts` (MODIFIED)
- `apps/web/src/lib/api/routes.ts` (MODIFIED)
- `apps/web/src/app/(dashboard)/settings/profile/page.tsx` (NEW)

### Review Findings

- [x] [Review][Decision] User clearing birthDate or gender — AC1 specifies updating birth date and gender. The frontend sends `null` for empty fields. The backend API `UserService.java` ignores `null` values (`if (request.birthDate() != null)`). This means users cannot clear their date of birth or gender once entered. Should users be allowed to clear these fields? (If yes, backend needs to actively save nulls. If no, frontend needs to mark them required). [FIXED: Added Required rules to schema to match business logic]
- [x] [Review][Patch] Inconsistent `birthDate` validation [`UpdateUserRequest.java`] — The `UpdateUserRequest` DTO omits `@NotNull` for `birthDate` (unlike RegistrationRequest), but the `date_of_birth` column is `nullable = false` in the DB schema. This mismatch can lead to unexpected 500 errors. [FIXED: Added @NotNull to DTO]
