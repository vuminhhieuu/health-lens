# Story 5.1: Security Config Fail-Fast Hardening

Status: done

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

- [x] Validate JWT key length/strength at startup.
- [x] Validate CORS origins and credentials rules.
- [x] Enable/validate CSRF strategy for cookie-authenticated endpoints.
- [x] Add production profile fail-fast checks.
- [x] Add tests for invalid JWT, invalid CORS, and CSRF enforcement.

### Review Findings

- [x] [Review][Patch] Login now materializes a refresh-token session without requiring CSRF [apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java:124] — Decision: require CSRF on login and add/confirm a pre-login CSRF bootstrap flow for the web app. Fixed with `GET /api/v1/auth/csrf`, login CSRF enforcement, and frontend token bootstrap.
- [x] [Review][Patch] Frontend refresh/logout calls are not updated to send cross-origin XSRF headers [apps/web/src/lib/api/apiClient.ts:11] — Fixed by bootstrapping `XSRF-TOKEN` and setting `X-XSRF-TOKEN` from the cookie for unsafe auth endpoints.
- [x] [Review][Patch] JWT "weak" secret validation only checks length and a small default marker list [apps/api/src/main/java/com/healthlens/api/config/SecurityStartupValidator.java:66] — Fixed by adding strict-profile diversity/class checks in addition to length and known default markers.
- [x] [Copilot][Patch] Logout CSRF enforcement could be bypassed because `refresh_token` cookie path is scoped to `/api/v1/auth/refresh` — Fixed by requiring CSRF for logout unconditionally and adding logout CSRF tests.
- [x] [Copilot][Patch] CSRF endpoint and repository could drift on header/cookie names — Fixed by introducing backend security constants and using the relative auth route constant in the controller.
- [x] [Copilot][Patch] Frontend sent XSRF header on safe requests — Fixed by attaching the header only to unsafe auth cookie endpoints.
- [x] [Copilot][Patch] JWT default marker and startup log were too broad/misleading — Fixed by narrowing default secret checks to exact known values and removing hardcoded `corsCredentials=true` from the startup log.

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

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests com.healthlens.api.config.SecurityStartupValidatorTest --tests com.healthlens.api.config.SecurityConfigCsrfTest` - RED phase failed before production validator existed; passed after implementation.
- `cd apps/api && ./gradlew test` - passed full API regression suite.
- `cd apps/api && ./gradlew test --tests com.healthlens.api.controller.AuthControllerTest --tests com.healthlens.api.config.SecurityConfigCsrfTest --tests com.healthlens.api.config.SecurityStartupValidatorTest` - passed after review fixes.
- `cd apps/api && ./gradlew test` - passed full API regression suite after review fixes.
- `cd apps/api && ./gradlew test --tests com.healthlens.api.controller.AuthControllerTest --tests com.healthlens.api.config.SecurityConfigCsrfTest --tests com.healthlens.api.config.SecurityStartupValidatorTest` - passed after Copilot PR review follow-ups.
- `cd apps/api && ./gradlew test` - passed full API regression suite after Copilot PR review follow-ups.
- `git diff --check` - passed after Copilot PR review follow-ups.
- `cd packages/shared && pnpm build` - not run successfully because `tsc`/`node_modules` are missing in the workspace.
- `cd apps/web && pnpm exec tsc --noEmit` - not run successfully because `tsc`/`node_modules` are missing in the workspace.

### Completion Notes List

- Added startup validation for JWT secret length, strict-profile default JWT secrets, and strict-profile wildcard CORS with credentials.
- Enabled CSRF token enforcement for refresh-cookie authenticated `/api/v1/auth/refresh` and `/api/v1/auth/logout` requests while preserving bearer/non-cookie auth flows.
- Materialized CSRF tokens through Spring Security's cookie repository and login flow so cookie-based refresh calls can supply the required token.
- Added security tests for invalid JWT config, invalid production CORS config, valid production config, CSRF rejection, and CSRF-compatible refresh flows.
- Addressed code review findings by requiring CSRF for login, adding `/api/v1/auth/csrf`, adding frontend CSRF bootstrap/header handling, and strengthening strict-profile JWT weak-secret heuristics.
- Addressed Copilot PR review follow-ups for logout CSRF, CSRF route/header constants, frontend preflight reduction, narrowed JWT default secret checks, and safe startup logging.

### File List

- `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`
- `apps/api/src/main/java/com/healthlens/api/config/SecurityStartupValidator.java`
- `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java`
- `apps/api/src/main/java/com/healthlens/api/constants/SecurityConstants.java`
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/test/java/com/healthlens/api/config/SecurityConfigCsrfTest.java`
- `apps/api/src/test/java/com/healthlens/api/config/SecurityStartupValidatorTest.java`
- `apps/api/src/test/java/com/healthlens/api/controller/AuthControllerTest.java`
- `apps/web/src/lib/api/apiClient.ts`
- `packages/shared/constants/api.ts`

### Change Log

- 2026-05-19T11:22:46+07:00 - Implemented security config fail-fast hardening and CSRF enforcement; full API test suite passed.
- 2026-05-19T12:05:59+07:00 - Addressed code review findings, added CSRF bootstrap/login enforcement/frontend XSRF support, strengthened JWT secret validation, and passed full API test suite.
- 2026-05-19T12:47:38+07:00 - Addressed Copilot PR review follow-ups for logout CSRF, constants, frontend XSRF header scope, JWT marker precision, startup logging, and passed full API test suite.
