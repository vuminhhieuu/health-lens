# HealthLens Core Feature & Infrastructure Issues Synthesis

Date: 2026-05-16

## Purpose

Tài liệu này tổng hợp các vấn đề đã rà soát trong quá trình review HealthLens, tập trung vào:

- Core feature: OCR, LLM, RAG, AI explanation/recommendation.
- Provider switching: khả năng đổi OCR/LLM/Embedding/RAG provider bằng config/env.
- Infrastructure code: deployment, CI/CD, observability, security hardening, audit, backup/restore, queue reliability.

Nguồn tham chiếu chính:

- `_bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md`
- `_bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md`
- `_bmad-output/planning-artifacts/review-source/PRODUCTION-READINESS.md`
- `_bmad-output/planning-artifacts/review-source/REVIEW-PRODUCTION-MASTER.md`
- `_bmad-output/planning-artifacts/review-source/production-review/p0-gates-checklist.md`
- `_bmad-output/planning-artifacts/review-source/production-review/audit-logging-mini-adr.md`
- `_bmad-output/implementation-artifacts/epic-7/7-6-unified-audit-spine-with-correlation-ids.md`
- `_bmad-output/implementation-artifacts/epic-10/*.md`
- Code hiện tại trong `apps/api`, `apps/web`, `services/ocr-service`, `docker`, `.github/workflows`.

External research references:

- PaddleOCR PP-OCRv5 performance: https://paddlepaddle.github.io/PaddleOCR/main/en/version3.x/algorithm/PP-OCRv5/PP-OCRv5.html
- PaddleOCR OCR usage/model docs: https://paddlepaddle.github.io/PaddleOCR/main/en/version3.x/pipeline_usage/OCR.html
- EasyOCR official README: https://github.com/JaidedAI/EasyOCR
- Spring AI Chat Model API: https://docs.spring.io/spring-ai/reference/api/chatmodel.html
- Spring AI OpenAI Chat properties: https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html
- Spring AI provider selection upgrade notes: https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/upgrade-notes.html
- Spring AI Qdrant VectorStore: https://docs.spring.io/spring-ai/reference/api/vectordbs/qdrant.html
- Render Free docs: https://render.com/docs/free
- Hugging Face Spaces hardware docs: https://huggingface.co/docs/hub/main/spaces-overview
- Koyeb instance docs: https://www.koyeb.com/docs/reference/instances
- Fly.io pricing docs: https://fly.io/docs/about/pricing/
- Oracle Cloud Always Free docs: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- Google Cloud Run pricing: https://cloud.google.com/run/pricing

## Executive Summary

HealthLens đã có nền tảng implementation khá rộng: upload, OCR microservice, LLM explanation, Qdrant/RAG ingestion, Docker, CI basic, staging deployment guide. Tuy nhiên, các core feature liên quan OCR/LLM/RAG và production infrastructure vẫn chưa đạt mức production-ready.

Những blocker lớn nhất:

1. OCR pipeline chưa provider-agnostic đúng nghĩa, đặc biệt PDF đang đi qua image-oriented flow.
2. OCR reliability thiếu retry/DLQ/idempotency nên có nguy cơ mất job hoặc xử lý trùng.
3. Parser/LLM extraction còn fragile, prompt inline và JSON repair heuristic yếu.
4. RAG không còn "trống" hoàn toàn, nhưng corpus quality/coverage/versioning/review workflow chưa đủ rõ.
5. Provider switching hiện mới đổi được một phần bằng env; OCR và vector/RAG chưa plug-and-play.
6. Infrastructure production baseline còn thiếu: telemetry, alerting, correlation ID, backup/restore, runbook, security scanning, deploy rollback.
7. Security infrastructure còn nhiều story ready-for-dev: JWT validation, CSRF, CORS validation, SSRF protection, admin session storage.

## 1. OCR PDF Pipeline Mismatch

### Problem

Upload flow cho phép PDF, nhưng OCR consumer hiện gọi image-oriented method cho mọi file type. Review production gọi đây là gần như "broken by design".

### Evidence

- Web upload cho phép PDF.
- Backend upload URL hỗ trợ `application/pdf`.
- `OcrJobConsumer` gọi `ocrService.processImage(downloadUrl)` mà không phân biệt PDF/image.
- `services/ocr-service` hiện là EasyOCR image OCR service.

### Impact

- PDF có thể OCR kém hoặc fail.
- Metrics bị parse sai.
- AI explanation/recommendation dựa trên dữ liệu sai.
- User mất niềm tin vì file gốc và kết quả OCR không khớp.

### Proposed Solution

Thiết kế lại OCR theo hướng provider-agnostic và MIME-aware:

- Job payload phải có `recordId`, `fileKey`, `mimeType/fileType`, `correlationId`.
- Router chọn provider theo capability:
  - PDF có text layer: extract text trực tiếp.
  - PDF scan: render page -> document OCR.
  - Image: image OCR provider.
- PDF primary nên là document OCR provider như AWS Textract, Google Document OCR, Azure Document Intelligence.
- Fallback PDF render -> image OCR chỉ là last resort.

### Priority

P0.

## 2. OCR Provider Contract Chưa Đủ Chuẩn

### Problem

`OcrResult` hiện chủ yếu gồm text/confidence/source/language. Contract này quá mỏng để đổi provider hoặc parse layout xét nghiệm ổn định.

### Impact

- Parser chỉ nhận text phẳng nên dễ mất cấu trúc bảng.
- Không trace được page/block/line confidence.
- Khó debug provider nào fail, fail ở page nào, lý do gì.
- Không đủ metadata cho audit/compliance khi dùng OCR provider ngoài.

### Proposed Solution

Nâng OCR result contract:

