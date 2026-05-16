---
stepsCompleted:
  - bmad-help-routing
  - step-01-validate-prerequisites-adapted
  - step-02-design-epics-adapted
  - step-03-create-stories-adapted
inputDocuments:
  - _bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md
  - _bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md
  - _bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md
  - _bmad-output/planning-artifacts/review-source/PRODUCTION-READINESS.md
  - _bmad-output/planning-artifacts/review-source/REVIEW-PRODUCTION-MASTER.md
  - _bmad-output/planning-artifacts/review-source/production-review/p0-gates-checklist.md
  - _bmad-output/planning-artifacts/review-source/production-review/audit-logging-mini-adr.md
  - _bmad-output/planning-artifacts/epics.md
  - _bmad-output/planning-artifacts/architecture.md
workflowType: improvement-epics
project_name: health-lens
user_name: ie303
date: "2026-05-16"
documentPurpose: "Backlog bổ sung từ review findings, research và discussion về OCR/LLM/RAG/provider/infrastructure/UI consistency"
---

# HealthLens - Core Feature & Infrastructure Improvement Epics

## Overview

Tài liệu này gom nhóm các vấn đề đã review thành các epics và user stories có thể đưa vào sprint planning hoặc correct-course. Đây là backlog bổ sung, không thay thế file `_bmad-output/planning-artifacts/epics.md` gốc.

Nguồn chính là `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md`, đã tổng hợp:

- Review docs trong `docs/`.
- Codebase check trong `apps/api`, `apps/web`, `services/ocr-service`, `docker`, `.github/workflows`.
- Discussion/research về PaddleOCR, self-host OCR, provider switching, RAG online, toast, ngôn ngữ, tổ chức code.

## Priority Legend

- **P0**: cần làm trước production/go-live hoặc trước khi mở rộng usage có real health data.
- **P1**: cần làm trong improve phase để đạt product/engineering readiness.
- **P2**: cleanup/polish, nên làm sau khi P0/P1 ổn định.

## Epic List

1. **OCR Reliability & Document Intelligence Foundation**
2. **AI Provider Abstraction & Configuration Governance**
3. **LLM/RAG Safety, Quality & Evidence Governance**
4. **Production Infrastructure, Observability & Operations**
5. **Security, Privacy & Compliance Hardening**
6. **Frontend UX Consistency, Language & Accessibility**
7. **Code Organization & Maintainability**
8. **CI/CD Quality Gates & Release Safety**

---

## Epic 1: OCR Reliability & Document Intelligence Foundation

### Epic Goal

Biến OCR từ một flow image-oriented dễ lỗi thành pipeline document-aware, provider-agnostic, có contract chuẩn, retry/DLQ/idempotency, benchmark provider và fallback rõ ràng.

### Story 1.1: MIME-Aware OCR Router For PDF And Image

**Priority:** P0

As a user uploading lab results,
I want the system to process PDFs and images using the correct OCR path,
So that scanned PDFs, text-layer PDFs, and images produce reliable extracted metrics.

**Scope**

- Add MIME-aware OCR job payload: `recordId`, `fileKey`, `mimeType`, `correlationId`, `jobId`.
- Route image files to image OCR provider.
- Route PDF files to PDF-capable flow.
- Detect PDF text layer before rendering/fallback where feasible.
- Keep manual entry/review flow when OCR cannot safely parse.

**Acceptance Criteria**

**Given** a user uploads an image file  
**When** the OCR job starts  
**Then** the router selects an image-capable provider  
**And** the job records selected provider, MIME type, and correlation ID.

**Given** a user uploads a PDF with text layer  
**When** the OCR job starts  
**Then** the router attempts text extraction or a PDF-capable provider before image fallback.

**Given** a user uploads a scanned PDF  
**When** no text layer is available  
**Then** the router uses a document OCR path or page-render fallback  
**And** stores page-level metadata.

**Given** the file MIME type is unsupported  
**When** the job is processed  
**Then** the record is marked as OCR failed with a user-facing recovery path.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 1
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `services/ocr-service/app.py`

### Story 1.2: Provider-Agnostic OCR Result Contract

**Priority:** P0

As a developer maintaining OCR providers,
I want every OCR provider to return a normalized result contract,
So that parser, audit, debugging, and fallback logic do not depend on provider-specific payloads.

**Scope**

- Define normalized OCR result fields:
  - `provider`
  - `modelVersion`
  - `mimeType`
  - `pages`
  - `blocks`
  - `lines`
  - `text`
  - `confidence`
  - `diagnostics`
  - `providerRequestId`
  - `latencyMs`
  - `retentionMode`
- Support page/block/line-level confidence.
- Preserve enough layout metadata for table parsing.

**Acceptance Criteria**

**Given** any OCR provider succeeds  
**When** the result is persisted  
**Then** the stored result includes provider, model version, confidence, diagnostics, latency, and retention mode.

**Given** OCR output contains multiple pages  
**When** the parser reads the OCR result  
**Then** it can access page number and line/block boundaries.

**Given** a provider fails partially  
**When** a fallback provider is attempted  
**Then** diagnostics include failed provider name and failure category without leaking secrets.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 2
- `apps/api/src/main/java/com/healthlens/api/dto/OcrResult.java`

### Story 1.3: OCR Queue Retry, Idempotency And DLQ

