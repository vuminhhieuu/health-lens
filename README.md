# HealthLens

> Intelligent healthcare document processing and health metric explanation platform.

## Overview

HealthLens is a monorepo containing a full-stack healthcare application that helps users:
- Upload and extract health metrics from medical documents (lab results, prescriptions)
- Get AI-powered explanations of health metrics in Vietnamese
- Track and manage health records over time

## Architecture

| Component | Technology | Dev | Staging | Production |
|-----------|------------|-----|---------|------------|
| Database | PostgreSQL | Docker | Neon | Neon |
| Redis | Redis | Docker | Railway | Railway |
| Object Storage | MinIO/S3 | Docker | S3 | S3 |
| Email | SMTP | Mailhog/Resend | Resend | Resend |
| Vector DB | Qdrant | Cloud | Cloud | Cloud |
| LLM | Groq API | Cloud | Cloud | Cloud |
| API | Spring Boot | Docker | Railway | Docker SSH |
| Web | Next.js | Docker | Vercel | Docker SSH |

## Project Structure

```
healthlens/
├── apps/
│   ├── api/              # Spring Boot 4.0 REST API
│   ├── web/              # Next.js 16 frontend
│   └── mobile/           # Expo mobile app (Phase 2)
├── packages/
│   └── shared/           # Shared types, schemas, constants
├── services/
│   └── ocr-service/      # EasyOCR FastAPI service
├── docker/               # Docker configuration
│   ├── compose.yml       # Base services (Redis, MinIO)
│   ├── compose.dev.yml   # Development (PostgreSQL, API, Web)
│   ├── compose.prod.yml  # Production (API, Web)
│   └── scripts/          # Helper scripts
├── .env                  # Development environment
├── .env.production       # Production environment
└── .env.example         # Environment template
```

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Docker | 24+ | For containerized services |
| Docker Compose | 2.20+ | Service orchestration |

---

## Quick Start

```bash
# 1. Copy environment file
cp .env.example .env

# 2. Configure .env with your credentials
#    - Add Groq API key
#    - Add Qdrant credentials
#    - (Optional) Add Resend API key for real emails

# 3. Start development environment
cd docker
./scripts/up.sh

# 4. View logs
./scripts/logs.sh api -f
```

---

## Development Environment

### Services

| Service | Port | Description |
|---------|------|-------------|
| API | 8080 | Spring Boot backend |
| Web | 3000 | Next.js frontend |
| PostgreSQL | 5432 | Local database |
| Redis | 6379 | Caching & sessions |
| MinIO | 9000/9001 | S3 storage + Console |
| Mailhog | 1025/8025 | Email testing UI |

### Start Development

```bash
# All services (API, Web, PostgreSQL, Redis, MinIO)
cd docker
./scripts/up.sh

# With Mailhog (email testing)
./scripts/up.sh --mail

# With OCR Service (~2GB RAM)
./scripts/up.sh --ocr

# Rebuild images
./scripts/up.sh --build

# Stop
./scripts/down.sh
```

### Email Configuration

Dev environment supports two options:

**Option 1: Mailhog (Default)**
```bash
# Emails captured locally, view at http://localhost:8025
# No real emails sent
```

**Option 2: Resend (Real emails)**
```bash
# In .env, uncomment Resend section:
MAIL_ENABLED=true
MAIL_DRIVER=resend
RESEND_API_KEY=re_xxxxxxxxxxxx
MAIL_FROM=noreply@healthlens.vn
```

### Scripts

```bash
./scripts/up.sh          # Start all services
./scripts/up.sh --mail  # Include Mailhog
./scripts/up.sh --build # Rebuild images
./scripts/down.sh       # Stop services
./scripts/down.sh -v    # Stop and remove volumes
./scripts/logs.sh api   # View API logs
```

---

## Environment Files

| File | Purpose | Git |
|------|---------|-----|
| `.env` | Development | `.gitignore` |
| `.env.staging` | Vercel staging | `.gitignore` |
| `.env.staging.api` | Railway staging | `.gitignore` |
| `.env.production` | Production server | `.gitignore` |
| `.env.example` | Template | ✅ Commit |

### .env Variables

```bash
# Database
DB_HOST=postgres
DB_NAME=healthlens_dev

# Email
MAIL_HOST=mailhog          # or Resend API
RESEND_API_KEY=re_xxx

# External Services
GROQ_API_KEY=sk-xxxxx
QDRANT_HOST=https://xxx.qdrant.io
```

---

## Deployment

### Branch Strategy

| Branch | Deploy To | Purpose |
|--------|-----------|---------|
| `dev` | Vercel + Railway | Development testing |
| `staging` | Vercel + Railway | Pre-production testing |
| `main` | Production server | Live production |

### Staging Deployment

Push to `staging` branch → Manual deploy:

**1. Create staging branch:**
```bash
git checkout dev
git checkout -b staging
git push origin staging
```

**2. Configure services:**

| Service | Platform | Setup Guide |
|---------|----------|-------------|
| Web | Vercel | [Staging Deployment](docs/STAGING_DEPLOYMENT.md) |
| API | Railway | Import repo, set `apps/api` as root |
| Database | Neon | Free tier PostgreSQL |
| OCR | Railway | Import repo, set `services/ocr-service` as root |
| Storage | MinIO Cloud / AWS S3 | Create bucket |

**3. Environment variables:**
- See `.env.staging` for Web (Vercel)
- See `.env.staging.api` for API (Railway)

### Production (Docker SSH)

Push to `main` branch → GitHub Actions:
1. Build Docker images
2. Push to GHCR
3. SSH to server
4. Pull images & restart containers

Required GitHub Secrets:
- `PRODUCTION_SSH_KEY`
- `PRODUCTION_HOST`
- `PRODUCTION_USER`

---

## Testing

```bash
# All tests
pnpm test

# API tests
cd apps/api && ./gradlew test

# Web tests
cd apps/web && pnpm test
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | User registration |
| POST | `/api/v1/auth/login` | User login |
| POST | `/api/v1/auth/refresh` | Refresh token |
| GET | `/api/v1/profiles` | List profiles |
| POST | `/api/v1/health-records` | Upload record |
| POST | `/api/v1/ocr/extract` | OCR extraction |

---

## Troubleshooting

### Port Conflicts
```bash
lsof -i :8080  # API
lsof -i :3000  # Web
lsof -i :5432  # PostgreSQL
```

### Database Issues
```bash
# Check PostgreSQL logs
docker compose logs postgres

# Recreate database
docker compose -f compose.yml -f compose.dev.yml down -v
docker compose -f compose.yml -f compose.dev.yml up -d
```

### OCR Memory
OCR requires ~2GB RAM. Enable with `--ocr` profile.

---

## Contributing

```bash
git checkout -b feature/my-feature
git commit -m 'feat: add feature'
git push origin feature/my-feature
# Open Pull Request
```

## Support

- Docs: `/docs`
- Architecture: `_bmad-output/planning-artifacts/`
- Stories: `_bmad-output/implementation-artifacts/`
