# Research Report: Technical Infrastructure & Security Restructuring

**Project:** HealthLens  
**Date:** 2026-04-15  
**Research Type:** Technical Infrastructure  
**Author:** ie303  
**Status:** ✅ Research Complete

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Analysis](#current-state-analysis)
3. [Docker Multi-Environment Setup](#docker-multi-environment-setup)
4. [Secrets Management](#secrets-management)
5. [Project Structure & Constants Organization](#project-structure--constants-organization)
6. [Integration Patterns](#integration-patterns)
7. [Architectural Patterns](#architectural-patterns)
8. [Implementation Recommendations](#implementation-recommendations)
9. [Priority Roadmap](#priority-roadmap)

---

## 1. Executive Summary

### Research Goals
Thiết lập Docker workflows cho dev/staging/prod, tổ chức lại project structure với constants/security files, và xác định các infra cần thiết.

### Key Findings

| Area | Status | Priority |
|------|--------|----------|
| Docker Multi-Environment | ⚠️ Partial (chỉ có dev) | HIGH |
| Secrets Management | ❌ CRITICAL - Có secrets trong code | CRITICAL |
| Project Constants | ❌ Không có centralized constants | HIGH |
| API Routes Organization | ❌ Hardcoded trong code | MEDIUM |
| CI/CD Deploy Workflows | ⚠️ Chỉ có CI build | HIGH |
| Backend Architecture | ✅ Layered architecture tốt | - |

### Critical Issues Found

1. **NGUY HIỂM**: Qdrant API key đã bị commit vào git
2. **NGUY HIỂM**: Credentials hardcoded trong `application.yml`
3. **CẦN CẢI THIỆN**: Không có Docker workflows cho staging/production
4. **CẦN CẢI THIỆN**: Không có centralized constants package

---

## 2. Current State Analysis

### 2.1 Project Overview

```
health-lens/
├── apps/
│   ├── api/                 # Spring Boot Backend (Java 21)
│   │   ├── src/main/java/com/healthlens/
│   │   │   ├── api/         # Controllers
│   │   │   ├── service/     # Business Logic
│   │   │   ├── repository/  # Data Access
│   │   │   ├── dto/         # Data Transfer Objects
│   │   │   ├── entity/      # JPA Entities
│   │   │   └── config/      # Configuration
│   │   ├── src/main/resources/
│   │   │   ├── application.yml       # ⚠️ Có secrets!
│   │   │   ├── application-docker.yml
│   │   │   └── application-dev.yml
│   │   └── Dockerfile
│   ├── web/                 # Next.js Frontend (React 19)
│   │   ├── src/
│   │   │   ├── app/
│   │   │   ├── components/
│   │   │   └── lib/
│   │   └── Dockerfile
│   └── mobile/              # React Native (Expo)
├── services/
│   └── ocr-service/         # Python FastAPI + EasyOCR
│       ├── app.py
│       └── Dockerfile
├── packages/
│   └── shared/              # Shared types & utilities
│       ├── constants/
│       │   └── index.ts     # ⚠️ Chưa có gì!
│       └── schemas/
├── docker/
│   ├── docker-compose.dev.yml
│   └── ...
├── .env                     # ⚠️ Secrets (đã ignore)
├── .env.example
├── .github/
│   └── workflows/
│       └── ci.yml           # Chỉ có CI, không có deploy
└── _bmad/                   # BMAD project management
```

### 2.2 Backend Endpoints (Spring Boot)

| Controller | Base Path | Status |
|------------|-----------|--------|
| AuthController | `/api/v1/auth` | Partial (register only) |
| OcrController | `/api/ocr` | Implemented |
| ReferenceDataController | `/api/v1/reference-data` | Implemented |

**NOT IMPLEMENTED (planned):**
- `/api/v1/auth/login` - Login endpoint
- `/api/v1/auth/verify-email` - Email verification
- `/api/v1/auth/refresh-token` - Token refresh
- `/api/v1/profiles/*` - Profile management
- `/api/v1/health-records/*` - Health records CRUD
- `/api/v1/documents/upload` - Document upload

### 2.3 Frontend API Calls

**Current State:** 
- Base URL: `http://localhost:8080` (hardcoded fallback)
- Endpoint: `POST /api/v1/auth/register` (hardcoded)
- No centralized API constants

### 2.4 External Services

| Service | Provider | Purpose |
|---------|----------|---------|
| Database | Neon PostgreSQL | Primary database |
| Vector DB | Qdrant Cloud | Embeddings storage |
| AI/LLM | Groq API | Medical analysis |
| OCR | EasyOCR (self-hosted) | Text extraction |
| Storage | MinIO/S3 | File storage |

---

## 3. Docker Multi-Environment Setup

### 3.1 Current State

✅ **Có:**
- `docker-compose.dev.yml` cho local development
- Dockerfiles cho tất cả services (api, web, ocr-service)
- Redis integration

❌ **Thiếu:**
- `docker-compose.staging.yml`
- `docker-compose.prod.yml`
- `.env.staging`, `.env.production` templates
- GitHub Actions deploy workflows

### 3.2 Best Practices for Docker Multi-Environment

**Recommended Structure:**
```
docker/
├── docker-compose.base.yml       # Shared services (Redis, etc.)
├── docker-compose.dev.yml        # Local development
├── docker-compose.staging.yml    # Staging environment
└── docker-compose.prod.yml      # Production environment
```

**Compose Override Pattern:**
```bash
# Development
docker compose -f docker-compose.base.yml -f docker-compose.dev.yml up

# Staging
docker compose -f docker-compose.base.yml -f docker-compose.staging.yml up

# Production
docker compose -f docker-compose.base.yml -f docker-compose.prod.yml up
```

### 3.3 Docker Compose Base Template

```yaml
# docker/docker-compose.base.yml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    networks:
      - healthlens-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  healthlens-network:
    driver: bridge
```

### 3.4 Environment-Specific Configurations

**Development (`docker-compose.dev.yml`):**
```yaml
services:
  api:
    build:
      context: ../apps/api
      dockerfile: Dockerfile
    env_file:
      - ../.env
    ports:
      - "8080:8080"
    profiles:
      - development

  web:
    build:
      context: ../apps/web
      dockerfile: Dockerfile
    env_file:
      - ../.env
    ports:
      - "3000:3000"
    depends_on:
      - api

  ocr-service:
    build:
      context: ../services/ocr-service
      dockerfile: Dockerfile
    env_file:
      - ../.env
    ports:
      - "8001:8001"
    profiles:
      - with-ocr
```

**Staging (`docker-compose.staging.yml`):**
```yaml
services:
  api:
    environment:
      - SPRING_PROFILES_ACTIVE=staging
    deploy:
      replicas: 1
      resources:
        limits:
          memory: 1G
    restart: on-failure

  web:
    environment:
      - NEXT_PUBLIC_API_URL=https://api.staging.healthlens.com
    deploy:
      replicas: 2
```

**Production (`docker-compose.prod.yml`):**
```yaml
services:
  api:
    environment:
      - SPRING_PROFILES_ACTIVE=production
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 2G
      restart_policy:
        condition: on-failure
        delay: 10s
        max_attempts: 3
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose -q -O- http://localhost:8080/actuator/health/liveness"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

  web:
    environment:
      - NEXT_PUBLIC_API_URL=https://api.healthlens.com
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
```

### 3.5 Multi-Stage Dockerfile (Spring Boot)

```dockerfile
# apps/api/Dockerfile
FROM eclipse-temurin:21-jre-alpine AS base
WORKDIR /app

FROM base AS builder
COPY target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM base AS runtime
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

### 3.6 GitHub Actions Deploy Workflow

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  push:
    branches: [main, develop]
  pull_request:
    types: [closed]
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      
      - name: Build and push staging images
        if: github.ref == 'refs/heads/develop'
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: healthlens/api:staging,healthlens/web:staging
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-staging:
    name: Deploy to Staging
    runs-on: ubuntu-latest
    environment: staging
    needs: build
    if: github.ref == 'refs/heads/develop'
    steps:
      - name: Deploy to Staging Server
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.STAGING_HOST }}
          username: ${{ secrets.STAGING_USER }}
          key: ${{ secrets.STAGING_SSH_KEY }}
          script: |
            cd /app/health-lens
            docker compose -f docker/docker-compose.staging.yml pull
            docker compose -f docker/docker-compose.staging.yml up -d

  deploy-production:
    name: Deploy to Production
    runs-on: ubuntu-latest
    environment: production
    needs: deploy-staging
    if: github.event.pull_request.merged == true && github.base_ref == 'main'
    steps:
      - name: Deploy to Production
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /app/health-lens
            docker compose -f docker/docker-compose.prod.yml pull
            docker compose -f docker/docker-compose.prod.yml up -d
```

---

## 4. Secrets Management

### 4.1 Current Issues (CRITICAL)

**`apps/api/src/main/resources/application.yml`:**
```yaml
# ❌ NGUY HIỂM - Đã commit vào git!
spring:
  datasource:
    url: jdbc:postgresql://...
  ai:
    openai:
      api-key: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...  # Qdrant key!

qdrant:
  host: b189ed5b-51e0-4209-8703-12d4728ed745.us-east-2-0.aws.cloud.qdrant.io
  api-key: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...  # Đã bị expose!
```

### 4.2 Required Actions (IMMEDIATE)

1. **Rotate Qdrant API Key** - Tạo key mới tại https://cloud.qdrant.io
2. **Remove hardcoded secrets** từ `application.yml`
3. **Verify `.env` đã được gitignore**

### 4.3 Best Practices for Secrets Management

**Principle 1: Never Commit Secrets**
```bash
# .gitignore
.env
.env.*
!.env.example
secrets/
*.pem
```

**Principle 2: Use Environment Variables**
```yaml
# application.yml - ✅ ĐÚNG
spring:
  datasource:
    url: ${DATABASE_URL}
    password: ${DB_PASSWORD}
  ai:
    openai:
      api-key: ${GROQ_API_KEY}

qdrant:
  host: ${QDRANT_HOST}
  api-key: ${QDRANT_API_KEY}
```

**Principle 3: Fail Fast with Required Variables**
```yaml
# Use ${VAR:?error} syntax
spring:
  datasource:
    password: ${DB_PASSWORD:?DB_PASSWORD is required}
```

**Principle 4: Use *_FILE Pattern for Sensitive Data**
```yaml
# Docker/K8s secret mount
# spring.datasource.password được đọc từ file
spring:
  datasource:
    password-file: /run/secrets/db_password
```

### 4.4 Environment Templates

**`.env.example` (Committed to git):**
```bash
# API Base URLs
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080

# Database
DATABASE_URL=postgresql://user:password@host:5432/dbname

# AI Services
GROQ_API_KEY=                    # Required - Get from Groq Console
OPENROUTER_API_KEY=               # Optional

# Vector Database
QDRANT_HOST=your-qdrant-host.qdrant.io
QDRANT_API_KEY=                   # Required - Get from Qdrant Cloud

# OCR Service
OCR_SERVICE_URL=http://localhost:8001
OCR_TIMEOUT_MS=60000

# JWT Configuration
JWT_SECRET=                       # Required - Generate secure random string
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Object Storage (MinIO/S3)
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=healthlens

# Allowed Origins
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8081
```

**`.env.staging` (Not committed):**
```bash
# Override for staging environment
API_BASE_URL=https://api.staging.healthlens.com
NEXT_PUBLIC_API_BASE_URL=https://api.staging.healthlens.com
ALLOWED_ORIGINS=https://staging.healthlens.com
```

**`.env.production` (Not committed):**
```bash
# Override for production environment
API_BASE_URL=https://api.healthlens.com
NEXT_PUBLIC_API_BASE_URL=https://app.healthlens.com
ALLOWED_ORIGINS=https://healthlens.com,https://www.healthlens.com
```

### 4.5 Docker Secrets (Production)

```yaml
# docker-compose.prod.yml
services:
  api:
    secrets:
      - db_password
      - groq_api_key
      - qdrant_api_key
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DB_PASSWORD_FILE=/run/secrets/db_password
      - GROQ_API_KEY_FILE=/run/secrets/groq_api_key
      - QDRANT_API_KEY_FILE=/run/secrets/qdrant_api_key

secrets:
  db_password:
    file: ./secrets/db_password.txt
  groq_api_key:
    external: true
  qdrant_api_key:
    external: true
```

### 4.6 GitHub Environments Secrets

```yaml
# .github/workflows/deploy.yml
jobs:
  deploy-staging:
    environment: staging  # Requires secrets configured in GitHub Settings
    steps:
      - run: echo ${{ secrets.GROQ_API_KEY }}

  deploy-production:
    environment: production  # Requires manual approval + secrets
    steps:
      - run: echo ${{ secrets.GROQ_API_KEY }}
```

**Configure in GitHub:** Settings → Environments → staging/production → Add secrets

---

## 5. Project Structure & Constants Organization

### 5.1 Recommended Constants Structure

```
packages/shared/
├── constants/
│   ├── index.ts              # Barrel export
│   ├── api.ts               # API endpoints
│   ├── error-codes.ts       # ErrorCode enum
│   ├── status.ts            # Status enums
│   └── config.ts            # App-wide config
├── schemas/
│   ├── index.ts
│   ├── auth.ts              # Auth Zod schemas
│   ├── profile.ts
│   └── health-record.ts
├── config/
│   ├── index.ts
│   └── env.ts               # Environment variables
└── types/
    ├── index.ts
    └── ...
```

### 5.2 API Constants (`packages/shared/constants/api.ts`)

```typescript
// Resource naming convention (Google API Design Guide)
export const API_VERSION = 'v1';

export const ApiPaths = {
  AUTH: {
    BASE: `/api/${API_VERSION}/auth`,
    REGISTER: `/api/${API_VERSION}/auth/register`,
    LOGIN: `/api/${API_VERSION}/auth/login`,
    VERIFY_EMAIL: `/api/${API_VERSION}/auth/verify-email`,
    REFRESH: `/api/${API_VERSION}/auth/refresh`,
  },
  
  PROFILES: {
    BASE: `/api/${API_VERSION}/profiles`,
    LIST: `/api/${API_VERSION}/profiles`,
    GET: (id: string) => `/api/${API_VERSION}/profiles/${id}`,
    CREATE: `/api/${API_VERSION}/profiles`,
    UPDATE: (id: string) => `/api/${API_VERSION}/profiles/${id}`,
    DELETE: (id: string) => `/api/${API_VERSION}/profiles/${id}`,
  },
  
  HEALTH_RECORDS: {
    BASE: `/api/${API_VERSION}/health-records`,
    LIST: `/api/${API_VERSION}/health-records`,
    GET: (id: string) => `/api/${API_VERSION}/health-records/${id}`,
    CREATE: `/api/${API_VERSION}/health-records`,
    UPLOAD_IMAGE: (id: string) => `/api/${API_VERSION}/health-records/${id}/image`,
    ANALYZE: (id: string) => `/api/${API_VERSION}/health-records/${id}/analyze`,
  },
  
  REFERENCE_DATA: {
    BASE: `/api/${API_VERSION}/reference-data`,
    INDEX: (id: string) => `/api/${API_VERSION}/reference-data/${id}/index`,
    SEARCH: `/api/${API_VERSION}/reference-data/search`,
  },
  
  OCR: {
    BASE: `/api/ocr`,
    EXTRACT: `/api/ocr/extract`,
  },
} as const;

export const API_TIMEOUT = {
  DEFAULT: 30_000,
  OCR: 60_000,
  AI_ANALYSIS: 120_000,
} as const;
```

### 5.3 Error Codes (`packages/shared/constants/error-codes.ts`)

```typescript
export const ErrorCode = {
  // Authentication
  UNAUTHORIZED: 'UNAUTHORIZED',
  INVALID_CREDENTIALS: 'INVALID_CREDENTIALS',
  TOKEN_EXPIRED: 'TOKEN_EXPIRED',
  TOKEN_INVALID: 'TOKEN_INVALID',
  
  // Validation
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  INVALID_INPUT: 'INVALID_INPUT',
  
  // Resources
  NOT_FOUND: 'NOT_FOUND',
  ALREADY_EXISTS: 'ALREADY_EXISTS',
  RESOURCE_CONFLICT: 'RESOURCE_CONFLICT',
  
  // Server
  INTERNAL_ERROR: 'INTERNAL_ERROR',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
  AI_SERVICE_ERROR: 'AI_SERVICE_ERROR',
  OCR_SERVICE_ERROR: 'OCR_SERVICE_ERROR',
} as const;

export type ErrorCode = typeof ErrorCode[keyof typeof ErrorCode];
```

### 5.4 Status Enums (`packages/shared/constants/status.ts`)

```typescript
export const ProfileStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  PENDING: 'PENDING',
} as const;

export const UserStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  BANNED: 'BANNED',
  PENDING_VERIFICATION: 'PENDING_VERIFICATION',
} as const;

export const HealthRecordStatus = {
  DRAFT: 'DRAFT',
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
} as const;

export type ProfileStatus = typeof ProfileStatus[keyof typeof ProfileStatus];
export type UserStatus = typeof UserStatus[keyof typeof UserStatus];
export type HealthRecordStatus = typeof HealthRecordStatus[keyof typeof HealthRecordStatus];
```

### 5.5 Environment Config (`packages/shared/config/env.ts`)

```typescript
export const ENV = {
  API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080',
  OCR_SERVICE_URL: process.env.EXPO_PUBLIC_OCR_SERVICE_URL ?? 'http://localhost:8001',
} as const;

export const isDevelopment = process.env.NODE_ENV === 'development';
export const isProduction = process.env.NODE_ENV === 'production';
```

### 5.6 API Client Pattern

```typescript
// apps/web/src/lib/api/client.ts
import axios from 'axios';
import { ApiPaths, API_TIMEOUT } from '@healthlens/shared/constants';
import { ENV } from '@healthlens/shared/config';

export const apiClient = axios.create({
  baseURL: ENV.API_BASE_URL,
  timeout: API_TIMEOUT.DEFAULT,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for auth
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Handle token refresh or logout
    }
    return Promise.reject(error);
  }
);

// Usage
export const authApi = {
  register: (data: RegisterInput) => 
    apiClient.post(ApiPaths.AUTH.REGISTER, data),
  
  login: (data: LoginInput) => 
    apiClient.post(ApiPaths.AUTH.LOGIN, data),
};

export const profileApi = {
  getAll: () => apiClient.get(ApiPaths.PROFILES.LIST),
  getById: (id: string) => apiClient.get(ApiPaths.PROFILES.GET(id)),
  create: (data: CreateProfileInput) => 
    apiClient.post(ApiPaths.PROFILES.CREATE, data),
  update: (id: string, data: UpdateProfileInput) => 
    apiClient.put(ApiPaths.PROFILES.UPDATE(id), data),
  delete: (id: string) => 
    apiClient.delete(ApiPaths.PROFILES.DELETE(id)),
};
```

### 5.7 Spring Boot Constants Class

```java
// apps/api/src/main/java/com/healthlens/api/constants/ApiConstants.java
package com.healthlens.api.constants;

public final class ApiConstants {
    
    private ApiConstants() {}
    
    // Base paths
    public static final String API = "/api";
    public static final String API_V1 = "/api/v1";
    
    // Auth paths
    public static final String AUTH = API_V1 + "/auth";
    public static final String AUTH_REGISTER = AUTH + "/register";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_VERIFY_EMAIL = AUTH + "/verify-email";
    public static final String AUTH_REFRESH = AUTH + "/refresh";
    
    // Profile paths
    public static final String PROFILES = API_V1 + "/profiles";
    public static final String PROFILE_BY_ID = PROFILES + "/{id}";
    
    // Health Record paths
    public static final String HEALTH_RECORDS = API_V1 + "/health-records";
    public static final String HEALTH_RECORD_BY_ID = HEALTH_RECORDS + "/{id}";
    public static final String HEALTH_RECORD_IMAGE = HEALTH_RECORD_BY_ID + "/image";
    public static final String HEALTH_RECORD_ANALYZE = HEALTH_RECORD_BY_ID + "/analyze";
    
    // Reference Data paths
    public static final String REFERENCE_DATA = API_V1 + "/reference-data";
    public static final String REFERENCE_DATA_INDEX = REFERENCE_DATA + "/{id}/index";
    public static final String REFERENCE_DATA_SEARCH = REFERENCE_DATA + "/search";
    
    // OCR paths
    public static final String OCR = "/api/ocr";
    public static final String OCR_EXTRACT = OCR + "/extract";
}
```

**Controller Usage:**
```java
@RestController
@RequestMapping(ApiConstants.AUTH)
@RequiredArgsConstructor
public class AuthController {
    
    @PostMapping("/register")  // Equivalent to AUTH_REGISTER
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        // implementation
    }
}
```

---

## 6. Integration Patterns

### 6.1 System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Frontend (Next.js)                            │
│                         localhost:3000                               │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   API Client Layer                           │   │
│  │              (packages/shared/constants/api.ts)              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────────────┘
                              │ HTTP/REST
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Backend (Spring Boot)                          │
│                    localhost:8080 / api:8080                         │
│  ┌──────────────┬──────────────┬─────────────┬───────────────────┐   │
│  │  Auth API   │  Profiles   │Health Recs │   AI/OCR Service  │   │
│  │ Controller  │ Controller  │ Controller  │   Integration     │   │
│  └──────────────┴──────────────┴─────────────┴───────────────────┘   │
│                              │                                        │
│  ┌───────────────────────────┼───────────────────────────────────┐  │
│  │              Service Layer │                                   │  │
│  │  AuthService │ ProfileService │ HealthRecordService │ AiService │  │
│  └───────────────────────────┼───────────────────────────────────┘  │
│                              │                                        │
│  ┌───────────────────────────┼───────────────────────────────────┐  │
│  │            Repository Layer │                                 │  │
│  │  UserRepo │ ProfileRepo │ HealthRecordRepo │ ReferenceDataRepo │  │
│  └───────────────────────────┼───────────────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
   ┌──────────┐         ┌──────────┐          ┌──────────┐
   │  Neon    │         │ Qdrant   │          │   Groq   │
   │PostgreSQL│         │VectorDB  │          │   API    │
   └──────────┘         └──────────┘          └──────────┘
```

### 6.2 Docker Networking

**CRITICAL FIX REQUIRED:**

```yaml
# ❌ SAI - Đang dùng localhost trong container
environment:
  - OCR_SERVICE_URL=http://localhost:8001

# ✅ ĐÚNG - Dùng container name làm DNS
environment:
  - OCR_SERVICE_URL=http://ocr-service:8001
```

**Docker Network Configuration:**
```yaml
services:
  web:
    networks:
      - healthlens-network
  api:
    networks:
      - healthlens-network
    environment:
      - OCR_SERVICE_URL=http://ocr-service:8001
  ocr-service:
    networks:
      - healthlens-network
  redis:
    networks:
      - healthlens-network

networks:
  healthlens-network:
    driver: bridge
```

### 6.3 Service-to-Service Communication

**Spring Boot → OCR Service:**
```java
@Service
public class OcrService {
    
    @Value("${app.ocr.service.url}")
    private String ocrServiceUrl;
    
    public OcrResult extractText(String imageUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<OcrRequest> request = new HttpEntity<>(
            new OcrRequest(imageUrl), 
            headers
        );
        
        ResponseEntity<OcrResult> response = restTemplate.exchange(
            ocrServiceUrl + "/ocr/extract",
            HttpMethod.POST,
            request,
            OcrResult.class
        );
        
        return response.getBody();
    }
}
```

**Spring Boot → Groq AI:**
```java
@Configuration
public class GroqAiConfig {
    
    @Bean
    public ChatModel chatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url}") String baseUrl) {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();
    }
}

@Service
public class AiAnalysisService {
    
    private final ChatModel chatModel;
    
    public String analyze(String text) {
        Prompt prompt = new Prompt(
            "Analyze this medical text: " + text
        );
        return chatModel.call(prompt).getResult().getOutput().getContent();
    }
}
```

### 6.4 Resilience Patterns

**Circuit Breaker (Required for AI calls):**
```java
@Configuration
public class ResilienceConfig {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        Map<String, CircuitBreakerConfig> configs = new HashMap<>();
        
        configs.put("aiService", CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build());
        
        configs.put("ocrService", CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(5)
            .build());
        
        return new CircuitBreakerRegistry(configs);
    }
}

@Service
public class AiAnalysisService {
    
    private final CircuitBreaker aiCircuitBreaker;
    private final ChatModel chatModel;
    
    public AiAnalysisService(CircuitBreakerRegistry registry, ChatModel chatModel) {
        this.aiCircuitBreaker = registry.circuitBreaker("aiService");
        this.chatModel = chatModel;
    }
    
    public String analyze(String text) {
        return CircuitBreaker.decorateSupplier(aiCircuitBreaker, () -> {
            Prompt prompt = new Prompt("Analyze: " + text);
            return chatModel.call(prompt).getResult().getOutput().getContent();
        }).get();
    }
    
    // Fallback method
    public String analyzeFallback(String text, Exception e) {
        log.error("AI service failed: {}", e.getMessage());
        return "Analysis temporarily unavailable. Please try again later.";
    }
}
```

---

## 7. Architectural Patterns

### 7.1 Layered Architecture (Current)

```
┌─────────────────────────────────────┐
│         Presentation Layer           │  ← Controllers
│   @RestController @RequestMapping   │
├─────────────────────────────────────┤
│          Service Layer               │  ← Business Logic
│   @Service @Transactional           │
├─────────────────────────────────────┤
│        Repository Layer              │  ← Data Access
│   @Repository extends JpaRepository │
├─────────────────────────────────────┤
│           Data Layer                 │  ← Entities
│   @Entity @Table JPA                │
└─────────────────────────────────────┘
```

**Controller Example:**
```java
@RestController
@RequestMapping(ApiConstants.AUTH)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    @Operation(summary = "Register new user")
    @ApiResponse(responseCode = "201", description = "User registered")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "User registered successfully"));
    }
}
```

**Service Example:**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public AuthResponse register(RegisterRequest request) {
        // Validate
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS, "Email already registered");
        }
        
        // Create user
        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .status(UserStatus.PENDING_VERIFICATION)
            .build();
        
        user = userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        return AuthResponse.of(user, accessToken, refreshToken);
    }
}
```

### 7.2 Database Architecture

**Neon PostgreSQL - Schema-per-Service:**

```sql
-- Schema per domain
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS profile;
CREATE SCHEMA IF NOT EXISTS health;
CREATE SCHEMA IF NOT EXISTS reference_data;

-- Set search path per service
ALTER DATABASE healthlens SET search_path TO auth, profile, health, public;
```

**Qdrant Collections:**
```
healthlens/                    # Medical document embeddings
├── medical_terms             # Medical terminology
├── health_records            # User health record summaries
└── reference_data           # Reference medical data
```

### 7.3 Security Architecture

**Spring Security Configuration:**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/ocr/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                // Protected endpoints
                .anyRequest().authenticated()
            )
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "https://healthlens.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 7.4 Observability

