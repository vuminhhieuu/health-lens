# Story 7.5: Split Admin Audit Log Page Into Feature Modules

Status: done

## Delivery

**Implemented via:** `remaining-production-review/epic-6-cleanup-accessibility-maintainability/6-4-admin-audit-log-hardening-and-decomposition.md` (done, 2026-05-21).

Scope overlap: Phase C / L-14 of 6.4 delivered this story’s frontend decomposition. No separate PR required for 7.5. Do not re-split the audit-log route.

**Not in 7.5 scope (6.4 only):** API export BOM/L-16, `actorEmail` API tests, reference-data duplicate nav.

## Execution Scope

**Area:** Frontend admin audit surface, page decomposition, feature modules  
**Priority:** P2  
**Epic:** core-improvements / epic-7-code-organization

## Story

As a frontend developer, I want the admin audit-log page split into focused modules, so that filters, citation review, CSV export, detail panels, and rendering can evolve without one oversized route file.

## Acceptance Criteria

1. **Given** the admin audit-log page loads, **When** filters, pagination, and detail drill-down are used, **Then** behavior matches the pre-refactor flow. — **Met** (code + 6.4 UX smoke 8/8 Pass, 2026-05-21).
2. **Given** citation review and CSV export are used, **When** the refactor is complete, **Then** those actions still work without route-level regressions. — **Met** (`OnlineRagCitationPanel`, `useAuditLogExport`).
3. **Given** the page source is reviewed, **When** code is inspected, **Then** route composition is separated from feature hooks, helpers, and UI modules. — **Met** (`page.tsx` ~133 lines compose-only).

## Tasks / Subtasks

- [x] Extract audit-log filter and query parameter helpers into a focused `lib` module.
- [x] Extract audit-log table, detail modal, and pagination UI into feature components.
- [x] Extract citation-review panel logic from the route page.
- [x] Keep URL synchronization and export behavior unchanged.
- [x] Preserve existing tests or add focused coverage where needed. — API tests via 6.4; web URL smoke: `auditLog.smoke.test.ts` (4 tests). Component tests not required for 7.5 closure.

## Verification

| Check | Result |
|-------|--------|
| Module layout matches Likely Files | Pass |
| `pnpm lint` (web) | Pass (2026-05-21) |
| Automated API audit tests (via 6.4) | Pass |
| `auditLog.smoke.test.ts` (URL helpers) | Pass (2026-05-21) |
| Manual UX smoke (6.4 checklist) | Pass (8/8, 2026-05-21 — shared with 6.4) |

## Dev Notes

- Refactor-only; admin UX unchanged (6.4 guardrails).
- Route params, search params, and API behavior stable.
- Component names use `*Section` suffix (`AuditLogFiltersSection`, etc.) — equivalent to proposed `AuditLogFilters` / `AuditLogTable` in planning tables.

### Module map (as built)

| Module | Path |
|--------|------|
| Lib | `apps/web/src/lib/admin/auditLog.ts` |
| Hooks | `useAuditLogFilters.ts`, `useAuditLogExport.ts`, `useAdminMaterialSymbols.ts` |
| Components | `AuditLogPageHeader`, `AuditLogFiltersSection`, `AuditLogTableSection`, `AuditLogDetailModal`, `OnlineRagCitationPanel` |
| Route | `apps/web/src/app/admin/audit-log/page.tsx` (compose only) |

## Likely Files

- `apps/web/src/app/admin/audit-log/page.tsx`
- `apps/web/src/components/admin/audit-log/`
- `apps/web/src/hooks/admin/`
- `apps/web/src/lib/admin/auditLog.ts`

## References

- `remaining-production-review/.../6-4-admin-audit-log-hardening-and-decomposition.md` (delivery story)
- `epic-7-issues-and-proposed-stories.md`
- `code-organization-assessment.md`
- `source-code-architecture-review.md`

## Dev Agent Record

### Agent Model Used

Auto (Cursor) — close-out sync 2026-05-21

### Debug Log References

- Delivered in remaining-6-4 implementation; this file updated to `done` without duplicate code work.
- `cd apps/web && pnpm lint` — pass (2026-05-21)
- `AdminAuditLogServiceTest` + `UnifiedAuditLogWriterTest` — pass (2026-05-21, via 6.4)

### Completion Notes List

- All 7.5 tasks satisfied by 6.4 Phase C (L-14): lib + hooks + components + thin `page.tsx`.
- Sprint key `core-7-5-split-admin-audit-log-page-into-feature-modules` set to `done` to match implementation reality.
- UX smoke closed via 6.4 checklist (8/8 Pass). Optional follow-up: web component tests for table/modal if desired later.

### File List

- `apps/web/src/lib/admin/auditLog.smoke.test.ts`
- `apps/web/src/lib/admin/auditLog.ts`
- `apps/web/src/hooks/admin/useAuditLogFilters.ts`
- `apps/web/src/hooks/admin/useAuditLogExport.ts`
- `apps/web/src/hooks/admin/useAdminMaterialSymbols.ts`
- `apps/web/src/components/admin/audit-log/AuditLogPageHeader.tsx`
- `apps/web/src/components/admin/audit-log/AuditLogFiltersSection.tsx`
- `apps/web/src/components/admin/audit-log/AuditLogTableSection.tsx`
- `apps/web/src/components/admin/audit-log/AuditLogDetailModal.tsx`
- `apps/web/src/components/admin/audit-log/OnlineRagCitationPanel.tsx`
- `apps/web/src/app/admin/audit-log/page.tsx`
