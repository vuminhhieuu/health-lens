# Deployment Guide

**Last updated:** 2026-05-16

## Local Development Deployment

Local development uses Docker Compose:

- `docker/compose.yml` provides base Redis and MinIO services.
- `docker/compose.dev.yml` adds PostgreSQL, Mailhog, API, web, and optional OCR.
- `docker/scripts/up.sh`, `logs.sh`, `down.sh`, and `cleanup.sh` wrap common lifecycle commands.

## Staging Reference

The current staging reference stack is documented in [STAGING_DEPLOYMENT.md](./STAGING_DEPLOYMENT.md). The README identifies this staging split:

| Layer | Staging service |
| --- | --- |
| Web | Vercel |
| API | Render |
| OCR | Render |
| PostgreSQL | Neon |
| Redis | Upstash |
| Storage | Cloudflare R2 |

## API Deployment Concerns

The API requires:

- `SPRING_PROFILES_ACTIVE` set for the target environment.
- `DB_URL`, database credentials, and Flyway access.
- Redis host/port/password and SSL settings where applicable.
- JWT secret with enough entropy.
- Mail configuration for verification, reset, invitation, and deletion email flows.
- Object storage endpoint, bucket, credentials, and region.
- Groq/OpenAI-compatible chat provider configuration.
- Embedding and Qdrant configuration when retrieval features are enabled.

## Web Deployment Concerns

The web app requires:

- `NEXT_PUBLIC_API_BASE_URL` pointing to the deployed API.
- CORS allowed origins configured in the API.
- Route compatibility with shared `ApiPaths`.

## OCR Deployment Concerns

The OCR service requires:

- Enough CPU/RAM for EasyOCR model loading.
- Persistent or cached model storage if cold-start time matters.
- Network access from the API.
- `OCR_SERVICE_URL` configured in the API.

## CI/CD

GitHub workflows are present at:

- `.github/workflows/ci.yml`
- `.github/workflows/deploy.yml`

The README states that CI covers linting, type checks, API tests, and build validation for relevant app/package changes.