**Health Check Endpoints:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
    db:
      enabled: true
    redis:
      enabled: true
```

**Structured Logging:**
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n"
  level:
    root: INFO
    com.healthlens: DEBUG
    org.springframework.security: DEBUG
```

---

## 8. Implementation Recommendations

### 8.1 Immediate Actions (Week 1)

#### 1. Rotate Qdrant API Key 🔴 CRITICAL
```bash
# 1. Go to https://cloud.qdrant.io
# 2. Generate new API key
# 3. Update .env file
# 4. Update GitHub Secrets (staging, production)
```

#### 2. Remove Hardcoded Secrets
```bash
# Remove from application.yml
git filter-branch --tree-filter 'rm -f apps/api/src/main/resources/application.yml' HEAD
```

#### 3. Create .env.example Template
```bash
# Generate from current .env (without actual values)
grep -v '^#\|^\s*#' .env | sed 's/=.*/=/' > .env.example
```

### 8.2 Short-term (Week 2-3)

#### 1. Create Constants Package
```
packages/shared/
├── constants/
│   ├── index.ts
│   ├── api.ts
│   ├── error-codes.ts
│   ├── status.ts
│   └── config.ts
├── config/
│   └── index.ts
└── package.json
```

#### 2. Setup Docker Staging/Production
```
docker/
├── docker-compose.base.yml
├── docker-compose.dev.yml (update)
├── docker-compose.staging.yml (new)
└── docker-compose.prod.yml (new)
```