```text
provider
modelVersion
mimeType
pages[]
blocks[]
lines[]
text
confidence
diagnostics[]
providerRequestId
latencyMs
retentionMode
```

Tối thiểu mỗi block/line nên có:

```text
pageNumber
text
confidence
boundingBox
kind: line | word | table_cell | paragraph
```

### Priority

P0.

## 3. Parser Robustness Yếu

### Problem

`parseMetricsByRegex` dựa vào regex/heuristics line-based. LLM extraction fallback cũng parse JSON theo cách fragile.

### Impact

- Form xét nghiệm Việt Nam khác layout sẽ dễ fail.
- Bảng nhiều cột hoặc reference range nằm dòng khác dễ bị parse sai.
- Sai unit/value/reference range dẫn tới status và recommendation sai.

### Proposed Solution

- Tạo test corpus 20-50 mẫu xét nghiệm Việt Nam, gồm PDF text-layer, PDF scan, ảnh chụp rõ/mờ, nhiều bệnh viện/lab.
- Đo riêng:
  - text extraction accuracy,
  - metric name accuracy,
  - value/unit accuracy,
  - reference range accuracy,
  - false positive metric rate.
- Parser nên dùng block/line layout thay vì chỉ text phẳng.
- Thêm confidence gate:
  - `low_ocr_confidence`
  - `low_parse_confidence`
  - `ambiguous_table_layout`
  - `missing_reference_range`
- Khi confidence thấp, chuyển `review_required`, không tự coi là done.

### Priority

P0.

## 4. OCR Queue Reliability: Ack/DB Race, No DLQ

### Problem

Review disposition xác nhận:

- `OcrJobConsumer` ack message sau khi gọi service.
- Nếu DB write fail nhưng ack thành công: mất job.
- Nếu DB success nhưng ack fail: job chạy lại.
- Exception path có nguy cơ ack luôn, không có DLQ/poison queue.

### Impact

- Mất kết quả OCR.
- Double-processing.
- Không đo được failure reason chính xác.
- Không đạt SLA OCR.

### Proposed Solution

Implement OCR job reliability layer:

- Message state machine:
  - `queued`
  - `processing`
  - `succeeded`
  - `failed_retryable`
  - `failed_terminal`
  - `dead_lettered`
- Idempotency key: `recordId + jobId + fileKey/version`.
- Retry policy có backoff và max attempts.
- DLQ stream/table cho poison messages.
- Persist failure reason: `timeout`, `provider_error`, `low_confidence`, `invalid_file`, `parse_error`.
- Ack chỉ sau khi DB state được persist thành công.
- Nếu ack fail sau DB success, reprocess phải idempotent.

### Priority

P0.

## 5. EasyOCR vs PaddleOCR

### Problem

Hiện service OCR self-host dùng EasyOCR. User nghe review PaddleOCR tốt hơn EasyOCR và muốn cân nhắc đổi.

### Research Findings

EasyOCR:

- Official README nói hỗ trợ 80+ languages, CPU-only bằng `gpu=False`.
- Repo hiện dùng EasyOCR `vi/en`, service image-only.
- HealthLens OCR README tự ghi RAM khoảng `~500MB`, warm request 3-8s CPU, Docker image khoảng 2GB.

PaddleOCR:

- PaddleOCR docs nói PP-OCRv5 là default OCR pipeline và cải thiện 13 điểm phần trăm so với PP-OCRv4 trong nhiều scenario.
- PP-OCRv5 mobile CPU peak RAM khoảng 2.2GB.
- PP-OCRv5 server CPU peak RAM khoảng 4.0GB.
- PaddleOCR có hệ sinh thái document/layout tốt hơn, phù hợp hướng document OCR hơn EasyOCR.

### Recommendation

Không replace thẳng EasyOCR bằng PaddleOCR. Nên:

1. Tạo `OcrProvider` interface.
2. Giữ `EasyOcrProvider`.
3. Thêm `PaddleOcrProvider`.
4. Benchmark EasyOCR vs PaddleOCR trên dataset HealthLens.
5. Nếu Paddle thắng rõ, set `OCR_PROVIDER_PRIMARY=paddleocr`.

### Priority

P1 cho improve phase, P0 nếu PDF/image OCR quality là launch gate.

## 6. Self-host OCR Hạ Tầng Free Không Thực Tế Cho Production

### Problem

Self-host OCR cần RAM/CPU đáng kể. Free PaaS thường không đủ hoặc cold start quá nặng.

### Research Findings

- Render Free: spin down sau 15 phút idle, local filesystem ephemeral, 750 free instance hours/tháng. Không phù hợp OCR model cold start.
- Koyeb Free: 512MB RAM, 0.1 vCPU, 2GB SSD, không đủ cho PaddleOCR/EasyOCR production.
- Fly.io: free allowance hiện chủ yếu legacy; paid 2GB/4GB mới thực tế.
- Hugging Face Spaces Free CPU Basic: 2 vCPU, 16GB RAM free, phù hợp demo/staging, nhưng không phù hợp production health data nếu private/security/compliance cần nghiêm.
- Oracle Always Free Ampere A1: official docs nói tương đương 4 OCPU + 24GB RAM, khả thi nhất để self-host free, nhưng có rủi ro capacity, ARM compatibility, vận hành VPS thủ công.
- Cloud Run: có free tier theo usage, nhưng OCR cold start/model loading có thể phát sinh chi phí và latency.

### Recommendation

- Local/staging/demo: thử PaddleOCR trên Hugging Face Spaces hoặc Oracle Always Free.
- Production MVP: ưu tiên external OCR provider có consent/retention/kill-switch; self-host chỉ làm fallback hoặc khi có server đủ ổn định.
- Không phụ thuộc Render/Koyeb free cho OCR production.

### Priority

P1 architecture/deployment decision.