**Priority:** P0

As an operations owner,
I want OCR jobs to retry safely and dead-letter poison messages,
So that jobs are not lost, duplicated, or silently acknowledged after failure.

**Scope**

- Add OCR job state machine:
  - `queued`
  - `processing`
  - `succeeded`
  - `failed_retryable`
  - `failed_terminal`
  - `dead_lettered`
- Add idempotency key: `recordId + jobId + fileKey/version`.
- Add retry count, backoff, max attempts.
- Add DLQ stream/table.
- Ack Redis stream only after DB state is persisted.

**Acceptance Criteria**

**Given** DB persistence fails after provider OCR succeeds  
**When** the job handler exits  
**Then** the Redis message is not acknowledged  
**And** retry can resume idempotently.

**Given** a provider timeout occurs  
**When** attempts remain  
**Then** the job is marked `failed_retryable` and scheduled with backoff.

**Given** a poison message exceeds max attempts  
**When** the final attempt fails  
**Then** it is moved to DLQ with failure reason and correlation ID.

**Given** the same job is delivered twice  
**When** the idempotency key already succeeded  
**Then** the second delivery does not duplicate metrics or overwrite confirmed user data.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 4
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`

### Story 1.4: Parser Confidence Gates And Review Required State

**Priority:** P0

As a user reviewing extracted health metrics,
I want low-confidence OCR/parse results to be clearly flagged,
So that incorrect medical values are not treated as reliable.

**Scope**

- Add parse confidence categories:
  - `low_ocr_confidence`
  - `low_parse_confidence`
  - `ambiguous_table_layout`
  - `missing_reference_range`
  - `unit_mismatch`
- Introduce `review_required` state before user confirmation.
- Prevent low-confidence extraction from being treated as fully complete.
- Surface confidence/reason in review UI.

**Acceptance Criteria**

**Given** OCR confidence is below threshold  
**When** metrics are parsed  
**Then** record status becomes `review_required`  
**And** user sees clear reasons before saving.

**Given** parser cannot confidently identify unit or reference range  
**When** review page loads  
**Then** the affected metric is highlighted for correction.

**Given** the user confirms corrected values  
**When** the record is saved  
**Then** metric source captures OCR/manual/corrected provenance.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 3
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`

### Story 1.5: PaddleOCR Adapter And HealthLens Benchmark

**Priority:** P1

As a technical owner,
I want to benchmark PaddleOCR against EasyOCR on HealthLens documents,
So that provider selection is evidence-based instead of assumption-based.

**Scope**

- Add `PaddleOcrProvider` behind OCR provider interface.
- Keep `EasyOcrProvider` available.
- Build benchmark dataset with representative Vietnamese lab result images/PDFs.
- Measure text accuracy, metric name accuracy, value/unit accuracy, reference range accuracy, latency, RAM usage.
- Produce decision report.

**Acceptance Criteria**

**Given** benchmark dataset exists  
**When** EasyOCR and PaddleOCR run against the same files  
**Then** report includes accuracy, latency, memory, error categories, and recommendation.

**Given** PaddleOCR is selected as primary  
**When** `OCR_PROVIDER_PRIMARY=paddleocr`  
**Then** the system uses PaddleOCR without changing core health record flow.

**Given** PaddleOCR fails  
**When** fallback order includes EasyOCR or cloud OCR  
**Then** fallback behavior follows configured policy and records diagnostics.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 5
- `services/ocr-service/README.md`

### Story 1.6: External OCR Fallback Consent, Retention And Kill Switch

**Priority:** P0 if external OCR is enabled, otherwise P1

As a privacy-conscious user,
I want clear controls around sending health documents to external OCR services,
So that sensitive files are processed only with appropriate consent, retention, and audit controls.

**Scope**

- Add external OCR consent policy.
- Add provider retention mode config.
- Add region/cross-border config.
- Add kill switch for each external OCR provider.
- Audit provider request ID, retention mode, region, and fallback reason.

**Acceptance Criteria**

**Given** external OCR is disabled by kill switch  
**When** local OCR fails  
**Then** system does not call external provider  
**And** user is routed to manual/retry flow.

**Given** external OCR is enabled  
**When** a document is sent to cloud OCR  
**Then** audit metadata includes provider, request ID, retention mode, and correlation ID.

**Given** consent is missing  
**When** fallback would require external OCR  
**Then** the system blocks external transfer and explains the next option.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 7 and 8
- `apps/api/src/main/java/com/healthlens/api/service/GoogleCloudVisionClient.java`
- `apps/api/src/main/java/com/healthlens/api/service/AwsTextractClient.java`

---

## Epic 2: AI Provider Abstraction & Configuration Governance

### Epic Goal

Cho phép đổi provider đã implement bằng env/config một cách thật sự an toàn, fail-fast khi cấu hình sai, và không để provider-specific code rò vào core business flow.

### Story 2.1: Generic AI Chat Provider Configuration

**Priority:** P1

As a deployer,
I want LLM provider configuration to use generic AI names instead of Groq-specific names,
So that switching OpenAI-compatible providers is clear and low-risk.

**Scope**

- Introduce generic env keys:
  - `AI_CHAT_PROVIDER`
  - `AI_CHAT_BASE_URL`
  - `AI_CHAT_API_KEY`
  - `AI_CHAT_MODEL`
  - `AI_CHAT_TIMEOUT_MS`