#### 3. Add GitHub Actions Deploy Workflow
```
.github/workflows/
├── ci.yml (existing)
└── deploy.yml (new)
```

### 8.3 Medium-term (Week 4-6)

#### 1. Add Resilience Patterns
- Circuit Breaker for AI calls
- Retry logic for external services
- Fallback methods

#### 2. Implement Observability
- Distributed tracing (OpenTelemetry)
- Metrics (Micrometer + Prometheus)
- Structured logging

#### 3. Security Hardening
- Rate limiting
- Input validation
- SQL injection prevention

---

## 9. Priority Roadmap

### Phase 1: Security Fix (Week 1) 🔴 CRITICAL
| Task | Priority | Status |
|------|----------|--------|
| Rotate Qdrant API key | CRITICAL | ❌ |
| Remove secrets from git | CRITICAL | ❌ |
| Create .env.example | HIGH | ❌ |
| Verify .gitignore | HIGH | ❌ |

### Phase 2: Docker Setup (Week 2)
| Task | Priority | Status |
|------|----------|--------|
| Create docker-compose.base.yml | HIGH | ❌ |
| Create docker-compose.staging.yml | HIGH | ❌ |
| Create docker-compose.prod.yml | HIGH | ❌ |
| Update existing docker-compose.dev.yml | MEDIUM | ❌ |