## 7. External OCR Consent, Retention, Kill-switch

### Problem

Scope đã chấp nhận dùng OCR provider ngoài, nhưng cần kiểm soát privacy/compliance.

### Impact

Tài liệu y tế là sensitive health data. Nếu gửi sang provider ngoài mà thiếu consent/retention/audit, rủi ro compliance và trust rất cao.

### Proposed Solution

- Consent riêng hoặc consent scope rõ cho external OCR.
- Persist:
  - provider,
  - provider request id,
  - provider region,
  - retention mode,
  - consent version.
- Kill-switch env để tắt external provider.
- Không log raw OCR text, presigned URL, token, full fileKey.
- Ưu tiên region gần VN như Singapore nếu provider hỗ trợ.

### Priority

P0 nếu production dùng provider ngoài.

## 8. OCR Microservice Security

### Problem

API `/api/ocr/*` không public theo disposition, nhưng `services/ocr-service` expose `POST /ocr` không auth/rate limit nếu deploy public.

### Impact

- Abuse tài nguyên OCR.
- SSRF/download URL abuse.
- Lộ xử lý tài liệu nếu endpoint public.

### Proposed Solution

- OCR service chỉ nằm trong private network nếu có thể.
- Nếu public, thêm API key/service-to-service auth.
- Thêm rate limit.
- Thêm SSRF protection:
  - block loopback/link-local/private ranges nếu không whitelisted,
  - disable redirect following,
  - validate resolved/final IP.
- Domain whitelist cho object storage URLs.

### Priority

P1, hoặc P0 nếu OCR service public.

## 9. LLM Prompt Inline, Versioning Yếu

### Problem

Prompt OCR -> LLM extraction nằm inline trong Java string. JSON repair heuristic chỉ đóng ngoặc đơn giản.

### Impact

- Khó maintain/test/version/rollback.
- Model output đổi có thể làm parse fail.
- Không audit được prompt version rõ ràng.

### Proposed Solution

- Externalize prompt templates:
  - `resources/prompts/ocr-extraction/v1.md`
  - `resources/prompts/metric-explanation/v2.md`
  - `resources/prompts/recommendation/v1.md`
- Persist prompt version/model/provider trong result metadata.
- Dùng JSON schema/structured output nếu provider hỗ trợ.
- Nếu output invalid:
  - retry with repair prompt,
  - hoặc dùng JSON repair library,
  - cuối cùng fallback regex nhưng phải mark `source=ocr_regex_fallback`.

### Priority

P1.

## 10. LLM Guardrails Cho Medical Explanation

### Problem

LLM không được tự bịa reference range hoặc đưa chẩn đoán. Review yêu cầu reference range phải lấy từ reference data.

### Impact

- Sai thông tin y tế.
- Rủi ro product/legal/compliance.
- User có thể hiểu sai trạng thái sức khỏe.

### Proposed Solution

- LLM chỉ giải thích dựa trên:
  - metric value,
  - status đã tính bằng reference data,
  - reference range approved,
  - RAG snippet approved.
- Prompt bắt buộc:
  - không chẩn đoán,
  - khuyến nghị tham khảo bác sĩ,
  - không tự tạo ngưỡng chuẩn.
- Capture observability:
  - model,
  - prompt version,
  - tokens,
  - latency,
  - source,
  - fallback reason.

### Priority

P1.

## 11. RAG Không Trống, Nhưng Chưa Đủ Production Governance

### Problem

`REVIEW-FULL-v2` nói Qdrant chưa có ingestion. `REVIEW-DISPOSITION` cập nhật lại: finding này outdated vì đã có `MetricExplanationIngestionJob/Service` và dataset `ai/metric-explanations.vi.json`.

Vấn đề thật còn lại: corpus quality/coverage/versioning/review workflow chưa đủ rõ.

### Impact

- RAG hit có thể thấp hoặc không đáng tin.
- Corpus thay đổi khó audit/rollback.
- Khó biết explanation đến từ source nào/version nào.

### Proposed Solution

- Treat RAG corpus như reference data:
  - curated,
  - versioned,
  - admin-reviewable,
  - có source/citation metadata,
  - có rollback.
- Naming collection theo model/dimension/version:
  - `healthlens-metric-explanations-v1-1536`
- Startup validation:
  - collection exists,
  - embedding dimension matches,
  - corpus version ingested.
- Khi RAG miss, fallback source phải rõ:
  - `qdrant`
  - `reference-data`
  - `generic`

### Priority

P1.

## 12. Provider Switching Chưa Plug-and-play

### Problem

User mong muốn đổi provider kiểu chỉ thay key trong `.env`. Code hiện tại chỉ hỗ trợ một phần.

### Current State

LLM:

- Dùng Spring AI OpenAI starter trỏ sang Groq.
- Đổi được nếu provider OpenAI-compatible.
- Nhưng naming vẫn Groq-specific: `GROQ_API_KEY`, `GroqAiConfig`, `groqChatClient`.

OCR:

- Có `OCR_PROVIDER_PRIMARY`, `OCR_PROVIDER_FALLBACK_ORDER`.
- Nhưng provider hardcode bằng `switch` trong `OcrService`.
- Supported provider hardcode: `easyocr`, `gcv`, `textract`.
- Thêm `paddleocr` bắt buộc sửa code.

Embedding:

- Có `EMBEDDING_BASE_URL`, `EMBEDDING_API_KEY`, `EMBEDDING_MODEL`.
- Đổi được nếu OpenAI-compatible.
- Nhưng phải khớp Qdrant dimension và reindex.

RAG/vector DB:

- Java dùng Spring AI `VectorStore`, nhưng config gắn Qdrant.
- Đổi vector DB khác cần dependency/config/code/test.

### Proposed Solution

Provider abstraction:

```text
Core feature code
  -> AiGateway / OcrGateway / RetrievalGateway
      -> Provider Registry
          -> Provider Adapter
```

Config rename:

```env
AI_CHAT_PROVIDER=openai-compatible
AI_CHAT_BASE_URL=https://api.groq.com/openai
AI_CHAT_API_KEY=...
AI_CHAT_MODEL=...

AI_EMBEDDING_PROVIDER=openai-compatible
AI_EMBEDDING_BASE_URL=...
AI_EMBEDDING_API_KEY=...
AI_EMBEDDING_MODEL=...
AI_EMBEDDING_DIMENSION=1536

OCR_PROVIDER_PRIMARY=paddleocr
OCR_PROVIDER_FALLBACK_ORDER=gcv,textract,easyocr
VECTOR_STORE_PROVIDER=qdrant
```

Rules:

- Provider đã implement thì đổi bằng env.
- Provider mới thì thêm adapter riêng, không sửa core business flow.
- Startup validation phải fail-fast nếu config trỏ tới provider chưa có bean.

### Priority

P1 architecture hardening.

## 13. Qdrant/Embedding Config Footgun

### Problem

`.env.example` ghi `QDRANT_HOST=https://xxx.qdrant.io` và `QDRANT_PORT=6333`, trong khi `application-docker.yml` comment yêu cầu host thuần, không có `https://`, và Qdrant gRPC port thường là `6334`.

Ngoài ra default `QDRANT_VECTOR_DIMENSION=1024`, nhưng `text-embedding-3-small` thường là 1536 nếu dùng default dimension.

### Impact

- Deploy fail do invalid host/port.
- Vector dimension mismatch gây lỗi indexing/search hoặc buộc recreate collection.

### Proposed Solution

- Sửa `.env.example`:
  - `QDRANT_HOST=xxx.qdrant.io`
  - `QDRANT_PORT=6334`
  - `AI_EMBEDDING_DIMENSION=1536`
- Startup validation:
  - reject `QDRANT_HOST` chứa `http://` hoặc `https://`,
  - check collection dimension,
  - warn/fail nếu embedding model dimension không khớp collection.

### Priority

P1.

## 14. Infrastructure Code: Foundation Có, Production Baseline Chưa Đủ

### Current State

Đã có:

- Docker compose dev/prod.
- Dockerfiles API/Web/OCR.
- CI build/test/lint.
- Deploy workflow.
- Staging deployment guide.
- Infra foundation story verified.

### Problems

- Production deploy workflow build OCR image nhưng không deploy OCR image.
- `docker/compose.prod.yml` không có OCR service.
- `deploy.replicas` trong Docker Compose không có tác dụng nếu không dùng Swarm.
- Rollback chủ yếu là down/up, chưa có blue-green/canary.
- Chưa có migration strategy/rollback strategy rõ.
- Chưa có production runbook.

### Proposed Solution

- Decide production topology:
  - managed PaaS per service,
  - Docker single-server,
  - or Kubernetes/Swarm.
- Nếu single-server Docker Compose:
  - bỏ `deploy.replicas` gây hiểu nhầm,
  - thêm OCR service hoặc external OCR endpoint rõ,
  - thêm reverse proxy thật,
  - health-gated deploy,
  - rollback to previous image tag.
- Deploy workflow:
  - pull/tag API, Web, OCR đầy đủ,
  - verify all health endpoints,
  - fail deploy nếu health không pass,
  - retain previous images for rollback.

### Priority

P1 before production.

## 15. Observability/Monitoring/Alerting Thiếu

### Problem

P0 gates yêu cầu API/OCR/Redis/DB/storage có metrics, alerts, structured logs, correlation IDs. Hiện mới có Actuator health/metrics cơ bản trong docker profile.

### Impact

- OCR fail silent.
- Không biết queue backlog.
- Không biết LLM cost/latency/error.
- Incident response yếu.

### Proposed Solution

Metrics tối thiểu:

- API:
  - request rate,
  - p95/p99 latency,
  - error rate,
  - DB pool usage.
- OCR:
  - queue depth,
  - job latency,
  - fail rate by provider,
  - confidence distribution,
  - DLQ count.
- LLM:
  - latency,
  - error rate,
  - token/cost proxy,
  - fallback rate,
  - low-quality rate.
- RAG:
  - hit rate,
  - top score,
  - retrieval latency,
  - source distribution.
- Storage:
  - upload failure,
  - signed URL failure,
  - delete failure.

Implementation options:

- Actuator + Prometheus endpoint.
- Grafana dashboard.
- Alertmanager or managed alerts.
- Structured JSON logs.
- Correlation ID via MDC.

### Priority

P0/P1.

## 16. Audit Spine & Correlation ID Chưa Implement

### Problem

Story 7.6 đang ready-for-dev. Audit ADR yêu cầu canonical audit layer nhưng code chưa khép kín.

### Impact

- Không trace được request end-to-end.
- Không truy vết OCR provider usage/retention.
- Không đủ compliance/admin incident review.

### Proposed Solution

- Add correlation middleware/filter:
  - accept/generate `correlation_id`,
  - put into MDC,
  - echo response header.
- Carry correlation id through Redis stream OCR jobs.
- Add canonical tables:
  - `audit_events`
  - `user_activity_events`
  - optional `ocr_job_events`
- Redaction rules:
  - no raw OCR text,
  - no presigned URL,
  - no auth header,
  - no token query param,
  - no full fileKey.
- Retention/anonymization job.

### Priority

P0/P1.

## 17. Backup/Restore/Runbook/SLO Chưa Có

### Problem

Deployment docs có staging setup, nhưng chưa chứng minh restore path. P0 gate nói rõ fail nếu chỉ có deployment docs.

### Impact

