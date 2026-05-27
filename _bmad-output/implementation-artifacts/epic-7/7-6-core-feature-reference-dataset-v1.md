# Story 7.6: Core Feature Reference Dataset v1

Status: done

## Execution scope

**Phase 1 - Web MVP:** Story thuộc Epic 7, nhưng phạm vi đã được nâng từ "import dataset ban đầu" thành **reference-data quality foundation cho core features**. Không chỉ thêm nhiều dòng CSV. Story này phải bảo đảm OCR, phân loại, Explain/RAG/LLM và Recommendations dùng được dữ liệu tham chiếu có alias, ngưỡng, provenance và coverage đủ rộng.

Dataset `docs/reference-data/initial-reference-ranges.csv` hiện tại chỉ được xem là **MVP/demo smoke seed** để kiểm parser/import. Không được xem là trusted core-feature dataset.

## Story

As an admin/product owner,
I want có Core Feature Reference Dataset v1 đã được kiểm nguồn, có alias, provenance, ngưỡng attention và đồng bộ RAG,
so that OCR review, phân loại chỉ số, Explain/RAG/LLM và khuyến nghị hoạt động nhất quán thay vì rơi về `no_data` hoặc generic fallback.

## Acceptance Criteria

1. **Given** OCR trích xuất tên chỉ số từ phiếu xét nghiệm tiếng Việt/Anh, có viết tắt, có dấu/không dấu hoặc lỗi OCR phổ biến, **When** hệ thống enrich metric không có reference range in trên tài liệu, **Then** `ReferenceDataService.classifyMetric(...)` match được metric bằng canonical name hoặc alias active cho toàn bộ Priority A metrics đã chọn.
2. **Given** dataset Core Feature Reference Dataset v1, **When** import qua admin import workflow, **Then** file import có thể tạo/sửa metric, range và alias; mỗi range có `minValue`, `maxValue`, `attentionMin`, `attentionMax`, `gender`, `minAge`, `maxAge` khi áp dụng.
3. **Given** một giá trị xét nghiệm lệch nhẹ hoặc lệch rõ, **When** phân loại bằng system fallback range, **Then** hệ thống phân biệt đúng `normal`, `attention`, `abnormal`, hoặc `no_data` dựa trên normal interval và attention bounds, không set attention bounds bằng đúng min/max trừ khi đã có lý do được ghi trong provenance.
4. **Given** dataset được commit, **When** kiểm provenance, **Then** mỗi metric/range có nguồn công khai đáng tin cậy, source URL/title/publisher, ngày truy cập, loại range (`reference_interval`, `clinical_decision_threshold`, hoặc `lab_specific_interval`), đơn vị, conversion note nếu có và reviewer note.
5. **Given** Priority A metric có reference data active, **When** người dùng mở Explain hoặc LLM explanation, **Then** `metric-explanations.vi.json` hoặc approved RAG corpus có chunk cùng metric key/aliases; retrieval không rơi về generic fallback cho các metric Priority A trong test.
6. **Given** dataset và RAG corpus mới, **When** chạy test backend, **Then** có test chứng minh representative OCR names phân loại được, RAG retrieve được chunk tương ứng, import preview/confirm không bỏ qua approval/audit workflow và dataset không chứa PHI.
7. **Given** tài liệu xét nghiệm đã in reference range riêng, **When** hệ thống classify metric, **Then** document-provided range vẫn thắng system fallback range và UI/source badge vẫn thể hiện đúng `referenceRangeSource=document`.
8. **Given** dataset có chỉ số chưa được model hỗ trợ tốt như qualitative urinalysis, **When** curate dữ liệu, **Then** không ép vào numeric reference range nếu cần data model khác; ghi rõ deferred scope.

## Tasks / Subtasks

