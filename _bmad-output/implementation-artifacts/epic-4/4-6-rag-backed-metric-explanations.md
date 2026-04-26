# Story 4.6: RAG-backed giải thích chỉ số với Qdrant

Status: ready-for-dev

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

- Task 1 — Backend: Qdrant knowledge ingestion & indexing (AC: #1, #5)
  - Thiết kế chunk schema cho metric explanation (metric key, aliases, whatIsIt, relatedTo, impact)
  - Tạo job ingest từ curated source vào Qdrant collection hiện có
  - Embed + upsert points với metadata filter theo metric/alias/language
- Task 2 — Backend: Runtime retrieval pipeline (AC: #1, #3, #4)
  - Tạo service `MetricExplanationRetrievalService` gọi Qdrant top-k
  - Compose `knowledgeSnippet` từ retrieval results
  - Fallback chain: Qdrant miss -> `ReferenceDataService.buildMetricKnowledgeSnippet` -> generic
  - Emit structured logs/metrics: source, hit/miss, score, latency
- Task 3 — Backend: LLM integration tightening (AC: #2, #3)
  - Cập nhật `LlmService` dùng `knowledgeSnippet` từ retrieval service
  - Giữ output contract 3 dòng cố định, plain Vietnamese, no diagnosis
  - Cập nhật cache key strategy để có thể invalidate theo prompt/retrieval version
- Task 4 — Web (Phase 1): UX safety & quality indicators (AC: #3, #4)
  - Đảm bảo UI không hiển thị lỗi kỹ thuật khi retrieval miss
  - Giữ fallback explanation user-friendly và đúng format 3 ý
- Task 5 — Tests & Evaluation (AC: #5)
  - Unit test retrieval service: hit, miss, timeout
  - Integration test explanation endpoint với Qdrant mocked/real test container
  - Chạy benchmark quality set 12 case và lưu kết quả chấm

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

- [x] Qdrant cloud + vector infra đã có từ Story 1.8 (`VectorStoreService`, endpoint index/search, cấu hình Qdrant).
- [x] Prompt contract 3 ý + fallback base đã có từ Story 4.3.
- [x] Explanation endpoint đã có sẵn để nối retrieval runtime.
- [x] Cache versioning prompt đã có (`prompt-version` trong cache key).
- [ ] Chưa có ingestion pipeline curated cho metric explanation chunks (phần mới của 4.6).
- [ ] Chưa có retrieval service chuyên biệt cho metric explanation top-k từ Qdrant.
- [ ] Chưa có benchmark quality gate 12-case tự động trong CI.

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

*[To be filled by dev agent]*

### Debug Log References

### Completion Notes List

### File List

