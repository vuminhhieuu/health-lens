# Story 2: Marketing Route Group And Root Routing Strategy

Status: done

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1

## Story

As a visitor,  
I want `/` to show public content without being forced through login,  
so that the marketing site and SEO pages are reachable while `/home` remains the signed-in dashboard.

## Acceptance Criteria

1. Route group `(marketing)` (or equivalent) hosts public pages without dashboard auth guard.
2. `apps/web/src/app/page.tsx` renders landing (or imports marketing home) — **no** `redirect("/home")` for anonymous users.
3. `/home` keeps existing `(dashboard)` auth guard → login when unauthenticated.
4. Auth pages: "Quay lại trang chủ" links to `/`, not `/home`.
5. Root `layout.tsx`: `lang="vi"`, remove Create Next App default title/description.

## Tasks / Subtasks

- [x] Add `(marketing)/layout.tsx` — minimal shell (header/footer optional).
- [x] Move or create public `page.tsx` under marketing group; update root `page.tsx`.
- [x] Audit auth pages for homepage links (`login`, `verify-email`, etc.).
- [x] Document policy: authenticated user on `/` — show unified landing (no separate auth variant); navigation via header, no inline CTA buttons.

### Review Findings

- [x] [Review][Decision] Marketing route group is present but does not own any route — Resolved by moving the root landing route into `apps/web/src/app/(marketing)/page.tsx` and deleting `apps/web/src/app/page.tsx`, so `(marketing)/layout.tsx` now owns the public `/` route. Evidence: `apps/web/src/app/(marketing)/page.tsx:1`, `apps/web/src/app/(marketing)/layout.tsx:1`.

- [x] [Review][Decision] Icon Trợ giúp trên `AuthenticatedTopHeader` trỏ `/questions` (FAQ công khai) thay vì `/huong-dan` (hướng dẫn trong app). — Resolved: chọn A, header authenticated trỏ `/huong-dan`.

- [x] [Review][Decision] Task ghi chính sách "landing + CTA Vào ứng dụng" nhưng `MarketingHome` hiện không có CTA nào. — Resolved: chọn A, cập nhật story/spec — landing thống nhất, điều hướng qua header.

- [x] [Review][Patch] Flash header guest khi user đã đăng nhập trong lúc `useAuthBootstrap` chạy [`MarketingHeader.tsx:60`]
- [x] [Review][Patch] Test consent thiếu assert `/questions` dù `ConsentModal` đã loại trừ route này [`routing-policy.test.ts:40`]
- [x] [Review][Patch] `MarketingArticle.tsx` không được import — dead code [`MarketingArticle.tsx:1`]
- [x] [Review][Patch] Kích thước brand "HealthLens" không đồng bộ: guest header `text-2xl`, authenticated header `text-xl` [`AuthenticatedTopHeader.tsx:48`]
- [x] [Review][Patch] `MarketingHome` vẫn dùng "PDF" thay vì tiếng Việt thuần theo chuẩn các trang public khác [`MarketingHome.tsx:8`]

- [x] [Review][Defer] Trùng lặp logic `logout` và query `currentUser` giữa `MarketingHeader` và `(dashboard)/layout` — deferred, tech debt nhỏ do refactor header [`MarketingHeader.tsx:24`]
- [x] [Review][Defer] Story File List chưa phản ánh ~16 file mới/sửa ngoài phạm vi ghi nhận ban đầu — deferred, cập nhật khi commit [`pae-2-marketing-route-group-and-root-routing.md:81`]

## Dev Notes

- Depends on `pae-3` for full landing content; this story can ship minimal placeholder hero if needed.
- `ConsentModal` must not show on public marketing routes (verify `ConsentModal` path exclusions).

### Likely Files

- `apps/web/src/app/page.tsx`
- `apps/web/src/app/(marketing)/layout.tsx`
- `apps/web/src/app/layout.tsx`
- `apps/web/src/app/(auth)/**/page.tsx` (homepage links)

### References

- `_bmad-output/planning-artifacts/architecture.md` (landing SSR)
- `pae-3-public-landing-page-ssr.md`

## Dev Agent Record

### Agent Model Used

Codex GPT-5

### Implementation Plan

- Dùng `apps/web/src/app/(marketing)/page.tsx` để render marketing home thay vì redirect `/home`.
- Thêm route group shell `(marketing)/layout.tsx` và component marketing home dùng lại từ root page.
- Giữ `/home` dưới `(dashboard)/layout.tsx`, không thay đổi auth guard hiện có.
- Bổ sung route policy test để khóa hành vi root public, auth home links, metadata và consent exclusion.

### Debug Log

- RED: `pnpm exec vitest run src/app/routing-policy.test.ts` fail 4/4 vì root còn `redirect("/home")`, thiếu `(marketing)/layout.tsx`, metadata còn Create Next App và consent chưa loại trừ route public.
- GREEN/REFACTOR: `pnpm test` pass lint và 5 Vitest tests.
- REVIEW PATCH: Chuyển root landing route từ `apps/web/src/app/page.tsx` sang `apps/web/src/app/(marketing)/page.tsx` để route group `(marketing)` thật sự host `/`.
- `pnpm exec tsc --noEmit` bị chặn bởi lỗi TypeScript/phụ thuộc hiện có ở chart/toast admin (`recharts`, `@radix-ui/react-toast`, implicit any), không phát sinh từ các file story này.
- `pnpm build` không chạy được do Node hiện tại là `18.16.0`, trong khi Next.js 16 yêu cầu `>=20.9.0`.
- `pnpm dev` cũng không khởi động được vì cùng lý do Node version.

### Completion Notes

- `/` hiện render landing công khai qua `(marketing)/page.tsx` và `MarketingHome` — nội dung thống nhất cho cả trạng thái đã/chưa đăng nhập; điều hướng qua header (không có CTA inline).
- `(dashboard)` auth guard vẫn bảo vệ `/home`; không thay đổi logic redirect login hiện có.
- Auth pages có text "Quay lại trang chủ" được audit bằng test để đảm bảo trỏ `/`, không trỏ `/home`.
- Root layout đã đổi `lang="vi"` và metadata HealthLens, bỏ title/description mặc định của Create Next App.
- `ConsentModal` ẩn trên `/`, `/privacy`, `/terms` để không hiện trên các route marketing/public.

### File List

- apps/web/src/app/(marketing)/page.tsx
- apps/web/src/app/(marketing)/MarketingHome.tsx
- apps/web/src/app/(marketing)/layout.tsx
- apps/web/src/app/page.tsx (deleted)
- apps/web/src/app/layout.tsx
- apps/web/src/app/routing-policy.test.ts
- apps/web/src/components/features/consent/ConsentModal.tsx
- _bmad-output/implementation-artifacts/public-account-experience/pae-2-marketing-route-group-and-root-routing.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-05-20: Implemented public root marketing routing policy and consent/auth link guardrail tests.
- 2026-05-20: Resolved review finding by making `(marketing)` the real App Router owner for `/`.
