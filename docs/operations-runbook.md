# Operations Runbook

**Last updated:** 2026-05-16

## Daily Checks

- Confirm the web app responds on the expected port.
- Confirm the API health endpoint is healthy.
- Confirm PostgreSQL, Redis, and storage dependencies are reachable.
- Review recent application logs for auth, OCR, and storage failures.

## Health Endpoints

| Component | Endpoint |
| --- | --- |
| API health | `/actuator/health` |
| API liveness | `/actuator/health/liveness` |
| API readiness | `/actuator/health/readiness` |
| API info | `/actuator/info` |
| OCR service | `GET /health` |

## Local Stack Commands

| Task | Command |
| --- | --- |
| Start stack | `pnpm docker:up` |
| Start with OCR | `pnpm docker:up:ocr` |
| View logs | `pnpm docker:logs` |
| Stop stack | `pnpm docker:down` |
| Remove volumes | `pnpm docker:down:volumes` |
| Clean all | `./scripts/docker/down.sh --clean --yes` |
| Preview cleanup | `pnpm docker:cleanup:dry-run` |
| Analyze Flyway squash groups | `pnpm db:analyze:flyway-squash` |

## Failure Triage

1. Check the API health endpoint first.
2. Check database connectivity and Flyway startup logs.
3. Check Redis and storage connectivity.
4. Check OCR service health if document extraction is failing.
5. Check web client auth refresh behavior if users are being redirected unexpectedly.

## Deployment Notes

- Local development uses Docker Compose.
- Staging uses the documented cloud split in [STAGING_DEPLOYMENT.md](STAGING_DEPLOYMENT.md).
- Production and staging behavior should follow the environment profiles in the API config.
- Provider switches for LLM, OCR, embedding, and RAG should follow [provider-switching-runbook.md](./provider-switching-runbook.md), including staging smoke tests and rollback preparation.

## Incident Data To Collect

- Affected environment and time window
- API health status
- OCR service health status
- Relevant request IDs or timestamps
- Recent deployment or config changes
- Logs from API, web, OCR, and infrastructure services
