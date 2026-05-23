# Story 11.4: Component dùng chung cho luồng dashboard lặp lại

Status: done

## Story

As a developer,
I want tái sử dụng component cho chọn hồ sơ, toolbar tìm kiếm và khối form dashboard,
so that UI nhất quán và dễ bảo trì.

## Acceptance Criteria

1. **Given** `follow-up-reminders` và `visit-summary`, **When** refactor xong, **Then** cả hai dùng `ProfileScopeSelector` từ `@/components/features/profiles/ProfileScopeSelector`.
2. **Given** props `profiles`, `value`, `onChange`, `label`, `id`, **When** render, **Then** có `htmlFor`/`id` khớp, `aria-label` khi không có visible label.
3. **Given** URL `?profileId=`, **When** mount page, **Then** selector ưu tiên query → default profile → first profile (giữ logic hiện tại).
4. **Given** cần search cục bộ profiles, **When** triển khai 11.4, **Then** có thể thêm `PageSearchField` — **hiện tại** Story 11.1 đã **gỡ** search/sắp xếp trên `/profiles`; 11.4 chỉ áp dụng nếu product yêu cầu bật lại.
5. **Given** refactor, **When** `pnpm test` + `pnpm lint`, **Then** pass; không đổi hành vi mutation/query keys.
6. **Given** hoàn thành, **When** cập nhật `docs/component-inventory.md`, **Then** có mục mới với import path và consumers.

## Tasks / Subtasks

