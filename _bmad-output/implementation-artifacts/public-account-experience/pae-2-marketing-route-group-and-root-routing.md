# Story 2: Marketing Route Group And Root Routing Strategy

Status: backlog

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

- [ ] Add `(marketing)/layout.tsx` — minimal shell (header/footer optional).
- [ ] Move or create public `page.tsx` under marketing group; update root `page.tsx`.
- [ ] Audit auth pages for homepage links (`login`, `verify-email`, etc.).
- [ ] Document policy: authenticated user on `/` — show landing with "Vào ứng dụng" vs redirect (default: landing + CTA).

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

(pending)
