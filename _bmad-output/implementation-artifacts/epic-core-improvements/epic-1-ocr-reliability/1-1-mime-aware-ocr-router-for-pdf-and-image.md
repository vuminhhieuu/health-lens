# Story 1.1: MIME-Aware OCR Router For PDF And Image

Status: ready-for-dev

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

- [ ] Task 1 - Extend OCR job payload (AC: #1, #5)
  - [ ] Include `recordId`, `fileKey`, `mimeType`, `jobId`, `correlationId`.
  - [ ] Ensure upload confirmation/publisher supplies MIME type from trusted metadata, not only client filename.
  - [ ] Add backward-compatible handling for existing payloads where needed.
- [ ] Task 2 - Implement MIME-aware router (AC: #1, #2, #3, #4)
  - [ ] Route `image/*` to existing image OCR flow.
  - [ ] Route `application/pdf` to a PDF path.
  - [ ] Reject unsupported MIME types with structured OCR failure reason.
  - [ ] Persist route/provider/path choice in diagnostics/logs.
- [ ] Task 3 - Add PDF handling baseline (AC: #2, #3)
  - [ ] Detect or attempt text-layer extraction for PDFs where feasible.
  - [ ] Add clear fallback seam for scanned PDF/document OCR.
  - [ ] Preserve page-level output placeholders for Story 1.2 contract.
- [ ] Task 4 - Update health record status/failure path (AC: #4)
  - [ ] Store failure reason for unsupported file type or PDF processing failure.
  - [ ] Ensure existing OCR failure UI can show manual entry/retry path.
- [ ] Task 5 - Tests (AC: #1-#5)
  - [ ] Unit tests for image MIME routing.
  - [ ] Unit/integration tests for PDF MIME routing.
  - [ ] Unsupported MIME test.
  - [ ] Legacy/missing MIME payload test.

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

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
