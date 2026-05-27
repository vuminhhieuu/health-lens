# Story 7.7: Activate RAG Corpus Ingestion Approval Pipeline

Status: ready-for-dev

## Story

As an admin/product owner,
I want quy trình ingest, review, approve và rollback RAG corpus hoạt động được qua API/admin workflow,
so that Explain/LLM thực sự retrieve approved Vietnamese medical chunks từ Qdrant thay vì chỉ fallback sang reference-data snippet hoặc generic text.

## Acceptance Criteria

1. **Given** `apps/api/src/main/resources/ai/metric-explanations.vi.json` có curated chunks, **When** admin kích hoạt ingestion với `sourceVersion` mới, **Then** hệ thống upsert chunks vào Qdrant và ghi một `rag_corpus_versions` row ở trạng thái `pending_review` với `sourceVersion`, `chunkCount`, `embeddingModel`, `embeddingDimension`, `reviewer`, `createdAt` và lỗi nếu có.
2. **Given** một corpus version đang `pending_review`, **When** admin approve version đó, **Then** version chuyển sang `approved`, có `approvedAt/effectiveDate`, và `MetricExplanationRetrievalService.retrieve(...)` dùng `activeApprovedVersion()` để query Qdrant với filter `language == 'vi' && sourceVersion == '<approved-version>'`.
3. **Given** chưa có approved corpus version, Qdrant miss hoặc Qdrant lỗi, **When** người dùng mở Explain, **Then** hệ thống fallback an toàn sang `ReferenceDataService.buildMetricKnowledgeSnippet(...)` rồi mới generic; UI không lỗi và audit/trace thể hiện đúng fallback path.
4. **Given** approved corpus version có lỗi vận hành, **When** admin rollback, **Then** version hiện tại bị đánh dấu `rolled_back`, previous approved version trở thành active, và retrieval không dùng version đã rollback.
5. **Given** môi trường embedding/Qdrant bị cấu hình sai, **When** admin chạy ingestion hoặc smoke test, **Then** lỗi hiển thị rõ ràng, không ghi version thành công giả, không expose secret, và có hướng kiểm tra dimension/reindex.
6. **Given** corpus v1 đã được ingest và approved, **When** backend test/smoke test query representative aliases như `Bạch cầu`, `WBC`, `Creatinin`, `GPT`, **Then** retrieval hit source `qdrant`, không rơi về `reference-data` hoặc `generic` cho các chunks đã có.
7. **Given** admin thao tác corpus governance, **When** ingestion/approve/rollback/inspect xảy ra, **Then** thao tác có audit log/admin trace phù hợp với pattern hiện có và chỉ admin đã MFA/session hợp lệ mới truy cập được.

## Tasks / Subtasks