### Phase 3: Constants Package (Week 2-3)
| Task | Priority | Status |
|------|----------|--------|
| Create packages/shared/constants/api.ts | HIGH | ❌ |
| Create packages/shared/constants/error-codes.ts | HIGH | ❌ |
| Create packages/shared/constants/status.ts | HIGH | ❌ |
| Create packages/shared/config/env.ts | MEDIUM | ❌ |
| Update frontend API client | HIGH | ❌ |
| Create ApiConstants.java for backend | MEDIUM | ❌ |

### Phase 4: CI/CD (Week 3-4)
| Task | Priority | Status |
|------|----------|--------|
| Create deploy.yml workflow | HIGH | ❌ |
| Configure GitHub Environments | HIGH | ❌ |
| Add staging secrets | HIGH | ❌ |
| Add production secrets | HIGH | ❌ |

### Phase 5: Resilience & Observability (Week 4-6)
| Task | Priority | Status |
|------|----------|--------|
| Add Circuit Breaker for AI | HIGH | ❌ |
| Add Circuit Breaker for OCR | MEDIUM | ❌ |
| Setup Prometheus metrics | MEDIUM | ❌ |
| Add distributed tracing | LOW | ❌ |

---

## Appendix A: File Checklist

### Files to Create
```
docker/
├── docker-compose.base.yml      # NEW
├── docker-compose.staging.yml   # NEW
└── docker-compose.prod.yml      # NEW

.github/workflows/
└── deploy.yml                   # NEW

packages/shared/
├── constants/
│   ├── api.ts                   # NEW
│   ├── error-codes.ts          # NEW
│   ├── status.ts                # NEW
│   └── config.ts               # NEW
├── config/
│   └── env.ts                   # NEW
└── package.json                # UPDATE

.env.example                     # NEW
.env.staging                     # NEW
.env.production                  # NEW

apps/api/src/main/java/com/healthlens/api/constants/
└── ApiConstants.java           # NEW
```

