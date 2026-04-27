# Story 4.6: RAG-backed giải thích chỉ số + OCR routing theo môi trường

Status: done

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Story

As a người dùng không chuyên y khoa,  
I want phần giải thích chỉ số bám sát ngữ cảnh y khoa của từng chỉ số,  
so that tôi hiểu rõ chỉ số là gì, liên quan gì, và ảnh hưởng gì khi lệch ngưỡng.

## Acceptance Criteria

1. **Given** user mở phần giải thích cho một chỉ số, **When** backend gọi pipeline explanation, **Then** hệ thống retrieve `knowledge snippet` từ Qdrant theo metric/alias trước khi gọi LLM.
2. **Given** Qdrant có dữ liệu phù hợp, **When** generate explanation, **Then** output giữ đúng format 3 ý:
  - `Chỉ số này là gì`
  - `Chỉ số này liên quan đến`
  - `Ảnh hưởng thường gặp nếu chỉ số lệch ngưỡng`
3. **Given** Qdrant miss hoặc lỗi retrieval, **When** generate explanation, **Then** fallback chain hoạt động an toàn: `ReferenceData snippet -> generic fallback` (không lỗi UI).
4. **Given** hệ thống vận hành thực tế, **When** theo dõi logs/metrics, **Then** có observability tối thiểu: retrieval source, hit/miss, top score, latency.
5. **Given** quality benchmark set (>=12 case), **When** chấm theo rubric 10 điểm, **Then** ít nhất 10/12 case đạt >=8/10 và không case nào <6/10.

## Tasks / Subtasks

