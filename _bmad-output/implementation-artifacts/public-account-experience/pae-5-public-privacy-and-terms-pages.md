# Story 5: Public Privacy And Terms Pages

Status: done

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P0

## Story

As an unauthenticated user,  
I want to read privacy policy and terms of service,  
so that I can make informed decisions before consenting.

## Acceptance Criteria

1. `/privacy` and `/terms` render without authentication (marketing or root-level routes).
2. Vietnamese static content: data types, purposes, retention, user rights (NĐ 13/2023); medical disclaimer; OCR accuracy limits.
3. Draft banner if legal text not final: "Bản nháp — đang cập nhật".
4. Linked from auth footers after `remaining-2-6` uses these routes (coordinate: implement pages **before** closing 2.6).
5. Included in `pae-4` sitemap.

## Tasks / Subtasks

- [x] `apps/web/src/app/(marketing)/privacy/page.tsx` and `terms/page.tsx` (or `/privacy` at app root outside dashboard).
- [x] Content source: markdown in `content/legal/` or inline — keep maintainable.
- [x] Align sections with `ConsentModal` copy themes (no contradictory promises).
- [x] Remove empty `apps/web/src/app/terms/` directory stub if present; replace with real `page.tsx`.
- [x] Tests: routes 200, no auth redirect.

### Review Findings

- [x] [Review][Patch] Test AC “routes 200” chưa được xác minh trực tiếp [`apps/web/src/app/(marketing)/public-legal-pages.test.ts`] — đã bổ sung assert sitemap + routable pages + smoke script.

## Dev Notes

- **Supersedes** duplicating full page creation inside `remaining-2-6` — 2.6 should only wire links + forgot-password UX.
- Epic 1.5 consent story references privacy links.

### Likely Files

- `apps/web/src/app/(marketing)/privacy/page.tsx`
- `apps/web/src/app/(marketing)/terms/page.tsx`
- `content/legal/privacy-vi.md`, `terms-vi.md` (optional)

### References

- `apps/web/src/components/features/consent/ConsentModal.tsx`
- `remaining-2-6-forgot-password-page-refactor-and-public-auth-link-consistency.md`

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes

- Added maintainable Vietnamese legal copy in `apps/web/src/content/legal/` (privacy + terms) aligned with ConsentModal themes (NĐ 13/2023, AES-256, no third-party sharing without consent, medical disclaimer, OCR limits).
- `LegalPage` + `LegalDraftBanner` render marketing routes without dashboard auth gate; draft banner shown.
- Vitest: `public-legal-pages.test.ts` verifies marketing placement, no auth bootstrap, required legal topics.

### File List

- `apps/web/src/content/legal/types.ts`
- `apps/web/src/content/legal/privacy-vi.ts`
- `apps/web/src/content/legal/terms-vi.ts`
- `apps/web/src/components/marketing/LegalDraftBanner.tsx`
- `apps/web/src/components/marketing/LegalPage.tsx`
- `apps/web/src/app/(marketing)/privacy/page.tsx`
- `apps/web/src/app/(marketing)/terms/page.tsx`
- `apps/web/src/app/(marketing)/public-legal-pages.test.ts`

### Change Log

- 2026-05-21: Implemented public privacy/terms pages with draft banner and tests.
