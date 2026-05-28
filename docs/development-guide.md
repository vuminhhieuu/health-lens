# Development Guide

**Last updated:** 2026-05-16

## Prerequisites

- Docker 24+
- Docker Compose 2.20+
- pnpm 10.13.1
- Java 21 for API development outside containers

## Install

```bash
pnpm install
```

## Environment

Create a local environment file from the example:

```bash
cp .env.example .env
```

Important variables include:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- `NEXT_PUBLIC_API_BASE_URL`
- `OCR_SERVICE_URL`
- `MINIO_ENDPOINT`, `MINIO_PUBLIC_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`
- `AI_CHAT_PROVIDER`, `AI_CHAT_API_KEY`, `AI_CHAT_BASE_URL`, `AI_CHAT_MODEL`, `AI_CHAT_TIMEOUT_MS`
- `EMBEDDING_API_KEY`, `EMBEDDING_BASE_URL`, `EMBEDDING_MODEL`
- `QDRANT_HOST`, `QDRANT_PORT`, `QDRANT_API_KEY`, `QDRANT_COLLECTION`
- `JWT_SECRET`

## Local Stack

Start the default development stack:

```bash
pnpm docker:up
```

Useful variants:

```bash
pnpm docker:up:build
pnpm docker:up --no-cache
pnpm docker:up:ocr
pnpm docker:up --rebuild-api
pnpm docker:up --rebuild-web
pnpm docker:up --ci
```

With pnpm, pass package-script arguments directly, for example `pnpm docker:logs api -f`.
With npm, pass them after `--`, for example `npm run docker:logs -- api -f`.

Default endpoints:

| Service | URL |
| --- | --- |
| Web | `http://localhost:3000` |
| API | `http://localhost:8080` |
| MinIO console | `http://localhost:9001` |
| Mailhog | `http://localhost:8025` |
| OCR, when enabled | `http://localhost:8001` |

## Repository Scripts

From the repository root:

```bash
pnpm dev
pnpm build
pnpm test
pnpm docker:up
pnpm docker:logs
pnpm docker:down
pnpm db:analyze:flyway-squash
```

Targeted commands:

```bash
cd apps/web && pnpm dev
cd apps/web && pnpm test
cd apps/api && ./gradlew test
cd apps/mobile && pnpm start
cd packages/shared && pnpm build
```

## Testing Notes

- Web tests use Vitest and Testing Library.
- API tests use Gradle/JUnit with Spring Boot test dependencies and Testcontainers dependencies.
- Root `pnpm test` runs recursive package tests where present.

## Code Ownership Notes

- Keep backend route constants in `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java` synchronized with `packages/shared/constants/api.ts`.
- Add frontend-consumed shared contracts to `packages/shared` instead of duplicating string constants across apps.
- Do not read generated folders such as `.next`, `node_modules`, `.gradle`, or `apps/api/build` when producing source-level analysis.
