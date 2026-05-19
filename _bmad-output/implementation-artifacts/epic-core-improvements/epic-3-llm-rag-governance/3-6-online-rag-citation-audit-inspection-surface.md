# Story 3.6: Online RAG Citation Audit Inspection Surface

Status: done

## Execution Scope

**Area:** Online RAG citation inspection, admin audit API/UI, answer-source linkage  
**Priority:** P1, P0 if online evidence becomes user-facing

## Story

As a compliance/admin user, I want online RAG citation metadata to be inspectable from an answer or explanation, so that HealthLens can prove which trusted online source snapshot influenced AI output and whether that source was approved for use.

## Acceptance Criteria

1. **Given** an AI answer/explanation uses or considers trusted online RAG evidence, **When** the response metadata is persisted or inspected, **Then** source URL, publisher, retrievedAt, snapshotHash, reviewStatus, excluded, and cacheHit are available without exposing raw source content by default.
2. **Given** an admin inspects a metric explanation or AI answer in the audit surface, **When** citation metadata exists, **Then** the admin can view the linked online RAG source metadata and distinguish approved, review-required, excluded, and rejected sources.
3. **Given** a source snapshot is stale, rejected, review-required, or excluded, **When** admin/audit view inspects the answer, **Then** the surface shows the review status and does not imply the evidence was approved for user-facing medical guidance.
4. **Given** route/API contracts are added for citation inspection, **When** backend and frontend are built, **Then** `ApiRoutes.java`, `packages/shared/constants/api.ts`, API DTOs, and web API calls remain synchronized.
5. **Given** citation/audit data is exported or listed, **When** filters by answer/record/metric/source/reviewStatus are used, **Then** pagination/export remains bounded and does not leak PHI, raw source snapshots, tokens, or secrets.

## Tasks / Subtasks

- [x] Define answer/explanation citation metadata contract for online RAG.
- [x] Persist or link online RAG source snapshot metadata to the AI answer/explanation context that used or considered it.
- [x] Add admin/audit API read surface for answer-linked online RAG citations.
- [x] Add admin UI inspection affordance or detail panel that displays citation metadata safely.
- [x] Add filters/tests for source URL, publisher, reviewStatus, snapshotHash, and answer/record context.
- [x] Add regression tests proving unreviewed/excluded sources are visible as audit metadata but not treated as approved evidence.

### Review Findings

- [x] [Review][Patch] Citation FK blocks hard purge of soft-deleted health records [apps/api/src/main/resources/db/migration/V038__create_online_rag_answer_citations.sql:3]
- [x] [Review][Patch] Online RAG fetch failures can discard otherwise valid curated Qdrant retrieval [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java:91]
- [x] [Review][Patch] Repeated explanation reads persist duplicate citation rows without idempotency [apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java:497]
- [x] [Review][Patch] Stale online snapshots are not distinguished in the admin citation surface [apps/web/src/app/admin/audit-log/page.tsx:544]
- [x] [Review][Patch] Invalid reviewStatus filters throw enum conversion errors instead of a bounded client error [apps/api/src/main/java/com/healthlens/api/service/admin/AdminOnlineRagCitationService.java:176]

## Dev Notes

- This is the follow-up for the deferred AC4 from Story 3.5 code review.
- Do not implement unrestricted/open web search.
- Do not expose `contentSnapshot` in default admin list/detail responses; citation inspection should show metadata first. If a raw snapshot preview is added later, gate it behind explicit admin permission and redaction rules.
- Treat online content as hostile unless allowlisted, cached, and approved. Admin UI labels must not imply review-required or excluded evidence was used as trusted medical guidance.
- Current Story 3.5 implementation introduced:
  - `OnlineRagSourceSnapshot`
  - `OnlineRagReviewStatus`
  - `OnlineRagSourceSnapshotRepository`
  - `TrustedOnlineRagSourceAdapter.OnlineRagSourceMetadata`
  - `TrustedOnlineRagSourceAdapter.OnlineRagRetrievalResult`
  - `app.ai.online-rag.*` configuration
- Current Metric Explanation flow exposes `MetricExplanationResponse` with `RetrievalTraceResponse`, prompt/model versions, and source string, but it does not yet provide online RAG citation records linked to an answer.
- Existing admin audit patterns:
  - Backend route constants live in `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`.
  - Shared frontend route constants live in `packages/shared/constants/api.ts`.
  - Admin audit controller/service pattern lives in `AdminAuditLogController` and `AdminAuditLogService`.
  - Admin audit UI lives in `apps/web/src/app/admin/audit-log/page.tsx`.
