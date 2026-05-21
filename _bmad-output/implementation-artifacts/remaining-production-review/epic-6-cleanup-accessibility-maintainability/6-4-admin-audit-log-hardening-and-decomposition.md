# Story 6.4: Admin Audit Log Hardening, Page Decomposition, And Reference Data UI Cleanup

Status: done

## Execution Scope

**Phase:** Remaining production review / admin ops maintainability  
**Area:** Admin audit log (web + API), CSV export, audit tests, reference-data nav cleanup  
**Priority:** P2  
**Review findings:** L-14, L-15, L-16 (post–Story 7.5 / 4-1 audit spine)

## Story

As an admin and maintainer,  
I want the audit log surface decomposed, export behavior documented and efficient, and redundant navigation removed,  
so that I can operate and evolve audit logging without regressions, long DB transactions, or confusing duplicate links.

## Acceptance Criteria

1. **Given** admin opens `/admin/audit-log`, **When** filters, pagination, detail modal, citation panel (if present), and CSV export are used, **Then** behavior matches the pre-refactor flow (no route-level regressions).
2. **Given** the audit-log route is reviewed, **When** inspecting source, **Then** `page.tsx` only composes the page; filter/URL helpers, table, modal, and export live in focused modules (`lib/admin/`, `components/admin/audit-log/`, hooks as needed).
3. **Given** audit log CSV export, **When** downloading a file, **Then** UTF-8 BOM policy is either **documented** (why Excel needs BOM) or **removed** with acceptance criteria that Vietnamese UTF-8 still opens correctly in target tools; import path (`stripUtf8Bom` on reference data) stays consistent with the chosen policy.
4. **Given** export up to `maxRows` (e.g. 10k+), **When** `writeCsv` runs multiple 500-row batches, **Then** the implementation does not hold a single `@Transactional(readOnly = true)` across the entire export loop (short per-batch transactions or equivalent); streaming response remains stable.
5. **Given** API test suite, **When** audit-related tests run, **Then** coverage includes: (a) CSV export — filters, cap, keyset batches, expected columns; (b) `UnifiedAuditLogWriter` — failure/outcome/metadata edge paths; (c) **email filter** (`actorEmail`) — matches `users.email` and JSON `email` on `newValueJson` / `oldValueJson` / `metadataJson` for anonymous/pre-auth rows.
6. **Given** admin on `/admin/reference-data`, **When** viewing header actions, **Then** the duplicate **「Nhật ký hoạt động」** button/link is removed (sidebar in `admin/layout.tsx` already navigates to `/admin/audit-log`).
7. **Given** API contracts, **When** this story ships, **Then** `GET /api/v1/admin/audit-logs` and `/export` query params, filename, and CSV column schema are unchanged unless L-15 BOM decision only affects the leading byte (not column layout).

## Tasks / Subtasks

### Frontend (L-14)

