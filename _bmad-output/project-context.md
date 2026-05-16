# HealthLens Project Context

**Last updated:** 2026-05-16

Canonical project-context copy: [docs/project-context.md](../docs/project-context.md)

## Purpose For AI Agents

HealthLens is a healthcare document processing monorepo for Vietnamese users. Before implementation work, read the canonical context file in `docs/project-context.md` and the master documentation index in `docs/index.md`.

## Critical Rules

- Keep `packages/shared/constants/api.ts` and `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java` synchronized when changing routes.
- Add schema changes with new Flyway migrations under `apps/api/src/main/resources/db/migration`.
- Preserve public deletion cancellation: `DELETE /api/v1/users/deletion-requests/cancel?token=...` must not require JWT.
- Use the query-based metric explanation route for names containing `/`: `/metrics/explanation?metricName=...`.
- Avoid logging health document contents, OCR raw text, tokens, or secrets.
- Use `apps/web/src/lib/api/apiClient.ts` for web API calls so auth refresh behavior remains consistent.

