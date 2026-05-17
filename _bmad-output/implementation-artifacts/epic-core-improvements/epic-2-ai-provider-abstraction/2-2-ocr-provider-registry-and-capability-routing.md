# Story 2.2: OCR Provider Registry And Capability Routing

Status: done

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

- [x] Define `OcrProvider` interface.
- [x] Define capability enum: `IMAGE_OCR`, `PDF_TEXT`, `PDF_SCAN`, `DOCUMENT_LAYOUT`.
- [x] Implement provider registry and fallback order validation.
- [x] Move hardcoded provider selection out of `HealthRecordService` and into OCR boundary.
- [x] Add unit tests for capability routing and invalid config.

### Review Findings

- [x] [Review][Patch] Textract stub PDF fallback discards prior page metadata [apps/api/src/main/java/com/healthlens/api/service/OcrService.java:460]
- [x] [Review][Patch] Failed PDF result hard-codes textract attribution after capability routing [apps/api/src/main/java/com/healthlens/api/service/OcrService.java:592]
- [x] [Review][Patch] Final PDF failure drops diagnostics from blank provider results [apps/api/src/main/java/com/healthlens/api/service/OcrService.java:392]
- [x] [Review][Patch] PDF text-layer extraction bypasses PDF_TEXT provider capability routing [apps/api/src/main/java/com/healthlens/api/service/OcrService.java:405]
- [x] [Review][Patch] Adding a new OCR provider still requires editing OcrService provider construction [apps/api/src/main/java/com/healthlens/api/service/OcrService.java:179]
- [x] [Review][Patch] PDF scan execution special-cases provider name instead of provider capability/input contract [apps/api/src/main/java/com/healthlens/api/service/OcrService.java:456]

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

GPT-5 Codex

### Debug Log References

- 2026-05-17T21:35+07:00 — Red test: `./gradlew test --tests com.healthlens.api.service.ocr.OcrProviderRegistryTest` failed because provider registry abstractions did not exist yet.
- 2026-05-17T21:42+07:00 — Targeted validation passed: `./gradlew test --tests com.healthlens.api.service.ocr.OcrProviderRegistryTest --tests com.healthlens.api.service.OcrServiceTest`.
- 2026-05-17T21:47+07:00 — Full API validation passed: `./gradlew test`. Test logs include existing scheduled `EmailConsumer` Redis warning during shutdown, but Gradle completed with `BUILD SUCCESSFUL`.
- 2026-05-17T23:41+07:00 — Review patch validation passed: `./gradlew test --tests com.healthlens.api.service.OcrServiceTest --tests com.healthlens.api.service.ocr.OcrProviderRegistryTest`.
- 2026-05-17T23:43+07:00 — Full API validation passed after review patches: `./gradlew test`. Existing scheduled `EmailConsumer` Redis warning appeared during shutdown; Gradle completed with `BUILD SUCCESSFUL`.

### Completion Notes List

- Added provider abstraction (`OcrProvider`, `OcrJob`) and capability model (`OcrCapability`) for image OCR, PDF text, PDF scan, and document layout.
- Added `OcrProviderRegistry` with configured primary/fallback order validation, fail-fast unsupported provider handling, MIME/capability routing, and mismatch diagnostics.
- Refactored `OcrService` image and scanned-PDF provider selection to use the registry instead of provider-name switch logic.
- Added unit tests covering invalid provider config and capability-based provider skipping/selection.
- Resolved code review findings by moving OCR provider adapters to Spring beans, routing PDF text through `PDF_TEXT`, removing PDF scan provider-name branching, preserving fallback pages/diagnostics, and avoiding hard-coded Textract attribution on final PDF failure.

### File List

- apps/api/src/main/java/com/healthlens/api/service/OcrService.java
- apps/api/src/main/java/com/healthlens/api/service/EasyOcrProviderAdapter.java
- apps/api/src/main/java/com/healthlens/api/service/GoogleCloudVisionOcrProvider.java
- apps/api/src/main/java/com/healthlens/api/service/PdfTextOcrProvider.java
- apps/api/src/main/java/com/healthlens/api/service/TextractOcrProvider.java
- apps/api/src/main/java/com/healthlens/api/service/ocr/OcrCapability.java
- apps/api/src/main/java/com/healthlens/api/service/ocr/OcrJob.java
- apps/api/src/main/java/com/healthlens/api/service/ocr/OcrProvider.java
- apps/api/src/main/java/com/healthlens/api/service/ocr/OcrProviderRegistry.java
- apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java
- apps/api/src/test/java/com/healthlens/api/service/ocr/OcrProviderRegistryTest.java
- _bmad-output/implementation-artifacts/sprint-status.yaml
- _bmad-output/implementation-artifacts/epic-core-improvements/epic-2-ai-provider-abstraction/2-2-ocr-provider-registry-and-capability-routing.md

### Change Log

- 2026-05-17 — Implemented OCR provider registry and capability routing; story ready for review.
- 2026-05-17 — Addressed review findings and marked story done.