- Không thể cam kết recovery.
- Mất DB/object storage có thể mất dữ liệu y tế.
- Không có SLA/SLO nội bộ để vận hành.

### Proposed Solution

- DB:
  - daily backup,
  - PITR nếu managed DB hỗ trợ,
  - restore drill before go-live.
- Object storage:
  - lifecycle policy,
  - delete policy,
  - backup/replication nếu cần.
- Runbook:
  - OCR provider outage,
  - Redis outage,
  - DB migration failure,
  - storage upload/delete failure,
  - LLM provider outage,
  - data deletion incident.
- SLO:
  - OCR completion p95 <= target,
  - API p95 latency <= target,
  - deletion SLA <= 72h,
  - uptime target.

### Priority

P0/P1.

## 18. Security Infrastructure Hardening Chưa Làm

### Problem

Story 10.1 đang ready-for-dev:

- JWT secret validation.
- CSRF protection for refresh token.
- CORS validation.
- SSRF protection.
- OCR controller whitelist.

### Impact

- Weak JWT secret có thể chỉ fail runtime hoặc tạo security risk.
- CSRF risk với cookie-based refresh/session.
- Wildcard CORS + credentials là cấu hình nguy hiểm.
- SSRF risk khi service fetch user-provided URL.

### Proposed Solution

Implement Story 10.1:

- `@PostConstruct` validate JWT secret >= 32 bytes.
- Custom CSRF strategy cho cookie-authenticated endpoints.
- Reject wildcard origins when `allowCredentials=true`.
- RestTemplate/WebClient SSRF protection:
  - disable redirects,
  - block private/link-local/loopback IPs,
  - validate final resolved IP,
  - domain whitelist for OCR file fetch.

### Priority

P0/P1.

## 19. CI/CD Security Gate Chưa Có

### Problem

CI hiện build/test/lint nhưng chưa có security scanning.

### Impact

- Vulnerable dependencies/images có thể deploy.
- Secret leak không bị chặn.
- Không có quality gate trước production.

### Proposed Solution

Implement Story 10.9:

- Dependency scanning:
  - Trivy FS or Snyk.
- SAST:
  - SonarCloud or equivalent.
- Docker image scanning:
  - Trivy image or Docker Scout.
- Secret scanning:
  - Gitleaks.
- CI artifacts:
  - upload reports.
- Quality gate:
  - fail on critical/high vulnerabilities.

### Priority

P1.

## 20. Admin Session Storage Risk

### Problem

Review production notes admin token stored in `sessionStorage`, creating XSS -> admin takeover risk.

### Impact

- One XSS can steal admin token.
- Admin has high privilege over reference data/governance.

### Proposed Solution

- Move admin auth to HttpOnly Secure SameSite cookie.
- Add CSRF strategy for cookie-authenticated mutation endpoints.
- Keep short session TTL and refresh rotation.
- Add route access checks and negative tests.

### Priority

P0.

## 21. Right-to-delete/Data Deletion Infrastructure

### Problem

Stories 10.3 and 10.11 show remaining issues:

- cancellation token exposure,
- deletion/cancel race,
- storage cleanup before bulk delete,
- FK/cascade/orphan cleanup,
- optimistic locking.

### Impact

- Token leak via URL/referrer/history.
- Data deletion may race with cancellation.
- Orphan records or zombie files in storage.
- Right-to-delete SLA not enforceable.

### Proposed Solution

- Store cancellation token hash only.
- Remove email from cancellation URL query.
- Use `SELECT FOR UPDATE SKIP LOCKED` for scheduler.
- Add optimistic locking.
- Delete storage objects before DB bulk delete or use compensation/reconciliation.
- Add reconciliation job for storage/DB drift.

### Priority

P0/P1.

## 22. Frontend Infrastructure/Client Reliability

### Problem

Story 10.4 and review findings show:

- direct `sessionStorage` access without try/catch,
- auth bootstrap retry not robust,
- 403 handling can leave stale auth,
- empty catch blocks swallow errors.

### Impact

- Safari/private mode/storage failure can crash flows.
- User gets logged out or stuck incorrectly.
- Support loses server error details.

### Proposed Solution

- Safe storage utility with memory fallback.
- Central auth bootstrap retry with exponential backoff.
- Clear auth only on definitive auth failures.
- Replace empty catch blocks with logging + user feedback.

### Priority

P1/P2 depending launch scope.

## 23. Production Readiness Priority Map

### P0

1. OCR PDF pipeline routing by MIME type.
2. OCR output contract + parser confidence gate.
3. OCR queue idempotency/retry/DLQ.
4. External OCR consent/retention/kill-switch if external providers are used.
5. Admin token storage hardening.
6. Security infra hardening: JWT, CSRF, CORS, SSRF.
7. Right-to-delete race/token/storage cleanup.
8. Audit/correlation baseline for compliance.
9. Backup/restore proof before go-live.

### P1

1. Provider abstraction and startup validation.
2. PaddleOCR provider adapter and benchmark.
3. LLM prompt externalization/versioning/schema validation.
4. RAG corpus governance/versioning.
5. Observability dashboards and alerts.
6. CI/CD security scanning.
7. Production deploy/rollback hardening.
8. Qdrant/embedding config cleanup.

### P2

1. UI polish around AI recommendation ordering.
2. Component refactors for review page.
3. Toast/error UX consistency.
4. Mobile deferred findings.
5. Dead code/constants/type cleanup.

## Recommended Next Stories

1. **OCR Provider-Agnostic Router + Contract**
   - Scope: `OcrProvider` interface, provider registry, MIME/capability routing, normalized OCR result.

2. **OCR Reliability: Retry, Idempotency, DLQ**
   - Scope: Redis stream retry policy, DLQ, state machine, failure reason column.

