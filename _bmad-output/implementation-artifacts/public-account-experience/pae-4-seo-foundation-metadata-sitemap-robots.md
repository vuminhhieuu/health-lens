# Story 4: SEO Foundation — Metadata, Sitemap, Robots

Status: done

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1 | **Depends on:** `pae-2`, `pae-5` (routes exist)

## Story

As a marketing owner,  
I want search engines to index public pages correctly,  
so that HealthLens can be discovered organically.

## Acceptance Criteria

1. `apps/web/src/app/sitemap.ts` includes `/`, `/privacy`, `/terms`, `/help` (`/support` redirects, not indexed).
2. `apps/web/src/app/robots.ts` allows public paths; disallows `/home`, `/admin`, dashboard paths.
3. Landing has `metadata` / `openGraph`: title, description, locale `vi_VN`.
4. Favicon + app icon in `apps/web/public/` (replace Next defaults).
5. Smoke: sitemap URLs return 200 (script or test).

## Tasks / Subtasks

- [x] Add `sitemap.ts`, `robots.ts` per Next.js App Router conventions.
- [x] `generateMetadata` on marketing landing.
- [ ] Add `site.webmanifest` optional P2.
- [x] Update root layout default metadata template for child routes.

## Dev Notes

- `prd-validation-report.md` flagged missing SEO strategy — this story closes that gap for web MVP.
- Do not index authenticated dashboard URLs.

### Likely Files

- `apps/web/src/app/sitemap.ts`
- `apps/web/src/app/robots.ts`
- `apps/web/src/app/(marketing)/page.tsx` (metadata)
- `apps/web/public/favicon.ico`

## Dev Agent Record

### Agent Model Used

Composer

### Implementation Plan

- Centralize site URL, sitemap paths, and robots disallow lists in `src/lib/seo/site.ts`.
- Root layout uses `createRootMetadata()` with `metadataBase`, title template, icons, and default OG `vi_VN`.
- Landing page exports `landingMetadata` with dedicated title/description and Open Graph locale.
- Vitest smoke maps each sitemap path to an App Router `page.tsx` and asserts sitemap/robots generators.

### Completion Notes

- Added `sitemap.ts` and `robots.ts` with shared SEO constants; robots disallows `/home`, `/admin`, and dashboard/auth prefixes.
- Landing `(marketing)/page.tsx` exports Vietnamese Open Graph metadata (`vi_VN`).
- Replaced default Next assets with branded `favicon.ico`, `favicon.svg`, `apple-touch-icon.png`, and `icon.svg` (+ PNG sizes).
- `pnpm test` in `apps/web`: 30 tests passed (including new `seo-foundation.test.ts`).

### File List

- `apps/web/src/lib/seo/site.ts` (new)
- `apps/web/src/lib/seo/metadata.ts` (new)
- `apps/web/src/app/sitemap.ts` (new)
- `apps/web/src/app/robots.ts` (new)
- `apps/web/src/app/seo-foundation.test.ts` (new)
- `apps/web/src/app/layout.tsx`
- `apps/web/src/app/(marketing)/page.tsx`
- `apps/web/src/app/routing-policy.test.ts`
- `apps/web/public/favicon.ico` (new)
- `apps/web/public/favicon.svg` (new)
- `apps/web/public/icon.svg` (new)
- `apps/web/public/apple-touch-icon.png` (new)
- `apps/web/public/icon-192.png` (new)
- `apps/web/public/icon-512.png` (new)
- `apps/web/scripts/smoke-sitemap-urls.mjs` (new)
- `apps/web/.env.example` (new)

### Change Log

- 2026-05-20: SEO foundation — sitemap, robots, landing/root metadata, HealthLens favicon set, vitest smoke.
- 2026-05-20: Code review — sitemap `/help`, smoke script, apple-touch 180px, dev site URL, `.env.example`.

### Review Findings

- [x] [Review][Decision] Sitemap đổi `/support` → `/help`; giữ redirect `/support` → `/help` cho link cũ
- [x] [Review][Patch] AC5 — thêm `scripts/smoke-sitemap-urls.mjs` + test assert script tồn tại
- [x] [Review][Patch] `apple-touch-icon.png` resize 180×180
- [x] [Review][Patch] `getSiteUrl()` dùng `http://localhost:3000` khi `NODE_ENV=development`; thêm `.env.example`
- [x] [Review][Patch] Gộp import `node:fs` trong `seo-foundation.test.ts`
- [x] [Review][Defer] SVG mặc định Next (`next.svg`, `vercel.svg`) vẫn còn trong `public/` — không ảnh hưởng runtime, dọn ở story dọn dẹp assets sau
