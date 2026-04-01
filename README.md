# HealthLens

> Intelligent healthcare document processing and health metric explanation platform.

## Overview

HealthLens is a monorepo containing a full-stack healthcare application that helps users:
- Upload and extract health metrics from medical documents (lab results, prescriptions)
- Get AI-powered explanations of health metrics in Vietnamese
- Track and manage health records over time

### Architecture

HealthLens uses **Option B+ (Fully Cloud)** architecture optimized for development on resource-constrained laptops (16GB RAM):

| Component | Technology | Location |
|-----------|------------|----------|
| Database | PostgreSQL | Neon Cloud (managed) |
| Vector DB | Qdrant | Qdrant Cloud (managed) |
| LLM | Groq API | Cloud (qwen-2.5-72b) |
| Embeddings | Groq API | Cloud (embed-multilingual-v3) |
| OCR | EasyOCR | Local Docker |
| Object Storage | MinIO / AWS S3 | Local / Cloud |
| Backend | Spring Boot 4.0 | Docker |
| Frontend | Next.js 16 | Docker |

## Project Structure

```
healthlens/
├── apps/
│   ├── api/           # Spring Boot 4.0 REST API
│   ├── web/           # Next.js 16 frontend
│   └── mobile/        # Expo mobile app (Phase 2)
├── packages/
│   └── shared/        # Shared types, schemas, constants
├── docker/
│   └── docker-compose.dev.yml
└── docs/              # Documentation
```

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | Required for Spring Boot |
| Node.js | 20+ | Required for Next.js |
| pnpm | 9+ | Package manager |
| Docker | 24+ | For containerized services |
| Docker Compose | 2.20+ | Service orchestration |

## Quick Start

### 1. Clone and Install

```bash
git clone https://github.com/your-org/healthlens.git
cd healthlens
pnpm install
```

### 2. Configure Environment

Copy the environment template and fill in your credentials:

```bash
cp .env.example .env
```

Required environment variables:

```bash
# Neon PostgreSQL (https://neon.tech)
NEON_DATABASE_URL=postgresql://user:pass@host/db?sslmode=require
NEON_USER=your-username
NEON_PASSWORD=your-password

# Groq API (https://console.groq.com)
GROQ_API_KEY=sk-xxxxx

# Qdrant Cloud (https://cloud.qdrant.io)
QDRANT_HOST=https://xxxx.cloud.qdrant.io
QDRANT_API_KEY=qdrant_api_key_xxxx
```

### 3. Start Services

```bash
# Start all core services (API, Web, MinIO)
docker compose -f docker/docker-compose.dev.yml up -d

# Or start with optional services
docker compose -f docker/docker-compose.dev.yml \
  --profile with-ocr \
  --profile with-mail \
  up -d
```

### 4. Verify Services

| Service | URL | Description |
|---------|-----|-------------|
| Web App | http://localhost:3000 | Frontend |
| API | http://localhost:8080 | REST API |
| API Health | http://localhost:8080/actuator/health | Health check |
| MinIO Console | http://localhost:9001 | Object storage UI |
| Mailhog | http://localhost:8025 | Email testing (with --profile with-mail) |

## Development

### Running Services Locally

For iterative development, run services outside Docker:

```bash
# API (Spring Boot)
cd apps/api
./gradlew bootRun --args='--spring.profiles.active=dev'

# Web (Next.js)
cd apps/web
pnpm dev
```

### Running Tests

```bash
# All tests
pnpm test

# API tests only
cd apps/api
./gradlew test

# Web tests only
cd apps/web
pnpm test
```

### Building for Production

```bash
# Build Docker images
docker compose -f docker/docker-compose.dev.yml build

# Or build individually
docker build -t healthlens/api ./apps/api
docker build -t healthlens/web ./apps/web
```

## Services Reference

### Core Services

| Service | Description | Port |
|---------|-------------|------|
| **api** | Spring Boot REST API | 8080 |
| **web** | Next.js frontend | 3000 |
| **minio** | S3-compatible object storage | 9000/9001 |

### Optional Services

Enable with `--profile` flag:

| Profile | Service | Description |
|---------|---------|-------------|
| `with-ocr` | EasyOCR | Local OCR processing (~500MB RAM) |
| `with-redis` | Redis | Caching layer |
| `with-mail` | Mailhog | Email testing interface |

## API Documentation

### Authentication

HealthLens uses JWT-based authentication. Include the token in requests:

```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/v1/health-records
```

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | User registration |
| POST | `/api/v1/auth/login` | User login |
| GET | `/api/v1/health-records` | List health records |
| POST | `/api/v1/health-records` | Upload new record |
| POST | `/api/v1/ocr/extract` | Extract metrics via OCR |
| POST | `/api/v1/llm/explain` | Get metric explanation |
| GET | `/api/v1/reference-data/search` | Search reference data |

## Troubleshooting

### Port Conflicts

If ports are already in use:

```bash
# Check what's using port 8080
lsof -i :8080

# Check what's using port 3000
lsof -i :3000
```

### Database Connection Issues

1. Verify Neon credentials in `.env`
2. Check Neon dashboard for connection issues
3. Ensure your IP is whitelisted in Neon

### OCR Service Not Starting

The OCR service requires significant memory. Ensure Docker has adequate resources:

```json
{
  "memory": 4096,
  "vm.memory": 4096
}
```

### MinIO Access Issues

Default credentials for local development:
- Access Key: `minioadmin`
- Secret Key: `minioadmin`

Create a bucket named `healthlens` in the MinIO console.

## Contributing

1. Create a feature branch: `git checkout -b feature/my-feature`
2. Commit changes: `git commit -m 'feat: add new feature'`
3. Push to branch: `git push origin feature/my-feature`
4. Open a Pull Request

## License

MIT License - see LICENSE file for details.

## Support

- Documentation: `/docs`
- Architecture: `_bmad-output/planning-artifacts/`
- Stories: `_bmad-output/implementation-artifacts/`
