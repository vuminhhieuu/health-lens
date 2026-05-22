# Story 11.3: Ghi chú cá nhân & ngữ cảnh sức khỏe theo hồ sơ

Status: done

## Story

As a người dùng,
I want ghi chú/mô tả ở hồ sơ cá nhân và quản lý bệnh nền/thuốc/dị ứng đúng chỗ,
so that dữ liệu không bị nhầm giữa tài khoản chủ và hồ sơ người thân.

## Acceptance Criteria

1. **Given** `/settings/profile`, **When** lưu mô tả và ghi chú cá nhân, **Then** persist trên bảng `users` (`personal_description`, `personal_notes`) — không ghi đè `profiles.notes` của hồ sơ gia đình.
2. **Given** chủ tài khoản tại `/settings/profile`, **When** mở section "Thông tin sức khỏe của tôi", **Then** chỉnh `chronic_conditions`, `current_medications`, `allergies` của profile `is_default=true` (API resolve default profile server-side).
3. **Given** `/profiles`, **When** render danh sách, **Then** **không** hiển thị profile `isDefault` (giữ filter hiện tại tại `profiles/page.tsx`).
4. **Given** tạo/sửa hồ sơ gia đình (non-default), **When** lưu trong `CreateProfileModal` / `EditProfileModal`, **Then** có 3 trường: Bệnh nền, Thuốc đang dùng, Dị ứng — tách khỏi ô "Ghi chú" chung.
5. **Given** `ProfileCard` / visit-summary, **When** hiển thị, **Then** có thể show badge/icon nếu có dị ứng (optional P2 polish — không chặn story).
6. **Given** shared profile view-only, **When** mở edit, **Then** trường y tế read-only hoặc ẩn (theo `canEdit`).
7. **Given** validation, **When** vượt giới hạn, **Then** lỗi tiếng Việt; đồng bộ Zod + backend (fix inconsistency 50 vs 100 chars displayName nếu chạm cùng file).
8. **Given** visit-summary checklist, **When** có dữ liệu profile, **Then** pre-fill gợi ý từ `currentMedications` / `allergies` (read-only hint, không auto-submit).

## Tasks / Subtasks

