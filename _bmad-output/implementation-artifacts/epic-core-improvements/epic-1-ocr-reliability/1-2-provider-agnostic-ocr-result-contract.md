# Story 1.2: Provider-Agnostic OCR Result Contract

Status: ready-for-dev

## Execution Scope

**Phase:** Core feature improvement / OCR reliability  
**Area:** Backend API DTO/domain model, OCR provider output, parser input contract, diagnostics/audit metadata  
**Priority:** P0

## Story

As a developer maintaining OCR providers,  
I want every OCR provider to return a normalized result contract,  
so that parser, audit, debugging, and fallback logic do not depend on provider-specific payloads.

## Context

The current OCR result is too thin for production document OCR. A flat text/confidence/source result loses page boundaries, layout, provider request IDs, retention mode, diagnostics, and model version. This blocks robust parsing and makes provider fallback hard to audit.

This story defines and wires a normalized OCR result contract. It should preserve compatibility for existing EasyOCR output while allowing future PaddleOCR, Textract, Google Vision/Document AI, or PDF text extraction adapters to produce richer results.

## Acceptance Criteria

1. **Given** any OCR provider succeeds, **When** the result is persisted or passed to the parser, **Then** the normalized result includes provider, model version, MIME type, aggregate text, confidence, diagnostics, latency, and retention mode.
2. **Given** OCR output contains multiple pages, **When** parser or review logic reads the OCR result, **Then** it can access page number and line/block boundaries.
3. **Given** a provider emits layout data, **When** the normalized contract is built, **Then** page/block/line-level confidence and bounding boxes are preserved where available.
4. **Given** a provider fails partially, **When** fallback is attempted, **Then** diagnostics include failed provider name and failure category without leaking credentials, raw presigned URLs, or sensitive headers.
5. **Given** existing EasyOCR output is returned, **When** normalized, **Then** it maps into the new contract with reasonable defaults for unsupported fields.

## Tasks / Subtasks

- [ ] Task 1 - Define normalized OCR domain/DTO model (AC: #1, #2, #3)
  - [ ] Add fields: `provider`, `modelVersion`, `mimeType`, `pages`, `blocks`, `lines`, `text`, `confidence`, `diagnostics`, `providerRequestId`, `latencyMs`, `retentionMode`.
  - [ ] Define line/block structures with `pageNumber`, `text`, `confidence`, `boundingBox`, `kind`.
  - [ ] Ensure JSON serialization is stable and test-covered.
- [ ] Task 2 - Map existing EasyOCR result into contract (AC: #5)
  - [ ] Preserve current text and confidence.
  - [ ] Add default single-page/page-unknown mapping if EasyOCR has no page concept.
  - [ ] Record provider/model defaults from config where possible.
- [ ] Task 3 - Wire parser input to normalized contract (AC: #1, #2)
  - [ ] Keep parser behavior compatible with current flat text.
  - [ ] Add ability to consume page/line text in order.
- [ ] Task 4 - Add diagnostics and redaction (AC: #4)
  - [ ] Define diagnostic categories.
  - [ ] Ensure no raw secrets, headers, tokens, or presigned URLs are logged/persisted in diagnostics.
- [ ] Task 5 - Tests (AC: #1-#5)
  - [ ] Serialization tests.
  - [ ] EasyOCR mapping tests.
  - [ ] Multi-page contract tests.
  - [ ] Diagnostic redaction tests.

## Dev Notes

### Implementation Guardrails

- Keep the contract provider-agnostic. Do not add fields named only for Textract, Google, or Paddle unless placed under generic metadata.
- Do not require every provider to support bounding boxes; optional fields should degrade gracefully.
- Prefer explicit diagnostic codes over free-form strings where parser/business logic depends on them.
- If persistence schema changes are needed, add a migration and maintain compatibility with existing records.
- This story should enable Story 1.4 confidence gates and Story 1.6 provider audit metadata.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/dto/OcrResult.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/api/src/main/java/com/healthlens/api/service/GoogleCloudVisionClient.java`
- `apps/api/src/main/java/com/healthlens/api/service/AwsTextractClient.java`
- `apps/api/src/main/java/com/healthlens/api/entity/HealthRecord.java`
- `apps/api/src/main/resources/db/migration/`
- `apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java`

### Dependencies

- Best after Story 1.1.
- Enables Story 1.4 and Story 1.6.

### References

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` section 2
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 1.2
- `apps/api/src/main/java/com/healthlens/api/dto/OcrResult.java`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
