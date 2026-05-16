# Core Improvements Duplicate Story Audit

Date: 2026-05-16

Status: historical audit, synchronization applied

## Purpose

Rà soát nguy cơ trùng scope giữa backlog/story mới trong `epic-core-improvements` và các implementation story đã có trong `_bmad-output/implementation-artifacts`.

## Resolution Applied

Theo quyết định mới, toàn bộ improvement stories được canonical hóa về:

`_bmad-output/implementation-artifacts/epic-core-improvements/`

Các story cũ đã được xóa khỏi vị trí cũ sau khi được đồng bộ/supersede:

- `_bmad-output/implementation-artifacts/epic-3/3-7-ocr-router-by-mime-type.md`
- `_bmad-output/implementation-artifacts/epic-7/7-6-unified-audit-spine-with-correlation-ids.md`
- `_bmad-output/implementation-artifacts/epic-8/8-4-persisted-product-event-tracking-foundation.md`
- `_bmad-output/implementation-artifacts/epic-10/*.md`

Vì vậy, các bảng bên dưới nên được đọc như **audit lịch sử trước khi sync**, không phải hướng dẫn reuse file cũ nữa.

## Summary

Có **1 trùng trực tiếp** cần xử lý trước khi dev:

- `epic-core-improvements/epic-1-ocr-reliability/1-1-mime-aware-ocr-router-for-pdf-and-image.md`
- `_bmad-output/implementation-artifacts/epic-3/3-7-ocr-router-by-mime-type.md`

Ngoài ra có một số **overlap một phần** giữa các story mới và story cũ. Các overlap này không nhất thiết phải xóa, nhưng cần ghi rõ story nào là canonical để tránh implement song song cùng một scope.

## Full Canonical Mapping For Future `epic-core-improvements` Story Creation

Use this table before creating additional implementation story files under `_bmad-output/implementation-artifacts/epic-core-improvements`.

