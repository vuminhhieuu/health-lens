# Story 11.4: Component dùng chung cho luồng dashboard lặp lại

Status: ready-for-dev

## Story

As a developer,
I want tái sử dụng component cho chọn hồ sơ, toolbar tìm kiếm và khối form dashboard,
so that UI nhất quán và dễ bảo trì.

## Acceptance Criteria

1. **Given** `follow-up-reminders` và `visit-summary`, **When** refactor xong, **Then** cả hai dùng `ProfileScopeSelector` từ `@/components/features/profiles/ProfileScopeSelector`.
2. **Given** props `profiles`, `value`, `onChange`, `label`, `id`, **When** render, **Then** có `htmlFor`/`id` khớp, `aria-label` khi không có visible label.
3. **Given** URL `?profileId=`, **When** mount page, **Then** selector ưu tiên query → default profile → first profile (giữ logic hiện tại).
4. **Given** `/profiles` page search và header search input (story 11.1), **When** refactor, **Then** dùng `PageSearchField` — controlled component, className override cho rounded-full header vs rounded-xl page.
5. **Given** refactor, **When** `pnpm test` + `pnpm lint`, **Then** pass; không đổi hành vi mutation/query keys.
6. **Given** hoàn thành, **When** cập nhật `docs/component-inventory.md`, **Then** có mục mới với import path và consumers.

## Tasks / Subtasks

- [ ] Task 1 — `ProfileScopeSelector` (AC: #1–#3)
  - [ ] Extract từ `follow-up-reminders/page.tsx` (select block ~lines 398–419)
  - [ ] Props: `profiles: { id, displayName }[]`, `value`, `onChange`, `label?`, `id?`, `disabled?`, `className?`
  - [ ] Export từ `components/features/profiles/`
- [ ] Task 2 — Refactor consumers (AC: #1, #3, #5)
  - [ ] `follow-up-reminders/page.tsx`
  - [ ] `visit-summary/page.tsx`
  - [ ] (Optional) `health-records/page.tsx` nếu có pattern select tương tự
- [ ] Task 3 — `PageSearchField` (AC: #4)
  - [ ] Props: `value`, `onChange`, `placeholder`, `ariaLabel`, `variant: 'header' | 'page'`
  - [ ] Icon Search từ lucide; keyboard accessible
  - [ ] Refactor `profiles/page.tsx` local search
  - [ ] Chuẩn bị cho `AuthenticatedTopHeader` (story 11.1 có thể dùng hoặc mở palette trực tiếp)
- [ ] Task 4 — (Optional P2) `DashboardFormSection` 
  - [ ] Header icon + title + description — extract nếu ≥3 chỗ giống follow-up form header
  - [ ] Chỉ làm nếu diff rõ ràng; không bắt buộc AC
- [ ] Task 5 — Docs & tests (AC: #5, #6)
  - [ ] Vitest: `ProfileScopeSelector.test.tsx` — render options, onChange
  - [ ] Cập nhật `docs/component-inventory.md`

## Dev Notes

### Duplication đã xác định

**Profile select** — gần như copy-paste giữa:
- `apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx`
- `apps/web/src/app/(dashboard)/visit-summary/page.tsx`

Cùng pattern:
- `useSearchParams().get("profileId")`
- `useMemo` chọn default/first
- `<select>` với class `min-h-12 w-full rounded-xl border border-[#b7e8e0]...`

**Search input** — hai kiểu:
- Header: `AuthenticatedTopHeader` — `rounded-full`, `w-64`
- Profiles page: inline search với `Search` icon + state `searchQuery`

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

- **Nên làm trước** `11-1` (header dùng `PageSearchField` hoặc wrapper).
- **Song song** `11-3` (modals có thể dùng `DashboardFormSection` nếu extract).

### Architecture compliance

- Client components (`"use client"`) — giữ nguyên.
- Không đổi API contracts.
- Styling: token màu hiện tại `#00685f`, `#b7e8e0`, `min-h-12` touch target (NFR-A2).

### Testing

```bash
cd apps/web && pnpm test -- ProfileScopeSelector
cd apps/web && pnpm lint && pnpm build
```

Manual: đổi profile trên follow-up + visit-summary với `?profileId=` deep link.

### References

- [Source: apps/web/src/app/(dashboard)/follow-up-reminders/page.tsx]
- [Source: apps/web/src/app/(dashboard)/visit-summary/page.tsx]
- [Source: apps/web/src/app/(dashboard)/profiles/page.tsx — searchQuery filter]
- [Source: _bmad-output/implementation-artifacts/remaining-production-review/epic-6-cleanup-accessibility-maintainability/6-2-accessibility-and-shared-ui-components-cleanup.md]
- [Source: docs/component-inventory.md]

## Dev Agent Record

### Agent Model Used

(pending)

### Completion Notes List

### File List
