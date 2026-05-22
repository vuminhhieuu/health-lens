---
project_name: health-lens
user_name: ie303
date: '2026-05-22'
workflowType: epics
platformFocus: Web-First (Phase 1)
source: user-request-via-bmad-create-story
---

# Epic 11: Bảo Mật Tài Khoản & Nâng Cao UX Làm Việc

## Mục tiêu Epic

Bổ sung các tính năng còn thiếu sau Phase 1 MVP: refactor header dashboard (gỡ search toàn cục), xác thực hai yếu tố cho người dùng, quản lý ghi chú/mô tả và ngữ cảnh sức khỏe theo đúng ranh giới tài khoản vs hồ sơ gia đình, và tái sử dụng component cho các luồng dashboard lặp lại.

## Phân tách phạm vi (theo yêu cầu sản phẩm)

| Vùng | Route | Dữ liệu | Ghi chú |
|------|-------|---------|---------|
| Hồ sơ cá nhân (chủ tài khoản) | `/settings/profile` | `User.personalNotes`, `User.personalDescription`; ngữ cảnh y tế chủ sở hữu gắn **default profile** | Không nhập bệnh nền/thuốc/dị ứng chung cho cả tài khoản |
| Hồ sơ gia đình | `/profiles` | `Profile.notes` + `chronicConditions`, `currentMedications`, `allergies` | **Loại trừ** `isDefault=true` (hồ sơ chủ) khỏi danh sách |
| Header dashboard | `AuthenticatedTopHeader` | Logo + actions; Trang chủ trong menu avatar | Sidebar/bottom nav điều hướng chính; `/profiles` không còn search toolbar |
| 2FA người dùng | `/settings/security` (mới) hoặc tab trong settings | TOTP user (tách admin MFA) | Tái sử dụng pattern `AdminTotpSecret` |

## Stories

### Story 11.1: Refactor thanh header dashboard

**Story key:** `11-1-refactor-dashboard-header`  
**File:** `11-1-refactor-dashboard-header.md`

As a người dùng đã đăng nhập,
I want thanh header dashboard gọn, nhất quán và tập trung vào điều hướng + hành động tài khoản,
so that tôi biết rõ mình đang ở đâu trong app và không bị phân tán bởi search toàn cục trên header.

**Acceptance Criteria (tóm tắt — chi tiết trong story file):**

1. `AuthenticatedTopHeader`: brand + nav (desktop) + actions — **không** ô search, palette, `Cmd/Ctrl+K`.
2. Giữ logic `isNavItemActive`, design token `shell.ts`, touch target a11y.
3. `(dashboard)/layout.tsx`: gỡ wiring palette/global search.
4. Dọn legacy `ApiRoutes.SEARCH`, `ApiPaths.SEARCH`, `GlobalSearch*` nếu còn.
5. `/profiles` local search **không đổi**.

**Phụ thuộc:** Không. **Ưu tiên:** P1.

---

### Story 11.2: Xác thực hai yếu tố (2FA) cho người dùng

As a người dùng,
I want bật xác thực TOTP cho tài khoản của tôi,
so that tài khoản an toàn hơn khi mật khẩu bị lộ.

**Acceptance Criteria:**

1. **Given** người dùng đã đăng nhập, **When** mở `/settings/security`, **Then** thấy trạng thái 2FA (tắt/bật) và CTA thiết lập — thay badge "Sắp có" tại `/settings/profile`.
2. **Given** bắt đầu setup, **When** gọi API setup, **Then** nhận QR/secret, backup codes (mã dùng một lần), hướng dẫn tiếng Việt.
3. **Given** nhập mã TOTP hợp lệ, **When** verify, **Then** 2FA được kích hoạt; audit log ghi `USER_TOTP_ENABLED`.
4. **Given** 2FA đã bật, **When** đăng nhập email/password thành công, **Then** bước thứ hai yêu cầu mã TOTP hoặc backup code trước khi cấp token.
5. **Given** người dùng muốn tắt 2FA, **When** xác nhận bằng mật khẩu + mã TOTP hiện tại, **Then** vô hiệu hóa an toàn.
6. **Given** admin MFA, **When** triển khai user 2FA, **Then** **không** dùng chung bảng/endpoint admin; tái sử dụng `TotpSecretCryptoService` với entity/repository riêng.

