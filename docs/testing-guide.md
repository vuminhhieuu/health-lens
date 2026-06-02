# Testing Guide

**Last updated:** 2026-05-16

## What To Run

| Scope | Command |
| --- | --- |
| Repo-wide | `pnpm test` |
| Web app | `cd apps/web && pnpm test` |
| API | `cd apps/api && ./gradlew test` |
| Shared package | `cd packages/shared && pnpm build` |

## Web Test Surface

- App Router page tests under `apps/web/src/app/**`.
- Component tests under `apps/web/src/components/**`.
- Vitest and Testing Library are the main tools.
- Use `pnpm lint` inside `apps/web` before or after UI work when the change is layout or interaction heavy.

## API Test Surface

- Spring Boot/JUnit tests run via Gradle.
- The API has Testcontainers dependencies available for database and service integration tests.
- Prefer integration tests for routes that touch auth, consent, health records, and reference data.

## Shared Package Checks

- `packages/shared` is the contract layer.
- Build it after route, schema, or constant changes to catch type regressions early.

## What To Verify After Common Changes

| Change | Verification |
| --- | --- |
| Route changes | Check `packages/shared/constants/api.ts` and `ApiRoutes.java` together |
| Auth changes | Run web tests and API tests |
| Schema changes | Add a Flyway migration and run API tests |
| UI changes | Run web tests and inspect affected pages |
| OCR changes | Test the API OCR route and service health path |

## Common Risk Areas

- Browser auth refresh handling in `apps/web/src/lib/api/apiClient.ts`
- Public account deletion cancellation flow
- Metric explanation routes with encoded metric names
- Consent enforcement and audit logging
- Shared API path constants drifting from backend route definitions
