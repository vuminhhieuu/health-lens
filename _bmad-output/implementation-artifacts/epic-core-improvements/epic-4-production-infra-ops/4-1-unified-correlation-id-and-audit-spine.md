# Story 4.1: Unified Correlation ID And Audit Spine

Status: done

## Execution Scope

**Area:** Backend/API, async jobs, audit events, correlation IDs  
**Priority:** P0

## Story

As a compliance and operations owner, I want every user action and background AI/OCR job correlated end-to-end, so that incidents and privacy audits can be reconstructed reliably.

## Acceptance Criteria

1. **Given** request enters API, **When** correlation id exists or not, **Then** system propagates or generates one and echoes it in response metadata.
2. **Given** admin mutation, consent change, share lifecycle, delete request, OCR event, LLM call, or RAG retrieval occurs, **When** event is recorded, **Then** audit row includes actor, action, resource, outcome, correlation id, trace/request id, and safe metadata.
3. **Given** audit/compliance event is written, **When** logs are emitted, **Then** raw OCR text, presigned URL, raw token, auth header, and email/token query params are excluded.
4. **Given** async queue/job processing occurs, **When** downstream logs/events are emitted, **Then** original correlation id is carried through.
5. **Given** retention class is due, **When** purge/anonymize job runs, **Then** retention policy is applied while preserving required evidence.

## Tasks / Subtasks

- [x] Add correlation filter and MDC propagation.
- [x] Add canonical audit event schema/store.
- [x] Add audit publishers for admin/share/delete/consent/OCR/LLM/RAG paths.
- [x] Add redaction utilities and tests.
- [x] Add retention/purge handling.
- [x] Add admin audit UI support for end-to-end trace inspection.
  - [x] Show `correlationId`, `requestId`, `traceId`, and safe `metadataJson` in the audit detail modal.
  - [x] Add `correlationId` search/filter to the admin audit log screen and API query/export flow.
  - [x] Add a trace-focused detail view or grouped section so operators can follow all audit rows for one correlation id in chronological order.
  - [x] Add frontend tests or focused component coverage for rendering/filtering correlation fields.

### Review Findings

- [x] [Review][Patch] OCR failure audit rows are classified as SUCCESS [apps/api/src/main/java/com/healthlens/api/audit/AuditOutcome.java:18]
- [x] [Review][Dismissed] OCR dead-letter transition writes duplicate audit rows [apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java:141] — false positive; focused test verifies one DLQ audit row.
- [x] [Review][Patch] “Xem cùng trace” can preserve unrelated draft filters and hide trace rows [apps/web/src/app/admin/audit-log/page.tsx:1235]
- [x] [Copilot][Patch] V042 outcome backfill did not match canonical `*_FAILED_*` / `*_DEAD_LETTERED` failure classification.
- [x] [Copilot][Patch] `metadata_json` duplicated `new_value_json` for before/after audits instead of remaining a separate safe metadata channel.
- [x] [Copilot][Patch] Removed stale OCR consumer `handleRecord` entry point after correlation-aware processing became canonical.
- [x] [Copilot][Patch] Trimmed correlation id before rendering/applying “Xem cùng trace”.
- [x] [Copilot][Patch] Admin audit DTO/CSV now use persisted `outcome` as source of truth with a legacy fallback.

## Dev Notes