**Phụ thuộc:** Epic 1 auth ổn định. **Ưu tiên:** P1.

---

### Story 11.3: Ghi chú cá nhân & ngữ cảnh sức khỏe theo hồ sơ

As a người dùng,
I want ghi chú/mô tả ở hồ sơ cá nhân và quản lý bệnh nền/thuốc/dị ứng đúng chỗ,
so that dữ liệu không bị nhầm giữa tài khoản chủ và hồ sơ người thân.

**Acceptance Criteria:**

1. **Given** trang `/settings/profile`, **When** lưu `personalDescription` và `personalNotes`, **Then** persist trên `users` (không trên `profiles` gia đình).
2. **Given** chủ tài khoản, **When** mở mục "Thông tin sức khỏe của tôi" trên `/settings/profile`, **Then** chỉnh `chronicConditions`, `currentMedications`, `allergies` của **default profile** (`isDefault=true`).
3. **Given** trang `/profiles`, **When** xem danh sách, **Then** **không** hiển thị hồ sơ `isDefault` (giữ hành vi hiện tại).
4. **Given** tạo/sửa hồ sơ gia đình (non-default), **When** lưu, **Then** có trường bệnh nền, thuốc đang dùng, dị ứng riêng từng hồ sơ; `notes` vẫn là ghi chú chung.
5. **Given** viewer chỉ xem hồ sơ chia sẻ, **When** mở form, **Then** không chỉnh sửa được trường y tế (read-only hoặc ẩn).
6. **Given** validation, **When** vượt giới hạn ký tự, **Then** thông báo tiếng Việt; đồng bộ Zod (`packages/shared`) và backend DTO.

**Phụ thuộc:** Story 2.3 (notes profile), `pae-9` (stub cleanup). **Ưu tiên:** P1.

---

### Story 11.4: Component dùng chung cho luồng dashboard lặp lại

As a developer,
I want tái sử dụng component cho chọn hồ sơ, toolbar tìm kiếm và khối form,
so that UI nhất quán và dễ bảo trì.

**Acceptance Criteria:**

1. **Given** `follow-up-reminders` và `visit-summary`, **When** refactor, **Then** dùng `ProfileScopeSelector` chung (props: profiles, value, onChange, label, emptyHint).
2. **Given** `/profiles` page local search, **When** refactor, **Then** dùng `PageSearchField` (variant `page` only — không gắn header).
3. **Given** thay đổi, **When** chạy test, **Then** không regression hành vi chọn hồ sơ từ query `?profileId=`.
4. **Given** hoàn thành, **When** cập nhật docs, **Then** `docs/component-inventory.md` liệt kê component mới + nơi dùng.
5. **Given** accessibility, **When** audit nhanh, **Then** label/`htmlFor`/`aria-*` giữ chuẩn `remaining-6-2`.

**Phụ thuộc:** Nên làm sau hoặc song song 11.1/11.3 (tránh conflict file). **Ưu tiên:** P2.

## Thứ tự đề xuất triển khai

1. `11-1-refactor-dashboard-header` (refactor header + gỡ legacy search)
2. `11-4` (`PageSearchField` tại `/profiles` — sau 11.1)
3. `11-3` (domain model + forms)
4. `11-2` (2FA — độc lập backend)

## BMad Help — Header & search

**Đã chốt (story 11.1):**

- **Header:** logo, bell, help, avatar — **Trang chủ** trong menu avatar; không nav/search trên header.
- **Breadcrumb hub:** không hiển thị tại `/health-records`, `/profiles`, `/settings`, `/settings/profile`.
- **Breadcrumb settings con:** `Cài đặt › trang`.
- **Không** search tại `/settings/*` hoặc marketing.

**Bước tiếp theo:** `bmad-dev-story` cho `11-1-refactor-dashboard-header`.
