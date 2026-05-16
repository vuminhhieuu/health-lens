# Story 2.2: OCR Provider Registry And Capability Routing

Status: ready-for-dev

## Execution Scope

**Area:** Backend OCR provider abstraction, provider registry, capability routing  
**Priority:** P0/P1

## Story

As a backend developer, I want OCR providers registered by capability, so that adding PaddleOCR, Textract, or Google OCR does not require changing core health record logic.

## Acceptance Criteria

1. **Given** providers are configured, **When** OCR router receives an image or PDF job, **Then** provider selection uses declared capabilities and fallback order.
2. **Given** a new provider is added, **When** it implements `OcrProvider` and registers capabilities, **Then** no health record service logic changes are required.
3. **Given** configured primary provider is unsupported, **When** production starts, **Then** app fails fast with provider validation error.
4. **Given** provider capability does not match MIME type, **When** routing happens, **Then** provider is skipped and diagnostic metadata records the reason.

## Tasks / Subtasks

- [ ] Define `OcrProvider` interface.
- [ ] Define capability enum: `IMAGE_OCR`, `PDF_TEXT`, `PDF_SCAN`, `DOCUMENT_LAYOUT`.
- [ ] Implement provider registry and fallback order validation.
- [ ] Move hardcoded provider selection out of `HealthRecordService` and into OCR boundary.
- [ ] Add unit tests for capability routing and invalid config.

## Dev Notes

- Coordinate with Story 1.1 and 1.2; this story owns registry/capability selection, not MIME payload design or result contract details.
- Existing EasyOCR should become one provider adapter, not the core path.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/api/src/main/java/com/healthlens/api/config/OcrServiceConfig.java`
- `apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java`

## References

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 2.2
- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 12 and 14

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