| Planned improvement story | Existing overlap | Duplication level | Recommendation |
| --- | --- | --- | --- |
| 1.1 MIME-Aware OCR Router For PDF And Image | `epic-3/3-7-ocr-router-by-mime-type.md` | Direct duplicate | Keep new split story as canonical; mark old 3.7 superseded. |
| 1.2 Provider-Agnostic OCR Result Contract | `epic-3/3-7-ocr-router-by-mime-type.md` | Partial/direct overlap | Keep new split story; mark contract portion of 3.7 superseded. |
| 1.3 OCR Queue Retry, Idempotency And DLQ | `epic-8/8-4-persisted-product-event-tracking-foundation.md` only mentions OCR outcome events | Low overlap | Create/keep new story. 8.4 is analytics events, not queue reliability. |
| 1.4 Parser Confidence Gates And Review Required State | `epic-3/3-3`, `epic-3/3-5`, `epic-3/3-6`, `epic-3/3-7` | Partial overlap | Keep as hardening story. Coordinate provenance with 3.6. |
| 1.5 PaddleOCR Adapter And HealthLens Benchmark | `epic-1/1-9-easyocr-service-setup.md` | Low overlap | Create/keep new story. It builds on EasyOCR baseline. |
| 1.6 External OCR Fallback Consent, Retention And Kill Switch | `epic-10/10-1`, `epic-10/10-10`, `epic-7/7-6` | Partial overlap | Keep only OCR-provider-specific policy. Depend on 10.1/10.10/7.6; do not duplicate generic SSRF/audit/consent infrastructure. |
| 2.1 Generic AI Chat Provider Configuration | `epic-1/1-7-groq-api-setup.md` | Partial overlap | Create new story as refactor/hardening. Existing 1.7 is done Groq setup, not provider-neutral cleanup. |
| 2.2 OCR Provider Registry And Capability Routing | `epic-3/3-7`, new 1.1/1.2/1.6 | Partial overlap | Create only after deciding whether provider registry belongs in Epic 1 or Epic 2. Avoid duplicating routing already in 1.1. |
| 2.3 Embedding And Vector Store Config Validation | `epic-1/1-8-qdrant-cloud-setup.md`, `epic-1/1-7-groq-api-setup.md` | Partial overlap | Create new hardening story. Existing stories are setup/done, not config validation/reindex guardrails. |
| 2.4 Provider Switching Documentation And Runbook | none direct | No duplicate | Create. |
| 3.1 Externalized Versioned LLM Prompt Templates | `epic-4/4-3`, `epic-4/4-4` only baseline LLM features | Low overlap | Create. |
| 3.2 Structured LLM Output Schema And Validation | `epic-4/4-3`, `epic-4/4-4` | Partial overlap | Create as AI safety hardening. |
| 3.3 RAG Corpus Governance And Admin Review Workflow | `epic-4/4-6-rag-backed-metric-explanations.md`, `epic-7/7-4`, `epic-7/7-5` | Partial overlap | Create only for corpus governance/versioning. Do not duplicate admin reference-data approval screens. |
| 3.4 Hybrid Curated + Internal Live RAG | `epic-4/4-6-rag-backed-metric-explanations.md` | Partial overlap | Create as improvement if 4.6 is done baseline. |
| 3.5 Trusted Online RAG Source Adapter With Citation And Cache | none direct | No duplicate | Create later only if online RAG is in scope. |
| 4.1 Unified Correlation ID And Audit Spine | `epic-7/7-6-unified-audit-spine-with-correlation-ids.md` | Direct duplicate | Reuse existing 7.6. Do not create a new implementation story unless superseding 7.6 explicitly. |
| 4.2 Observability Metrics And Alerting Baseline | `epic-8/8-4-persisted-product-event-tracking-foundation.md`, `epic-10/10-5-error-handling-audit.md` | Partial overlap | Create new story for ops metrics/alerts, but depend on 8.4 for persisted product events. |
| 4.3 Production Deploy Topology And Rollback Hardening | `epic-infra/infra-devops-foundation.md` | Partial overlap | Create only if infra foundation is done and this is a production hardening follow-up. |
| 4.4 Backup, Restore And Operational Runbook | `epic-infra/infra-devops-foundation.md` | Partial overlap | Create new story if backup/restore drill is not already covered. |
| 5.1 Security Config Fail-Fast Hardening | `epic-10/10-1-security-infrastructure-hardening.md` | Direct duplicate | Reuse 10.1. Do not create separate story unless splitting/superseding 10.1. |
| 5.2 SSRF Protection For OCR And Remote File Fetching | `epic-10/10-1-security-infrastructure-hardening.md` | Direct duplicate | Reuse 10.1 or split 10.1 into canonical smaller stories. |
| 5.3 Admin Session Storage Hardening | `epic-10/10-2-refresh-token-session-security.md`, maybe `epic-10/10-4` | Likely direct/partial overlap | Inspect 10.2 before creating. Prefer reuse/update 10.2 if it covers admin/session token storage. |
| 5.4 Right-To-Delete Race And Cleanup Correctness | `epic-10/10-3-data-deletion-security.md`, `epic-10/10-11-database-constraint-cleanup.md` | Direct/partial overlap | Reuse 10.3 for token/race. Reuse 10.11 for storage/FK cleanup. Create only a narrow follow-up if a gap remains. |
| 6.1 Global Toast Notification Foundation | `epic-10/10-5-error-handling-audit.md` | Partial overlap | Create. 10.5 is catch/error audit, not UX notification foundation. |
| 6.2 Replace Browser Alerts And Local Toasts | `epic-10/10-5-error-handling-audit.md`, `epic-1/1-5` review findings | Partial overlap | Create after 6.1. Avoid broad catch-block scope from 10.5. |
| 6.3 Vietnamese Language Normalization And Message Catalog | none direct, related to `epic-10/10-6` constants cleanup | Low overlap | Create. Keep copy/i18n separate from dead-code cleanup. |
| 6.4 Standard Loading, Empty And Error State Components | `epic-10/10-5-error-handling-audit.md` | Partial overlap | Create as UX component story, not catch-block audit. |
| 6.5 Medical Copy And Disclaimer Review | `epic-4/4-4-lifestyle-recommendations-with-disclaimer.md` | Partial overlap | Create as editorial/AI safety hardening if 4.4 is baseline done. |
| 7.1 Backend AI/OCR/RAG Package Boundary Refactor | `epic-10/10-6-dead-code-constants-cleanup.md` | Low/partial overlap | Create only as structural refactor. Do not include dead code/route constants cleanup already owned by 10.6. |
| 7.2 Split Health Record Review Page Into Feature Components And Hooks | none direct, related to bug stories | No direct duplicate | Create if refactor is desired. |
| 7.3 Extract Shared Profile Sharing Hook | none direct | No duplicate | Create. |
| 7.4 Email Template Cleanup | `epic-2/2-4-email-system-improvements.md` maybe baseline, `6.3` language story | Partial overlap | Create only for template/copy cleanup. Coordinate with 6.3. |
| 8.1 CI Security Scan Baseline | `epic-10/10-9-ci-cd-security-integration.md` | Direct duplicate | Reuse 10.9. Do not create a new story. |
| 8.2 AI/OCR Regression Test Corpus In CI | none direct | No duplicate | Create. |
| 8.3 Release Smoke Tests For Provider And Infrastructure Config | `epic-infra/infra-devops-foundation.md`, `epic-10/10-9` | Partial overlap | Create only for release smoke tests; do not duplicate security scans from 10.9. |

