## Deferred from: code review of 1-1-starter-template-setup.md (2026-03-29)

- Bổ sung persistence volumes cho Postgres/MinIO trong compose dev (`docker/docker-compose.dev.yml`) để giảm rủi ro mất dữ liệu local khi recreate container.

## Deferred from: code review of 1-7-groq-api-setup.md (2026-04-03)

- **F6** — Input format injection risk trong `String.format()` tại `LlmService.java:100`. `metricName`, `value`, `unit` dùng trực tiếp trong format string. Nên validate/sanitize input trước khi đưa vào prompt builder khi có user-provided data. (_Low priority, data hiện đến từ trusted source_)
- **F7** — `EmbeddingService.embedBatch()` không validate từng item trong list (null/blank strings). Thêm filter hoặc validation loop trước khi call API. (_Low priority, batch chưa có consumer trong codebase_)
- **F8** — AC-2: Embedding model thực tế khác spec. Spec yêu cầu `groq/embed-multilingual-v3` nhưng Groq không cung cấp embedding API. Quyết định embedding provider chính thức (Jina AI / OpenAI / self-hosted) sẽ được thực hiện trong **Story 1.8 (Qdrant Cloud Setup)** để đảm bảo vector dimensions khớp với Qdrant collection config.

## Deferred from: code review of 3-1-upload-pdf-image-library.md (2026-04-20)

- Schema `health_records` chưa có một số cột trong phần Dev Notes (`exam_date`, `ocr_confidence`) tại `apps/api/src/main/resources/db/migration/V010__create_health_records_table.sql`. Deferred vì hiện tại không chặn AC chính của story upload/confirm/enqueue.

## Deferred from: code review of 6-3-vietnamese-language-normalization-and-message-catalog.md (2026-05-17)

- Cancel-deletion client treats every backend 409 as cancellation success at `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx:148`. Deferred because it appears pre-existing and outside the direct 6.3 diff, but it can mislead users into believing deletion cancellation succeeded when backend refused it.

## Deferred from: code review of 3-5-trusted-online-rag-source-adapter-with-citation-and-cache.md (2026-05-19T14:40:36+07:00)

- AC4 admin/audit inspection surface is ambiguous — Defer admin/audit inspection surface to a follow-up story because this story should finish adapter-level retrieval, citation metadata, persistence, and cache hardening first; answer-linked admin/audit API/UI needs separate scope. Story created: `_bmad-output/implementation-artifacts/epic-core-improvements/epic-3-llm-rag-governance/3-6-online-rag-citation-audit-inspection-surface.md` (`core-3-6-online-rag-citation-audit-inspection-surface`).

## Deferred from: code review of 7-10-unify-email-event-delivery.md (2026-05-20T15:20:00+07:00)

- Sprint status includes unrelated `core-7-9-confirm-and-document-event-driven-architecture` status movement at `_bmad-output/implementation-artifacts/sprint-status.yaml:226`. Deferred because it appears to belong to the prior Story 7.9 workflow rather than the Story 7.10 email delivery implementation.

## Deferred from: code review of pae-2-marketing-route-group-and-root-routing.md (2026-05-20)

- Trùng lặp logic `logout` và query `currentUser` giữa `MarketingHeader` và `(dashboard)/layout` — tech debt nhỏ sau refactor header; có thể gom hook dùng chung sau.
- Story File List chưa phản ánh ~16 file mới/sửa ngoài phạm vi ghi nhận ban đầu — cập nhật khi commit.
