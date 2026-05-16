# Story 5.1: Security Config Fail-Fast Hardening

Status: ready-for-dev

## Execution Scope

**Area:** JWT, CSRF, CORS, startup validation, production security baseline  
**Priority:** P0

## Story

As a security owner, I want insecure production configuration to fail startup, so that weak JWT secrets, wildcard CORS, or missing CSRF controls never reach production.

## Acceptance Criteria

1. **Given** production profile has wildcard CORS with credentials, **When** app starts, **Then** startup fails with CORS validation error.
2. **Given** JWT secret is weak/default/too short, **When** app starts, **Then** startup fails before serving traffic.
3. **Given** cookie/session endpoints require CSRF, **When** CSRF token/header is missing, **Then** request is rejected.
4. **Given** security config is valid, **When** app starts, **Then** validation logs safe summary without secrets.

## Tasks / Subtasks

- [ ] Validate JWT key length/strength at startup.
- [ ] Validate CORS origins and credentials rules.
- [ ] Enable/validate CSRF strategy for cookie-authenticated endpoints.
- [ ] Add production profile fail-fast checks.
- [ ] Add tests for invalid JWT, invalid CORS, and CSRF enforcement.

## Dev Notes

- This file supersedes old story `epic-10/10-1-security-infrastructure-hardening.md` for JWT/CSRF/CORS scope.
- SSRF-specific scope lives in Story 5.2.

## Likely Files

- `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`
- `apps/api/src/main/resources/application.yml`
- `apps/api/src/test/java/com/healthlens/api/config/`

## References

- Old source: `_bmad-output/implementation-artifacts/epic-10/10-1-security-infrastructure-hardening.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 5.1

## Dev Agent Record

### Agent Model Used

{{agent_model_name_version}}

### Debug Log References

### Completion Notes List

### File List