## High-Risk Duplicate Set

These planned improvement stories should **not** be created as new implementation files without an explicit supersede/split decision:

1. **4.1 Unified Correlation ID And Audit Spine**
   - Existing canonical candidate: `_bmad-output/implementation-artifacts/epic-7/7-6-unified-audit-spine-with-correlation-ids.md`
   - Recommendation: reuse 7.6.

2. **5.1 Security Config Fail-Fast Hardening**
   - Existing canonical candidate: `_bmad-output/implementation-artifacts/epic-10/10-1-security-infrastructure-hardening.md`
   - Recommendation: reuse/split 10.1.

3. **5.2 SSRF Protection For OCR And Remote File Fetching**
   - Existing canonical candidate: `_bmad-output/implementation-artifacts/epic-10/10-1-security-infrastructure-hardening.md`
   - Recommendation: reuse/split 10.1.

4. **5.4 Right-To-Delete Race And Cleanup Correctness**
   - Existing canonical candidates:
     - `_bmad-output/implementation-artifacts/epic-10/10-3-data-deletion-security.md`
     - `_bmad-output/implementation-artifacts/epic-10/10-11-database-constraint-cleanup.md`
   - Recommendation: reuse 10.3 + 10.11; create only a narrow residual cleanup story if needed.

5. **8.1 CI Security Scan Baseline**
   - Existing canonical candidate: `_bmad-output/implementation-artifacts/epic-10/10-9-ci-cd-security-integration.md`
   - Recommendation: reuse 10.9.

6. **1.1 / 1.2 / 1.4 / 1.6 vs old 3.7**
   - Existing old story: `_bmad-output/implementation-artifacts/epic-3/3-7-ocr-router-by-mime-type.md`
   - Recommendation: mark old 3.7 superseded by new split OCR reliability stories.

## Existing Stories To Reuse Instead Of Recreating

- `epic-7/7-6-unified-audit-spine-with-correlation-ids.md`
  - Reuse for improvement Story 4.1.

- `epic-8/8-4-persisted-product-event-tracking-foundation.md`
  - Reuse as analytics/product event foundation.
  - Do not treat it as a full observability/alerting story. Improvement Story 4.2 can still exist for metrics/dashboards/alerts.

- `epic-10/10-1-security-infrastructure-hardening.md`
  - Reuse for improvement Stories 5.1 and 5.2 unless split.

- `epic-10/10-2-refresh-token-session-security.md`
  - Inspect/reuse before creating improvement Story 5.3.

- `epic-10/10-3-data-deletion-security.md`
  - Reuse for token hashing, cancellation URL, scheduler race, optimistic locking.

- `epic-10/10-4-frontend-hardening.md`
  - Reuse for safe storage/auth bootstrap retry. Do not merge toast/language UX into it.

- `epic-10/10-5-error-handling-audit.md`
  - Reuse for empty catch/error classification. Do not merge global toast foundation into it.

- `epic-10/10-6-dead-code-constants-cleanup.md`
  - Reuse for route constants/dead code cleanup. Do not merge AI/OCR/RAG package refactor into it.

- `epic-10/10-9-ci-cd-security-integration.md`
  - Reuse for improvement Story 8.1.

- `epic-10/10-10-audit-consent-hardening.md`
  - Reuse for generic audit/consent aspects. OCR external consent policy should depend on it, not duplicate it.

- `epic-10/10-11-database-constraint-cleanup.md`
  - Reuse for FK/cascade/orphan/storage-file deletion cleanup.

## Direct Duplicate

### D1. OCR MIME Router

**Existing story**

- `_bmad-output/implementation-artifacts/epic-3/3-7-ocr-router-by-mime-type.md`
- Status: `ready-for-dev`
- Scope: OCR route by MIME type, normalized OCR contract, fallback chain, safe metadata, confidence/status mapping.

**New story**