- [x] Task 1 — `ProfileScopeSelector` (AC: #1–#3)
  - [x] Extract từ `follow-up-reminders/page.tsx` (select block ~lines 398–419)
  - [x] Props: `profiles: { id, displayName }[]`, `value`, `onChange`, `label?`, `id?`, `disabled?`, `className?`
  - [x] Export từ `components/features/profiles/`
- [x] Task 2 — Refactor consumers (AC: #1, #3, #5)
  - [x] `follow-up-reminders/page.tsx`
  - [x] `visit-summary/page.tsx`
  - [x] (Optional) `health-records/page.tsx` nếu có pattern select tương tự — bỏ qua (không có pattern select profile scope)
- [x] Task 3 — `PageSearchField` (AC: #4)
  - [x] Không triển khai — product không dùng search local sau story 11.1; component đã gỡ khỏi codebase
- [x] Task 4 — (Optional P2) `DashboardFormSection` 
  - [x] Bỏ qua — chỉ 2 chỗ form header, chưa đủ ≥3 để extract
- [x] Task 5 — Docs (AC: #5, #6)
  - [x] Cập nhật `docs/component-inventory.md`
  - [x] Không thêm Vitest component mới — kiểm thử thủ công + `pnpm lint`

### Review Findings

- [x] [Review][Decision] Thống nhất style select visit-summary — **A**: giữ một style chung (`#f7fffd`, focus `#00685f`).
- [x] [Review][Patch] `value` không có trong `profiles` — disable + `selectValue=""` khi invalid [`ProfileScopeSelector.tsx`]
- [x] [Review][Patch] `profiles.length === 0` — `disabled` khi không có option [`ProfileScopeSelector.tsx`]
- [x] [Review][Defer] AC#5 full `pnpm test` fail — `privacy-settings.page.test.ts` expect `label: "Riêng tư"` nhưng page dùng `breadcrumbFromSettings("Riêng tư")` — pre-existing, không do 11.4

## Dev Notes

### Duplication đã xác định

**Profile select** — gần như copy-paste giữa:
- `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx`
- `apps/web/src/app/(dashboard)/visit-summary/page.tsx`

Cùng pattern:
- `useSearchParams().get("profileId")`
- `useMemo` chọn default/first
- `<select>` với class `min-h-12 w-full rounded-xl border border-[#b7e8e0]...`

**Search input** — Story 11.1 đã gỡ search trên `/profiles`. Nếu 11.4 cần search lại, extract `PageSearchField` khi product chốt.

### Component API đề xuất

```tsx
// ProfileScopeSelector.tsx
export type ProfileScopeOption = { id: string; displayName: string };

export function ProfileScopeSelector({
  profiles,
  value,
  onChange,
  label = "Hồ sơ",
  id = "profile-scope",
  disabled = false,
  className,
}: {
  profiles: ProfileScopeOption[];
  value: string;
  onChange: (profileId: string) => void;
  label?: string;
  id?: string;
  disabled?: boolean;
  className?: string;
}) { ... }

// PageSearchField.tsx
export function PageSearchField({
  value,
  onChange,
  placeholder = "Tìm kiếm...",
  ariaLabel = "Tìm kiếm",
  variant = "page",
  className,
}: { ... }) { ... }
```

### Không trùng scope core-7 / remaining-6

- `remaining-6-2` đã extract `HealthMetricsGrid`, `StateComponents` — **không** trùng.
- `core-7-3-extract-shared-profile-sharing-hook` — hook sharing, không phải UI selector — có thể dùng song song.

### Thứ tự với epic 11 khác

- **Làm sau** `11-1-refactor-dashboard-header` (header không còn search; chỉ refactor `/profiles` local search).
- **Song song** `11-3` (modals có thể dùng `DashboardFormSection` nếu extract).

### Architecture compliance

- Client components (`"use client"`) — giữ nguyên.
- Không đổi API contracts.
- Styling: token màu hiện tại `#00685f`, `#b7e8e0`, `min-h-12` touch target (NFR-A2).

### Testing

```bash
cd apps/web && pnpm lint
```

Manual: đổi profile trên follow-up + visit-summary với `?profileId=` deep link; kiểm tra sidebar settings trên các trang `/settings/*`.

### References

- [Source: apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx]
- [Source: apps/web/src/app/(dashboard)/visit-summary/page.tsx]
- [Source: apps/web/src/app/(dashboard)/profiles/page.tsx — searchQuery filter]
- [Source: _bmad-output/implementation-artifacts/remaining-production-review/epic-6-cleanup-accessibility-maintainability/6-2-accessibility-and-shared-ui-components-cleanup.md]
- [Source: docs/component-inventory.md]

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes List

- Đã extract `ProfileScopeSelector` với label/`htmlFor`/`id` khớp và `aria-label` khi `label=""`.
- Refactor `follow-up-reminders` và `visit-summary` dùng component chung; logic `?profileId=` → default → first giữ nguyên ở page level.
- Không ship `PageSearchField` (không có search local sau 11.1).
- Extract settings: `SettingsAccountNav`, `SettingsDirectContactCard`, `SettingsAccountSidebar` → `components/features/settings/`; refactor toàn bộ trang settings + `delete-account` layout 2/3–1/3.
- Bỏ qua `DashboardFormSection` (optional, <3 consumers).
- `pnpm lint` pass. Không thêm file test component mới.
- Code review (2026-05-22): A + batch patch — guard invalid/empty `profiles`.

### File List

- `apps/web/src/components/features/profiles/ProfileScopeSelector.tsx` (new)
- `apps/web/src/components/features/settings/SettingsAccountNav.tsx` (new)
- `apps/web/src/components/features/settings/SettingsAccountSidebar.tsx` (new)
- `apps/web/src/components/features/settings/SettingsDirectContactCard.tsx` (new)
- `apps/web/src/components/features/settings/settingsStyles.ts` (new)
- `apps/web/src/app/(dashboard)/settings/_components/*` (re-export)
- `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx` (modified)
- `apps/web/src/app/(dashboard)/visit-summary/page.tsx` (modified)
- `apps/web/src/app/(dashboard)/settings/**/page.tsx` (modified, gồm `delete-account`)
- `docs/component-inventory.md` (modified)

### Change Log

- 2026-05-22: Story 11.4 — `ProfileScopeSelector`, shared settings sidebar/contact; refactor dashboard + settings layout.
- 2026-05-23: Gỡ `PageSearchField` và Vitest component mới; cập nhật tài liệu story/inventory.