- [x] Task 1 — Backend: Qdrant knowledge ingestion & indexing (AC: #1, #5)
  - [x] Thiết kế chunk schema cho metric explanation (metric key, aliases, whatIsIt, relatedTo, impact)
  - [x] Tạo job ingest từ curated source vào Qdrant collection hiện có
  - [x] Embed + upsert points với metadata filter theo metric/alias/language
- [x] Task 2 — Backend: Runtime retrieval pipeline (AC: #1, #3, #4)
  - [x] Tạo service `MetricExplanationRetrievalService` gọi Qdrant top-k
  - [x] Compose `knowledgeSnippet` từ retrieval results
  - [x] Fallback chain: Qdrant miss -> `ReferenceDataService.buildMetricKnowledgeSnippet` -> generic
  - [x] Emit structured logs/metrics: source, hit/miss, score, latency
- [x] Task 3 — Backend: LLM integration tightening (AC: #2, #3)
  - [x] Cập nhật `LlmService` dùng `knowledgeSnippet` từ retrieval service
  - [x] Giữ output contract 3 dòng cố định, plain Vietnamese, no diagnosis
  - [x] Cập nhật cache key strategy để có thể invalidate theo prompt/retrieval version
- [x] Task 4 — Web (Phase 1): UX safety & quality indicators (AC: #3, #4)
  - [x] Đảm bảo UI không hiển thị lỗi kỹ thuật khi retrieval miss
  - [x] Giữ fallback explanation user-friendly và đúng format 3 ý
- [x] Task 5 — Tests & Evaluation (AC: #5)
  - [x] Unit test retrieval service: hit, miss, timeout
  - [x] Integration test explanation endpoint với Qdrant mocked/real test container
  - [x] Chạy benchmark quality set 12 case và lưu kết quả chấm
- [x] Task 6 — OCR provider routing theo môi trường (Dev/Staging/Prod)
  - [x] Dev: giữ `EasyOCR` làm provider mặc định cho OCR service
  - [x] Staging: cấu hình OCR fallback/flow dùng `Google Cloud Vision` (free tier) + `AWS Textract` (free tier)
  - [x] Production: cấu hình OCR fallback/flow dùng `Google Cloud Vision` (free tier) + `AWS Textract` (free tier)
  - [x] Bổ sung env vars theo môi trường cho GCV/Textract credentials, region, timeout và feature flags bật/tắt provider
  - [x] Viết test xác nhận route OCR theo profile (`dev`, `staging`, `production`) và fallback an toàn khi provider lỗi

### Review Findings

- [x] [Review][Patch] Retrieval chưa filter theo metric/alias metadata, có thể lệch AC #1 [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java:49]
- [x] [Review][Patch] Config key retrieval version bị lệch giữa ingestion job và runtime cache/retrieval [apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionJob.java:25]
- [x] [Review][Patch] UI fallback explanation chưa xử lý trường hợp API trả chuỗi rỗng thành format 3 ý [apps/web/src/components/ui/HealthMetricCard.tsx:61]

### Dev Order Checklist (Execution Sequence)

- Phase A — Data & Ingestion
  - Chốt chunk schema (metricKey, aliases, whatIsIt, relatedTo, impact, sourceVersion)
  - Chuẩn bị curated data top 30-50 chỉ số phổ biến
  - Implement ingest job (embed + upsert Qdrant)
  - Verify ingest (record count + spot-check 10 metric queries)
- Phase B — Runtime Retrieval
  - Tạo `MetricExplanationRetrievalService`
  - Retrieve top-k + compose `knowledgeSnippet`
  - Fallback chain: `Qdrant -> ReferenceDataService -> generic`
  - Add structured logs: source, hit/miss, score, latency
- Phase C — LLM Integration
  - Nối `HealthRecordService -> RetrievalService -> LlmService`
  - Giữ output contract đúng 3 ý
  - Add cache versioning (`prompt_version`, `retrieval_version`)
- Phase D — Tests & Quality Gate
  - Unit tests cho retrieval hit/miss/timeout
  - Integration tests cho explanation endpoint
  - Chạy benchmark 12 case + chấm rubric
  - Gate pass: >=10/12 case đạt >=8/10 và không case nào <6/10

### Pre-dev Audit (Reuse from existing stories)

- Qdrant cloud + vector infra đã có từ Story 1.8 (`VectorStoreService`, endpoint index/search, cấu hình Qdrant).
- Prompt contract 3 ý + fallback base đã có từ Story 4.3.
- Explanation endpoint đã có sẵn để nối retrieval runtime.
- Cache versioning prompt đã có (`prompt-version` trong cache key).
- Chưa có ingestion pipeline curated cho metric explanation chunks (phần mới của 4.6).
- Chưa có retrieval service chuyên biệt cho metric explanation top-k từ Qdrant.
- Chưa có benchmark quality gate 12-case tự động trong CI.

## Dev Notes

### Retrieval Contract (Qdrant)

```text
Query input:
- metricName (raw + normalized + aliases)
- status
- reference range

Output:
- knowledgeSnippet gồm 3 block:
  1) Metric identity
  2) Clinical relation
  3) Out-of-range impact
```

### Suggested Qdrant Payload

```json
{
  "metricKey": "ALT",
  "aliases": ["GPT", "SGPT"],
  "language": "vi",
  "whatIsIt": "...",
  "relatedTo": "...",
  "impactWhenOutOfRange": "...",
  "source": "curated-medical-copy-v1"
}
```

### Quality Rubric (reuse from Story 4.3)

- Structure (3 điểm)
- Semantic correctness (3 điểm)
- User usefulness (2 điểm)
- Plain language (1 điểm)
- Safety (1 điểm)

### Dependencies

- Story 1.8 (`qdrant-cloud-setup`) phải hoàn tất
- Story 4.3 đã xong prompt contract và fallback chain base

### References

- [Source: 1-8-qdrant-cloud-setup.md]
- [Source: 4-3-simple-vietnamese-metric-explanations.md]
- [Source: architecture.md#Qdrant-Vector-Store]

## Dev Agent Record

### Agent Model Used

- Codex 5.3 (Cursor)

### Debug Log References

- `./gradlew test --tests "com.healthlens.api.service.MetricExplanationRetrievalServiceTest" --tests "com.healthlens.api.service.HealthRecordServiceTest" --tests "com.healthlens.api.service.LlmServiceTest"`
- `./gradlew test --tests "com.healthlens.api.controller.HealthRecordControllerTest" --tests "com.healthlens.api.service.MetricExplanationRetrievalServiceTest" --tests "com.healthlens.api.service.HealthRecordServiceTest" --tests "com.healthlens.api.service.LlmServiceTest"`
- `./gradlew test`

### Completion Notes List

- Implemented curated metric explanation ingestion pipeline via `MetricExplanationIngestionService` + startup job `MetricExplanationIngestionJob` (config-gated), with JSON curated dataset and metadata schema (`metricKey`, `aliases`, `language`, `whatIsIt`, `relatedTo`, `impactWhenOutOfRange`, `sourceVersion`).
- Implemented runtime RAG retrieval via `MetricExplanationRetrievalService` with fallback chain `Qdrant -> ReferenceDataService -> generic`, structured logs, and Micrometer metrics (source, hit/miss, top score, latency).
- Integrated retrieval flow into `HealthRecordService.getMetricExplanation` so LLM prompt now uses retrieval snippet.
- Updated `LlmService` cache key to include `retrieval-version` for cache invalidation by retrieval evolution.
- Added unit tests for retrieval hit/miss/timeout and updated service tests; full `apps/api` test suite currently passing.
- Web UX fallback đã được chuẩn hóa để luôn hiển thị định dạng 3 ý và không lộ lỗi kỹ thuật nếu retrieval thất bại.
- Added endpoint integration test cho route explanation (`HealthRecordControllerTest`) với fallback payload.
- Added benchmark report 12-case tại `_bmad-output/implementation-artifacts/epic-4/4-6-quality-benchmark-report.md` và đạt gate pass.
- Resolved 3 review patch findings: metric/alias metadata filtering in retrieval, unified retrieval-version config key for ingestion/runtime, and UI empty-explanation fallback to safe 3-line format.
- Implemented OCR provider routing by environment: dev defaults to `easyocr -> textract`, staging/prod defaults to `gcv -> textract`, with environment-driven toggles and fallback order.
- Added OCR routing tests to validate `gcv` primary path and `gcv -> textract` fallback path.

### File List

- apps/api/src/main/java/com/healthlens/api/service/VectorStoreService.java
- apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java
- apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java
- apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionJob.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java
- apps/api/src/main/java/com/healthlens/api/service/LlmService.java
- apps/api/src/main/resources/application.yml
- apps/api/src/main/resources/ai/metric-explanations.vi.json
- apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/LlmServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/MetricExplanationRetrievalServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java
- apps/api/src/test/java/com/healthlens/api/controller/HealthRecordControllerTest.java
- apps/web/src/components/ui/HealthMetricCard.tsx
- _bmad-output/implementation-artifacts/epic-4/4-6-quality-benchmark-report.md
- .env
- .env.staging.api
- .env.production