- [x] Task 1 - Chốt quality bar và phạm vi Priority A (AC: #1, #4, #8)
  - [x] Dùng `docs/reference-data/core-feature-data-requirements.md` làm baseline.
  - [x] Chốt Priority A metrics tối thiểu: Glucose, HbA1c, Cholesterol, Triglycerides, HDL, LDL, Hemoglobin/HGB, WBC, RBC, Hematocrit/HCT, Platelet/PLT, MCV, MCH, MCHC, RDW, Neutrophils, Lymphocytes, Monocytes, Eosinophils, Basophils, AST/GOT/SGOT, ALT/GPT/SGPT, ALP, GGT, Bilirubin Total/Direct, Albumin, Total Protein, Creatinine, Urea/BUN, Sodium/Na+, Potassium/K+, Chloride/Cl-, Calcium, CRP.
  - [x] Tách rõ Priority B/deferred: TSH/FT4/FT3, Uric Acid, Ferritin/Iron, Vitamin D, urinalysis qualitative values.
  - [x] Không đưa thuốc, liều dùng, chẩn đoán bệnh hoặc khuyến nghị điều trị vào reference dataset.

- [x] Task 2 - Mở rộng schema import/reference data cho core dataset (AC: #2, #3, #4)
  - [x] Mở rộng import CSV/JSON để nhận `attentionMin`, `attentionMax`; không còn tự set bằng `minValue/maxValue` nếu input có giá trị riêng.
  - [x] Cho phép import alias list hoặc alias rows, gồm alias tiếng Việt, tiếng Anh, viết tắt, có dấu/không dấu và biến thể OCR thường gặp.
  - [x] Thiết kế lưu provenance ở mức metric/range hoặc artifact quản trị tương ứng: source URL/title/publisher, accessed date, range type, reviewer note, conversion note, method/specimen note nếu cần.
  - [x] Giữ tương thích backward-compatible với file smoke seed hiện tại; nếu file cũ thiếu `attentionMin/attentionMax`, preview phải ghi rõ default hoặc validation behavior.
  - [x] Không bypass Story 7.4/7.5: import vẫn tạo draft change set, publish/approve và audit đầy đủ.

- [x] Task 3 - Curate Core Feature Reference Dataset v1 artifact (AC: #1, #3, #4, #8)
  - [x] Tạo artifact mới trong `docs/reference-data/`, ví dụ `core-feature-reference-dataset-v1.csv` hoặc JSON nếu schema lồng alias/provenance phù hợp hơn CSV.
  - [x] Giữ `initial-reference-ranges.csv` là smoke/demo seed; không đổi nhãn thành trusted dataset.
  - [x] Mỗi value phải có nguồn con người kiểm tra được; không lấy range do LLM tự suy diễn.
  - [x] Nguồn ưu tiên: MedlinePlus, CDC/NIDDK, Mayo Clinic, ARUP/Test Directory hoặc nguồn peer-reviewed/public Việt Nam đã review tính áp dụng.
  - [x] Nếu nguồn là clinical decision threshold thay vì lab reference interval, phải đánh dấu đúng `rangeType` và không ghi nhầm thành normal lab interval.

- [x] Task 4 - Đồng bộ alias với OCR/review flow (AC: #1, #6, #7)
  - [x] Thêm test fixtures tên chỉ số giống phiếu xét nghiệm Việt Nam: `Bạch cầu`, `Bach cau`, `Huyet sac to`, `Creatinin`, `Ure`, `GOT`, `GPT`, `SGOT`, `SGPT`, `TC`, `TG`, `Na+`, `K+`, `Cl-`, v.v.
  - [x] Test `ReferenceDataService.classifyMetric(...)` với profile age/gender để chứng minh range context đúng.
  - [x] Test document-provided range vẫn được ưu tiên trong `HealthRecordService.enrichMetric(...)` khi OCR có `referenceRange`.
  - [x] Test metric không hỗ trợ hoặc unit không rõ vẫn trả `no_data` có kiểm soát, không phân loại sai.

- [x] Task 5 - Đồng bộ Explain/RAG/LLM corpus (AC: #5, #6)
  - [x] Mở rộng `apps/api/src/main/resources/ai/metric-explanations.vi.json` hoặc corpus approved tương ứng cho toàn bộ Priority A metrics.
  - [x] Bảo đảm `metricKey` và aliases trong RAG khớp canonical names/aliases trong reference DB.
  - [x] Test `MetricExplanationRetrievalService.retrieve(...)` không rơi về `generic` cho Priority A metrics đã có reference data.
  - [x] Prompt/LLM vẫn chỉ dùng exact reference range được truyền vào và không tự bịa range.

- [x] Task 6 - Import/admin workflow verification (AC: #2, #4, #6)
  - [x] Test `ReferenceDataAdminService.previewImport(...)` parse được artifact mới, hiển thị lỗi theo line number và file dưới 5MB.
  - [x] Test `confirmImport(...)` tạo draft change set cho metric/range/alias/provenance đúng shape.
  - [x] Test single-admin publish trực tiếp và multi-admin submit/approve vẫn đi qua workflow hiện có.
  - [x] Test audit log có actor/timestamp/diff/source/IP hoặc metadata tương đương cho thay đổi dataset.

- [x] Task 7 - Tài liệu vận hành và safety guardrails (AC: #4, #7, #8)
  - [x] Cập nhật provenance docs để nói rõ dataset phục vụ fallback/reference trong app, không thay thế range in trên phiếu xét nghiệm hoặc tư vấn bác sĩ.
  - [x] Ghi rollback path: deactivate/edit metric/range qua admin UI hoặc change set mới nếu phát hiện nguồn sai.
  - [x] Ghi rõ scope không xử lý PHI; artifact chỉ chứa dữ liệu tham chiếu công khai.
  - [x] Ghi rõ các chỉ số qualitative deferred vì cần data model khác.

### Review Findings

- [x] [Review][Patch] Legacy or blank-alias imports can delete existing aliases on approval [apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java:862]
- [x] [Review][Patch] Missing units and ProfileService reclassification can bypass the new unit safety guard [apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java:403]
- [x] [Review][Patch] JSON alias arrays and alias-only rows are not actually supported by import [apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java:1114]
- [x] [Review][Patch] Priority A RAG aliases are not aligned with the real reference dataset aliases [apps/api/src/main/resources/ai/metric-explanations.vi.json:67]
- [x] [Review][Patch] Backend tests do not prove Priority A retrieval avoids generic fallback [apps/api/src/test/java/com/healthlens/api/service/MetricExplanationRetrievalServiceTest.java:298]
- [x] [Review][Patch] Dataset tests do not verify the committed artifact contains no PHI [apps/api/src/test/java/com/healthlens/api/service/ReferenceDataAdminServiceTest.java:937]
- [x] [Review][Patch] Import preview silently defaults missing attention bounds without a visible warning/default marker [apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java:1055]
- [x] [Review][Patch] CSV parser can corrupt quoted multiline provenance/reviewer notes [apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java:759]
- [x] [Review][Patch] Unit normalization rejects common safe spelling variants such as x10^9/L and K/uL [apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java:411]

## Dev Notes

### Core feature findings that drive this upgrade

- `OcrService` trích xuất `name`, `value`, `unit`, optional `flag`, optional `referenceRange`. Nếu tài liệu có range usable, classification dùng document range và set `referenceRangeSource=document`.
- Nếu OCR không có range usable, `HealthRecordService.enrichMetric(...)` gọi `ReferenceDataService.classifyMetric(...)`. Không match được metric/alias thì status thành `no_data`, `referenceRangeSource=none`, Explain/RAG/LLM mất context.
- `ReferenceDataService` resolve metric bằng canonical `ReferenceMetric.name` hoặc `reference_metric_aliases.alias_normalized`, chọn active ranges theo gender/age, rồi dùng `min/max` cho `normal` và `attentionMin/attentionMax` cho `attention/abnormal`.
- `ReferenceDataAdminService.ImportRowCandidate.toRangeRequest()` hiện set `attentionMin=minValue` và `attentionMax=maxValue`. Đây là lý do dataset hiện tại không thể phân biệt lệch nhẹ và lệch rõ khi import.
- `MetricExplanationRetrievalService` query approved active RAG corpus theo metric key/alias. Nếu miss Qdrant thì fallback sang `ReferenceDataService.buildMetricKnowledgeSnippet(...)`, sau đó generic. Corpus hiện tại chỉ có khoảng 12 chunks.
- `LlmService.generateExplanationResult(...)` truyền exact reference range vào prompt và reject output tự bịa range. Vì vậy range/provenance phải được chuẩn hóa trước khi mở rộng LLM explanation.

### Current artifacts

- `docs/reference-data/initial-reference-ranges.csv`: smoke/demo seed, 15 rows/12 metric names.
- `docs/reference-data/initial-reference-ranges.provenance.md`: provenance cho smoke seed.
- `docs/reference-data/core-feature-data-requirements.md`: phân tích yêu cầu dữ liệu theo OCR/RAG/LLM/Explain/Recommendations.
- `apps/api/src/main/resources/ai/metric-explanations.vi.json`: curated Vietnamese explanation chunks hiện có cho ALT, AST, GLUCOSE, HBA1C, HDL, LDL, TRIGLYCERIDE, CHOL, WBC, RBC, HGB, PLT.

### Existing capabilities to reuse

- Story 7.3: import preview/confirm CSV/JSON, BOM UTF-8, line-number validation, file size limit 5MB, owner-only confirm, TTL handling.
- Story 7.4: single-admin publish trực tiếp hoặc multi-admin submit/approve.
- Story 7.5: audit log cho thay đổi reference data. Không seed thẳng production bằng SQL trong story này nếu không có quyết định kiến trúc riêng.
- DB đã có `reference_metrics`, `reference_ranges`, `reference_metric_aliases`; `reference_ranges` đã có `attention_min` và `attention_max`.

### Implementation guardrails

- Ưu tiên mở rộng parser/service hiện có thay vì tạo pipeline import mới.
- Nếu cần thay đổi DB schema cho provenance, thêm migration mới và cập nhật entity/repository/API response tương ứng.
- Nếu chọn JSON cho dataset mới vì alias/provenance phức tạp, CSV import cũ vẫn phải tiếp tục hoạt động.
- Unit conversion không được âm thầm chuyển nếu chưa có metadata rõ ràng. Unsupported unit phải được báo lỗi hoặc trả `no_data`, không tự đoán.
- Không dùng LLM để tạo giá trị range. LLM chỉ có thể hỗ trợ format/kiểm tra, mọi giá trị phải có source và reviewer note.
- Lab printed range/document range phải luôn được ưu tiên hơn system fallback range.

### Suggested test focus

- `ReferenceDataAdminServiceTest`: import preview/confirm extended fields, alias/provenance payload, backward compatibility với smoke seed.
- `ReferenceDataServiceTest`: canonical/alias matching, age/gender range selection, attention vs abnormal thresholds.
- `HealthRecordServiceTest`: document range wins over fallback range.
- `MetricExplanationRetrievalServiceTest`: Priority A metric retrieval không generic fallback khi corpus có chunk matching alias.

### References

- [Source: docs/reference-data/core-feature-data-requirements.md]
- [Source: apps/api/src/main/java/com/healthlens/api/service/OcrService.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java]
- [Source: apps/api/src/main/java/com/healthlens/api/service/LlmService.java]
- [Source: apps/api/src/main/resources/ai/metric-explanations.vi.json]
- [Source: apps/api/src/main/resources/db/migration/V013__create_reference_data_tables.sql]
- [Source: apps/api/src/main/resources/db/migration/V014__create_reference_range_audit_logs_table.sql]
- [Source: _bmad-output/implementation-artifacts/epic-7/7-3-import-reference-data-csv-json.md]
- [Source: _bmad-output/implementation-artifacts/epic-7/7-4-approve-reference-data-change-set.md]
- [Source: _bmad-output/implementation-artifacts/epic-7/7-5-reference-data-audit-log-view.md]
- [MedlinePlus CBC](https://medlineplus.gov/lab-tests/complete-blood-count-cbc/)
- [MedlinePlus Comprehensive Metabolic Panel](https://medlineplus.gov/ency/article/003468.htm)
- [MedlinePlus Cholesterol Levels](https://medlineplus.gov/lab-tests/cholesterol-levels/)
- [CDC/NIDDK HbA1c context](https://www.cdc.gov/diabetes/diabetes-testing/prediabetes-a1c-test.html)
- [Mayo Clinic Liver Function Tests](https://www.mayoclinic.org/tests-procedures/liver-function-tests/about/pac-20394595)
- [Vietnam-local candidate source](https://vjol.vista.gov.vn/SK-PT/vi/article/view/77311)

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests ReferenceDataAdminServiceTest --tests ReferenceDataServiceTest --tests MetricExplanationRetrievalServiceTest --tests HealthRecordServiceTest` - pass
- `cd apps/api && ./gradlew test` - pass
- `cd apps/api && ./gradlew test` - pass after code-review patch batch

### Completion Notes List

- Mở rộng admin import CSV/JSON để nhận `attentionMin`, `attentionMax`, alias list và provenance metadata; import cũ thiếu attention bounds vẫn backward-compatible bằng default về min/max.
- `confirmImport(...)` hiện giữ attention bounds thật trong draft change set và lưu alias/provenance vào `changes_json`; publish/approve áp alias vào `reference_metric_aliases` thay vì bypass workflow.
- Tạo `core-feature-reference-dataset-v1.csv` với Priority A coverage, aliases OCR/Vietnamese/English, attention bounds và source/provenance per row; tạo provenance/runbook đi kèm và giữ smoke seed cũ nguyên vai trò demo.
- Đồng bộ `metric-explanations.vi.json` cho Priority A metrics và normalize alias RAG có dấu/không dấu để tránh generic fallback.
- Thêm guard unit mismatch trong system fallback classification để unit không rõ/không khớp trả `no_data` thay vì phân loại sai.
- Bổ sung tests cho import artifact v1, attention/alias/provenance shape, alias OCR representative, document range priority, unit mismatch và RAG Priority A coverage.
- Xử lý code-review batch: legacy imports không xóa alias khi không có alias, JSON alias arrays/alias-only rows được hỗ trợ, CSV quoted multiline được parse đúng, preview expose attention default markers, ProfileService truyền normalized unit, unit variants an toàn được normalize, RAG aliases đồng bộ hơn với dataset và test chứng minh không generic fallback.

### File List

- apps/api/src/main/java/com/healthlens/api/dto/response/AdminReferenceImportPreviewRowResponse.java
- apps/api/src/main/java/com/healthlens/api/repository/ReferenceMetricAliasRepository.java
- apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java
- apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java
- apps/api/src/main/java/com/healthlens/api/service/ReferenceDataAdminService.java
- apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java
- apps/api/src/main/resources/ai/metric-explanations.vi.json
- apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/MetricExplanationRetrievalServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/ReferenceDataAdminServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/ReferenceDataServiceTest.java
- docs/reference-data/core-feature-data-requirements.md
- docs/reference-data/core-feature-reference-dataset-v1.csv
- docs/reference-data/core-feature-reference-dataset-v1.provenance.md

### Change Log

- 2026-05-23: Implemented Core Feature Reference Dataset v1 import/schema, dataset artifact, RAG corpus alignment, unit safety guard, and backend regression tests.