3. **AI Provider Config Contract**
   - Scope: rename `GROQ_*` to `AI_CHAT_*`, generic `AiModelConfig`, startup validation.

4. **Security Infrastructure Hardening**
   - Scope: Story 10.1.

5. **Correlation ID + Audit Spine**
   - Scope: Story 7.6 plus OCR/LLM/RAG event metadata.

6. **Ops Readiness Baseline**
   - Scope: metrics, structured logs, alert rules, backup/restore drill, runbook, SLO/SLA.

7. **CI/CD Security Integration**
   - Scope: Story 10.9.

## Final Assessment

Các vấn đề đã phát hiện không chỉ là "code feature bug". Chúng nằm ở giao điểm giữa core product logic và infrastructure code:

- OCR/LLM/RAG cần provider abstraction, observability, retry, audit, validation.
- Deployment cần rõ service topology, rollback, health gates, backup/restore.
- Security cần fail-fast config, CSRF/CORS/SSRF hardening, token storage hardening.
- Compliance cần correlation ID, audit spine, retention policy và redaction.

Hướng đi chuyên nghiệp là không cố biến mọi provider thành "chỉ thay key là đổi được" một cách ảo. Mục tiêu đúng là:

> Provider đã implement thì đổi được bằng env/config. Provider mới thì thêm adapter riêng, không sửa core business flow. Production config sai thì fail-fast ở startup, không silent fallback.

---

## Addendum: RAG Online, UI Consistency, Language Consistency, Code Organization

Phần này bổ sung sau khi rà tiếp các câu hỏi mới:

- Có nên làm RAG online không?
- Thông báo thành công/thất bại đã dùng toast đồng bộ chưa?
- Ngôn ngữ đã thống nhất tiếng Việt có dấu chưa?
- Tổ chức code đã clean/clear chưa?

## 24. RAG Online / Web-Augmented RAG

### Current State

Hiện tại HealthLens đang dùng hướng gần với **offline/curated RAG**:

- Corpus nằm trong `apps/api/src/main/resources/ai/metric-explanations.vi.json`.
- Ingestion qua `MetricExplanationIngestionService`.
- Retrieval qua `MetricExplanationRetrievalService`.
- Vector store qua `VectorStoreService` và Spring AI `VectorStore`.
- Fallback khi Qdrant miss là `ReferenceDataService.buildMetricKnowledgeSnippet(...)`.

Đây là hướng tương đối an toàn cho domain y tế vì knowledge base có thể được kiểm duyệt, version, test và audit.

### What "RAG Online" Means

"RAG online" có thể hiểu theo vài mức:

1. **Online retrieval from internal live data**
   - Ví dụ: lấy reference range mới nhất từ DB, lấy profile/history hiện tại, lấy guideline đã được admin approve.
   - Đây là hướng nên làm.

2. **Online retrieval from external trusted sources**
   - Ví dụ: gọi API/search tới WHO, CDC, NIH, guideline chính thống, hoặc một curated medical source.
   - Có thể làm, nhưng phải có allowlist nguồn, cache, citation, snapshot và moderation.

3. **Open web search RAG**
   - Ví dụ: truy vấn web realtime rồi đưa kết quả vào LLM.
   - Không nên dùng trực tiếp cho HealthLens MVP/production, đặc biệt cho giải thích chỉ số sức khỏe.

### Research Notes

Spring AI hiện có kiến trúc RAG/advisor hỗ trợ các pattern như query rewrite, vector-store RAG và modular RAG. Tài liệu Spring AI cũng mô tả `QuestionAnswerAdvisor`, `VectorStore`, `SearchRequest`, và khả năng rewrite query cho vector store hoặc web search engine.

Các khảo sát RAG trong healthcare năm 2025 nhấn mạnh rằng RAG giúp giảm vấn đề knowledge cutoff nhưng vẫn có rủi ro lớn: hallucination, retrieval precision thấp, nguồn không đáng tin, và thiếu validation lâm sàng. Với medical RAG, hướng tốt hơn là evidence-based retrieval, source credibility ranking, reranking và citation.

### Why Open Web RAG Is Risky Here

Open web RAG tạo thêm rủi ro:

- **Nguồn y tế không kiểm duyệt**: blog, SEO content, forum, nội dung AI-generated.
- **Không ổn định theo thời gian**: cùng một query hôm nay và tháng sau có thể trả nguồn khác.
- **Prompt injection qua retrieved content**: nội dung web có thể chứa instruction độc hại.
- **Khó audit**: nếu không lưu snapshot nguồn, khó giải thích vì sao AI trả lời như vậy.
- **Không phù hợp medical safety**: HealthLens đang giải thích chỉ số sức khỏe, nên nguồn cần được kiểm duyệt.

### Recommended Solution

Không nên chuyển thẳng sang open web RAG. Nên làm theo lộ trình:

1. **Hybrid Curated + Live Internal RAG**
   - Curated corpus vẫn là nguồn chính.
   - Reference data DB là nguồn structured truth.
   - Profile/history của user chỉ dùng khi có consent và access control.

2. **Trusted Online Source Adapter**
   - Chỉ cho phép domain/source allowlist.
   - Có `sourceType`, `sourceUrl`, `publisher`, `retrievedAt`, `snapshotHash`, `license`, `reviewStatus`.
   - Cache kết quả, không gọi web tùy tiện mỗi lần user request.

3. **Source Governance**
   - Admin review/approve nguồn trước khi đưa vào medical explanation.
   - Version corpus theo ngày/release.
   - Lưu citations ngắn và source metadata trong audit/log.

4. **RAG Guardrails**
   - LLM chỉ được dùng retrieved evidence + structured reference range.
   - Nếu không có nguồn đủ tin cậy thì trả lời thận trọng, không suy đoán.
   - Có rerank, threshold score, và `review_required` khi retrieval yếu.

