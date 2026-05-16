# Story 1.6: External OCR Fallback Consent, Retention And Kill Switch

Status: ready-for-dev

## Execution Scope

**Phase:** Core feature improvement / OCR privacy and fallback controls  
**Area:** Backend provider config, consent/privacy policy, audit metadata, fallback behavior, OCR provider clients  
**Priority:** P0 if external OCR is enabled; otherwise P1

## Story

As a privacy-conscious user,  
I want clear controls around sending health documents to external OCR services,  
so that sensitive files are processed only with appropriate consent, retention, and audit controls.

## Context

Cloud OCR providers such as AWS, Google, or Azure may be necessary as fallback when self-host OCR cannot handle PDFs or quality is poor. However, health documents are sensitive. External OCR fallback must not be a silent implementation detail; it needs consent, region/retention controls, kill switches, audit metadata, and clear failure behavior when disabled.

## Acceptance Criteria

1. **Given** external OCR is disabled by kill switch, **When** local OCR fails, **Then** the system does not call external provider and routes the user to manual/retry flow.
2. **Given** external OCR is enabled, **When** a document is sent to cloud OCR, **Then** audit metadata includes provider, provider request ID, retention mode, region, fallback reason, and correlation ID.
3. **Given** required consent is missing, **When** fallback would require external OCR, **Then** the system blocks external transfer and explains the next option.
4. **Given** provider retention/no-retention mode is required by environment policy, **When** app starts, **Then** missing or unsafe retention config fails fast in production.
5. **Given** a provider call fails, **When** fallback chain continues or ends, **Then** diagnostics capture sanitized failure reason without leaking credentials, signed URLs, or raw document text.

## Tasks / Subtasks

- [ ] Task 1 - Add external OCR provider config controls (AC: #1, #4)
  - [ ] Provider kill switch per external provider.
  - [ ] Region/cross-border config.
  - [ ] Retention mode config.
  - [ ] Production startup validation.
- [ ] Task 2 - Add consent/policy gate (AC: #3)
  - [ ] Define whether existing consent covers external OCR transfer.
  - [ ] Block external OCR when consent/policy is missing.
  - [ ] Return clear fallback reason for UI/manual flow.
- [ ] Task 3 - Add audit/diagnostics metadata (AC: #2, #5)
  - [ ] Provider request ID.
  - [ ] Retention mode.
  - [ ] Region.
  - [ ] Fallback reason.
  - [ ] Correlation ID.
  - [ ] Sanitized provider error category.
- [ ] Task 4 - Wire fallback behavior (AC: #1, #2, #5)
  - [ ] Local OCR primary path remains default.
  - [ ] External provider call only occurs when config + consent + policy pass.
  - [ ] Disabled external fallback routes to retry/manual entry.
- [ ] Task 5 - Tests (AC: #1-#5)
  - [ ] Kill switch test.
  - [ ] Missing consent test.
  - [ ] Audit metadata test.
  - [ ] Production config fail-fast test.
  - [ ] Provider error redaction test.

## Dev Notes

### Implementation Guardrails

- Do not silently send health documents to external providers as a fallback.
- Do not log raw OCR text, signed URLs, request headers, credentials, or full provider payloads.
- Keep provider-specific clients behind an OCR provider/fallback abstraction.
- `AwsTextractClient` is currently not production-ready if enabled; do not expose it as safe until implementation and config validation are complete.
- Align with audit/correlation work. If unified audit spine is not implemented, store enough metadata now to migrate later.

### Likely Files

- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`
- `apps/api/src/main/java/com/healthlens/api/service/GoogleCloudVisionClient.java`
- `apps/api/src/main/java/com/healthlens/api/service/AwsTextractClient.java`
- `apps/api/src/main/java/com/healthlens/api/config/OcrServiceConfig.java`
- `apps/api/src/main/resources/application.yml`
- `apps/api/src/main/resources/application-docker.yml`
- `apps/api/src/test/java/com/healthlens/api/service/OcrServiceTest.java`
- `apps/api/src/test/java/com/healthlens/api/config/OcrProfileDefaultsConfigTest.java`

### Dependencies

- Best after Story 1.2 for normalized diagnostics.
- Strongly related to Story 4.1 audit spine and Story 5.1 security config validation.

### References

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md` sections 7 and 8
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 1.6
- `apps/api/src/main/java/com/healthlens/api/service/GoogleCloudVisionClient.java`
- `apps/api/src/main/java/com/healthlens/api/service/AwsTextractClient.java`

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
