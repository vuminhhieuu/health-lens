# HealthLens Project Context

**Last updated:** 2026-05-16

## Purpose For AI Agents

Use this file as compact implementation context before editing HealthLens. It captures repository-specific rules that are easy to miss from code alone.

## Project Shape

HealthLens is a healthcare document processing monorepo for Vietnamese users. It contains:

- `apps/web`: Next.js 16 web app.
- `apps/mobile`: Expo mobile app.
- `apps/api`: Spring Boot 4 API on Java 21.
- `services/ocr-service`: FastAPI EasyOCR service.
- `packages/shared`: shared TypeScript constants, schemas, and types.
- `docker`: local compose stack and helper scripts.

## Non-Negotiable Synchronization Rules

- Keep `packages/shared/constants/api.ts` and `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java` synchronized when adding or changing routes.
- Prefer shared constants/schemas in `packages/shared` for frontend-consumed contracts.
- Treat Flyway migrations as append-only once committed; create a new migration for schema changes.
- Preserve public account deletion cancellation behavior: `DELETE /api/v1/users/deletion-requests/cancel?token=...` must not require JWT.
- Preserve preferred metric explanation query route for metric names containing `/`: `/metrics/explanation?metricName=...`.

## Implementation Patterns

- Web API calls should use `apps/web/src/lib/api/apiClient.ts` so auth headers, refresh retry behavior, and revocation handling stay consistent.
- Web route constants should import or re-export shared `ApiPaths`.
- API controllers should use `ApiRoutes` constants when possible.
- API business logic belongs in services, not controllers.
- Database access belongs in repositories.
- Cross-cutting audit/consent behavior uses annotations and aspects.

## Security And Privacy Notes

- This is a healthcare-adjacent app; avoid logging health document contents, OCR raw text, tokens, or secrets.
- Keep consent checks intact for health-data flows.
- Keep audit trails for profile sharing and health record access changes.
- Token values should be stored hashed or treated as sensitive depending on the existing implementation.

## Testing Expectations

- For web changes, run `cd apps/web && pnpm test` when touching UI/auth/API-client behavior.
- For API changes, run `cd apps/api && ./gradlew test`.
- For shared package contract changes, run `cd packages/shared && pnpm build`, then relevant web/mobile checks.
- For route changes, check both backend route registry and shared frontend route constants.

## Generated Folders To Ignore

Do not use these as source-of-truth during analysis:

- `node_modules/`
- `.next/`
- `.gradle/`
- `apps/api/build/`
- `services/ocr-service/__pycache__/`