### Priority

P1 nếu mục tiêu là nâng chất lượng AI explanation.

P0 nếu hệ thống bắt đầu trả lời medical advice có nguồn online realtime.

### Local Files

- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java`
- `apps/api/src/main/java/com/healthlens/api/service/VectorStoreService.java`
- `apps/api/src/main/resources/ai/metric-explanations.vi.json`

### External References

- Spring AI RAG reference: https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- Spring AI vector database reference: https://docs.spring.io/spring-ai/reference/api/vectordbs.html
- Healthcare RAG survey: https://link.springer.com/article/10.1007/s00521-025-11666-9
- Medical RAG technical/ethical review: https://arxiv.org/abs/2511.05901

## 25. Toast / Notification UX Chưa Đồng Bộ

### Current State

Frontend chưa có toast provider toàn cục:

- `apps/web/package.json` chưa có `sonner`, `react-hot-toast` hoặc một toast package tương đương.
- `apps/web/src/components/providers.tsx` chỉ wrap `QueryClientProvider` và Radix `Theme`.
- `ConsentModal` tự implement local toast bằng state riêng.
- Nhiều page vẫn dùng `alert(...)`.
- Một số form dùng inline error state (`setSubmitError`, `setSaveError`, `setFormError`) thay vì một notification policy thống nhất.

Ví dụ cụ thể:

- `apps/web/src/app/(dashboard)/home/page.tsx`: dùng `alert` cho update quyền, invite, revoke.
- `apps/web/src/app/(dashboard)/health-records/page.tsx`: dùng pattern tương tự.
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`: `alert("Lưu kết quả khám thành công!")`.
- `apps/web/src/app/admin/login/page.tsx`: `alert('Đã sao chép mã dự phòng')`.
- `apps/web/src/components/features/consent/ConsentModal.tsx`: local toast tự dựng.

### Problem

UX hiện tại không đồng bộ:

- Browser `alert` block UI và trông không cùng design system.
- Toast local trong `ConsentModal` không dùng lại được.
- Error inline và toast chưa có rule rõ: lỗi nào nằm gần field, lỗi nào hiện global.
- Không có một API chung kiểu `notify.success(...)`, `notify.error(...)`.

### Recommended Solution

Tạo notification system toàn cục:

1. Thêm toast provider trong `Providers`.
2. Chọn một thư viện nhẹ như `sonner` hoặc tự xây `ToastProvider` nếu muốn tránh dependency.
3. Tạo wrapper nội bộ:

```ts
notify.success("Đã cập nhật quyền truy cập.");
notify.error(extractApiDetail(error, "Không thể cập nhật quyền truy cập."));
notify.info("Đang xử lý yêu cầu...");
```

4. Chuẩn hóa policy:
   - Field validation: hiện cạnh field.
   - Form submit failure: inline summary + toast nếu cần.
   - Background action success/failure: toast.
   - Blocking/destructive confirmation: modal, không dùng `confirm`.
   - Long-running task: toast loading/progress hoặc status component.

5. Replace toàn bộ `alert(...)`.
6. Refactor `ConsentModal` bỏ toast local, dùng global notification.

### Priority

P1 cho polish/product readiness.

Không phải P0 về correctness, nhưng ảnh hưởng mạnh đến cảm nhận sản phẩm và consistency.

## 26. Ngôn Ngữ Chưa Đồng Bộ: Tiếng Việt Có Dấu

### Current State

Codebase đang trộn:

- Tiếng Việt có dấu.
- Tiếng Việt không dấu.
- English message.
- Mixed English/Vietnamese trong response, email, exception, test name, prompt.

Ví dụ:

- `AuthService`: `"Email nay da duoc dang ky"`, `"Email hoac mat khau khong dung"`, `"Vui long xac thuc email truoc khi dang nhap"`.
- `AuthController`: `"Tai khoan da tao. Kiem tra email de xac thuc."`, `"Mat khau da duoc dat lai thanh cong."`.
- DTO validation: `"Email khong hop le"`, `"Mat khau khong duoc de trong"`.
- `EmailService`: password reset email còn không dấu.
- `ResetPassword` frontend: `"Token khong hop le hoac da het han."`, `"Khong the ket noi den may chu. Vui long thu lai."`.
- `LlmService`: fallback context `"khong ro tuoi"`, `"khong ro gioi tinh"`.

### Problem

Đây không chỉ là vấn đề copywriting:

- Backend trả message không dấu thì frontend hiển thị trực tiếp sẽ thiếu chuyên nghiệp.
- Không có source of truth cho message.
- Khó thêm i18n sau này.
- Tests đang assert message không dấu, khiến việc sửa copy dễ làm vỡ test.
- Email template chưa đồng bộ chất lượng với UI.

### Recommended Solution

1. **Chốt language policy**
   - Product language hiện tại: tiếng Việt có dấu.
   - Tone: rõ ràng, ngắn, thân thiện, không quá kỹ thuật.
   - Medical disclaimer phải nhất quán.

2. **Backend error contract**
   - API nên trả `code` ổn định và `message` có dấu.
   - Frontend nên ưu tiên map theo `code`, không phụ thuộc tuyệt đối vào raw backend message.

