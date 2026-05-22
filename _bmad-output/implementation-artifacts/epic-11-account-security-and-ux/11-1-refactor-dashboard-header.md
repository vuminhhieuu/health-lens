# Story 11.1: Refactor thanh header dashboard

Status: done

<!-- story_key: 11-1-refactor-dashboard-header -->

## Story

As a người dùng đã đăng nhập,
I want thanh header dashboard gọn và menu avatar rõ ràng,
so that tôi điều hướng qua sidebar/bottom nav, truy cập Trang chủ từ menu tài khoản, và không bị phân tán bởi search hoặc nav trùng lặp trên header.

## Mục tiêu

| Trước | Sau |
|-------|-----|
| Header: logo + nav 3 mục + search + actions | Header: **logo + actions** (bell, help, avatar) |
| Avatar menu: Hồ sơ, Cài đặt, Đăng xuất | Thêm **Trang chủ** (`/home`) đầu menu |
| Breadcrumb `top-16`, `text-sm` — dính header | `top-[4.5rem]`, `text-base`, component chung |
| Hub: Kết quả khám, Profiles, Settings có breadcrumb | **Không** breadcrumb trên hub |
| Settings con: Trang chủ › Cài đặt › trang | **Cài đặt › trang** |
| `/profiles`: search + Sắp xếp (stub) | **Đã gỡ** |

**Header:**

```
┌ HealthLens ─────────────────────────── 🔔  ❓  👤 ┐
└ Avatar menu: Trang chủ | Hồ sơ | Cài đặt | Đăng xuất
```

## Acceptance Criteria

1. **Given** `(dashboard)`, **When** xem `AuthenticatedTopHeader`, **Then** chỉ logo + `NotificationBell` + Help + avatar — **không** search, nav ngang, palette.
2. **Given** menu avatar, **When** mở dropdown, **Then** có **Trang chủ** → `/home`, rồi Hồ sơ cá nhân, Cài đặt, Đăng xuất; avatar cố định `h-11 w-11`.
3. **Given** sidebar (desktop) / bottom nav (mobile), **When** điều hướng, **Then** không regression.
4. **Given** trang hub (`/health-records`, `/profiles`, `/settings`, `/settings/profile`), **When** load, **Then** **không** hiển thị breadcrumb.
5. **Given** trang con settings (`/settings/*` trừ hub), **When** có breadcrumb, **Then** pattern **Cài đặt › *tên trang*** (không Trang chủ).
6. **Given** breadcrumb sticky, **When** render, **Then** offset `top-[4.5rem]` khớp header; typography `text-base py-4`; dùng `DashboardBreadcrumbBar`.
7. **Given** `/profiles`, **When** xem danh sách, **Then** **không** ô tìm kiếm / nút Sắp xếp.
8. **Given** hoàn thành, **When** `pnpm lint` và `./gradlew test`, **Then** pass.

## Tasks / Subtasks

- [x] Task 1 — Refactor header (`AuthenticatedTopHeader`, `layout.tsx`, `MarketingHeader`)
- [x] Task 2 — Breadcrumb (`DashboardBreadcrumbBar`, `dashboardBreadcrumbTrails`, `shell.ts`, `DashboardPageShell`)
- [x] Task 3 — Đồng bộ breadcrumb toàn app + review record page
- [x] Task 4 — Gỡ search/sort `/profiles`
- [x] Task 5 — About settings: icon external link đồng nhất (Trung tâm trợ giúp)
- [x] Task 6 — Không thêm unit test frontend mới (theo yêu cầu PR)

## Dev Notes

### File chính

| File | Thay đổi |
|------|------------|
| `AuthenticatedTopHeader.tsx` | Gỡ nav/search; menu avatar + Trang chủ |
| `DashboardBreadcrumbBar.tsx` | Component breadcrumb dùng chung (mới) |
| `dashboardBreadcrumbTrails.ts` | Trail helpers (mới) |
| `DashboardPageShell.tsx` | Dùng `DashboardBreadcrumbBar` |
| `shell.ts` | `authenticatedHeaderOffsetClass`, breadcrumb tokens |
| `(dashboard)/layout.tsx` | `pt-[4.5rem]`; bỏ `navItems` header |
| `profiles/page.tsx` | Gỡ search + Sắp xếp |
| `settings/about/page.tsx` | `trailingIcon="external"` cho help row |
| `health-records/review/.../page.tsx` | Breadcrumb đồng bộ |

### Hub — không breadcrumb

- `/health-records`, `/profiles`, `/settings`, `/settings/profile`

### Settings — breadcrumb `Cài đặt › trang`

- privacy, notifications, change-password, about, delete-account, coming-soon

### Không thêm test file mới

- Đã xóa: `AuthenticatedTopHeader.test.tsx`, `DashboardBreadcrumbBar.test.tsx`, `dashboardBreadcrumbTrails.test.ts`
- Giữ mở rộng `shell.test.ts` cho layout tokens

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes List

- Pivot story từ global search sang refactor header + UX breadcrumb.
- Header: nav chuyển sidebar; Trang chủ trong avatar menu.
- Breadcrumb đồng bộ; fix offset header 72px.
- Profiles: bỏ toolbar tìm kiếm/sắp xếp.

### File List

- `apps/web/src/components/layout/AuthenticatedTopHeader.tsx`
- `apps/web/src/components/layout/DashboardBreadcrumbBar.tsx` (new)
- `apps/web/src/components/layout/DashboardPageShell.tsx`
- `apps/web/src/lib/layout/dashboardBreadcrumbTrails.ts` (new)
- `apps/web/src/lib/layout/shell.ts`
- `apps/web/src/lib/layout/shell.test.ts`
- `apps/web/src/app/(dashboard)/layout.tsx`
- `apps/web/src/app/(dashboard)/profiles/page.tsx`
- `apps/web/src/components/marketing/MarketingHeader.tsx`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `apps/web/src/app/(dashboard)/settings/about/page.tsx`
- Các trang dùng `breadcrumbFromHome` / `breadcrumbFromSettings`
- `_bmad-output/.../11-1-refactor-dashboard-header.md` (renamed)
- `_bmad-output/planning-artifacts/*`, `sprint-status.yaml`

## Change Log

- 2026-05-22: Hoàn tất refactor header, breadcrumb, profiles toolbar; xóa unit test frontend mới.
- 2026-05-22: Đổi tên `11-1-refactor-dashboard-header`.
- 2026-05-22: Pivot từ global search (không triển khai).
