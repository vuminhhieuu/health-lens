---
story_id: "infra-1"
epic: "infra"
story_key: "infra-1-local-dev-docker"
title: "Local Development Environment với Docker Compose"
status: "ready-for-dev"
priority: "P1"
created_date: "2026-04-01"
input_artifacts: ["architecture.md", "docker-compose.dev.yml", ".env.example"]
---

# Story: Local Development Environment với Docker Compose

## User Story Statement

As a nhóm phát triển,
I want có môi trường development local với Docker Compose,
So that các thành viên có thể develop và test nhanh trên máy local với tất cả services cần thiết.

## Business Value

- Developer workflow hiệu quả
- Tách biệt môi trường dev khỏi production
- Dễ dàng onboarding thành viên mới
- Test integration locally trước khi push

## Technical Context

### Tech Stack
- Docker & Docker Compose
- Spring Boot (API)
- Next.js (Web)
- PostgreSQL (Neon - cloud, không cần local)
- Redis (optional)
- MinIO (local S3)
- EasyOCR (optional)

### Current State
- ✅ `apps/api/Dockerfile` exists
- ✅ `apps/web/Dockerfile` exists
- ⚠️ `docker-compose.dev.yml` references non-existent `services/ocr-service`
- ⚠️ Missing `.env` file template

## Requirements

### Functional Requirements
1. **FR-Infra.1.1**: Docker Compose file khởi động tất cả services cần thiết (api, web, minio, redis, mailhog, ocr-service)
2. **FR-Infra.1.2**: Cấu hình healthcheck cho từng service
3. **FR-Infra.1.3**: Environment variables được quản lý qua `.env` file
4. **FR-Infra.1.4**: Volume mounting cho hot-reload (dev mode)
5. **FR-Infra.1.5**: Network configuration cho inter-service communication
6. **FR-Infra.1.6**: Cleanup script để stop/remove all services

### Non-Functional Requirements
- **NFR-Infra.1.1**: Khởi động tất cả services trong < 3 phút
- **NFR-Infra.1.2**: Hot-reload works cho cả Spring Boot và Next.js
- **NFR-Infra.1.3**: Logs dễ debug từ docker-compose logs

## Acceptance Criteria

### Given
Khi developer chạy `docker-compose up`

### When
Tất cả services được build và start

### Then
- [ ] API available tại `http://localhost:8080`
- [ ] Web available tại `http://localhost:3000`
- [ ] MinIO console tại `http://localhost:9001`
- [ ] Health checks pass cho tất cả services
- [ ] Logs hiển thị rõ ràng

### Given
Khi developer edit code

### When
Services tự động rebuild/restart

### Then
- [ ] Spring Boot hot-reload works
- [ ] Next.js hot-reload works
- [ ] Không cần restart toàn bộ container

## Implementation Details

### Files to Create/Modify

```
docker/
├── docker-compose.dev.yml      # Update (fix OCR service reference)
├── .env                        # Create (from .env.example)
└── scripts/
    ├── start-dev.sh           # Create
    └── stop-dev.sh            # Create

.env.example                    # Update (ensure complete)
```

### Services Configuration

| Service | Port | Environment | Notes |
|---------|------|-------------|-------|
| api | 8080 | docker | Spring Boot with hot-reload |
| web | 3000 | docker | Next.js with hot-reload |
| minio | 9000/9001 | dev | S3-compatible storage |
| redis | 6379 | optional | Profile: with-redis |
| ocr-service | 8001 | optional | Profile: with-ocr |
| mailhog | 1025/8025 | optional | Profile: with-mail |

### Docker Compose Profiles

```yaml
profiles:
  - with-redis    # Include Redis
  - with-ocr      # Include OCR service
  - with-mail     # Include Mailhog
  - with-minio    # Include MinIO
```

### Commands Reference

```bash
# Start all core services (api + web)
docker compose -f docker/docker-compose.dev.yml up

# Start with optional services
docker compose -f docker/docker-compose.dev.yml --profile with-ocr --profile with-redis up

# View logs
docker compose -f docker/docker-compose.dev.yml logs -f

# Stop all
docker compose -f docker/docker-compose.dev.yml down
```

## Dependencies

- **Pre-requisite**: Docker Desktop installed
- **Pre-requisite**: `.env` file configured với Neon, Groq, Qdrant credentials

## Testing Checklist

- [ ] `docker compose config` validates without errors
- [ ] All services start successfully
- [ ] Health endpoints return 200
- [ ] Hot-reload works on file changes
- [ ] Logs are readable

## Notes

- OCR service hiện tại không tồn tại - cần quyết định: bỏ qua hoặc tạo mới
- Neon PostgreSQL là cloud-based, không cần local DB
- Sử dụng profiles để toggle optional services