- [ ] Task 1 - Expose admin RAG corpus governance API (AC: #1, #2, #4, #7)
  - [ ] Thêm route constants backend trong `ApiRoutes` và frontend/shared constants trong `packages/shared/constants/api.ts`; giữ đồng bộ theo `docs/project-context.md`.
  - [ ] Tạo controller admin, ví dụ `AdminRagCorpusController`, dưới `apps/api/src/main/java/com/healthlens/api/controller/admin/`.
  - [ ] API tối thiểu: inspect versions, ingest bundled corpus/resource với `sourceVersion`, approve version, rollback active version, và non-destructive smoke query.
  - [ ] Reuse `MetricExplanationIngestionService`, `RagCorpusGovernanceService`, `MetricExplanationRetrievalService`; không tạo pipeline ingestion mới.
  - [ ] Bảo vệ endpoint bằng admin auth/MFA pattern hiện có; không cho user thường gọi.

- [ ] Task 2 - Make ingestion operationally safe and observable (AC: #1, #5, #7)
  - [ ] Trả về `IngestionReport` có chunk count, embedding model/dimension, errors và source version.
  - [ ] Nếu Qdrant/vector store/governance write fail, không ghi version thành công giả.
  - [ ] Không log `EMBEDDING_API_KEY`, `QDRANT_API_KEY`, corpus raw health data, hoặc nội dung nhạy cảm.
  - [ ] Bổ sung audit event hoặc unified admin audit payload cho ingestion/approval/rollback.
  - [ ] Kiểm tra trạng thái hiện tại: `.env` có `APP_AI_EXPLANATION_INGESTION_ENABLED=false`; story này không được chỉ bật env rồi coi là xong.

- [ ] Task 3 - Add admin UI for corpus lifecycle (AC: #1, #2, #4, #5)
  - [ ] Thêm trang admin, ví dụ `apps/web/src/app/admin/rag-corpus/page.tsx` hoặc vị trí nhất quán với admin navigation hiện có.
  - [ ] Hiển thị danh sách versions: source version, status, reviewer, effective date, chunk count, embedding config, created/approved/rollback metadata.
  - [ ] Cho phép admin chạy ingest bundled `metric-explanations.vi.json` với source version nhập tay, approve pending version và rollback active version.
  - [ ] Hiển thị lỗi rõ ràng cho duplicate sourceVersion, Qdrant unavailable, dimension mismatch, empty/invalid corpus.
  - [ ] Dùng `apps/web/src/lib/api/apiClient.ts`; không gọi fetch thô bỏ qua auth/refresh behavior.

- [ ] Task 4 - Activate end-to-end retrieval path with approved corpus (AC: #2, #3, #6)
  - [ ] Đảm bảo retrieval chỉ query Qdrant khi có approved active version.
  - [ ] Đảm bảo filter `language == 'vi' && sourceVersion == '<activeVersion>'` giữ nguyên hoặc được test rõ.
  - [ ] Sau approve v1, smoke query representative aliases phải trả `RetrievalTrace.source == "qdrant"` và `hit == true`.
  - [ ] Fallback path vẫn hoạt động khi không có active version hoặc vector search lỗi.

- [ ] Task 5 - Tests and smoke coverage (AC: #1-#7)
  - [ ] Backend unit/webmvc tests cho admin endpoints: auth required, inspect, ingest, approve, rollback, duplicate version, ingestion failure.
  - [ ] Service tests cho `RagCorpusGovernanceService` và `MetricExplanationIngestionService` nếu thiếu case activation/rollback/failure.
  - [ ] Retrieval tests chứng minh approved version hit Qdrant và no-approved-version fallback reference-data.
  - [ ] Web tests cho admin page nếu project pattern hiện tại có test tương ứng.
  - [ ] Run `cd apps/api && ./gradlew test`; nếu thay shared constants/web UI thì run relevant `pnpm` checks.

- [ ] Task 6 - Documentation and runbook update (AC: #5, #6)
  - [ ] Cập nhật `docs/provider-switching-runbook.md` hoặc thêm `docs/rag-corpus-runbook.md` với flow: configure embedding/Qdrant, ingest, approve, smoke test, rollback.
  - [ ] Ghi rõ `QDRANT_HOST` phải là hostname thuần, `QDRANT_VECTOR_DIMENSION` phải khớp embedding model và collection.
  - [ ] Ghi rõ khi đổi `EMBEDDING_MODEL`, `EMBEDDING_BASE_URL`, hoặc dimension thì phải dùng collection sạch/reindex rồi ingest lại corpus.

## Dev Notes

### Current Runtime Finding

- Runtime hiện tại **chưa active RAG end-to-end**: container API đang có `APP_AI_EXPLANATION_INGESTION_ENABLED=false`.
- DB `rag_corpus_versions` hiện không có row nào, nên `RagCorpusGovernanceService.activeApprovedVersion()` trả empty.
- Vì `MetricExplanationRetrievalService.retrieve(...)` chỉ query Qdrant khi có approved active version, Explain hiện sẽ fallback sang `ReferenceDataService.buildMetricKnowledgeSnippet(...)`, rồi generic nếu miss.
- API health local có readiness `UP`, nhưng Qdrant client log từng báo không lấy được server version. Story cần smoke test Qdrant/embedding thay vì chỉ tin app startup.

### Existing Code To Reuse

- `MetricExplanationIngestionService.ingestWithReport(...)` đã load JSON corpus, tạo Spring AI `Document`, upsert bằng `VectorStoreService.upsertDocuments(...)`, rồi ghi pending version qua governance.
- `MetricExplanationIngestionJob` đã có startup runner nhưng mặc định tắt bằng `app.ai.explanation.ingestion.enabled=false`. Không nên chỉ bật runner mặc định trong dev/prod vì cần review/approve và kiểm soát sourceVersion.
- `RagCorpusGovernanceService` đã có `recordPendingIngestion(...)`, `approveVersion(...)`, `activeApprovedVersion(...)`, `rollbackToPreviousApprovedVersion(...)`, `inspectMetadata()`.
- `MetricExplanationRetrievalService` đã có Qdrant path, fallback path, retrieval trace và audit event `RAG_RETRIEVAL`.
- `VectorStoreService` dùng Spring AI `VectorStore.add(...)` và `similaritySearch(...)`; không tự gọi Qdrant SDK trực tiếp nếu không cần.

### Architecture / Implementation Constraints

- Backend: Java 21, Spring Boot 4.0.3, Spring AI `2.0.0-M4`, Qdrant vector store starter, PostgreSQL/Flyway.
- Web: Next.js admin UI; API calls phải qua `apps/web/src/lib/api/apiClient.ts`.
- Route constants phải đồng bộ giữa `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java` và `packages/shared/constants/api.ts`.
- Flyway migrations append-only nếu cần schema mới; hiện đã có `V035__create_rag_corpus_versions.sql`.
- Không commit `.env`, không expose secrets, không log API keys.
- Không thay đổi numeric reference dataset trong story này; Story 7.6 đã xử lý dataset và RAG corpus source.
- Không mở uncontrolled web RAG. Online RAG/citation flow là scope riêng; story này tập trung curated corpus + Qdrant + governance.

### Suggested API Shape

- `GET /api/v1/admin/rag-corpus/versions` - inspect corpus version metadata.
- `POST /api/v1/admin/rag-corpus/ingestions` - ingest bundled corpus with requested `sourceVersion`.
- `POST /api/v1/admin/rag-corpus/versions/{sourceVersion}/approve` - approve pending version.
- `POST /api/v1/admin/rag-corpus/versions/{sourceVersion}/rollback` - rollback active problematic version to previous approved version.
- `POST /api/v1/admin/rag-corpus/smoke-test` - run a safe representative retrieval check and return trace/source/hit/fallback path.

Developer may adjust names to fit existing route conventions, but must keep shared/backend constants synchronized.

### Test Data

- Bundled corpus source: `apps/api/src/main/resources/ai/metric-explanations.vi.json`.
- Story 7.6 expanded corpus to 36 Priority A chunks.
- Representative smoke queries: `Bạch cầu`, `WBC`, `Creatinin`, `GPT`, `SGPT`, `Na+`, `K+`, `CRP`.

### Previous Story Intelligence

- Story 7.6 is done and added curated reference dataset + `metric-explanations.vi.json` alignment.
- Important 7.6 lesson: tests that mock Qdrant hit do not prove runtime pipeline is active. This story must verify DB governance state plus Qdrant retrieval after approve.
- Do not treat file existence as RAG readiness. Readiness requires: corpus upserted to Qdrant, DB version approved/effective, retrieval trace source `qdrant`.

### References

- [Source: _bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md#Story-3.3-RAG-Corpus-Governance-And-Admin-Review-Workflow]
- [Source: _bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md#Story-3.4-Hybrid-Curated--Internal-Live-RAG]
- [Source: _bmad-output/implementation-artifacts/epic-7/7-6-core-feature-reference-dataset-v1.md]
- [Source: apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionJob.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/RagCorpusGovernanceService.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/VectorStoreService.java]
- [Source: apps/api/src/main/resources/db/migration/V035__create_rag_corpus_versions.sql]
- [Source: apps/api/src/main/resources/application.yml]
- [Source: docs/environment-reference.md]
- [Source: docs/provider-switching-runbook.md]
- [Source: docs/project-context.md]

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

### Completion Notes List

- Ultimate context engine analysis completed - comprehensive developer guide created.

### File List