- Deprecate or alias `GROQ_*`.
- Rename provider-specific config/classes where appropriate.
- Document supported provider classes: OpenAI-compatible vs native SDK providers.

**Acceptance Criteria**

**Given** an OpenAI-compatible provider is configured  
**When** the app starts  
**Then** chat client is created using generic `AI_CHAT_*` env keys.

**Given** required API key/model/base URL is missing  
**When** production profile starts  
**Then** startup fails with a clear configuration error.

**Given** legacy `GROQ_*` env exists  
**When** generic env is absent  
**Then** app either maps legacy values with warning or fails according to documented migration policy.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 12
- `apps/api/src/main/java/com/healthlens/api/config/GroqAiConfig.java`
- `apps/api/src/main/resources/application.yml`

### Story 2.2: OCR Provider Registry And Capability Routing

**Priority:** P0/P1

As a backend developer,
I want OCR providers registered by capability,
So that adding PaddleOCR, Textract, or Google OCR does not require changing core health record logic.

**Scope**

- Create `OcrProvider` interface.
- Add provider registry keyed by provider name and capability.
- Capabilities include `IMAGE_OCR`, `PDF_TEXT`, `PDF_SCAN`, `DOCUMENT_LAYOUT`.
- Move hardcoded `switch` logic into registry/router.
- Keep fallback order config.

**Acceptance Criteria**

**Given** providers are configured  
**When** OCR router receives image or PDF job  
**Then** provider selection uses declared capabilities and fallback order.

**Given** a new provider is added  
**When** it implements `OcrProvider` and registers capabilities  
**Then** no health record service logic changes are required.

**Given** configured primary provider is unsupported  
**When** production starts  
**Then** app fails fast with provider validation error.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 12 and 13
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`

### Story 2.3: Embedding And Vector Store Config Validation

**Priority:** P1

As a deployer,
I want embedding model dimension and vector store configuration validated at startup,
So that RAG does not silently fail or produce low-quality retrieval.

**Scope**

- Validate embedding model dimension against Qdrant collection dimension.
- Clarify `QDRANT_HOST` format and port expectations.
- Fail fast on host containing protocol if client expects host only.
- Document reindex requirement when embedding model changes.
- Add startup health validation for vector store.

**Acceptance Criteria**

**Given** embedding dimension is 1536 but collection is 1024  
**When** app starts in production  
**Then** startup fails with a clear reindex/dimension mismatch message.

**Given** `QDRANT_HOST` includes `https://` when unsupported  
**When** config is loaded  
**Then** app fails fast or normalizes according to documented rules.

**Given** vector store is unavailable  
**When** health check runs  
**Then** the AI/RAG health status reports degraded/unavailable.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 13
- `apps/api/src/main/resources/application-docker.yml`
- `apps/api/src/main/java/com/healthlens/api/service/EmbeddingService.java`
- `apps/api/src/main/java/com/healthlens/api/service/VectorStoreService.java`

### Story 2.4: Provider Switching Documentation And Runbook

**Priority:** P1

As an operator,
I want a provider switching runbook,
So that changing LLM/OCR/embedding/RAG providers is predictable and reversible.

**Scope**

- Document which providers are switchable by env only.
- Document which providers require adapter code.
- Document rollback steps.
- Include required smoke tests.
- Include expected cost/latency/privacy tradeoffs.

**Acceptance Criteria**

**Given** a deployer wants to switch LLM provider  
**When** they read the runbook  
**Then** they can identify env keys, compatibility limits, smoke tests, and rollback steps.

**Given** a provider requires native SDK support  
**When** the runbook describes it  
**Then** it clearly says env-only switching is not enough.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 12 and 14

---

## Epic 3: LLM/RAG Safety, Quality & Evidence Governance

### Epic Goal

Đảm bảo AI explanation/recommendation dựa trên nguồn có kiểm soát, prompt versioned/testable, RAG có governance, và online RAG không tạo rủi ro medical safety.

### Story 3.1: Externalized Versioned LLM Prompt Templates

**Priority:** P1

As an AI feature maintainer,
I want prompts stored as versioned templates outside large Java strings,
So that prompt behavior can be reviewed, tested, and rolled back.

**Scope**

- Move LLM prompts from inline Java string to resource templates.
- Add prompt version metadata.
- Add tests for template rendering and required guardrail clauses.
- Record prompt version in AI output metadata.

**Acceptance Criteria**

**Given** a recommendation is generated  
**When** AI metadata is persisted/logged  
**Then** prompt version and model version are available for audit.

**Given** a prompt template is edited  
**When** tests run  
**Then** tests verify required medical disclaimer and no-invent-reference-ranges instruction.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 9
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`

### Story 3.2: Structured LLM Output Schema And Validation

**Priority:** P0/P1

As a user receiving AI explanations,
I want AI output to follow a strict schema,
So that malformed or hallucinated responses are rejected or safely degraded.

**Scope**

- Define JSON schema for AI explanation/recommendation output.
- Replace fragile JSON repair with schema validation and controlled retry.
- Add safe fallback explanation when output is invalid.
- Add metrics for invalid schema, retry count, token usage, latency.

**Acceptance Criteria**

**Given** LLM returns invalid JSON  
**When** schema validation fails  
**Then** system retries according to policy or returns safe fallback.

**Given** LLM output invents a reference range not present in structured data  
**When** validation runs  
**Then** output is rejected or corrected through fallback.

**Given** AI generation completes  
**When** metrics are recorded  
**Then** latency, model, token usage, outcome, and validation status are captured.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 9 and 10
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`

