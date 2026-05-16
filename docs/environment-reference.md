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
| `GROQ_API_KEY` | API | OpenAI-compatible chat provider key |
| `GROQ_BASE_URL` | API | Chat provider base URL |
| `GROQ_CHAT_MODEL` | API | Chat model name |
| `EMBEDDING_API_KEY` | API | Embedding provider key |
| `EMBEDDING_BASE_URL` | API | Embedding provider base URL |
| `EMBEDDING_MODEL` | API | Embedding model name |
| `QDRANT_HOST` | API | Qdrant host |
| `QDRANT_PORT` | API | Qdrant port |
| `QDRANT_API_KEY` | API | Qdrant API key |
| `QDRANT_COLLECTION` | API | Qdrant collection name |
| `QDRANT_VECTOR_DIMENSION` | API | Vector dimension |

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

