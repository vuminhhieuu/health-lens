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

Bổ sung các tính năng còn thiếu sau Phase 1 MVP: tìm kiếm toàn cục, xác thực hai yếu tố cho người dùng, quản lý ghi chú/mô tả và ngữ cảnh sức khỏe theo đúng ranh giới tài khoản vs hồ sơ gia đình, và tái sử dụng component cho các luồng dashboard lặp lại.

## Phân tách phạm vi (theo yêu cầu sản phẩm)

| Vùng | Route | Dữ liệu | Ghi chú |
|------|-------|---------|---------|
| Hồ sơ cá nhân (chủ tài khoản) | `/settings/profile` | `User.personalNotes`, `User.personalDescription`; ngữ cảnh y tế chủ sở hữu gắn **default profile** | Không nhập bệnh nền/thuốc/dị ứng chung cho cả tài khoản |
| Hồ sơ gia đình | `/profiles` | `Profile.notes` + `chronicConditions`, `currentMedications`, `allergies` | **Loại trừ** `isDefault=true` (hồ sơ chủ) khỏi danh sách |
| Tìm kiếm toàn cục | Header dashboard (`AuthenticatedTopHeader`) | Command palette | Giữ tìm kiếm cục bộ trên `/profiles` và `/profiles/[id]/history` |
| 2FA người dùng | `/settings/security` (mới) hoặc tab trong settings | TOTP user (tách admin MFA) | Tái sử dụng pattern `AdminTotpSecret` |

## Stories

### Story 11.1: Tìm kiếm toàn cục — Header & Command Palette

As a người dùng đã đăng nhập,
I want tìm nhanh hồ sơ, kết quả khám và điều hướng từ thanh search trên header,
so that tôi không phải mở từng trang để tra cứu.

**Acceptance Criteria:**

1. **Given** người dùng ở bất kỳ trang dashboard nào, **When** focus vào ô search header hoặc nhấn `Ctrl/Cmd+K`, **Then** mở command palette với placeholder tiếng Việt rõ ràng.
2. **Given** người dùng gõ từ khóa ≥2 ký tự, **When** debounce 300ms, **Then** hiển thị nhóm kết quả: Hồ sơ (owned + shared), Kết quả khám gần đây, Điều hướng nhanh (Trang chủ, Kết quả khám, Hồ sơ gia đình, Cài đặt).
3. **Given** chọn một hồ sơ, **When** Enter/click, **Then** điều hướng đúng (`/profiles/{id}/history` hoặc hub tương ứng).
4. **Given** không có kết quả, **When** hiển thị empty state, **Then** gợi ý thử từ khóa khác; không crash.
5. **Given** API search lỗi, **When** hiển thị lỗi, **Then** dùng `ErrorState`/`notify` theo pattern `core-6-4`; ô search header vẫn hoạt động cho điều hướng tĩnh.
6. **Given** trang `/profiles`, **When** dùng search cục bộ trang, **Then** hành vi lọc danh sách gia đình **không bị thay đổi** (header search ≠ local filter).

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
2. **Given** `/profiles` và header search, **When** refactor, **Then** dùng `PageSearchField` chung (controlled value, debounce optional, aria-label).
3. **Given** thay đổi, **When** chạy test, **Then** không regression hành vi chọn hồ sơ từ query `?profileId=`.
4. **Given** hoàn thành, **When** cập nhật docs, **Then** `docs/component-inventory.md` liệt kê component mới + nơi dùng.
5. **Given** accessibility, **When** audit nhanh, **Then** label/`htmlFor`/`aria-*` giữ chuẩn `remaining-6-2`.

**Phụ thuộc:** Nên làm sau hoặc song song 11.1/11.3 (tránh conflict file). **Ưu tiên:** P2.

## Thứ tự đề xuất triển khai

1. `11-4` (nền component) — có thể song song đầu sprint
2. `11-3` (domain model + forms)
3. `11-1` (search — dùng component từ 11-4)
4. `11-2` (2FA — độc lập backend)

## BMad Help — Vị trí thanh search

**Khuyến nghị (đã chốt trong story 11.1):**

- **Chính:** `AuthenticatedTopHeader` trên mọi trang `(dashboard)` — đúng UX-DR7 (Web: header + sidebar).
- **Phụ:** Giữ search cục bộ tại `/profiles` (lọc thẻ gia đình) và `/profiles/[id]/history` (lọc kết quả khám).
- **Không đặt** search toàn cục tại `/settings/*` hoặc trang marketing.

**Bước tiếp theo sau create-story:** `bmad-dev-story` cho `11-4` hoặc `11-3` tùy ưu tiên team.