Ví dụ:

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Email hoặc mật khẩu không đúng."
}
```

3. **Centralize frontend copy**
   - Tối thiểu tạo `apps/web/src/lib/i18n/messages.ts`.
   - Nếu sau này đa ngôn ngữ thì nâng lên `next-intl` hoặc tương đương.

4. **Centralize backend messages**
   - Tối thiểu tạo enum/error-code constants.
   - Có thể dùng Spring `MessageSource` nếu muốn i18n chuẩn.

5. **Email templates**
   - Đưa toàn bộ email về tiếng Việt có dấu.
   - Không build email HTML dài bằng string inline trong service nếu có thể tránh.

6. **Testing**
   - Tests nên assert error code hoặc visible copy có dấu.
   - Không giữ test copy không dấu như source of truth.

### Priority

P1 cho product readiness.

P0 nếu các message liên quan auth/security/compliance gây hiểu nhầm cho user.

## 27. Code Organization Chưa Thật Sự Clean/Clear

### Current State

Backend đang tổ chức theo technical layer:

- `controller`
- `service`
- `repository`
- `entity`
- `dto`
- `config`
- `security`

Frontend có route-based Next App Router:

- `app/(auth)`
- `app/(dashboard)`
- `app/admin`
- `components/features`
- `components/ui`
- `hooks`
- `lib`
- `stores`

Cấu trúc này đủ dùng cho MVP, nhưng khi feature tăng, một số service/page đã bắt đầu phình to và trộn nhiều trách nhiệm.

### Backend Issues

1. **Service package quá rộng**
   - OCR, LLM, RAG, auth, profile share, reference data, deletion đều nằm chung `service`.
   - Khi thêm provider abstraction, package này sẽ khó đọc hơn.

2. **AI-related code chưa có bounded context rõ**
   - `LlmService`, `EmbeddingService`, `VectorStoreService`, `MetricExplanationRetrievalService`, `MetricExplanationIngestionService`, `OcrService` đang nằm cạnh các business service thông thường.
   - Nên tách nhóm như `ai`, `ocr`, `rag`, `provider`.

3. **Provider-specific config naming**
   - `GroqAiConfig` thể hiện provider cụ thể, trong khi mục tiêu là provider-agnostic.

4. **Long service risk**
   - `HealthRecordService`, `LlmService`, `OcrService`, `EmailService` có dấu hiệu ôm nhiều trách nhiệm.
   - Email HTML inline trong `EmailService` làm service khó test và khó maintain.

5. **Error/message chưa chuẩn**
   - Exception message nằm rải rác trong service/controller/DTO.

### Frontend Issues

1. **Page components làm quá nhiều việc**
   - `health-records/review/[recordId]/page.tsx` chứa nhiều state, validation, API call, rendering và flow control.
   - `home/page.tsx` và `health-records/page.tsx` có logic share/invite/revoke lặp lại.

2. **Notification pattern không tập trung**
   - `alert`, local toast, inline error cùng tồn tại.

3. **Copy nằm rải rác**
   - Không có message catalog.

4. **Feature modules chưa đủ sâu**
   - Có `components/features`, nhưng logic route vẫn nằm nhiều trong `app`.
   - Nên chuyển các flow lớn thành hook/component theo feature.

### Recommended Solution

Không cần big-bang refactor. Nên refactor theo các story có giá trị:

1. **AI package boundary**
   - `com.healthlens.api.ai.chat`
   - `com.healthlens.api.ai.embedding`
   - `com.healthlens.api.ai.rag`
   - `com.healthlens.api.ocr`
   - `com.healthlens.api.provider`

2. **Provider gateway**
   - `AiGateway`
   - `OcrGateway`
   - `RetrievalGateway`
   - adapter registry riêng.

3. **Frontend notification foundation**
   - `components/providers/ToastProvider`
   - `lib/notify.ts`
   - replace alert.

4. **Frontend copy catalog**
   - `lib/i18n/messages.ts`.

5. **Split large pages**
   - Review page: `useRecordReviewState`, `useRecordSave`, `MetricTable`, `RecordMetadataForm`, `OcrFailurePanel`.
   - Home/health-record share logic: shared hook `useProfileSharing`.

6. **Email template cleanup**
   - Move inline HTML to template files.
   - Keep service responsible for composing variables and sending.

### Priority

P1/P2. Không nên làm trước OCR/LLM/RAG reliability P0, nhưng nên đi kèm các story improve để tránh technical debt tăng tiếp.

## 28. Additional Product/Engineering Gaps To Track

Ngoài các phần đã nêu, nên đưa thêm các việc sau vào backlog:

1. **Design system consistency**
   - Button radius, color usage, modal layout, toast placement, error state cần cùng quy chuẩn.

2. **Accessibility**
   - Toast cần `aria-live`.
   - Modal focus trap/keyboard behavior cần kiểm tra.
   - Form error cần liên kết với input bằng `aria-describedby`.

3. **Loading/empty/error states**
   - Một số màn có inline status, một số im lặng hoặc alert.
   - Cần chuẩn hóa state component.

4. **Medical copy review**
   - Các câu AI/recommendation/disclaimer cần review riêng để tránh hiểu là chẩn đoán.

5. **RAG citation UX**
   - Nếu dùng RAG online/trusted source, UI nên hiển thị "Nguồn tham khảo" hoặc metadata nội bộ cho audit/admin.

6. **Admin content governance**
   - Nếu corpus RAG được update thường xuyên, cần màn admin hoặc quy trình PR/review rõ ràng.

## Addendum Priority Update

### New P0 Candidates

- Không dùng open web RAG trực tiếp cho medical answer nếu chưa có allowlist, cache, citation, audit và safety gate.
- Chuẩn hóa message auth/security/compliance có dấu nếu đang hiển thị trực tiếp cho user.

### New P1 Candidates

- Global toast/notification foundation.
- Replace `alert(...)`.
- Vietnamese copy normalization across frontend/backend/email.
- Hybrid curated + internal live RAG.
- RAG source metadata and citation/audit model.
- AI/OCR/RAG package boundary cleanup.

### New P2 Candidates

- Split large route components.
- Extract duplicated sharing hooks.
- Move email inline HTML to templates.
- Add accessibility checks for toast/modal/form errors.
