# Story 3: Public Landing Page SSR

Status: backlog

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1 | **Depends on:** `pae-2`

## Story

As a prospective user,  
I want a clear Vietnamese landing page explaining HealthLens,  
so that I understand the product before registering.

## Acceptance Criteria

1. Sections: hero, how-it-works (3 steps), trust/compliance (NĐ 13, medical disclaimer), feature highlights, CTAs (Đăng ký / Đăng nhập).
2. Server-rendered (RSC/SSR); no auth required.
3. Mobile-responsive; touch-friendly CTAs.
4. Teal brand aligned with auth pages; no decorative background blobs (per auth hardening guidance).
5. No fake patient metrics or misleading demo health data.

## Tasks / Subtasks

- [ ] Implement `apps/web/src/app/(marketing)/page.tsx` (or root marketing page).
- [ ] Reuse design tokens / Tailwind classes from auth dashboard palette.
- [ ] Link CTAs to `/register`, `/login`.
- [ ] Footer links to `/privacy`, `/terms`, `/support` (routes from `pae-5` + `remaining-2-6`).
- [ ] Optional: snapshot/visual test for critical sections.

## Dev Notes

- PRD: SSR landing + mobile-responsive landing.
- Stitch: Display/hero specs in `STITCH-AI-SPECIFICATIONS.md` (Phase 2+ reference).

### Likely Files

- `apps/web/src/app/(marketing)/page.tsx`
- `apps/web/src/components/marketing/` (optional extract)

## Dev Agent Record

### Agent Model Used

(pending)