- If adding new database tables, use append-only Flyway migration. Do not modify `V037__create_online_rag_source_snapshots.sql` once committed.
- Prefer a narrow read model or DTO over reusing JPA entities directly in API responses.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `packages/shared/constants/api.ts`
- `apps/api/src/main/java/com/healthlens/api/entity/OnlineRagSourceSnapshot.java`
- `apps/api/src/main/java/com/healthlens/api/repository/OnlineRagSourceSnapshotRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourceAdapter.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/MetricExplanationResponse.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/`
- `apps/api/src/main/java/com/healthlens/api/service/admin/`
- `apps/web/src/app/admin/audit-log/page.tsx`
- `apps/web/src/lib/api/apiClient.ts`
- New Flyway migration if answer-source linkage needs a table.
- New API/web tests for admin citation inspection.

## References

- `_bmad-output/implementation-artifacts/deferred-work.md` entry from Story 3.5 code review.
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-3-llm-rag-governance/3-5-trusted-online-rag-source-adapter-with-citation-and-cache.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-3-llm-rag-governance/3-4-hybrid-curated-plus-internal-live-rag.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Epic 3.
- `docs/project-context.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.MetricExplanationRetrievalServiceTest`
- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.HealthRecordServiceTest --tests com.healthlens.api.service.MetricExplanationRetrievalServiceTest`
- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.admin.AdminOnlineRagCitationServiceTest --tests com.healthlens.api.service.HealthRecordServiceTest --tests com.healthlens.api.service.MetricExplanationRetrievalServiceTest`
- `cd apps/api && ./gradlew test`
- `pnpm --dir packages/shared build` không chạy được vì workspace thiếu `node_modules`/`tsc`.
- `pnpm --dir apps/web test` không chạy được vì workspace thiếu `node_modules`/`vitest`.

### Completion Notes List

- Created by `bmad-create-story` follow-up from deferred AC4 admin/audit inspection finding.
- Đã mở rộng `MetricExplanationResponse` với `onlineRagCitations` chỉ chứa metadata an toàn: source URL, publisher, retrievedAt, snapshotHash, reviewStatus, excluded, cacheHit và các cờ audit liên quan.
- Đã thêm bảng append-only `online_rag_answer_citations` để lưu link giữa health record, metric, answer hash và online RAG source snapshot metadata; không lưu raw `contentSnapshot`.
- Đã nối retrieval với `TrustedOnlineRagSourceAdapter`: approved source mới được đưa vào prompt context, còn review-required/excluded/rejected chỉ được giữ làm audit metadata.
- Đã thêm admin API `/api/v1/admin/online-rag-citations` và `/export`, đồng bộ với shared route constants, hỗ trợ filter theo answer hash, record, metric, source URL, publisher, snapshot hash, reviewStatus và pagination/export bounded.
- Đã thêm panel citation online RAG trên trang admin audit log, hiển thị trạng thái approved/review-required/rejected/excluded rõ ràng và cảnh báo không coi nguồn chưa approved là evidence y khoa đã duyệt.

### File List

- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-3-llm-rag-governance/3-6-online-rag-citation-audit-inspection-surface.md`
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/AdminOnlineRagCitationController.java`
- `apps/api/src/main/java/com/healthlens/api/dto/admin/OnlineRagCitationEntryDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/admin/OnlineRagCitationPageDto.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/MetricExplanationResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/OnlineRagCitationResponse.java`
- `apps/api/src/main/java/com/healthlens/api/entity/OnlineRagAnswerCitation.java`
- `apps/api/src/main/java/com/healthlens/api/repository/OnlineRagAnswerCitationRepository.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`
- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminOnlineRagCitationService.java`
- `apps/api/src/main/java/com/healthlens/api/service/rag/TrustedOnlineRagSourceAdapter.java`
- `apps/api/src/main/resources/db/migration/V038__create_online_rag_answer_citations.sql`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/MetricExplanationRetrievalServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/admin/AdminOnlineRagCitationServiceTest.java`
- `apps/web/src/app/admin/audit-log/page.tsx`
- `packages/shared/constants/api.ts`

### Change Log

- 2026-05-19T15:53:03+07:00 - Created follow-up story for answer-linked online RAG citation audit inspection surface.
- 2026-05-19T16:37:34+07:00 - Implemented online RAG citation response metadata, answer-link persistence, admin API/export, audit UI panel, route synchronization, and regression tests.