- [x] Task 1 — DB migration (AC: #1, #2, #4)
  - [x] `users`: `personal_description TEXT`, `personal_notes TEXT` (max 500 mỗi field — validate app layer)
  - [x] `profiles`: `chronic_conditions TEXT`, `current_medications TEXT`, `allergies TEXT` (max 1000 mỗi — hoặc JSON array nếu muốn structured; **khuyến nghị TEXT đơn giản** cho MVP)
- [x] Task 2 — Backend DTO & API (AC: #1–#4, #6, #7)
  - [x] `PUT /api/v1/users/me` mở rộng body: `personalDescription`, `personalNotes`
  - [x] `PUT /api/v1/users/me/health-context` — update default profile clinical fields only
  - [x] `PUT /api/v1/profiles/{id}` mở rộng: `chronicConditions`, `currentMedications`, `allergies`
  - [x] GET responses include fields (profile list + me)
  - [x] Ownership/share rules unchanged
- [x] Task 3 — Shared schemas (AC: #7)
  - [x] `packages/shared/schemas/user.ts` — update user profile schema
  - [x] `packages/shared/schemas/profile.ts` — thêm 3 trường; **thống nhất** `displayName` max (chọn 100 cả create/update)
  - [x] `pnpm build` trong `packages/shared`
- [x] Task 4 — Web `/settings/profile` (AC: #1, #2)
  - [x] Section "Mô tả & ghi chú cá nhân" (2 textarea)
  - [x] Section "Thông tin sức khỏe của tôi" — 3 trường default profile
  - [x] Giữ copy: "Bệnh nền, thuốc và dị ứng của người thân → quản lý tại Hồ sơ gia đình"
  - [x] Không thêm form bệnh nền chung cho cả account ngoài default profile
- [x] Task 5 — Web profile modals (AC: #3, #4, #6)
  - [x] `CreateProfileModal`, `EditProfileModal` — 3 trường mới
  - [x] `profileMappings.ts` map fields
  - [x] Disable khi `canEdit === false`
- [x] Task 6 — Tests (AC: #1–#7)
  - [x] `UserServiceTest` — personal fields, health-context, clear-field behavior
  - [x] `ProfileServiceTest` — clinical fields + default profile endpoint

### Review Findings

- [x] [Review][Decision] Dữ liệu y tế trong `SharedProfileResponse` cho người xem chỉ-read — **Quyết định:** giữ expose API + modal read-only cho `view` (lựa chọn 1).

- [x] [Review][Patch] Không xóa được nội dung đã lưu (null sentinel) — Backend luôn apply personal/clinical fields; frontend `normalizeOptionalTextField` gửi `null` khi xóa.

- [x] [Review][Patch] `profiles/page.tsx` không truyền clinical fields vào `EditProfileModal` — Đã map `chronicConditions`, `currentMedications`, `allergies` cho family + shared cards.

- [x] [Review][Patch] Lưu `/settings/profile` không atomic — Thông báo lỗi cụ thể khi health-context fail, invalidate `currentUser`, cache từ response health-context khi thành công.

- [x] [Review][Defer] `notes` max 1000 (create) vs 500 (update) vẫn lệch [`profile.ts`, `CreateProfileRequest.java`] — pre-existing; story chỉ yêu cầu thống nhất `displayName`.

- [x] [Review][Defer] Thiếu `UserController` WebMvc test cho `PUT /me/health-context` — coverage gap, không chặn AC.

## Dev Notes

### Mô hình dữ liệu (ranh giới sản phẩm)

```
User (tài khoản chủ)
├── personalDescription, personalNotes     → /settings/profile
└── owns Profile(isDefault=true)           → clinical fields tại section "của tôi"
    ├── chronicConditions
    ├── currentMedications
    └── allergies

Profile(isDefault=false)  → /profiles (gia đình)
├── displayName, notes (ghi chú chung — đã có story 2.3)
├── chronicConditions, currentMedications, allergies  → MỚI
```

**Quan trọng:** Trang `/profiles` **đã loại** default profile:

```246:261:apps/web/src/app/(dashboard)/profiles/page.tsx
    const familyProfiles = otherProfiles
      .filter((p) => !p.isDefault)
      .map((profile) => ({
```

Không đổi hành vi này — chủ tài khoản quản lý "hồ sơ sức khỏe của tôi" qua `/settings/profile`, không qua danh sách gia đình.

### Entity hiện tại

- `User`: chưa có notes/description — chỉ `fullName`, `birthDate`, `gender`, avatar.
- `Profile`: có `notes` (TEXT) — dùng cho ghi chú chung; thêm 3 cột clinical riêng.

### API đề xuất

```
PUT /api/v1/users/me
{ "fullName", "birthDate", "gender", "personalDescription", "personalNotes" }

PUT /api/v1/users/me/health-context
{ "chronicConditions", "currentMedications", "allergies" }
→ server tìm profile WHERE user_id = me AND is_default = true

PUT /api/v1/profiles/{profileId}
{ "displayName", "notes", "birthDate", "gender",
  "chronicConditions", "currentMedications", "allergies" }
```

### Zod (packages/shared)

```typescript
// profile.ts — bổ sung
chronicConditions: z.string().max(1000).optional().nullable(),
currentMedications: z.string().max(1000).optional().nullable(),
allergies: z.string().max(1000).optional().nullable(),

// user schema — bổ sung  
personalDescription: z.string().max(500).optional().nullable(),
personalNotes: z.string().max(500).optional().nullable(),
```

**Fix kỹ thuật nợ:** `createProfileSchema` max 50 vs `updateProfileSchema` max 100 — thống nhất **100** (REVIEW-DISPOSITION 5.6).

### UX copy (tiếng Việt)

| Trường | Label gợi ý |
|--------|-------------|
| personalDescription | Mô tả ngắn về bạn |
| personalNotes | Ghi chú cá nhân (không thay thế hồ sơ y tế) |
| chronicConditions | Bệnh nền / tình trạng lâu dài |
| currentMedications | Thuốc đang dùng |
| allergies | Dị ứng đã biết |

Disclaimer nhỏ dưới section y tế: "Thông tin tham khảo — trao đổi với bác sĩ trước khi quyết định điều trị."

### visit-summary liên kết (AC #8)

File `visit-summary/page.tsx` có checklist thuốc/dị ứng thủ công — đọc `selectedProfile` clinical fields làm **gợi ý** (muted text), không auto-check.

### Privacy

- Clinical fields là health-adjacent — không log nội dung trong application logs.
- Shared viewers read-only — audit không cần mở rộng ngoài profile access hiện có.

### Files (dự kiến)

**Backend:** `User.java`, `Profile.java`, DTOs, `UserService`, `ProfileService`, `UserController`, migration

**Frontend:** `settings/profile/page.tsx`, `CreateProfileModal`, `EditProfileModal`, `profileMappings.ts`, schemas

### References

- [Source: _bmad-output/implementation-artifacts/epic-2/2-3-profile-name-and-notes.md]
- [Source: _bmad-output/implementation-artifacts/public-account-experience/pae-9-profile-settings-stub-cleanup.md]
- [Source: _bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md#52-existing-conditions--allergies-fields]
- [Source: apps/web/src/app/(dashboard)/visit-summary/page.tsx — checklist copy]

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes List

- Added V049 migration for `users.personal_*` and `profiles` clinical columns.
- Extended `PUT /users/me`, new `PUT /users/me/health-context`, and profile create/update APIs with validation (500/1000 char limits, Vietnamese errors).
- `GET /users/me` returns personal fields plus default-profile clinical context; profile list/shared responses include clinical fields.
- `/settings/profile`: personal notes section + owner health context; saves via dual API calls.
- Family profile modals: 3 clinical textareas; read-only when `canEdit` is false.
- Visit-summary print checklist shows muted profile hints for medications/allergies (no auto-fill).
- Unified `displayName` max length to 100 in create/update (Zod + backend).
- Tests: `UserServiceTest`, `ProfileServiceTest` (unit — personal/clinical/clear-field coverage).

### File List

- apps/api/src/main/resources/db/migration/V049__personal_and_profile_health_context.sql
- apps/api/src/main/java/com/healthlens/api/entity/User.java
- apps/api/src/main/java/com/healthlens/api/entity/Profile.java
- apps/api/src/main/java/com/healthlens/api/dto/request/UpdateHealthContextRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/request/UpdateUserRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/request/CreateProfileRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/request/UpdateProfileRequest.java
- apps/api/src/main/java/com/healthlens/api/dto/response/UserResponse.java
- apps/api/src/main/java/com/healthlens/api/dto/response/ProfileResponse.java
- apps/api/src/main/java/com/healthlens/api/dto/response/SharedProfileResponse.java
- apps/api/src/main/java/com/healthlens/api/service/UserService.java
- apps/api/src/main/java/com/healthlens/api/service/ProfileService.java
- apps/api/src/main/java/com/healthlens/api/controller/UserController.java
- apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java
- apps/api/src/test/java/com/healthlens/api/service/UserServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/ProfileServiceTest.java
- apps/api/src/test/java/com/healthlens/api/controller/UserControllerTest.java
- packages/shared/schemas/user.ts
- packages/shared/schemas/profile.ts
- packages/shared/schemas/index.ts
- packages/shared/constants/api.ts
- apps/web/src/lib/api/routes.ts
- apps/web/src/lib/profileMappings.ts
- apps/web/src/app/(dashboard)/settings/profile/page.tsx
- apps/web/src/components/features/profiles/CreateProfileModal.tsx
- apps/web/src/components/features/profiles/EditProfileModal.tsx
- apps/web/src/app/(dashboard)/visit-summary/page.tsx
- _bmad-output/implementation-artifacts/sprint-status.yaml

## Change Log

- 2026-05-22: Implemented personal account notes and per-profile clinical health context (story 11.3).
- 2026-05-22: Code review fixes — clear-field sentinel, profiles page clinical mapping, partial-save error handling; D1: view-only may read clinical data.
- 2026-05-22: Removed frontend Vitest file `profile-settings.validation.test.ts` (validation covered by shared Zod + backend tests).
