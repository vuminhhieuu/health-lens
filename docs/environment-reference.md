# Environment Reference

**Last updated:** 2026-05-16

This file collects the environment variables and runtime settings that matter most across the repository.

## Root Variables

| Variable | Used by | Purpose |
| --- | --- | --- |
| `COMPOSE_PROJECT_NAME` | Docker Compose | Namespaces container and volume names |
| `COMPOSE_NETWORK` | Docker Compose | Overrides the default bridge network name |
| `WEB_PORT` | Docker Compose / web | Web app port mapping |
| `API_PORT` | Docker Compose / API | API port mapping |
| `DB_PORT` | Docker Compose / PostgreSQL | Local PostgreSQL host port |
| `REDIS_HOST_PORT` | Docker Compose / Redis | Local Redis host port |
| `MAILHOG_SMTP_PORT` | Docker Compose / Mailhog | SMTP port for captured mail |
| `MAILHOG_UI_PORT` | Docker Compose / Mailhog | Mailhog UI port |

## Database

| Variable | Used by | Purpose |
| --- | --- | --- |
| `DB_URL` | API | JDBC connection string |
| `DB_HOST` | API / compose | Database hostname |
| `DB_PORT` | API / compose | Database port |
| `DB_NAME` | API / compose | Database name |
| `DB_USERNAME` | API / compose | Database username |
| `DB_PASSWORD` | API / compose | Database password |

## Web

| Variable | Used by | Purpose |
| --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | Web | Base URL for browser API calls |

## OCR

| Variable | Used by | Purpose |
| --- | --- | --- |
| `OCR_SERVICE_URL` | API | FastAPI OCR service URL |
| `OCR_PROVIDER_PRIMARY` | API | Primary OCR provider selector |
| `OCR_PROVIDER_FALLBACK_ORDER` | API | OCR fallback provider order |
| `OCR_TIMEOUT_MS` | API | OCR request timeout |
| `OCR_TEXTRACT_ENABLED` | API | Enable AWS Textract path |
| `OCR_GCV_ENABLED` | API | Enable Google Cloud Vision path |
| `OCR_GCV_PROJECT_ID` | API | GCV project ID |

## AI And Retrieval

| Variable | Used by | Purpose |
| --- | --- | --- |
| `AI_CHAT_PROVIDER` | API | Chat provider selector. Currently only `openai-compatible` is supported for env-only switching |
| `AI_CHAT_API_KEY` | API | OpenAI-compatible chat provider key |
| `AI_CHAT_BASE_URL` | API | Chat provider base URL |
| `AI_CHAT_MODEL` | API | Chat model name |
| `AI_CHAT_TIMEOUT_MS` | API | Chat request timeout budget in milliseconds for app-level policy |
| `EMBEDDING_API_KEY` | API | Embedding provider key |
| `EMBEDDING_BASE_URL` | API | Embedding provider base URL |
| `EMBEDDING_MODEL` | API | Embedding model name |
| `QDRANT_HOST` | API | Qdrant host |
| `QDRANT_PORT` | API | Qdrant port |
| `QDRANT_API_KEY` | API | Qdrant API key |
| `QDRANT_COLLECTION` | API | Qdrant collection name |
| `QDRANT_VECTOR_DIMENSION` | API | Vector dimension |

Migration note: `GROQ_API_KEY`, `GROQ_BASE_URL`, and `GROQ_CHAT_MODEL` are accepted as a temporary legacy fallback when the matching `AI_CHAT_*` variable is absent. New deployments should use only `AI_CHAT_*`. Native non-OpenAI-compatible providers require a code adapter before they can be selected.

Provider switching note: follow [provider-switching-runbook.md](./provider-switching-runbook.md) before changing LLM, OCR, embedding, or vector store providers. The runbook defines env-only compatibility, smoke tests, rollback steps, and adapter-required cases.

Qdrant host rules: set `QDRANT_HOST` to the hostname only, for example `cluster-id.qdrant.io`; do not include protocol, port, path, query, fragment, or user-info. Use Qdrant gRPC port `6334` unless the provider explicitly gives another gRPC port. Production and staging fail fast on invalid host values. Local/dev can normalize URL-style `http://` or `https://` host values as a convenience, including an explicit URI port when `QDRANT_PORT` is not set.

Embedding/vector reindex rule: `QDRANT_VECTOR_DIMENSION` must match the active `EMBEDDING_MODEL` output dimension and the existing Qdrant collection dimension. The default `text-embedding-3-small` uses 1536 dimensions in this project. Production and staging startup validation fails fast on dimension mismatch instead of recreating or reindexing a collection automatically.

Reindex procedure when changing `EMBEDDING_MODEL`, `EMBEDDING_BASE_URL`, or `QDRANT_VECTOR_DIMENSION`:

1. Confirm the new embedding model dimension with the provider documentation or a staging embedding call.
2. Set `QDRANT_VECTOR_DIMENSION` to that dimension and point staging at a new or empty Qdrant collection.
3. Reingest the trusted RAG corpus into the collection using the current ingestion job/admin flow.
4. Run `/actuator/health` and verify the `aiRag` component is `UP`.
5. Promote the same embedding/vector settings to production only after staging retrieval checks pass.

## Storage And Mail

| Variable | Used by | Purpose |
| --- | --- | --- |
| `MINIO_ENDPOINT` | API / compose | S3-compatible storage endpoint |
| `MINIO_PUBLIC_ENDPOINT` | API | Public object-storage endpoint |
| `MINIO_ACCESS_KEY` | API / compose | Storage access key |
| `MINIO_SECRET_KEY` | API / compose | Storage secret key |
| `MINIO_BUCKET` | API | Storage bucket name |
| `MINIO_BUCKET_CHECK_ON_STARTUP` | API | Validate bucket on startup |
| `MINIO_BUCKET_AUTO_CREATE` | API | Auto-create bucket in dev |
| `MINIO_CORS_CONFIGURE` | API | Configure storage CORS |
| `MINIO_CORS_ALLOWED_ORIGINS` | API | Allowed object-storage origins |
| `MAIL_HOST` | API / compose | SMTP host |
| `MAIL_PORT` | API / compose | SMTP port |
| `MAIL_USERNAME` | API / compose | SMTP username |
| `MAIL_PASSWORD` | API / compose | SMTP password |
| `MAIL_FROM` | API | Default sender address |

## Security

| Variable | Used by | Purpose |
| --- | --- | --- |
| `JWT_SECRET` | API | JWT signing secret |
| `APP_COOKIE_SECURE` | API | Secure cookie flag |
| `ADMIN_TOTP_ENCRYPTION_KEY` | API | Encrypt admin TOTP secrets |

## Runtime Profiles

| Profile | Purpose |
| --- | --- |
| `dev` | Local development defaults with Docker Compose |
| `staging` | Staging deployment against managed cloud services |
| `production` | Production deployment defaults |

## Canonical Sources

- [README.md](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/README.md)
- [apps/api/src/main/resources/application.yml](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/api/src/main/resources/application.yml)
- [docker/compose.yml](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/docker/compose.yml)
- [docker/compose.dev.yml](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/docker/compose.dev.yml)
