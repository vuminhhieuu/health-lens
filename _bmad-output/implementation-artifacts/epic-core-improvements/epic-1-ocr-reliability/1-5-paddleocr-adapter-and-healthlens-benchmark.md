# Story 1.5: PaddleOCR Adapter And HealthLens Benchmark

Status: ready-for-dev

## Execution Scope

**Phase:** Core feature improvement / OCR provider quality  
**Area:** OCR microservice/provider adapter, benchmark tooling, documentation, provider config  
**Priority:** P1

## Story

As a technical owner,  
I want to benchmark PaddleOCR against EasyOCR on HealthLens documents,  
so that provider selection is evidence-based instead of assumption-based.

## Context

Research suggests PaddleOCR may be stronger than EasyOCR for document/layout OCR, but PaddleOCR has heavier runtime requirements. Free/self-host infrastructure constraints make a blind replacement risky. This story adds a PaddleOCR adapter and benchmark path so HealthLens can decide based on real Vietnamese lab documents, latency, memory, and extraction quality.

## Acceptance Criteria

1. **Given** a benchmark dataset exists, **When** EasyOCR and PaddleOCR run against the same files, **Then** the report includes accuracy, latency, memory, error categories, and recommendation.
2. **Given** PaddleOCR is selected as primary, **When** `OCR_PROVIDER_PRIMARY=paddleocr`, **Then** the system uses PaddleOCR without changing core health record flow.
3. **Given** PaddleOCR fails, **When** fallback order includes EasyOCR or cloud OCR, **Then** fallback behavior follows configured policy and records diagnostics.
4. **Given** the runtime environment is under-provisioned, **When** PaddleOCR starts or processes a job, **Then** failure is clear and does not crash the whole API.
5. **Given** benchmark documents contain sensitive health data, **When** fixtures are committed or shared, **Then** they are sanitized, synthetic, or stored outside git according to policy.

## Tasks / Subtasks

- [ ] Task 1 - Define benchmark dataset policy (AC: #1, #5)
  - [ ] Use sanitized/synthetic Vietnamese lab result fixtures.
  - [ ] Cover image, scanned PDF, text-layer PDF, clear/mờ images, multiple hospitals/labs.
  - [ ] Document expected labels/ground truth format.
- [ ] Task 2 - Add PaddleOCR provider adapter (AC: #2, #3)
  - [ ] Add provider config flag/name.
  - [ ] Return normalized OCR result from Story 1.2 contract.
  - [ ] Support provider failure diagnostics.
- [ ] Task 3 - Add benchmark runner/report (AC: #1)
  - [ ] Compare EasyOCR and PaddleOCR on the same fixture set.
  - [ ] Measure text accuracy, metric name accuracy, value/unit accuracy, reference range accuracy.
  - [ ] Measure latency and memory where feasible.
  - [ ] Produce markdown report artifact.
- [ ] Task 4 - Runtime safety (AC: #4)
  - [ ] Document RAM/CPU requirements.
  - [ ] Add startup/health behavior for unavailable PaddleOCR runtime.
  - [ ] Ensure API fallback/manual path remains available.
- [ ] Task 5 - Tests/docs (AC: #1-#5)
  - [ ] Adapter mapping tests.
  - [ ] Config selection tests.
  - [ ] Benchmark docs.

## Dev Notes

### Implementation Guardrails

- Do not replace EasyOCR directly before benchmark evidence exists.
- Do not commit real patient data.
- Do not make PaddleOCR a hard dependency for local dev unless it is optional/profile-gated.
- If PaddleOCR runs as a separate Python service, keep API contract compatible with the existing OCR service pattern.
- This story is best after Story 1.2 and provider registry work. If provider registry is not ready, implement the adapter behind a minimal feature flag without spreading hardcoded conditionals.

### Likely Files

- `services/ocr-service/app.py`
- `services/ocr-service/requirements.txt`
- `services/ocr-service/Dockerfile`
- `services/ocr-service/README.md`
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/api/src/main/resources/application.yml`
- `docs/` or `_bmad-output/planning-artifacts/research/` for benchmark report
- `apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java`

### Dependencies

- Prefer after Story 1.2.
- Strongly benefits from Epic 2 Story 2.2 OCR provider registry.

### References

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 5 and 6
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 1.5
- `services/ocr-service/README.md`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
