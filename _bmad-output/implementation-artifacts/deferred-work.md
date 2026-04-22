## Deferred from: code review of 1-1-starter-template-setup.md (2026-03-29)

- Bổ sung persistence volumes cho Postgres/MinIO trong compose dev (`docker/docker-compose.dev.yml`) để giảm rủi ro mất dữ liệu local khi recreate container.

## Deferred from: code review of 1-7-groq-api-setup.md (2026-04-03)

- **F6** — Input format injection risk trong `String.format()` tại `LlmService.java:100`. `metricName`, `value`, `unit` dùng trực tiếp trong format string. Nên validate/sanitize input trước khi đưa vào prompt builder khi có user-provided data. (_Low priority, data hiện đến từ trusted source_)
- **F7** — `EmbeddingService.embedBatch()` không validate từng item trong list (null/blank strings). Thêm filter hoặc validation loop trước khi call API. (_Low priority, batch chưa có consumer trong codebase_)
- **F8** — AC-2: Embedding model thực tế khác spec. Spec yêu cầu `groq/embed-multilingual-v3` nhưng Groq không cung cấp embedding API. Quyết định embedding provider chính thức (Jina AI / OpenAI / self-hosted) sẽ được thực hiện trong **Story 1.8 (Qdrant Cloud Setup)** để đảm bảo vector dimensions khớp với Qdrant collection config.

## Deferred from: code review of 3-1-upload-pdf-image-library.md (2026-04-20)

- Schema `health_records` chưa có một số cột trong phần Dev Notes (`exam_date`, `ocr_confidence`) tại `apps/api/src/main/resources/db/migration/V010__create_health_records_table.sql`. Deferred vì hiện tại không chặn AC chính của story upload/confirm/enqueue.
