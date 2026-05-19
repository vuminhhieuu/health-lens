# Story 5: Public Privacy And Terms Pages

Status: backlog

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

- [ ] `apps/web/src/app/(marketing)/privacy/page.tsx` and `terms/page.tsx` (or `/privacy` at app root outside dashboard).
- [ ] Content source: markdown in `content/legal/` or inline — keep maintainable.
- [ ] Align sections with `ConsentModal` copy themes (no contradictory promises).
- [ ] Remove empty `apps/web/src/app/terms/` directory stub if present; replace with real `page.tsx`.
- [ ] Tests: routes 200, no auth redirect.

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

(pending)
