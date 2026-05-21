# Epic 7 Code Organization Assessment

Status: assessment  
Date: 2026-05-20 (baseline); **post-6.4 audit-log sync: 2026-05-21**  
Scope: project structure, backend package boundaries, frontend feature organization, shared contracts, and refactor backlog fit.

> **Post-6.4 / core-7-5 (2026-05-21):** `admin/audit-log/page.tsx` decomposed to ~133 lines compose-only + `lib/admin/auditLog.ts`, `hooks/admin/*`, `components/admin/audit-log/*`. Line counts in §1 below marked *(pre-refactor)* where stale.

Related deeper review: `source-code-architecture-review.md`  
Consolidated backlog synthesis: `epic-7-issues-and-proposed-stories.md`

## Summary

The existing four Epic 7 stories are valid and should stay. They address real maintainability pain in the current codebase:

- `7-1` covers the most urgent backend AI/OCR/RAG boundary problem.
- `7-2` covers the largest web page hotspot: the health record review page.
- `7-3` covers duplicated profile sharing mutations across dashboard surfaces.
- `7-4` covers inline email template maintainability.

However, those four stories do not cover all code organization debt visible in the project. The audit found three additional areas worth tracking: admin frontend page decomposition, backend large-domain service decomposition, and shared contract/type organization.

## Current Structure Observed

The repository follows the documented monorepo shape:

- `apps/api`: Spring Boot 4 API on Java 21.
- `apps/web`: Next.js 16 App Router web app.
- `apps/mobile`: Expo mobile app.
- `services/ocr-service`: FastAPI EasyOCR service.
- `packages/shared`: shared TypeScript constants, schemas, and types.
- `docs`: canonical project knowledge.
- `_bmad-output`: planning and implementation artifacts.

The documented source tree remains broadly accurate, but implementation has grown beyond the original flat service/page structure in a few places.

## Backend Findings

### 1. AI/OCR/RAG boundaries are partially extracted but incomplete

Current state:

- `apps/api/src/main/java/com/healthlens/api/service/ocr/` already contains `OcrProvider`, `OcrCapability`, `OcrJob`, and `OcrProviderRegistry`.
- `apps/api/src/main/java/com/healthlens/api/service/rag/` already contains trusted online RAG adapter/client/policy classes.
- But core implementation classes still live in generic `service/`, including:
  - `LlmService.java` - 1,446 lines.
  - `OcrService.java` - 1,081 lines.
  - `EmbeddingService.java`.
  - `VectorStoreService.java`.
  - `MetricExplanationRetrievalService.java`.
  - `MetricExplanationIngestionService.java`.
  - OCR provider adapters like `EasyOcrProviderAdapter`, `PdfTextOcrProvider`, `TextractOcrProvider`, `GoogleCloudVisionOcrProvider`.

Assessment:

`7-1` is necessary and should be executed before deeper AI/RAG changes. It should explicitly move provider adapters and AI service classes out of generic `service/`, not only create package names.

Recommended package map:

```text
com.healthlens.api.ai.chat
com.healthlens.api.ai.embedding
com.healthlens.api.ai.rag
com.healthlens.api.ai.prompt
com.healthlens.api.ocr
com.healthlens.api.ocr.provider
com.healthlens.api.ocr.job
```

Keep this as package refactor only. Do not split methods or change behavior inside `7-1` unless imports require it.

### 2. Large backend services remain outside the current four stories

Largest backend hotspots:

- `HealthRecordService.java` - 1,686 lines.
- `ReferenceDataAdminService.java` - 1,496 lines.
- `LlmService.java` - 1,446 lines.
- `OcrService.java` - 1,081 lines.
- `AdminAuditLogService.java` - 841 lines.
- `AuthService.java` - 620 lines.

Assessment:

`7-1` will improve `LlmService` and `OcrService` location, but it does not solve broad service responsibility size. The two non-AI hotspots most worth tracking are:

- `HealthRecordService`: upload reservation, record access checks, history/detail mapping, OCR completion replay, delete/purge, PDF/recommendation orchestration.
- `ReferenceDataAdminService`: CRUD, import preview/session, change set publication, validation, and range handling.