- `_bmad-output/implementation-artifacts/epic-core-improvements/epic-1-ocr-reliability/1-1-mime-aware-ocr-router-for-pdf-and-image.md`
- Status: `ready-for-dev`
- Scope: MIME-aware OCR router for PDF/image.

**Assessment**

This is a direct duplicate/overlap. The existing Story 3.7 already covers MIME routing and also bundles normalized contract, fallback, safe metadata, and confidence/status mapping.

**Recommended resolution**

Use one canonical path:

1. Keep the new split-story model as canonical because it decomposes the large 3.7 scope into smaller stories:
   - 1.1 MIME routing
   - 1.2 normalized contract
   - 1.3 retry/DLQ/idempotency
   - 1.4 confidence/review-required
   - 1.6 external OCR fallback privacy controls

2. Mark old `epic-3/3-7-ocr-router-by-mime-type.md` as `superseded` or `replaced-by` the new split stories.

Alternative: keep Story 3.7 canonical and delete/ignore new Story 1.1 + parts of 1.2/1.4/1.6. This is less recommended because 3.7 is too broad for one implementation story.

## Partial Overlaps

### O1. Normalized OCR Contract

**Existing overlap**

- `_bmad-output/implementation-artifacts/epic-3/3-7-ocr-router-by-mime-type.md`

**New story**

- `epic-core-improvements/epic-1-ocr-reliability/1-2-provider-agnostic-ocr-result-contract.md`

**Assessment**

Partial overlap. Story 3.7 AC #2 includes normalized OCR result. New Story 1.2 expands it into a dedicated contract story.

**Recommendation**

If new split model is canonical, keep Story 1.2 and mark the contract portion of Story 3.7 as superseded.

### O2. Confidence / Review Required State

**Existing overlaps**

- `_bmad-output/implementation-artifacts/epic-3/3-3-ocr-extract-and-review-list.md` (done)
- `_bmad-output/implementation-artifacts/epic-3/3-5-ocr-failure-recovery-flow.md` (done)
- `_bmad-output/implementation-artifacts/epic-3/3-7-ocr-router-by-mime-type.md` (ready-for-dev)
- `_bmad-output/implementation-artifacts/epic-3/3-6-metric-source-tag-and-profile-link.md` (ready-for-dev)

**New story**

- `epic-core-improvements/epic-1-ocr-reliability/1-4-parser-confidence-gates-and-review-required-state.md`

**Assessment**

Not a pure duplicate. Existing stories implemented baseline confidence UI/failure flow, but review findings show gaps around parser confidence, low-confidence reasons, provenance, and preventing unsafe auto-finalization.

**Recommendation**

Keep Story 1.4 as a hardening/follow-up story. Reference 3.3 and 3.5 as prior implementation. Coordinate with 3.6 to avoid duplicate source/provenance work.

### O3. Metric Provenance / Source Tag

**Existing story**

- `_bmad-output/implementation-artifacts/epic-3/3-6-metric-source-tag-and-profile-link.md`
- Status: `ready-for-dev`

**New overlap**

- Story 1.4 includes metric source/provenance for corrected values.

**Assessment**

Partial overlap. Story 3.6 covers `ocr` vs `manual` source tags and profile binding. Story 1.4 needs more nuanced provenance: OCR/manual/corrected/partial confidence reasons.

**Recommendation**

Do not implement source/provenance twice. Either:

- Expand Story 3.6 to include `corrected`/confidence provenance and let Story 1.4 consume it, or
- Mark the source/provenance part of 3.6 as superseded by Story 1.4 while keeping profile binding/query scope in 3.6.

### O4. EasyOCR / PaddleOCR Provider Work

**Existing story**

- `_bmad-output/implementation-artifacts/epic-1/1-9-easyocr-service-setup.md`
- Status: `done`

**New story**

- `epic-core-improvements/epic-1-ocr-reliability/1-5-paddleocr-adapter-and-healthlens-benchmark.md`

**Assessment**

Not duplicate. Existing story implemented EasyOCR baseline. New story benchmarks PaddleOCR and adds adapter path.

**Recommendation**

Keep Story 1.5. It should reference 1.9 as baseline, not replace it.

### O5. External OCR Fallback / SSRF / Audit

**Existing overlaps**

- `_bmad-output/implementation-artifacts/epic-10/10-1-security-infrastructure-hardening.md`
- `_bmad-output/implementation-artifacts/epic-10/10-10-audit-consent-hardening.md`
- `_bmad-output/implementation-artifacts/epic-7/7-6-unified-audit-spine-with-correlation-ids.md`

