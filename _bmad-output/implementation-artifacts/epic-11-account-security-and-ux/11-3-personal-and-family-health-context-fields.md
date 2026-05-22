# Story 11.3: Ghi chú cá nhân & ngữ cảnh sức khỏe theo hồ sơ

Status: ready-for-dev

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

- [ ] Task 1 — DB migration (AC: #1, #2, #4)
  - [ ] `users`: `personal_description TEXT`, `personal_notes TEXT` (max 500 mỗi field — validate app layer)
  - [ ] `profiles`: `chronic_conditions TEXT`, `current_medications TEXT`, `allergies TEXT` (max 1000 mỗi — hoặc JSON array nếu muốn structured; **khuyến nghị TEXT đơn giản** cho MVP)
- [ ] Task 2 — Backend DTO & API (AC: #1–#4, #6, #7)
  - [ ] `PUT /api/v1/users/me` mở rộng body: `personalDescription`, `personalNotes`
  - [ ] `PUT /api/v1/users/me/health-context` — update default profile clinical fields only
  - [ ] `PUT /api/v1/profiles/{id}` mở rộng: `chronicConditions`, `currentMedications`, `allergies`
  - [ ] GET responses include fields (profile list + me)
  - [ ] Ownership/share rules unchanged
- [ ] Task 3 — Shared schemas (AC: #7)
  - [ ] `packages/shared/schemas/user.ts` — update user profile schema
  - [ ] `packages/shared/schemas/profile.ts` — thêm 3 trường; **thống nhất** `displayName` max (chọn 100 cả create/update)
  - [ ] `pnpm build` trong `packages/shared`
- [ ] Task 4 — Web `/settings/profile` (AC: #1, #2)
  - [ ] Section "Mô tả & ghi chú cá nhân" (2 textarea)
  - [ ] Section "Thông tin sức khỏe của tôi" — 3 trường default profile
  - [ ] Giữ copy: "Bệnh nền, thuốc và dị ứng của người thân → quản lý tại Hồ sơ gia đình"
  - [ ] Không thêm form bệnh nền chung cho cả account ngoài default profile
- [ ] Task 5 — Web profile modals (AC: #3, #4, #6)
  - [ ] `CreateProfileModal`, `EditProfileModal` — 3 trường mới
  - [ ] `profileMappings.ts` map fields
  - [ ] Disable khi `canEdit === false`
- [ ] Task 6 — Tests (AC: #1–#7)
  - [ ] `UserServiceTest` — personal fields
  - [ ] `ProfileServiceTest` — clinical fields + default profile endpoint
  - [ ] Web: form validation tests

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

(pending)

### Completion Notes List

### File List
