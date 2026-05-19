# Story 4: SEO Foundation — Metadata, Sitemap, Robots

Status: backlog

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1 | **Depends on:** `pae-2`, `pae-5` (routes exist)

## Story

As a marketing owner,  
I want search engines to index public pages correctly,  
so that HealthLens can be discovered organically.

## Acceptance Criteria

1. `apps/web/src/app/sitemap.ts` includes `/`, `/privacy`, `/terms`, `/support` (if exists).
2. `apps/web/src/app/robots.ts` allows public paths; disallows `/home`, `/admin`, dashboard paths.
3. Landing has `metadata` / `openGraph`: title, description, locale `vi_VN`.
4. Favicon + app icon in `apps/web/public/` (replace Next defaults).
5. Smoke: sitemap URLs return 200 (script or test).

## Tasks / Subtasks

- [ ] Add `sitemap.ts`, `robots.ts` per Next.js App Router conventions.
- [ ] `generateMetadata` on marketing landing.
- [ ] Add `site.webmanifest` optional P2.
- [ ] Update root layout default metadata template for child routes.

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

(pending)
