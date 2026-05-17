# Story 1.1: MIME-Aware OCR Router For PDF And Image

Status: done

## Execution Scope

**Phase:** Core feature improvement / OCR reliability  
**Area:** Backend API, OCR job payload, OCR routing, storage/file metadata, OCR microservice integration  
**Priority:** P0

## Story

As a user uploading lab results,  
I want the system to process PDFs and images using the correct OCR path,  
so that scanned PDFs, text-layer PDFs, and images produce reliable extracted metrics.

## Context

Current upload flow allows PDFs and images, but the OCR consumer path is image-oriented. Review findings identify this as a production blocker: PDF files can be routed through an image OCR method without explicit PDF handling, creating unreliable extraction and parsing.

This story creates the MIME-aware routing foundation. It should not attempt to solve every OCR provider abstraction detail; Story 1.2 and 1.5 build on the contract/provider work. The key outcome is that OCR jobs carry file type context and the backend chooses a file-type-appropriate processing path.

## Acceptance Criteria

1. **Given** a user uploads an image file, **When** the OCR job starts, **Then** the router selects an image-capable OCR path and records selected provider/path, MIME type, record id, and correlation id.
2. **Given** a user uploads a PDF with a text layer, **When** the OCR job starts, **Then** the router attempts text-layer extraction or a PDF-capable provider before any image-render fallback.
3. **Given** a user uploads a scanned PDF without text layer, **When** the OCR job starts, **Then** the router uses a document OCR path or page-render fallback and preserves page-level metadata for downstream parsing.
4. **Given** the uploaded file MIME type is unsupported, **When** the OCR job is processed, **Then** the health record is marked OCR failed with a structured failure reason and the UI can route the user to retry/manual entry.
5. **Given** legacy jobs without MIME metadata exist, **When** the consumer receives them, **Then** the system either derives MIME type safely from persisted upload metadata or fails terminally with a clear migration-compatible reason.

## Tasks / Subtasks

- [x] Task 1 - Extend OCR job payload (AC: #1, #5)
  - [x] Include `recordId`, `fileKey`, `mimeType`, `jobId`, `correlationId`.
  - [x] Ensure upload confirmation/publisher supplies MIME type from trusted metadata, not only client filename.
  - [x] Add backward-compatible handling for existing payloads where needed.
- [x] Task 2 - Implement MIME-aware router (AC: #1, #2, #3, #4)
  - [x] Route `image/*` to existing image OCR flow.
  - [x] Route `application/pdf` to a PDF path.
  - [x] Reject unsupported MIME types with structured OCR failure reason.
  - [x] Persist route/provider/path choice in diagnostics/logs.
- [x] Task 3 - Add PDF handling baseline (AC: #2, #3)
  - [x] Detect or attempt text-layer extraction for PDFs where feasible.
  - [x] Add clear fallback seam for scanned PDF/document OCR.
  - [x] Preserve page-level output placeholders for Story 1.2 contract.
- [x] Task 4 - Update health record status/failure path (AC: #4)
  - [x] Store failure reason for unsupported file type or PDF processing failure.
  - [x] Ensure existing OCR failure UI can show manual entry/retry path.
- [x] Task 5 - Tests (AC: #1-#5)
  - [x] Unit tests for image MIME routing.
  - [x] Unit/integration tests for PDF MIME routing.
  - [x] Unsupported MIME test.
  - [x] Legacy/missing MIME payload test.

### Review Findings

- [x] [Review][Decision] PDF route does not yet deliver AC2/AC3 behavior — Resolved by expanding this story: PDFBox text-layer extraction now runs before document-provider fallback, and page-level metadata preserves actual PDF page count.
- [x] [Review][Patch] Disabled/default Textract PDF path is stored as `low_confidence` instead of `pdf_processing_failed` [apps/api/src/main/java/com/healthlens/api/service/OcrService.java:215]
- [x] [Review][Patch] Consumer accepts arbitrary `image/*` payload MIME even though upload creation only supports JPEG/PNG [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java:227]
- [x] [Review][Patch] Legacy missing MIME fallback trusts file extension instead of persisted trusted metadata or terminal failure [apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java:193]
- [x] [Review][Patch] PDF OCR failure logging can leak signed/internal URL details through raw exception messages [apps/api/src/main/java/com/healthlens/api/service/OcrService.java:231]

## Dev Notes

### Implementation Guardrails

- Do not keep calling a method named `processImage(...)` for every file type without a router abstraction; that hides the PDF behavior bug.
- Do not trust only file extension. Use persisted content type from upload confirmation/storage metadata where possible.
- Avoid putting provider-specific logic into `HealthRecordService`; keep routing under OCR-specific classes/services.
- If full PDF text extraction requires a new library, choose conservatively and document the dependency. Do not introduce large native tooling without validating container impact.
- This story may create a minimal PDF path/fallback hook; provider registry work is covered more fully in Epic 2 Story 2.2.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java`
- `apps/api/src/main/java/com/healthlens/api/dto/response/ConfirmUploadResponse.java`
- `apps/api/src/main/java/com/healthlens/api/dto/request/CreateUploadUrlRequest.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrJobConsumerTest.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java`

### Dependencies

- Should precede Story 1.2, 1.3, 1.4, and 1.5 where possible.
- Coordinate with audit/correlation work if Story 4.1 is implemented in parallel.

### References

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 1
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 1.1
- `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `services/ocr-service/app.py`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- 2026-05-16: RED - `./gradlew test --tests com.healthlens.api.service.OcrJobConsumerTest --tests com.healthlens.api.service.OcrServiceTest --tests com.healthlens.api.service.HealthRecordServiceTest` failed at compile because MIME-aware router API did not exist yet.
- 2026-05-16: GREEN - Targeted service tests passed after adding MIME-aware routing, upload payload metadata, and consumer diagnostics.
- 2026-05-16: REGRESSION - Full API suite `./gradlew test` passed.

### Completion Notes List

- Upload reservation now persists normalized server-selected MIME type and confirm upload publishes `jobId`, `correlationId`, `recordId`, `fileKey`, `mimeType`, and `profileId`.
- OCR consumer resolves MIME from payload first and falls back to file-key extension for legacy jobs; unsupported/unknown MIME fails terminally with `unsupported_mime_type`.
- OCR service now exposes `processDocument(...)` as the MIME-aware router: `image/*` uses the existing image provider order, while `application/pdf` uses a PDF document OCR path via Textract with page-level placeholder metadata.
- Raw OCR diagnostics now include MIME type, route, provider, record id, job id, correlation id, and page metadata; PDF provider failure stores `pdf_processing_failed`.
- No new runtime dependency was added; PDF text-layer extraction uses the existing PDFBox dependency.
- Code review follow-ups resolved: PDFBox text-layer extraction added, scanned PDFs preserve page-count metadata through document-provider fallback, Textract stub maps to PDF processing failure, unsupported image subtypes are rejected, missing legacy MIME fails terminally, and PDF OCR logs no longer include raw exception messages.

### File List

- apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java
- apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java
- apps/api/src/main/java/com/healthlens/api/service/OcrService.java
- apps/api/src/test/java/com/healthlens/api/service/HealthRecordServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/OcrJobConsumerTest.java
- apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java
- _bmad-output/implementation-artifacts/sprint-status.yaml
- _bmad-output/implementation-artifacts/epic-core-improvements/epic-1-ocr-reliability/1-1-mime-aware-ocr-router-for-pdf-and-image.md

### Change Log

- 2026-05-16: Implemented MIME-aware OCR routing foundation for image/PDF uploads and marked story ready for review.
- 2026-05-16: Addressed code review findings and marked story done.