These should not be folded into `7-1`, because that would turn a safe package-boundary refactor into a behavioral refactor.

### 3. Backend DTO/entity/repository layout is still mostly layer-first

Current state:

- `dto/request`, `dto/response`, `entity`, `repository`, `controller`, and `service` are mostly grouped by technical layer.
- Admin has started to split into `controller/admin`, `dto/admin`, and `service/admin`.

Assessment:

This is acceptable for the current project size, but the API is approaching the point where feature/domain grouping would improve navigation. A full migration from layer-first to feature-first is risky and not recommended as part of Epic 7. Prefer incremental package extraction for the most volatile domains first: AI/OCR/RAG, reference data admin, health records, and sharing.

## Frontend Findings

### 1. Route pages are doing too much

Largest page hotspots:

- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` - 2,446 lines.
- `apps/web/src/app/admin/audit-log/page.tsx` - 1,813 lines *(pre-refactor; **~133** as of 2026-05-21 — story 6.4 / 7.5 done)*.
- `apps/web/src/app/admin/reference-data/approvals/page.tsx` - 1,098 lines.
- `apps/web/src/app/admin/reference-data/page.tsx` - 1,035 lines.
- `apps/web/src/app/(dashboard)/home/page.tsx` - 741 lines.
- `apps/web/src/app/admin/login/page.tsx` - 690 lines.

Assessment:

`7-2` correctly targets the biggest page. But after `7-2`, the next clear frontend organization debt is admin page decomposition. The admin pages combine types, query/mutation logic, filter state, API parameter builders, formatting helpers, modal code, table rendering, and page layout in one file.

### 2. Existing feature component structure is useful but underused

Current state:

- `components/features/health-records`, `components/features/profiles`, `components/features/upload`, and `components/features/consent` exist.
- `hooks/` exists and already contains app-level hooks such as `useAccountDeletion` and `useAuthBootstrap`.
- Many feature-specific query/mutation hooks are still embedded directly in pages.

Assessment:

Epic 7 should reinforce the pattern:

```text
components/features/<feature>/
hooks/use<FeatureAction>.ts
lib/<feature>/
```

The page should coordinate route params, top-level loading/error state, and composition. API calls, mutations, type conversion, and repeated modal/table pieces should move into feature modules.

### 3. Shared UI and design consistency cleanup is visible but should be separate

Many pages repeat similar button, badge, modal, state, and color class patterns. `components/ui/StateComponents.tsx` and `DashboardPageShell.tsx` exist, but admin and dashboard pages still carry one-off UI utilities.

Assessment:

This is real debt, but it is more design-system/component-system work than code organization. Do not mix it into `7-2` or `7-3`. Track separately if visual consistency becomes a priority.

## Shared Contracts Findings

Current state:

- `packages/shared/constants/api.ts` is canonical for frontend route paths.
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java` is canonical for backend route paths.
- Shared schemas currently cover auth/profile/user, while many newer domains use local page/service DTO types.

Assessment:

The non-negotiable route synchronization rule is documented and still important. The next maintainability issue is not route constants, but feature contract drift: admin reference data, health record review, sharing, and audit log types are defined locally in pages or backend DTOs without a shared generated or manually maintained TS contract.

This should be tracked as a lightweight contract organization story, not bundled into current refactors.

## Gaps Outside The Four Existing Stories

### Story 7.5 - Split Admin Audit Log Page Into Feature Modules — **done (2026-05-21)**

Delivered via `remaining-6-4-admin-audit-log-hardening-and-decomposition` (Phase C) and closed in `7-5-split-admin-audit-log-page-into-feature-modules.md`. Citation helpers remain in `lib/admin/auditLog.ts` + `OnlineRagCitationPanel.tsx` (optional future split to `onlineRagCitations.ts` is follow-up only).

*(Pre-refactor rationale retained for history: monolith ~1,813 lines; filters, URL sync, citation panel, CSV export, modal, helpers in one route file.)*

### Recommended Add: Story 7.6 - Split Admin Reference Data Pages Into Feature Modules

Priority: P2  
Area: Frontend admin reference data

Rationale:

`admin/reference-data/page.tsx`, `admin/reference-data/approvals/page.tsx`, and `admin/reference-data/import/page.tsx` repeat admin API/query/mutation/notice patterns and carry large local helper blocks.