### Files to Modify
```
apps/api/src/main/resources/
├── application.yml              # REMOVE secrets
├── application-dev.yml          # UPDATE env refs
└── application-docker.yml       # UPDATE env refs

apps/web/src/lib/api/
├── client.ts                    # UPDATE with constants
└── index.ts                    # UPDATE exports

packages/shared/constants/index.ts  # UPDATE barrel export
packages/shared/package.json        # UPDATE exports
```

### Files to Delete
```
apps/api/src/main/resources/application.yml  # REPLACE with env-based
```

---

## Appendix B: Security Checklist

- [ ] Rotate all committed secrets
- [ ] Update .gitignore to exclude sensitive files
- [ ] Add secrets to GitHub Environments
- [ ] Enable 2FA on GitHub
- [ ] Review CORS configuration
- [ ] Add rate limiting
- [ ] Enable audit logging
- [ ] Setup security scanning (Dependabot)

---

## Appendix C: References

1. [Spring Boot Microservices Best Practices](https://mdsanwarhossain.me/blog-spring-boot-microservices.html)
2. [Docker Multi-Environment Deployment](https://medium.com/@matthitachi/how-to-deploy-docker-containers-automatically-using-github-actions-with-staging-production-d7b5b7d900ec)
3. [Docker Compose Networking](https://docs.docker.com/compose/networking/)
4. [Database per Service Pattern](https://microservices.io/patterns/data/database-per-service.html)
5. [Resource Names for TypeScript Monorepos](https://www.robinwieruch.de/typescript-monorepo-resource-names/)
6. [Turborepo Shared Types](https://www.magnumcode.com/blog/turborepo-shared-types-monorepo)
7. [Cloud-Native Architecture Patterns](https://medium.com/@reiqwan/cloud-native-architecture-patterns-for-2025-building-enterprise-systems-that-scale-7c465142aaa4)
8. [Spring Boot API Best Practices](https://dev.to/kamlesh_patil/spring-boot-project-structure-best-practices-used-in-production-4h85)
9. [REST API URL Structure Best Practices](https://dev.to/devcorner/rest-api-url-structure-best-practices-with-spring-boot-example-35od)
10. [API Gateway Patterns](https://mydaytodo.com/mastering-the-api-gateway-pattern-in-microservices-a-comprehensive-2025-guide/)

---

*Report generated: 2026-04-15*  
*Research Type: Technical Infrastructure & Security Restructuring*