- This file supersedes old story `epic-7/7-6-unified-audit-spine-with-correlation-ids.md`.
- Keep compliance audit separate from product analytics events.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/aspect/AuditableAspect.java`
- `apps/api/src/main/java/com/healthlens/api/service/*`
- `apps/api/src/main/resources/db/migration/`

## References

- Old canonical source: `_bmad-output/implementation-artifacts/epic-7/7-6-unified-audit-spine-with-correlation-ids.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 4.1

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.correlation.CorrelationIdFilterTest' --tests 'com.healthlens.api.audit.AuditRedactorTest' --tests 'com.healthlens.api.audit.AuditEventRecorderTest'`
- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.correlation.CorrelationIdFilterTest' --tests 'com.healthlens.api.audit.*' --tests 'com.healthlens.api.service.DataDeletionServiceTest.executeDataDeletion_wipesAllUserData'`
- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.service.admin.AdminAuditLogServiceTest'`
- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.service.OcrJobConsumerTest' --tests 'com.healthlens.api.service.OcrJobStateServiceTest' --tests 'com.healthlens.api.service.LlmServiceTest' --tests 'com.healthlens.api.service.MetricExplanationRetrievalServiceTest' --tests 'com.healthlens.api.service.DataDeletionServiceTest'`
- `cd apps/api && ./gradlew test`
- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.service.admin.AdminAuditLogServiceTest'`
- `pnpm --filter web test`
- `cd apps/api && ./gradlew test`
- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.service.OcrJobStateServiceTest' --tests 'com.healthlens.api.service.LlmServiceTest' --tests 'com.healthlens.api.service.MetricExplanationRetrievalServiceTest' --tests 'com.healthlens.api.service.HealthRecordServiceTest'`
- `cd apps/api && ./gradlew test`
- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.audit.AuditEventRecorderTest' --tests 'com.healthlens.api.audit.UnifiedAuditLogWriterTest' --tests 'com.healthlens.api.service.admin.AdminAuditLogServiceTest'`
- `cd apps/api && ./gradlew test --tests 'com.healthlens.api.service.OcrJobConsumerTest'`
- `pnpm --filter web test`
- `cd apps/api && ./gradlew test`

### Completion Notes List

- Thêm `CorrelationIdFilter` và `CorrelationContext` để nhận/generate `X-Correlation-Id`, `X-Request-Id`, `X-Trace-Id`, echo qua response headers và đưa vào MDC/ThreadLocal.
- Mở rộng bảng/entity `audit_logs` bằng outcome, correlation/request/trace id và metadata JSON; admin audit DTO/CSV hiển thị các trường mới.
- `AuditEventRecorder` nay redacts metadata trước khi ghi audit; raw OCR text, token/auth header, presigned URL/query token/email bị loại bỏ.
- Bổ sung audit publishers cho OCR job lifecycle, LLM calls, RAG retrieval và completion của right-to-delete purge/anonymize policy.
- OCR stream consumer khôi phục correlation id từ payload khi xử lý async, sau đó cleanup context.
- Full API regression pass; có log warning nền từ `EmailConsumer` trong quá trình shutdown test nhưng Gradle test pass.
- Bổ sung filter `correlationId` cho admin audit API/query/export; khi lọc theo trace, backend trả danh sách theo thứ tự thời gian tăng dần để xem end-to-end.
- Cập nhật Admin Audit Log UI: thêm ô lọc Correlation ID, hiển thị trace ids và metadata an toàn trong detail modal, nút “Xem cùng trace”, badge trace mode, và action/resource labels cho OCR/LLM/RAG.
- Thêm frontend coverage cho helper tạo params/URL và định dạng trace identifiers.
- Code review follow-up: sửa phân loại outcome cho các action failure dạng `*_FAILED_*`/`*_DEAD_LETTERED`, đổi “Xem cùng trace” sang trace-only filters, thêm coverage tương ứng; xác nhận DLQ audit không bị ghi trùng.
- Investigation follow-up: API logs confirmed LLM fallback rows came from provider schema failures; future LLM/RAG/OCR audit rows now carry the triggering user actor when available.
- Copilot review follow-up: đồng bộ migration outcome backfill, tách event metadata khỏi before/after JSON, dùng persisted outcome trong admin DTO/CSV, cleanup OCR consumer entry point cũ, và tránh trace button với correlation id rỗng sau trim.

### File List

- `apps/api/src/main/java/com/healthlens/api/audit/AuditActions.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditEventRecorder.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditOutcome.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditRedactor.java`
- `apps/api/src/main/java/com/healthlens/api/audit/AuditResourceTypes.java`
- `apps/api/src/main/java/com/healthlens/api/audit/UnifiedAuditLogWriter.java`
- `apps/api/src/main/java/com/healthlens/api/correlation/CorrelationContext.java`
- `apps/api/src/main/java/com/healthlens/api/correlation/CorrelationIdFilter.java`
- `apps/api/src/main/java/com/healthlens/api/controller/admin/AdminAuditLogController.java`
- `apps/api/src/main/java/com/healthlens/api/dto/admin/AuditLogEntryDto.java`
- `apps/api/src/main/java/com/healthlens/api/entity/AuditLog.java`
- `apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java`
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobStateService.java`
- `apps/api/src/main/java/com/healthlens/api/service/admin/AdminAuditLogService.java`
- `apps/api/src/main/resources/db/migration/V042__audit_correlation_spine.sql`
- `apps/web/src/app/admin/audit-log/page.tsx`
- `apps/web/src/app/admin/audit-log/page.test.ts`
- `apps/api/src/test/java/com/healthlens/api/audit/AuditEventRecorderTest.java`
- `apps/api/src/test/java/com/healthlens/api/audit/AuditOutcomeTest.java`
- `apps/api/src/test/java/com/healthlens/api/audit/AuditRedactorTest.java`
- `apps/api/src/test/java/com/healthlens/api/audit/UnifiedAuditLogWriterTest.java`
- `apps/api/src/test/java/com/healthlens/api/correlation/CorrelationIdFilterTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/DataDeletionServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/MetricExplanationRetrievalServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrJobConsumerTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrJobStateServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/admin/AdminAuditLogServiceTest.java`

### Change Log

- 2026-05-19: Implemented unified correlation id propagation, canonical audit metadata, redaction, async OCR correlation carry-through, AI/RAG/OCR audit publishers, and retention evidence audit for Story 4.1.
- 2026-05-19: Added follow-up UI task for admin end-to-end trace inspection before story can return to review.
- 2026-05-20: Implemented admin audit trace UI, correlationId filter/query/export flow, chronological trace sorting, and frontend helper tests.
- 2026-05-20: Addressed code review findings, verified focused/full API and web tests, and moved story to done.
- 2026-05-20: Attributed LLM/RAG/OCR audit rows to the triggering user where available after log investigation of anonymous AI/OCR rows.
- 2026-05-20: Addressed Copilot PR #88 comments for migration outcome parity, separate audit metadata, persisted outcome reads, OCR consumer cleanup, and trimmed trace filtering.
