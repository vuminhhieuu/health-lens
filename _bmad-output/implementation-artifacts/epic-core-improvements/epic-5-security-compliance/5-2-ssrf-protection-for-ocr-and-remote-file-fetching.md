# Story 5.2: SSRF Protection For OCR And Remote File Fetching

Status: ready-for-dev

## Execution Scope

**Area:** OCR remote fetching, SSRF defenses, domain allowlist, network safety  
**Priority:** P0

## Story

As a security owner, I want OCR file fetching protected against SSRF, so that internal networks and metadata endpoints cannot be accessed through OCR inputs.

## Acceptance Criteria

1. **Given** OCR input URL resolves to private IP, **When** OCR service attempts fetch, **Then** request is blocked and logged.
2. **Given** OCR input URL redirects to blocked IP, **When** request follows redirect, **Then** redirect is rejected or redirects are disabled.
3. **Given** OCR input URL is from allowed storage domain, **When** OCR fetches it, **Then** request proceeds with timeout and size limits.
4. **Given** DNS rebinding or IP obfuscation is attempted, **When** URL validation runs, **Then** final resolved IP is validated before request.

## Tasks / Subtasks

- [ ] Add trusted domain allowlist for OCR fetches.
- [ ] Block loopback, private, link-local, metadata, and unspecified ranges.
- [ ] Disable redirects or validate final target.
- [ ] Add timeout and max file size.
- [ ] Add SSRF tests for redirect, IP literal, DNS/private range, and allowed storage.

## Dev Notes

- This file supersedes old story `epic-10/10-1-security-infrastructure-hardening.md` for SSRF/OCR fetching scope.
- Do not rely on blacklist-only validation.

## Likely Files

- `services/ocr-service/app.py`
- `apps/api/src/main/java/com/healthlens/api/controller/OcrController.java`
- `apps/api/src/main/java/com/healthlens/api/service/OcrService.java`

## References

- Old source: `_bmad-output/implementation-artifacts/epic-10/10-1-security-infrastructure-hardening.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 5.2

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
