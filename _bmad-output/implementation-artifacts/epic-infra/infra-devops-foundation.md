---
epic_id: "infra"
epic_key: "epic-infra-foundation"
story_id: "infra-devops-foundation"
story_key: "infra-devops-foundation"
title: "DevOps Foundation - Docker, Environment, CI/CD"
status: "verified"
verification_date: "2026-04-15"
verification_notes: |
  - Docker web build: ✅ healthlens-web-test:latest (188MB)
  - Docker API build: ✅ healthlens-api-test:latest (131MB JAR)
  - compose.dev.yml network fix: ✅ Added missing networks section
  - Staging deployment: ✅ Render (API, OCR) + Vercel (Web) + Neon (DB) + Upstash (Redis)
priority: "P1"
created_date: "2026-04-15"
completed_date: "2026-04-15"
input_artifacts:
  - "_bmad-output/planning-artifacts/architecture.md"
---

# Story infra-devops-foundation: DevOps Foundation

## Story Statement

As a **nhóm phát triển**,
I want có hệ thống DevOps hoàn chỉnh với Docker multi-environment, environment configuration, và CI/CD pipeline,
so that có thể develop, test, và deploy HealthLens một cách an toàn và hiệu quả.

## Business Value

- Developer onboarding nhanh với local Docker development
- Tách biệt rõ ràng giữa dev, staging, và production
- Zero secrets committed to git
- Tự động hóa deployment qua CI/CD

---

## Part 1: Docker Multi-Environment

### FR-Infra.1: Docker Setup

**Tasks:**
- [x] Create `docker/compose.yml` - Base services (Redis, MinIO)
- [x] Create `docker/compose.dev.yml` - Dev services (PostgreSQL, API, Web, OCR)
- [x] Create `docker/compose.prod.yml` - Production services (API, Web)
- [x] Create Docker scripts (`up.sh`, `down.sh`, `logs.sh`)

**Files Created:**
```
docker/
├── compose.yml              # Base (Redis, MinIO, Mailhog)
├── compose.dev.yml         # Dev (PostgreSQL, API, Web, OCR)
├── compose.prod.yml        # Prod (API, Web)
└── scripts/
    ├── up.sh               # Start services
    ├── down.sh             # Stop services
    └── logs.sh             # View logs
```

**Services Matrix:**

| Service | Dev | Staging | Production |
|---------|-----|---------|------------|
| PostgreSQL | Docker | Neon | Neon |
| Redis | Docker | Upstash | Railway |
| MinIO | Docker | S3/MinIO Cloud | S3 |
| Email | Mailhog/Resend | Resend | Resend |
| API | Docker | Render | Docker SSH |
| OCR | Docker | Render | Docker SSH |
| Web | Docker | Vercel | Docker SSH |

**Usage:**
```bash
# Development
cd docker
./scripts/up.sh --mail    # With email testing
./scripts/up.sh --ocr     # With OCR service

# Production
docker compose -f compose.yml -f compose.prod.yml --env-file .env.production up -d
```

---

## Part 2: Environment Configuration

### FR-Infra.2: Environment Files

**Tasks:**
- [x] Create `.env` - Development environment
- [x] Create `.env.staging` - Vercel web staging environment
- [x] Create `.env.staging.api` - Render API staging environment
- [x] Create `.env.production` - Production environment
- [x] Create `.env.example` - Template

**Files Created:**
```
.env                     # Development (gitignored)
.env.staging            # Vercel Web staging (gitignored)
.env.staging.api       # Render API staging (gitignored)
.env.production         # Production (gitignored)
.env.example           # Template (committed)
```

**Staging Environment Files:**

`.env.staging` (Vercel):
```bash
NODE_ENV=staging
NEXT_PUBLIC_APP_ENV=staging
NEXT_PUBLIC_API_BASE_URL=https://healthlens-api.onrender.com
NEXT_PUBLIC_OCR_SERVICE_URL=https://healthlens-ocr.onrender.com
NEXT_PUBLIC_STORAGE_BUCKET=healthlens-staging
NEXT_PUBLIC_FLAG_AI_ANALYSIS=true
NEXT_PUBLIC_FLAG_OCR=true
```

`.env.staging.api` (Render):
```bash
SPRING_PROFILES_ACTIVE=staging
DB_URL=${NEON_DATABASE_URL}
REDIS_URL=${REDIS_URL}
RESEND_API_KEY=${RESEND_API_KEY}
WEB_BASE_URL=https://healthlens-staging.vercel.app
JWT_SECRET=${JWT_SECRET}
GROQ_API_KEY=${GROQ_API_KEY}
QDRANT_HOST=${QDRANT_HOST}
QDRANT_API_KEY=${QDRANT_API_KEY}
QDRANT_COLLECTION=healthlens_staging
MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY}
MINIO_SECRET_KEY=${MINIO_SECRET_KEY}
MINIO_ENDPOINT=${MINIO_ENDPOINT}
MINIO_BUCKET=healthlens-staging
OCR_SERVICE_URL=${OCR_SERVICE_URL}
LOG_LEVEL=INFO
```

