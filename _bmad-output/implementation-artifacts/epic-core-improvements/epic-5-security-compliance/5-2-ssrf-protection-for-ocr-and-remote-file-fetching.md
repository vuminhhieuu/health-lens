# Story 5.2: SSRF Protection For OCR And Remote File Fetching

Status: done

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

- [x] Add trusted domain allowlist for OCR fetches.
- [x] Block loopback, private, link-local, metadata, and unspecified ranges.
- [x] Disable redirects or validate final target.
- [x] Add timeout and max file size.
- [x] Add SSRF tests for redirect, IP literal, DNS/private range, and allowed storage.

### Review Findings

- [x] [Review][Patch] HTTP errors from remote fetch can escape and become 500 instead of client download failure [services/ocr-service/app.py:223]
- [x] [Review][Patch] DNS rebinding protection is incomplete because validated DNS result is not pinned to the actual socket connection [services/ocr-service/app.py:182]

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

GPT-5 Codex

### Debug Log References

- `python3 -m py_compile app.py test_ssrf_protection.py` (pass)
- `python3 -m pytest -q` (blocked in local env: `No module named pytest`)
- `python3 -m py_compile app.py test_ssrf_protection.py` after patch fixes (pass)

### Completion Notes List

- Added SSRF-safe remote fetch flow in `services/ocr-service/app.py` with strict host allowlist enforcement.
- Added IP safety validation using `ipaddress` and DNS resolution checks against private/loopback/link-local/multicast/unspecified/reserved ranges.
- Implemented manual redirect handling (`allow_redirects=False`) with per-hop URL and resolved-IP revalidation before each request.
- Enforced remote fetch timeout and streamed response size guardrails to prevent oversized payload downloads.
- Added SSRF unit tests in `services/ocr-service/test_ssrf_protection.py` for IP literal blocking, private DNS blocking, redirect-to-blocked target, and allowed storage domain acceptance.

### File List

- `services/ocr-service/app.py`
- `services/ocr-service/test_ssrf_protection.py`

## Change Log

- 2026-05-19: Implemented Story 5.2 SSRF protections for OCR remote fetch pipeline and added SSRF coverage tests.
- 2026-05-19: Applied code-review patch fixes for HTTP error mapping and connect-time peer IP validation.