Suggested scope:

- Extract reference metric types and payload builders.
- Extract shared admin reference-data query keys.
- Extract metric editor modal and change-set diff panel.
- Keep approval/import behavior unchanged.

### Recommended Add: Story 7.7 - Backend Health Record Service Responsibility Split

Priority: P2, with careful test gate  
Area: Backend health records

Rationale:

`HealthRecordService.java` is the largest backend service and mixes upload lifecycle, access control, history/detail mapping, OCR completion, deletion/purge, PDF/recommendation assembly, and share-aware behavior.

Suggested scope:

- Extract access policy/helper for owner/shared/edit checks.
- Extract upload reservation/format validation helper.
- Extract mapping/assembler code for history/detail responses.
- Preserve public API behavior and audit behavior.

Do this after `7-1` and `7-2`, because it has higher regression risk.

### Recommended Add: Story 7.8 - Shared Frontend API Contract Types For High-Churn Features

Priority: P3  
Area: Shared contracts and frontend type organization

Rationale:

Many page-local types duplicate backend response/request shapes. This is manageable now, but risky for health-record review, reference-data admin, audit logs, and sharing flows.

Suggested scope:

- Move high-churn frontend API types into `packages/shared` or `apps/web/src/types/<feature>.ts`.
- Start with manual organization, not code generation.
- Do not change route behavior.
- Keep `packages/shared/constants/api.ts` and backend `ApiRoutes.java` synchronized.

## Existing Story Adjustments

### 7-1 Backend AI/OCR/RAG Package Boundary Refactor

Keep story. Strengthen acceptance criteria:

- Provider adapters are no longer in generic `service/`.
- Prompt rendering and prompt template loading have a clear `ai.prompt` home.
- Package map is documented in the story completion notes.
- Existing tests under `service`, `service/ocr`, `service/rag`, and `config` still pass after imports move.

### 7-2 Split Health Record Review Page

Keep story. Add explicit non-goals:

- Do not redesign the page.
- Do not change save/retry/share/delete behavior.
- Do not introduce new API contract shapes.

Add suggested extraction targets:

- `components/features/health-records/review/MetricTable.tsx`
- `components/features/health-records/review/RecordMetadataForm.tsx`
- `components/features/health-records/review/OcrFailurePanel.tsx`
- `components/features/health-records/review/RecordShareModal.tsx`
- `hooks/useRecordReviewState.ts`
- `hooks/useRecordReviewSave.ts`

### 7-3 Extract Shared Profile Sharing Hook

Keep story. This remains correctly scoped.

Suggested hook contract:

- centralize invite/update/revoke mutations.
- centralize query invalidation.
- centralize standardized notifications.
- keep page-specific rendering and selected profile state in pages.

### 7-4 Email Template Cleanup

Keep story. This is narrow and valuable.

Suggested adjustment:

- Include `EmailConsumer` only if template variables or event payload handling are touched.
- Keep URL/token generation in existing auth/deletion/share services.
- Add rendering tests for all templates moved out of inline strings.

## Recommended Execution Order

1. `7-1` Backend AI/OCR/RAG package boundary refactor.
2. `7-2` Health record review page split.
3. `7-3` Shared profile sharing hook.
4. `7-4` Email template cleanup.
5. Proposed `7-5` Admin audit log page decomposition.
6. Proposed `7-6` Admin reference data page decomposition.
7. Proposed `7-7` Backend health record service responsibility split.
8. Proposed `7-8` Shared frontend API contract types.

## Do Not Do In Epic 7

- Do not perform a full backend feature-first package migration.
- Do not combine behavior changes with package moves.
- Do not redesign admin/dashboard UI while extracting components.
- Do not introduce generated API clients unless there is a separate architecture decision.
- Do not update sprint status until the user chooses which proposed stories should become official backlog.

## Verification Expectations

For backend refactors:

```bash
cd apps/api && ./gradlew test
```

For web refactors:

```bash
cd apps/web && pnpm test
```

If web has no active test runner coverage for the touched page, add focused component or hook tests only where the extraction changes logic. For purely presentational extraction, manual browser verification can be enough if no behavior moved.