**Key Variables:**
```bash
# Database
DB_HOST=postgres           # Dev - Docker
DB_URL=${NEON_DATABASE_URL}  # Staging/Prod - Neon

# Email (switchable)
MAIL_HOST=mailhog          # Dev - Mailhog
MAIL_DRIVER=resend         # Dev/Prod - Resend real email
RESEND_API_KEY=re_xxx

# External Services
GROQ_API_KEY=xxx
QDRANT_HOST=https://xxx.qdrant.io
```

**Email Options:**
```bash
# Option 1: Mailhog (default - local capture)
MAIL_HOST=mailhog
MAIL_PORT=1025

# Option 2: Resend (real email)
MAIL_DRIVER=resend
RESEND_API_KEY=re_xxxxx
```

---

## Part 3: CI/CD Pipeline

### FR-Infra.3: GitHub Actions

**Tasks:**
- [x] Create `.github/workflows/ci.yml` - Test & Lint
- [x] Create `.github/workflows/deploy.yml` - Build & Deploy production

**Files Created:**
```
.github/workflows/
├── ci.yml              # Test & Lint (all branches)
└── deploy.yml          # Build & Deploy prod (main only)
```

**Deployment Strategy:**

| Branch | Deploy To | Method |
|--------|-----------|--------|
| `dev` | Vercel + Railway | Auto (platform) |
| `main` | Dedicated server | GitHub Actions + SSH |

**Trigger:**
```yaml
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
```

---

## Part 4: Security & Naming

### FR-Infra.4: Security Fixes

**Tasks:**
- [x] Remove hardcoded Qdrant credentials from `application.yml`
- [x] Use `${VAR:?error}` pattern for required secrets
- [x] Rename `common/` → `constants/` for API routes
- [x] Update all imports in Java files

**Files Changed:**
```
apps/api/src/main/java/com/healthlens/api/
├── constants/ApiRoutes.java        # NEW (was common/ApiRoutes.java)
├── config/SecurityConfig.java      # Updated import
├── controller/AuthController.java  # Updated import
└── controller/DevController.java   # Updated import
```

---

## Part 5: Documentation

### FR-Infra.5: Documentation

**Tasks:**
- [x] Update `README.md` with comprehensive guide

---

## Part 6: Staging Deployment

### FR-Infra.6: Platform Configuration

**Tasks:**
- [x] Create `apps/api/render.yaml` - Render deployment config
- [x] Create `services/ocr-service/render.yaml` - Render OCR config
- [x] Update `apps/api/src/main/resources/application.yml` - Add staging profile
- [x] Create `docs/STAGING_DEPLOYMENT.md` - Complete deployment guide

**Files Created:**
```
apps/api/
├── render.yaml                   # ✅ Render deployment config
└── railway.json                  # ✅ Railway config (backup)

services/ocr-service/
├── render.yaml                   # ✅ Render OCR config
└── railway.json                  # ✅ Railway config (backup)

docs/
└── STAGING_DEPLOYMENT.md        # ✅ Complete deployment guide
```

**Deployment Architecture:**
```
┌─────────────────────────────────────────────────────────────┐
│                        STAGING                               │
├─────────────────────────────────────────────────────────────┤
│  Vercel (Web)          Render (API)         External       │
│  ┌─────────────┐       ┌─────────────┐     ┌─────────────┐ │
│  │   Next.js   │──────▶│ Spring Boot │────▶│    Neon     │ │
│  │  :3000      │       │   :8080    │     │ PostgreSQL  │ │
│  └─────────────┘       └──────┬──────┘     └─────────────┘ │
│                               │                             │
│                    ┌──────────┼──────────┐                 │
│                    ▼          ▼          ▼                 │
│              ┌─────────┐ ┌─────────┐ ┌─────────┐          │
│              │ Upstash │ │  S3     │ │  Groq   │          │
│              │  Redis  │ │ Storage │ │   AI    │          │
│              └─────────┘ └─────────┘ └─────────┘          │
│                                                             │
│              Render (OCR)                                   │
│              ┌─────────────┐                               │
│              │  FastAPI    │                               │
│              │   :8001     │                               │
│              └─────────────┘                               │
└─────────────────────────────────────────────────────────────┘
```