### Story 3.3: RAG Corpus Governance And Admin Review Workflow

**Priority:** P1

As an admin maintaining medical explanation content,
I want RAG corpus changes to be versioned, reviewed, and auditable,
So that AI explanations use trustworthy Vietnamese medical context.

**Scope**

- Add corpus metadata: source version, reviewer, approval status, effective date.
- Add ingestion report: inserted/updated/deleted chunks, errors, embedding model/dimension.
- Define review workflow for `metric-explanations.vi.json` or future admin-managed corpus.
- Ensure rollback to previous corpus version.

**Acceptance Criteria**

**Given** a corpus update is ingested  
**When** ingestion completes  
**Then** report includes source version, chunk count, embedding model, and errors.

**Given** a corpus chunk is not approved  
**When** retrieval runs  
**Then** the chunk is not used in user-facing explanation.

**Given** a corpus version causes issues  
**When** admin rolls back  
**Then** previous approved version becomes active.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 11
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionService.java`
- `apps/api/src/main/resources/ai/metric-explanations.vi.json`

### Story 3.4: Hybrid Curated + Internal Live RAG

**Priority:** P1

As a user receiving explanations,
I want AI explanations grounded in curated corpus plus current internal reference data,
So that outputs stay relevant without relying on uncontrolled web search.

**Scope**

- Use curated corpus as primary medical explanation context.
- Use reference data DB as structured truth for ranges and aliases.
- Optionally include user profile/history only with access control and consent.
- Add retrieval trace: source, hit/miss, score, fallback path.

**Acceptance Criteria**

**Given** Qdrant returns a relevant approved chunk  
**When** explanation is generated  
**Then** the prompt includes that chunk and structured reference range.

**Given** vector retrieval misses  
**When** fallback runs  
**Then** system uses internal reference data snippet, not open web.

**Given** user profile context is included  
**When** retrieval/prompt construction runs  
**Then** access control and consent are checked.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 24
- `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationRetrievalService.java`

### Story 3.5: Trusted Online RAG Source Adapter With Citation And Cache

**Priority:** P1, P0 if external online medical sources are user-facing

As a product owner,
I want online RAG limited to trusted and auditable sources,
So that HealthLens can use updated knowledge without exposing users to unvetted web content.

**Scope**

- Add trusted source allowlist.
- Add `sourceType`, `sourceUrl`, `publisher`, `retrievedAt`, `snapshotHash`, `license`, `reviewStatus`.
- Cache retrieved content snapshots.
- Prevent open web search from directly entering user-facing prompt.
- Add citation/audit metadata.

**Acceptance Criteria**

**Given** a source is not allowlisted  
**When** online retrieval is attempted  
**Then** the source is rejected.

**Given** a trusted source is retrieved  
**When** content is used  
**Then** snapshot metadata is persisted for audit.

**Given** retrieved online evidence is low quality or unreviewed  
**When** AI output is generated  
**Then** system excludes it or marks review required.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 24

---

## Epic 4: Production Infrastructure, Observability & Operations

### Epic Goal

Đưa production baseline từ mức "deploy được" lên mức "vận hành được": observability, alerting, runbook, backup/restore, deploy/rollback và service topology đầy đủ.

### Story 4.1: Unified Correlation ID And Audit Spine

**Priority:** P0

As a compliance and operations owner,
I want every user action and background AI/OCR job correlated end-to-end,
So that incidents and privacy audits can be reconstructed reliably.

**Scope**

- Implement correlation ID propagation across web/API/job logs.
- Add audit events for upload, OCR, LLM, RAG retrieval, provider fallback, data deletion, admin actions.
- Include user/profile/record IDs where allowed and redacted where required.
- Ensure logs avoid leaking PHI in plaintext.

**Acceptance Criteria**

**Given** a user uploads a file  
**When** OCR and AI explanation complete  
**Then** logs/audit events share a correlation ID across request, job, provider calls, and DB updates.

**Given** an admin reviews audit logs  
**When** filtering by record ID or correlation ID  
**Then** they can trace upload, OCR result, LLM/RAG calls, and final user confirmation.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 16 and 23
- `_bmad-output/implementation-artifacts/epic-7/7-6-unified-audit-spine-with-correlation-ids.md`

### Story 4.2: Observability Metrics And Alerting Baseline

**Priority:** P1

As an operator,
I want dashboards and alerts for OCR/LLM/RAG/API health,
So that failures are visible before users report them.

**Scope**

- Metrics:
  - OCR success/fail rate by provider.
  - OCR latency p50/p95/p99.
  - OCR DLQ size.
  - LLM latency, token usage, invalid schema rate.
  - RAG retrieval hit/miss/top score.
  - API error rate and latency.
- Alert rules for P0 failure conditions.
- Structured logs with correlation ID.

**Acceptance Criteria**

**Given** OCR failure rate exceeds threshold  
**When** alert evaluation runs  
**Then** an alert is triggered with provider and failure reason.

**Given** RAG hit rate drops unexpectedly  
**When** dashboard is viewed  
**Then** operator can see source, hit/miss, top score, and latency trends.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 15

### Story 4.3: Production Deploy Topology And Rollback Hardening

**Priority:** P0/P1

As a release owner,
I want production deployment to include all required services and rollback gates,
So that API/Web/OCR/Redis/Qdrant dependencies are deployed predictably.

**Scope**

- Ensure OCR service is included in production compose/deploy topology.
- Clarify Swarm/Kubernetes vs plain Docker Compose behavior.
- Add deploy health gates for API, web, OCR, DB, Redis, Qdrant.
- Add rollback procedure and smoke tests.
- Document migration strategy.

**Acceptance Criteria**

**Given** production deploy runs  
**When** deploy completes  
**Then** API, web, OCR, Redis, DB, storage, and vector store health checks pass.

**Given** a post-deploy smoke test fails  
**When** rollback is triggered  
**Then** previous stable version is restored according to runbook.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 14
- `.github/workflows/deploy.yml`
- `docker/compose.prod.yml`

### Story 4.4: Backup, Restore And Operational Runbook

**Priority:** P0

As an operator,
I want verified backup/restore procedures and runbooks,
So that health records, files, and vector data can be recovered after incidents.

**Scope**

- Backup PostgreSQL, object storage metadata/files, and Qdrant collection strategy.
- Define RPO/RTO.
- Run restore drill.
- Document incident procedures for OCR outage, LLM outage, DB issue, provider cost spike.

**Acceptance Criteria**

**Given** a backup exists  
**When** restore drill is executed  
**Then** application can read restored users/profiles/records/files.

**Given** Qdrant data is not backed up directly  
**When** restore is needed  
**Then** system can reingest corpus from source version and embedding config.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 17

---

## Epic 5: Security, Privacy & Compliance Hardening

### Epic Goal

Hoàn thiện các hardening còn thiếu cho JWT/CSRF/CORS/SSRF, admin session, right-to-delete, provider privacy và data deletion correctness.

### Story 5.1: Security Config Fail-Fast Hardening

**Priority:** P0

As a security owner,
I want insecure production configuration to fail startup,
So that weak JWT secrets, wildcard CORS, or missing CSRF controls never reach production.

**Scope**

- Validate JWT secret strength.
- Validate CORS origins no wildcard in production.
- Confirm CSRF strategy for cookie/session endpoints.
- Validate OCR/service outbound domain allowlists.
- Fail fast with clear error messages.

**Acceptance Criteria**

**Given** production profile has wildcard CORS  
**When** app starts  
**Then** startup fails with CORS validation error.

**Given** JWT secret is weak or default  
**When** app starts  
**Then** startup fails before serving traffic.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 18
- `_bmad-output/implementation-artifacts/epic-10/10-1-production-security-hardening.md`

### Story 5.2: SSRF Protection For OCR And Remote File Fetching

**Priority:** P0

As a security owner,
I want OCR file fetching protected against SSRF,
So that internal networks and metadata endpoints cannot be accessed through OCR inputs.

**Scope**

- Restrict OCR service to trusted storage domains or signed object URLs.
- Block private IP ranges, localhost, metadata IPs.
- Add request timeout and max file size.
- Add audit/logging for blocked attempts.

**Acceptance Criteria**

**Given** OCR input URL resolves to private IP  
**When** OCR service attempts fetch  
**Then** request is blocked and logged.

**Given** OCR input URL is from allowed storage domain  
**When** OCR fetches it  
**Then** request proceeds with timeout and size limits.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 8 and 18
- `services/ocr-service/app.py`

### Story 5.3: Admin Session Storage Hardening

**Priority:** P0/P1

As an admin,
I want admin authentication tokens protected from script-accessible storage,
So that XSS impact is reduced.

**Scope**

- Move admin token/session away from `sessionStorage`.
- Use HttpOnly secure cookie or server-backed session strategy.
- Add CSRF protection where cookies are used.
- Update admin API client and login/logout flows.

**Acceptance Criteria**

**Given** admin logs in successfully  
**When** browser storage is inspected by JavaScript  
**Then** admin access token is not available in `sessionStorage` or localStorage.

**Given** admin calls protected endpoint  
**When** CSRF token/session requirements are missing  
**Then** request is rejected.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 20
- `apps/web/src/app/admin/login/page.tsx`
- `apps/web/src/lib/api/adminApiClient.ts`

### Story 5.4: Right-To-Delete Race And Cleanup Correctness

**Priority:** P0

As a user exercising right-to-delete,
I want account and health data deletion to be complete and race-safe,
So that privacy obligations are met within the required window.

**Scope**

- Hash deletion/cancel tokens.
- Remove sensitive tokens/email from URLs where feasible.
- Add row locking / `SELECT FOR UPDATE SKIP LOCKED` or equivalent race prevention.
- Clean storage objects, DB rows, orphan references, audit-safe metadata.
- Handle cancel/delete race.

**Acceptance Criteria**

**Given** deletion job and cancel request race  
**When** both execute concurrently  
**Then** only one valid final state is committed.

**Given** deletion completes  
**When** cleanup verification runs  
**Then** user health records, files, refresh tokens, invites, and profile shares are removed or anonymized according to policy.

**Given** deletion token leaks from logs  
**When** inspected  
**Then** raw token is not present because stored/logged value is hashed or redacted.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 21
- `_bmad-output/implementation-artifacts/epic-10/10-3-right-to-delete-hardening.md`
- `_bmad-output/implementation-artifacts/epic-10/10-11-data-deletion-integrity-hardening.md`

---

## Epic 6: Frontend UX Consistency, Language & Accessibility

### Epic Goal

Chuẩn hóa trải nghiệm UI: toast, error handling, tiếng Việt có dấu, copy y tế, loading/empty/error states và accessibility cho các flow chính.

### Story 6.1: Global Toast Notification Foundation

**Priority:** P1

As a user,
I want success and failure notifications to look and behave consistently,
So that I can trust app feedback without disruptive browser alerts.

**Scope**

- Add global toast provider in `apps/web/src/components/providers.tsx`.
- Add `notify.success/error/info/loading` wrapper.
- Ensure toast has `aria-live`.
- Define notification policy:
  - field validation near field,
  - background actions use toast,
  - destructive confirmation uses modal,
  - long-running task uses progress/status.

**Acceptance Criteria**

**Given** any page triggers a success notification  
**When** `notify.success` is called  
**Then** a consistent toast appears with accessible live region.

**Given** user performs a background action  
**When** it fails  
**Then** error toast uses standardized copy and does not block UI.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 25
- `apps/web/src/components/providers.tsx`

### Story 6.2: Replace Browser Alerts And Local Toasts

**Priority:** P1

As a user,
I want all app notifications to use the same UI pattern,
So that dashboard, review, admin, and consent flows feel coherent.

**Scope**

- Replace `alert(...)` in:
  - home dashboard sharing actions,
  - health records sharing actions,
  - record review save success,
  - admin backup code copy.
- Refactor `ConsentModal` to use global notification.
- Keep inline form errors where field-level correction is required.

**Acceptance Criteria**

**Given** codebase is searched for `alert(`  
**When** replacement is complete  
**Then** no production UI path uses browser alert for normal success/failure notifications.

**Given** consent is accepted or rejected  
**When** feedback is shown  
**Then** the global toast system is used.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 25
- `apps/web/src/components/features/consent/ConsentModal.tsx`
- `apps/web/src/app/(dashboard)/home/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`
- `apps/web/src/app/admin/login/page.tsx`

### Story 6.3: Vietnamese Language Normalization And Message Catalog

**Priority:** P1, P0 for auth/security/compliance messages

As a Vietnamese user,
I want all user-facing messages to use proper Vietnamese with diacritics,
So that the product feels professional and clear.

**Scope**

- Create frontend message catalog.
- Normalize frontend visible copy to Vietnamese with diacritics.
- Normalize backend validation/error/email messages.
- Prefer stable backend error codes with localized frontend messages.
- Update tests away from no-diacritic strings where user-facing.

**Acceptance Criteria**

**Given** auth error occurs  
**When** user sees the message  
**Then** it is Vietnamese with diacritics and consistent tone.

**Given** backend returns an error  
**When** frontend maps it  
**Then** stable error code is preferred over brittle raw message matching.

**Given** password reset email is sent  
**When** user opens it  
**Then** email copy uses Vietnamese with diacritics.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 26
- `apps/api/src/main/java/com/healthlens/api/service/AuthService.java`
- `apps/api/src/main/java/com/healthlens/api/service/EmailService.java`
- `apps/web/src/app/(auth)/reset-password/page.tsx`

### Story 6.4: Standard Loading, Empty And Error State Components

**Priority:** P1/P2

As a user,
I want loading, empty, and error states to be clear and consistent,
So that I know what is happening and what to do next.

**Scope**

- Create reusable `LoadingState`, `EmptyState`, `ErrorState`, `InlineFieldError`.
- Apply to high-traffic flows:
  - profile list,
  - history,
  - record review,
  - upload/OCR status,
  - admin reference data.
- Add accessibility attributes.

**Acceptance Criteria**

**Given** a query is loading  
**When** page renders  
**Then** standardized loading state appears without layout jump.

**Given** no health records exist  
**When** user visits history  
**Then** standardized empty state includes clear next action.

**Given** an error is recoverable  
**When** error state appears  
**Then** user sees retry or next-step action.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 28

### Story 6.5: Medical Copy And Disclaimer Review

**Priority:** P1

As a user reading AI health explanations,
I want the language to be clear that HealthLens is informational,
So that I do not mistake the app for diagnosis or medical instruction.

**Scope**

- Review AI recommendation copy.
- Standardize disclaimers.
- Ensure abnormal metric warnings advise professional consultation without panic.
- Add tests/checks for required disclaimer text in AI outputs.

**Acceptance Criteria**

**Given** AI gives lifestyle recommendations  
**When** user views them  
**Then** copy includes concise medical disclaimer.

**Given** a metric is abnormal  
**When** explanation is shown  
**Then** it avoids diagnosis language and recommends appropriate follow-up.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 10 and 28
- `apps/api/src/main/java/com/healthlens/api/service/LlmService.java`

---

## Epic 7: Code Organization & Maintainability

### Epic Goal

Giảm technical debt đang tăng: tách bounded context AI/OCR/RAG/provider, split page lớn, gom logic lặp và email templates.

### Story 7.1: Backend AI/OCR/RAG Package Boundary Refactor

**Priority:** P1/P2

As a backend developer,
I want AI/OCR/RAG code grouped by bounded context,
So that provider abstraction and future AI changes do not make the generic service package harder to maintain.

**Scope**

- Introduce packages such as:
  - `com.healthlens.api.ai.chat`
  - `com.healthlens.api.ai.embedding`
  - `com.healthlens.api.ai.rag`
  - `com.healthlens.api.ocr`
  - `com.healthlens.api.provider`
- Move classes incrementally.
- Keep public behavior unchanged.
- Update tests/imports.

**Acceptance Criteria**

**Given** package refactor is complete  
**When** backend tests run  
**Then** behavior remains unchanged.

**Given** a developer needs OCR provider code  
**When** browsing source  
**Then** provider/router/contract classes are grouped under OCR/provider packages.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 27

### Story 7.2: Split Health Record Review Page Into Feature Components And Hooks

**Priority:** P1/P2

As a frontend developer,
I want the record review page split into focused hooks and components,
So that OCR failure, metric editing, saving, and metadata forms can evolve safely.

**Scope**

- Extract:
  - `useRecordReviewState`
  - `useRecordSave`
  - `MetricTable`
  - `RecordMetadataForm`
  - `OcrFailurePanel`
  - `ReviewActions`
- Preserve current UX and route behavior.
- Add/maintain tests for save, edit, delete, retry upload, keep partial.

**Acceptance Criteria**

**Given** record review page loads  
**When** user edits and saves metrics  
**Then** behavior matches pre-refactor flow.

**Given** OCR failed  
**When** user chooses manual entry or keep partial  
**Then** extracted components handle the same transitions.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 27
- `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`

### Story 7.3: Extract Shared Profile Sharing Hook

**Priority:** P2

As a frontend developer,
I want profile sharing mutations reused across dashboard pages,
So that invite/update/revoke behavior stays consistent.

**Scope**

- Extract `useProfileSharing`.
- Reuse in home and health-records pages.
- Replace duplicated alert/error logic with global notification.
- Keep query invalidation behavior.

**Acceptance Criteria**

**Given** user updates access level from home page  
**When** mutation succeeds  
**Then** shared hook invalidates relevant queries and shows standardized toast.

**Given** same action happens from health-records page  
**When** mutation fails  
**Then** error behavior is identical.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 27
- `apps/web/src/app/(dashboard)/home/page.tsx`
- `apps/web/src/app/(dashboard)/health-records/page.tsx`

### Story 7.4: Email Template Cleanup

**Priority:** P2, P1 for user-facing auth/privacy emails

As a maintainer,
I want email HTML stored in templates instead of long inline strings,
So that copy, localization, and tests are easier to manage.

**Scope**

- Move password reset, deletion, cancellation, invitation emails to template files.
- Ensure all templates use Vietnamese with diacritics.
- Keep variable substitution explicit and tested.

**Acceptance Criteria**

**Given** password reset email is sent  
**When** template is rendered  
**Then** output uses the template file and correct variables.

**Given** email copy changes  
**When** tests run  
**Then** template rendering remains valid.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 26 and 27
- `apps/api/src/main/java/com/healthlens/api/service/EmailService.java`
- `apps/api/src/main/resources/templates/email/*`

---

## Epic 8: CI/CD Quality Gates & Release Safety

### Epic Goal

Thêm automated gates để bắt security, dependency, static analysis, Docker image và secret issues trước deploy.

### Story 8.1: CI Security Scan Baseline

**Priority:** P1

As a release owner,
I want dependency, SAST, container, and secret scans in CI,
So that high-risk issues are caught before deployment.

**Scope**

- Add dependency scanning for Java/Node/Python.
- Add SAST.
- Add Docker image scanning for API/Web/OCR images.
- Add secret scanning.
- Define severity gates.

**Acceptance Criteria**

**Given** CI runs on pull request  
**When** scans complete  
**Then** results are visible in checks.

**Given** critical vulnerability is detected  
**When** severity gate evaluates  
**Then** CI fails.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 19
- `_bmad-output/implementation-artifacts/epic-10/10-9-ci-cd-security-gates.md`
- `.github/workflows/ci.yml`

### Story 8.2: AI/OCR Regression Test Corpus In CI

**Priority:** P1

As a quality owner,
I want OCR/parser/AI regression tests to run against representative fixtures,
So that improvements do not silently break core health metric extraction.

**Scope**

- Add sanitized fixture set for Vietnamese lab results.
- Add OCR parser regression tests independent of live OCR provider where possible.
- Add LLM schema validation tests with mocked model responses.
- Add RAG retrieval tests for known metrics/aliases.

**Acceptance Criteria**

**Given** parser code changes  
**When** CI runs  
**Then** fixture-based tests verify metric name/value/unit/reference extraction.

**Given** LLM service changes  
**When** CI runs  
**Then** invalid JSON and hallucinated range cases are tested.

**Given** RAG corpus changes  
**When** CI runs  
**Then** known metric queries return expected approved chunks.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 3, 9, 11
- `apps/api/src/test/java/com/healthlens/api/service/*`

### Story 8.3: Release Smoke Tests For Provider And Infrastructure Config

**Priority:** P1

As a release owner,
I want deploy-time smoke tests for configured providers and infrastructure,
So that bad env/config is caught before users hit broken flows.

**Scope**

- Smoke test API health.
- Smoke test OCR service health.
- Smoke test configured LLM/embedding provider in safe mode.
- Smoke test Qdrant connection/dimension.
- Smoke test Redis stream publish/consume in non-destructive mode.

**Acceptance Criteria**

**Given** deployment completes  
**When** smoke suite runs  
**Then** API, OCR, Redis, Qdrant, and AI provider checks pass or deployment fails.

**Given** provider key is invalid  
**When** smoke test calls provider health endpoint  
**Then** release is blocked before user traffic is shifted.

**Source References**

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 14, 15, 23
- `.github/workflows/deploy.yml`

---

## Coverage Map

| Issue Area | Covered By Stories |
| --- | --- |
| PDF OCR broken by image-oriented flow | 1.1 |
| OCR contract too thin | 1.2 |
| OCR ack/DB race, no retry/DLQ | 1.3 |
| Parser confidence and manual review | 1.4 |
| PaddleOCR vs EasyOCR decision | 1.5 |
| External OCR consent/retention/kill-switch | 1.6 |
| Provider switching by env/config | 2.1, 2.2, 2.3, 2.4 |
| Qdrant/embedding config footguns | 2.3 |
| LLM prompt/version/schema issues | 3.1, 3.2 |
| RAG corpus governance | 3.3 |
| RAG online risks | 3.4, 3.5 |
| Correlation ID/audit | 4.1 |
| Observability/alerting | 4.2 |
| Deploy topology/rollback | 4.3 |
| Backup/restore/runbook | 4.4 |
| Security hardening | 5.1, 5.2 |
| Admin token storage | 5.3 |
| Right-to-delete/data cleanup | 5.4 |
| Toast consistency | 6.1, 6.2 |
| Vietnamese copy normalization | 6.3 |
| Loading/empty/error/accessibility | 6.4 |
| Medical copy/disclaimer | 6.5 |
| Code organization | 7.1, 7.2, 7.3, 7.4 |
| CI/CD security and regression gates | 8.1, 8.2, 8.3 |

## Recommended Implementation Order

### First P0 Batch

1. Story 1.1: MIME-Aware OCR Router For PDF And Image
2. Story 1.2: Provider-Agnostic OCR Result Contract
3. Story 1.3: OCR Queue Retry, Idempotency And DLQ
4. Story 1.4: Parser Confidence Gates And Review Required State
5. Story 4.1: Unified Correlation ID And Audit Spine
6. Story 5.1: Security Config Fail-Fast Hardening
7. Story 5.2: SSRF Protection For OCR And Remote File Fetching
8. Story 5.4: Right-To-Delete Race And Cleanup Correctness

### Second P1 Batch

1. Story 2.1: Generic AI Chat Provider Configuration
2. Story 2.2: OCR Provider Registry And Capability Routing
3. Story 2.3: Embedding And Vector Store Config Validation
4. Story 3.1: Externalized Versioned LLM Prompt Templates
5. Story 3.2: Structured LLM Output Schema And Validation
6. Story 3.3: RAG Corpus Governance And Admin Review Workflow
7. Story 3.4: Hybrid Curated + Internal Live RAG
8. Story 4.2: Observability Metrics And Alerting Baseline
9. Story 6.1: Global Toast Notification Foundation
10. Story 6.3: Vietnamese Language Normalization And Message Catalog
11. Story 8.1: CI Security Scan Baseline
12. Story 8.2: AI/OCR Regression Test Corpus In CI

### Later P1/P2 Batch

1. Story 1.5: PaddleOCR Adapter And HealthLens Benchmark
2. Story 2.4: Provider Switching Documentation And Runbook
3. Story 3.5: Trusted Online RAG Source Adapter With Citation And Cache
4. Story 4.3: Production Deploy Topology And Rollback Hardening
5. Story 4.4: Backup, Restore And Operational Runbook
6. Story 6.2: Replace Browser Alerts And Local Toasts
7. Story 6.4: Standard Loading, Empty And Error State Components
8. Story 6.5: Medical Copy And Disclaimer Review
9. Story 7.1: Backend AI/OCR/RAG Package Boundary Refactor
10. Story 7.2: Split Health Record Review Page Into Feature Components And Hooks
11. Story 7.3: Extract Shared Profile Sharing Hook
12. Story 7.4: Email Template Cleanup
13. Story 8.3: Release Smoke Tests For Provider And Infrastructure Config

## Notes For Sprint Planning

- Không nên làm refactor package lớn trước khi khóa contract OCR/provider; nếu refactor trước, dễ phải đổi lại.
- Không nên bật open web RAG user-facing trước khi Story 3.5 có allowlist/cache/citation/audit.
- PaddleOCR nên đi sau provider registry, vì nếu làm trước sẽ dễ thành thay thế hardcoded một provider bằng một provider hardcoded khác.
- Toast/language work có thể chạy song song với backend P0 nếu write sets tách biệt.
- Security/data deletion stories nên được review kỹ bằng code review và test race/concurrency.