**New story**

- `epic-core-improvements/epic-1-ocr-reliability/1-6-external-ocr-fallback-consent-retention-and-kill-switch.md`

**Assessment**

Partial overlap. Story 10.1 owns generic SSRF/JWT/CSRF/CORS hardening. Story 7.6 owns unified audit spine. Story 10.10 owns consent aspect hardening. Story 1.6 owns OCR-provider-specific privacy/fallback policy.

**Recommendation**

Keep Story 1.6, but narrow it to OCR-provider policy:

- Provider kill switch.
- External OCR consent gate.
- Retention mode/region config.
- Provider request metadata.

Do not reimplement generic SSRF/Audit/Consent infrastructure inside Story 1.6. Instead depend on 10.1, 10.10, and 7.6 where available.

### O6. CI/CD Security

**Existing story**

- `_bmad-output/implementation-artifacts/epic-10/10-9-ci-cd-security-integration.md`

**New backlog story**

- `core-feature-infra-improvement-epics-and-stories.md` Story 8.1: CI Security Scan Baseline

**Assessment**

Direct duplicate at backlog level if Story 8.1 is later turned into a separate implementation artifact.

**Recommendation**

Do not create a new implementation story for improvement Story 8.1. Use existing `10-9-ci-cd-security-integration.md` as canonical, or create a superseding version only if the old story is too weak.

### O7. Data Deletion Security

**Existing story**

- `_bmad-output/implementation-artifacts/epic-10/10-3-data-deletion-security.md`

**New backlog story**

- `core-feature-infra-improvement-epics-and-stories.md` Story 5.4: Right-To-Delete Race And Cleanup Correctness

**Assessment**

Partial/direct overlap. Existing 10.3 covers token hashing, email removal from URL, `SELECT FOR UPDATE SKIP LOCKED`, optimistic locking. New 5.4 adds broader cleanup correctness.

**Recommendation**

Use 10.3 as canonical for token/race hardening. If additional cleanup scope is needed, create a follow-up story specifically for storage/object cleanup and orphan cleanup, not another broad deletion story.

### O8. Security Infrastructure Hardening

**Existing story**

- `_bmad-output/implementation-artifacts/epic-10/10-1-security-infrastructure-hardening.md`

**New backlog stories**

- Story 5.1: Security Config Fail-Fast Hardening
- Story 5.2: SSRF Protection For OCR And Remote File Fetching

**Assessment**

Direct overlap. Existing 10.1 already covers JWT secret validation, CSRF, CORS, and SSRF.

**Recommendation**

Do not create separate implementation artifacts for 5.1 and 5.2 unless they explicitly supersede 10.1. Prefer updating 10.1 or splitting it into two smaller canonical stories.

### O9. Frontend Hardening / Error Handling / Toast

**Existing overlaps**

- `_bmad-output/implementation-artifacts/epic-10/10-4-frontend-hardening.md`
- `_bmad-output/implementation-artifacts/epic-10/10-5-error-handling-audit.md`

**New backlog stories**

- Story 6.1: Global Toast Notification Foundation
- Story 6.2: Replace Browser Alerts And Local Toasts
- Story 6.4: Standard Loading, Empty And Error State Components

**Assessment**

Partial overlap only. Existing stories focus storage/auth retry and empty catch blocks. New stories focus product notification UX and standardized UI state components.

**Recommendation**

Keep new frontend UX stories, but avoid including generic empty-catch remediation already owned by 10.5.

## Recommended Canonical Decision

Use the new `epic-core-improvements` split model for OCR reliability, but mark old Story 3.7 as superseded.

Recommended action:

1. Update `_bmad-output/implementation-artifacts/epic-3/3-7-ocr-router-by-mime-type.md`:
   - `Status: superseded`
   - Add `Superseded by:` links to Stories 1.1, 1.2, 1.4, 1.6.

2. Keep the new Epic 1 files:
   - `1-1-mime-aware-ocr-router-for-pdf-and-image.md`
   - `1-2-provider-agnostic-ocr-result-contract.md`
   - `1-3-ocr-queue-retry-idempotency-and-dlq.md`
   - `1-4-parser-confidence-gates-and-review-required-state.md`
   - `1-5-paddleocr-adapter-and-healthlens-benchmark.md`
   - `1-6-external-ocr-fallback-consent-retention-and-kill-switch.md`

3. Before creating implementation stories for other improvement epics, check this audit to avoid duplicating existing Epic 10 and Epic 7 stories.