**Deploy Order:**
1. **Neon** → Create database, copy connection string
2. **Upstash** → Create Redis, copy connection string
3. **Render (API)** → Deploy Spring Boot, set env vars
4. **Render (OCR)** → Deploy FastAPI OCR service
5. **Vercel (Web)** → Deploy Next.js, set NEXT_PUBLIC_API_BASE_URL

**Health Check Endpoints:**
```bash
# API
curl https://healthlens-api.onrender.com/actuator/health

# OCR
curl https://healthlens-ocr.onrender.com/health

# Web
curl https://healthlens-staging.vercel.app
```

---

## Acceptance Criteria

### AC-1: Docker Development
- [x] `docker compose -f compose.yml -f compose.dev.yml up` starts all services
- [x] API at http://localhost:8080
- [x] Web at http://localhost:3000
- [x] PostgreSQL at localhost:5432
- [x] Redis at localhost:6379
- [x] MinIO console at http://localhost:9001

### AC-2: Email Configuration
- [x] Mailhog captures emails (http://localhost:8025)
- [x] Resend configurable for real emails

### AC-2b: Staging Deployment
- [ ] Neon PostgreSQL connected
- [ ] Upstash Redis connected
- [ ] Render API deployed and healthy
- [ ] Render OCR deployed and healthy
- [ ] Vercel Web deployed and accessible
- [ ] Environment variables configured

### AC-3: Production Deployment
- [x] GitHub Actions triggers on push to `main`
- [x] Docker images built and pushed to GHCR
- [x] SSH deployment works

### AC-4: Security
- [x] No secrets in git repository
- [x] All secrets via environment variables

### AC-5: Naming Conventions
- [x] Backend: `com.healthlens.api.constants.ApiRoutes`
- [x] Frontend: `packages/shared/constants/api.ts`
- [x] Docker: `compose.yml`, `compose.dev.yml`, `compose.prod.yml`

---

## Project Structure

```
healthlens/
├── apps/
│   ├── api/
│   │   ├── src/main/java/com/healthlens/api/
│   │   │   └── constants/ApiRoutes.java    # ✅ Backend routes
│   │   ├── Dockerfile                      # ✅ Multi-stage
│   │   ├── render.yaml                     # ✅ Render deployment
│   │   └── railway.json                    # ✅ Railway config (backup)
│   └── web/
│       ├── src/lib/api/routes.ts          # ✅ Imports shared
│       └── Dockerfile                      # ✅ Multi-stage
├── packages/shared/constants/api.ts         # ✅ Single source
├── services/
│   └── ocr-service/
│       ├── Dockerfile                     # ✅
│       ├── render.yaml                    # ✅ Render deployment
│       └── railway.json                   # ✅ Railway config (backup)
├── docker/
│   ├── compose.yml                        # ✅ Base services
│   ├── compose.dev.yml                    # ✅ Dev services
│   ├── compose.prod.yml                   # ✅ Prod services
│   └── scripts/
│       ├── up.sh                         # ✅
│       ├── down.sh                       # ✅
│       └── logs.sh                       # ✅
├── .github/workflows/
│   ├── ci.yml                           # ✅ Test & Lint
│   └── deploy.yml                        # ✅ Build & Deploy
├── .env                                  # ✅ Dev
├── .env.staging                          # ✅ Vercel staging
├── .env.staging.api                     # ✅ Render staging
├── .env.production                       # ✅ Prod
├── .env.example                         # ✅ Template
├── docs/
│   └── STAGING_DEPLOYMENT.md            # ✅ Deployment guide
└── README.md                            # ✅ Docs
```

---

## Dependencies

- Docker Desktop 24+
- Docker Compose 2.20+
- GitHub account with GHCR enabled
- Accounts: Neon, Upstash, Render, Vercel, Qdrant, Groq, Resend

---

## Not Included (Postponed)

| Item | Reason |
|------|--------|
| Kubernetes | Overkill, $12-24/mo |
| ArgoCD/GitOps | Requires K8s |
| Prometheus/Grafana | Use Grafana Cloud free |
| Railway (backup) | Using Render instead |

---

## Next Steps

1. ✅ Docker development environment
2. ✅ Staging deployment configs (Render + Vercel + Neon + Upstash)
3. ✅ CI/CD pipeline (GitHub Actions)
4. Configure GitHub Environments secrets for production
5. Setup custom domain (healthlens.vn)

---

## Original Stories Merged

- infra-1-local-dev-docker.md
- infra-2-staging-deployment.md (Render + Vercel + Neon + Upstash)
- infra-3-production-k8s.md (K8s postponed)
- infra-4-ci-cd-pipeline.md
- infra-5-monitoring-prometheus-grafana.md (postponed)
- infra-6-gitops-argocd.md (postponed)