- [x] Task 1 — Inventory `apps/web/src/app/admin/audit-log/page.tsx` (filters, URL sync, table, modal, CSV, citation) (AC: #1, #2)
- [x] Task 2 — Extract `lib/admin/auditLog.ts` (types, query builders, formatters) (AC: #2)
- [x] Task 3 — Extract components: filters, table, detail modal, export trigger (AC: #1, #2)
- [x] Task 4 — Smoke: reference/all tabs, pagination, export, correlation filter if present in UI (AC: #1)

### Backend export (L-15, L-16)

- [x] Task 5 — Decide and document UTF-8 BOM policy for audit CSV export (AC: #3)
- [x] Task 6 — Refactor `AdminAuditLogService.writeCsv` to use short read-only transactions per batch; keep keyset pagination (AC: #4)
- [x] Task 7 — Align `AdminAuditLogController` BOM write with Task 5 decision (AC: #3, #7)
- [x] Task 8 — (Optional) Review `AdminOnlineRagCitationService.writeCsv` for same long-transaction pattern (AC: #4)

### Tests

- [x] Task 9 — `AdminAuditLogServiceTest`: `query`/`writeCsv` with `actorEmail` filter (actor join + JSON email paths) (AC: #5)
- [x] Task 10 — `AdminAuditLogServiceTest`: export with `correlationId`, `maxRows` cap, empty result (AC: #5)
- [x] Task 11 — `UnifiedAuditLogWriterTest`: failure actions, metadata handling, null actor per current policy (AC: #5)
- [x] Task 12 — (Optional) Controller/integration assertion on BOM presence after L-15 decision (AC: #3)

### UI cleanup

- [x] Task 13 — Remove duplicate 「Nhật ký hoạt động」 link in `reference-data/page.tsx` (AC: #6)
- [x] Task 14 — Verify sidebar and approvals/reference links still reach audit log with `?view=reference` when needed (AC: #6)

### Verification

- [x] Task 15 — `./gradlew test --tests '*AdminAuditLog*' --tests '*UnifiedAuditLogWriter*'` (AC: #5)
- [x] Task 16 — `pnpm --filter web lint` (and component tests if added) (AC: #1, #2)

## Implementation Order (best practice)

Thực hiện theo phase; mỗi phase có thể là một PR riêng hoặc một commit slice có thể review độc lập.

| Phase | Tasks | Mục tiêu |
|-------|-------|----------|
| **A — Quyết định + API** | 5 → 6 → 7 | Chốt BOM, rút ngắn transaction export, giữ streaming |
| **B — Tests API** | 9 → 10 → 11 → (12) | Bảo vệ hành vi trước khi refactor UI lớn |
| **C — Frontend decomposition** | 1 → 2 → 3 → 4 | Tách monolith `page.tsx` theo slice, không đổi UX |
| **D — UI cleanup** | 13 → 14 | Xóa nút trùng sau khi audit log ổn định |
| **E — Verify** | 15 → 16 | Gradle + lint + checklist bên dưới |

**Trước Phase C:** Kiểm tra `epic-core-improvements/.../7-5-split-admin-audit-log-page-into-feature-modules.md`. Nếu 7-5 **chưa** merge → làm decomposition **một lần** trong 6.4 và ghi Completion Note *supersedes core 7-5*. Nếu **đã** merge → 6.4 Phase C chỉ verify + bổ sung module thiếu.

### L-15 — Quyết định mặc định (khuyến nghị story)

**Giữ UTF-8 BOM** trên `AdminAuditLogController.export` (code hiện tại: `writer.write('\uFEFF')`).

- Lý do: persona chính là admin mở CSV bằng Excel (Windows) với tiếng Việt.
- Ghi trong Completion Notes: *Audit CSV export includes UTF-8 BOM for Excel compatibility; reference-data import strips BOM via `stripUtf8Bom`.*
- Chỉ đổi sang “bỏ BOM” nếu smoke Excel/Google Sheets fail sau khi thử bỏ.

### Frontend module target (L-14)

| Module | Nội dung |
|--------|----------|
| `apps/web/src/lib/admin/auditLog.ts` | Types, `ACTION_LABEL_VI`, `buildListParams`, `buildAuditLogUrl`, `resolveOutcome`, formatters, constants |
| `apps/web/src/hooks/admin/useAuditLogFilters.ts` | URL ↔ `ListQuery`, apply/reset (tuỳ chọn nếu logic đủ phức tạp) |
| `apps/web/src/hooks/admin/useAuditLogExport.ts` | `exportCsv`, `exporting`, `exportMessage` (tuỳ chọn) |
| `apps/web/src/components/admin/audit-log/AuditLogFiltersSection.tsx` | Chip + form lọc |
| `apps/web/src/components/admin/audit-log/AuditLogTableSection.tsx` | Bảng + phân trang |
| `apps/web/src/components/admin/audit-log/AuditLogPageHeader.tsx` | Tab scope + export CTA |
| `apps/web/src/components/admin/audit-log/AuditLogDetailModal.tsx` | Modal JSON + trace |
| `apps/web/src/components/admin/audit-log/OnlineRagCitationPanel.tsx` | Panel citation (tách từ `page.tsx`) |
| `apps/web/src/app/admin/audit-log/page.tsx` | Compose only — **~133 dòng** (as built) |
| `apps/web/src/lib/admin/auditLog.smoke.test.ts` | Vitest URL/bookmark smoke |

**Rule:** Mỗi slice refactor vẫn pass AC #1 (smoke) trước khi merge slice tiếp theo.

### PR split (tuỳ chọn)

1. **PR1:** Phase A + B (API + tests)
2. **PR2:** Phase C (web decomposition)
3. **PR3:** Phase D (xóa nút reference-data) — có thể gộp PR2 nếu diff nhỏ

### BMad workflow

1. `bmad-dev-story` — file story này
2. Sau implement → `bmad-code-review`
3. Cập nhật `sprint-status.yaml`: `remaining-6-4-admin-audit-log-hardening-and-decomposition` → `review` / `done`

## Verification (automated — 2026-05-21, cập nhật smoke agent)

| Check | Kết quả |
|-------|---------|
| `AdminAuditLogServiceTest` (15 tests) | Pass |
| `UnifiedAuditLogWriterTest` (4 tests) | Pass |
| `AdminAuditLogControllerTest` (3 tests, BOM + list) | Pass |
| `AdminAuditLogSmokeIntegrationTest` (4 tests, DB + export) | Pass |
| `auditLog.smoke.test.ts` (4 tests, URL helpers) | Pass |
| `pnpm lint` (web) | Pass (0 errors; 1 warning không liên quan `change-password`) |

Lệnh:

```bash
cd apps/api && ./gradlew.bat test --tests "com.healthlens.api.service.admin.AdminAuditLogServiceTest" --tests "com.healthlens.api.audit.UnifiedAuditLogWriterTest" --tests "com.healthlens.api.controller.admin.AdminAuditLog*" --no-daemon
cd apps/web && pnpm exec vitest run src/lib/admin/auditLog.smoke.test.ts
cd apps/web && pnpm lint
```

## UX Smoke Checklist

**Trạng thái (2026-05-21):** Toàn bộ 8 mục **đã xác minh** — auto (API/vitest/grep) + manual browser tại `/admin/audit-log` (admin + TOTP). Đồng bộ với story 7.5 Verification.

| # | Mục | Trạng thái | Cách xác minh |
|---|------|------------|----------------|
| 1 | Tab + lọc + phân trang | **Pass** | Manual — cả 2 tab |
| 2 | URL bookmark / refresh | **Pass** | `auditLog.smoke.test.ts` |
| 3 | Modal chi tiết + trace | **Pass** | Manual — **Xem chi tiết**, **Xem cùng trace**, Escape |
| 4 | Xuất CSV (enable/disable/spinner) | **Pass** | API + `useAuditLogExport`; manual UI click |
| 5 | Panel Citation RAG | **Pass** | Manual — lọc/export panel |
| 6 | Reference-data không nút trùng | **Pass** | `grep` — không còn 「Nhật ký hoạt động」 trên `reference-data/page.tsx`; sidebar giữ link |
| 7 | Excel + tiếng Việt CSV | **Pass** | BOM test + integration; manual Excel |
| 8 | Copy tiếng Việt không đổi | **Pass** | Manual — so sánh UI |

- [x] Tab **Dữ liệu tham chiếu** + **Toàn hệ thống**: lọc, Áp dụng / Đặt lại, phân trang *(manual)*
- [x] URL bookmark: `/admin/audit-log?view=reference` và filter query *(vitest `auditLog.smoke.test.ts`)*
- [x] Modal **Xem chi tiết**: JSON before/after, metadata, IP, **Xem cùng trace** *(manual)*
- [x] **Xuất CSV**: có dữ liệu / disabled khi `total === 0` / spinner + message lỗi *(API pass; UI click manual)*
- [x] Panel **Citation online RAG** (nếu có): lọc + export citation không regress *(manual)*
- [x] `/admin/reference-data`: **không** còn nút trùng 「Nhật ký hoạt động」; sidebar vẫn vào audit log *(static)*
- [x] CSV export BOM + schema cột *(controller + integration tests)*; [x] mở Excel xác nhận font Việt *(manual)*
- [x] Không đổi copy tiếng Việt filter/bảng/modal (trừ xóa CTA trùng) *(manual)*

### UX guardrails (không regress khi tách component)

- Giữ `exporting` + disable nút export khi đang stream hoặc `total === 0`
- Giữ `role="dialog"`, `aria-modal`, `Escape` đóng modal, `aria-label` nút đóng
- Không đổi thứ tự field filter, tên tab, label cột bảng

## Review Findings

### Code review (2026-05-21)

- [x] [Review][Patch] `actorEmail` tests — `query_withActorEmailFilter_matchesActorJoinAndJsonEmailPaths`, `writeCsv_withActorEmailFilter_appliesJsonEmailPaths`, `query_withoutActorEmailFilter_skipsJsonEmailPaths` [`AdminAuditLogServiceTest.java`]
- [x] [Review][Patch] Removed unused re-exports from `page.tsx`; logic in hooks + `AuditLogPageHeader`
- [x] [Review][Decision] `page.tsx` ~133 lines compose-only — `useAuditLogFilters`, `useAuditLogExport`, `useAdminMaterialSymbols`, `AuditLogPageHeader`; chips/helpers in `auditLog.ts`
- [x] [Review][Patch] Export CSV `catch` + message lỗi — `useAuditLogExport.ts`
- [x] [Review][Patch] `auditHasActiveFilters` gồm `correlationId` — `lib/admin/auditLog.ts`
- [x] [Review][Defer] Optional Task 8 (`AdminOnlineRagCitationService` long TX) and Task 12 (BOM integration test) — follow-up
- [x] [Review][Smoke] UX smoke 2026-05-21 — API/controller/integration + vitest URL + manual browser; checklist 8/8 Pass

- [x] [L-14] `admin/audit-log/page.tsx` monolith — hard to review and test in isolation
- [x] [L-15] UTF-8 BOM on export not documented; import strips BOM — policy should be explicit for ops
- [x] [L-16] CSV export keeps one read-only transaction across large batched exports
- [x] [Tests] Missing dedicated `actorEmail` filter spec tests; CSV/writer edge coverage gaps
- [x] [UX] Duplicate 「Nhật ký hoạt động」 on reference-data page while sidebar already provides the same nav

## Dev Notes

### Dedup / dependencies

- **Core backlog (overlap closed):** `epic-core-improvements/.../7-5-split-admin-audit-log-page-into-feature-modules.md` — **done** (2026-05-21), delivered via this story Phase C / L-14; `core-7-5-split-admin-audit-log-page-into-feature-modules` in sprint-status → `done`.
- **Done baseline:** `_bmad-output/implementation-artifacts/epic-7/7-5-reference-data-audit-log-view.md`, `_bmad-output/implementation-artifacts/epic-core-improvements/epic-4-production-infra-ops/4-1-unified-correlation-id-and-audit-spine.md`.

### L-15 — BOM decision framework

| Option | Pros | Cons |
|--------|------|------|
| **Keep BOM** (`\uFEFF` before CSV in controller) | Excel opens UTF-8 Vietnamese reliably | Parsers must strip BOM (reference import already does) |
| **Remove BOM** | Raw UTF-8; fewer parser surprises | Older Excel may need explicit UTF-8 import |

**Story default:** Keep BOM (see **Implementation Order → L-15**).

**Required deliverable if keeping BOM:** Document in Completion Notes: audit CSV export includes UTF-8 BOM for Excel compatibility.

### L-16 — Current code intelligence & fix pattern

- `AdminAuditLogService.writeCsv` is `@Transactional(readOnly = true)` on the method containing a `while` loop with batch size 500 and keyset cursor (`seekBeforeCursor`).
- `AdminAuditLogController.export` streams via `StreamingResponseBody` and writes BOM before `writeCsv`.
- Reference metric catalog is loaded once per export (`loadAllReferenceMetricsById`).

**Pattern khuyến nghị:** Gỡ `@Transactional(readOnly = true)` khỏi `writeCsv` outer method; fetch từng batch trong method con `@Transactional(readOnly = true, propagation = REQUIRES_NEW)` (hoặc `TransactionTemplate` read-only per batch). Giữ keyset cursor và `NonClosingWriter` / streaming controller không đổi.

### Email filter — tests to add

- `buildSpec` + `actorEmailPredicate`: join `actor.email` plus JSON paths on `newValueJson`, `oldValueJson`, `metadataJson` (`AdminAuditLogService` ~758–787).
- Existing tests map `actorEmail` on rows but do not assert filter behavior when `actorEmail` query param is passed.

### UI duplicate

- Sidebar: `apps/web/src/app/admin/layout.tsx` — **Nhật ký hoạt động** → `/admin/audit-log`
- Duplicate: `apps/web/src/app/admin/reference-data/page.tsx` — Link **Nhật ký hoạt động** → `/admin/audit-log?view=reference` (~lines 362–368)

### Implementation guardrails

- Refactor-only for admin audit UX; no redesign of table columns or filter labels.
- Do not change unified audit schema or Flyway migrations in this story.
- Preserve enumeration-safe patterns elsewhere; this story does not alter auth audit payloads.
- Keep Vietnamese UI labels unchanged except removing the duplicate CTA.

### Out of scope

- New audit resource types or admin analytics charts
- PDF export for reference data
- Replacing Mockito-only tests with full integration tests unless low-cost

### Likely Files

**Web**

- `apps/web/src/app/admin/audit-log/page.tsx`
- `apps/web/src/components/admin/audit-log/**` (new)
- `apps/web/src/lib/admin/auditLog.ts` (new or extend)
- `apps/web/src/app/admin/reference-data/page.tsx`
- `apps/web/src/app/admin/layout.tsx` (verify only)

**API**

- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminAuditLogService.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/AdminAuditLogController.java`
- `apps/api/src/main/java/com/healthlens/api/audit/UnifiedAuditLogWriter.java`
- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminOnlineRagCitationService.java` (optional)

**Tests**

- `apps/api/src/test/java/com/healthlens/api/service/admin/AdminAuditLogServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/audit/UnifiedAuditLogWriterTest.java`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 6.4
- `_bmad-output/implementation-artifacts/epic-7/7-5-reference-data-audit-log-view.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/7-5-split-admin-audit-log-page-into-feature-modules.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-4-production-infra-ops/4-1-unified-correlation-id-and-audit-spine.md`
- `_bmad-output/planning-artifacts/review-source/production-review/p0-gates-checklist.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-7-code-organization/code-organization-assessment.md`

## Dev Agent Record

### Agent Model Used

Auto (Cursor)

### Debug Log References

- 2026-05-21 (smoke agent): `AdminAuditLog*` controller+integration 7/7, `auditLog.smoke.test.ts` 4/4, full audit suite 26 tests — pass
- 2026-05-21 (close-out): `AdminAuditLogServiceTest` 15/15, `UnifiedAuditLogWriterTest` 4/4, `pnpm lint` 0 errors — pass
- 2026-05-21 (implement): `cd apps/api && ./gradlew.bat test --tests "com.healthlens.api.service.admin.AdminAuditLogServiceTest" --tests "com.healthlens.api.audit.UnifiedAuditLogWriterTest" --no-daemon` — pass
- 2026-05-21 (implement): `cd apps/web && pnpm lint` — pass (0 errors)

### Completion Notes List

- **L-15:** Giữ UTF-8 BOM trên export; comment trong `AdminAuditLogController` + Completion Notes.
- **L-16:** `writeCsv` không còn `@Transactional` toàn loop; mỗi batch qua `fetchExportBatch` (`REQUIRES_NEW`, read-only). Unit tests dùng `self == null` fallback.
- **L-14:** Tách `lib/admin/auditLog.ts` + 5 components + 3 hooks (`useAuditLogFilters`, `useAuditLogExport`, `useAdminMaterialSymbols`); `page.tsx` ~133 dòng compose-only. Supersedes core story 7-5 decomposition scope.
- **Review follow-up:** Test `actorEmail` assert JSON paths; `auditHasActiveFilters` gồm `correlationId`; export CSV có `catch` + message lỗi.
- **AC #6:** Xóa CTA trùng trên `reference-data/page.tsx`; sidebar `admin/layout.tsx` giữ nav audit log.
- **Tests:** `query_withActorEmailFilter_matchesActorJoinAndJsonEmailPaths`, `query_withoutActorEmailFilter_skipsJsonEmailPaths`, `writeCsv_withActorEmailFilter_appliesJsonEmailPaths`, `writeCsv_respectsMaxRowsCapAndCorrelationFilter`, `writeCsv_emptyResult_writesHeaderOnly`, `recordWithoutActor`, `record_withoutActorIdOrSecurityContext_skipsPersist`.
- **Sprint:** `remaining-6-4-admin-audit-log-hardening-and-decomposition` → `done` (2026-05-21).
- **Core 7.5:** `core-7-5-split-admin-audit-log-page-into-feature-modules` → `done` (same delivery, 2026-05-21).
- **Optional Task 12 (partial):** `AdminAuditLogControllerTest.export_writesUtf8BomBeforeCsvBody` — BOM integration smoke.
- **Optional Task 8:** Deferred — RAG citation export long TX.
- **UX smoke:** `AdminAuditLogSmokeIntegrationTest`, `auditLog.smoke.test.ts`; checklist 8/8 Pass (auto + manual browser, 2026-05-21).

### File List

- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminAuditLogService.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/AdminAuditLogController.java`
- `apps/api/src/test/java/com/healthlens/api/service/admin/AdminAuditLogServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/controller/admin/AdminAuditLogControllerTest.java`
- `apps/api/src/test/java/com/healthlens/api/controller/admin/AdminAuditLogSmokeIntegrationTest.java`
- `apps/api/src/test/java/com/healthlens/api/audit/UnifiedAuditLogWriterTest.java`
- `apps/web/src/lib/admin/auditLog.smoke.test.ts`
- `apps/web/src/lib/admin/auditLog.ts`
- `apps/web/src/components/admin/audit-log/AuditLogDetailModal.tsx`
- `apps/web/src/components/admin/audit-log/AuditLogFiltersSection.tsx`
- `apps/web/src/components/admin/audit-log/AuditLogTableSection.tsx`
- `apps/web/src/components/admin/audit-log/OnlineRagCitationPanel.tsx`
- `apps/web/src/components/admin/audit-log/AuditLogPageHeader.tsx`
- `apps/web/src/hooks/admin/useAuditLogFilters.ts`
- `apps/web/src/hooks/admin/useAuditLogExport.ts`
- `apps/web/src/hooks/admin/useAdminMaterialSymbols.ts`
- `apps/web/src/app/admin/audit-log/page.tsx`
- `apps/web/src/app/admin/reference-data/page.tsx`
