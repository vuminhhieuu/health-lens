---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
inputDocuments: ['prd.md']
workflowType: 'architecture'
project_name: 'health-lens'
user_name: 'ie303'
date: '2026-03-17'
lastStep: 8
status: 'complete'
completedAt: '2026-03-17'
---

# Tài Liệu Quyết Định Kiến Trúc — HealthLens

_Tài liệu này được xây dựng hợp tác qua từng bước khám phá. Các phần được bổ sung khi chúng ta làm việc qua từng quyết định kiến trúc._

## Phân Tích Bối Cảnh Dự Án

### Tổng Quan Yêu Cầu

**Yêu Cầu Chức Năng:**
38 FRs được tổ chức thành 6 nhóm chức năng:
- **Quản Lý Tài Khoản & Hồ Sơ (FR1-FR8):** Xác thực, đa hồ sơ, quy trình đồng ý, quyền xóa dữ liệu
- **Tải Lên & Xử Lý (FR9-FR17):** Pipeline OCR, fallback nhập tay, gắn thẻ nguồn dữ liệu
- **Hiển Thị & Giải Thích (FR18-FR23):** Diễn giải bằng LLM, ngưỡng theo ngữ cảnh
- **Lịch Sử & Theo Dõi (FR24-FR28):** Xem timeline, cache offline, đồng bộ tự động
- **Chia Sẻ Gia Đình (FR29-FR32):** Hệ thống mời, truy cập chỉ đọc, cập nhật real-time
- **Quản Trị (FR33-FR38):** CRUD dữ liệu tham chiếu, audit log, MFA, dashboard thống kê

**Yêu Cầu Phi Chức Năng:**
16 NFRs định hình kiến trúc:
- **Hiệu Năng:** OCR ≤10s, LLM ≤5s, tải trang ≤2s, cập nhật dashboard ≤30s
- **Bảo Mật:** AES-256 at rest, TLS 1.2+ in transit, MFA admin, audit log, session 30 phút
- **Khả Năng Mở Rộng:** 500 người dùng đồng thời, 50 job OCR song song, năng lực tăng 10x
- **Khả Năng Truy Cập:** WCAG 2.1 AA, font tối thiểu 16px, chỉ báo trạng thái không phụ thuộc màu
- **Độ Tin Cậy:** 99% uptime (6:00-22:00 ICT), OCR timeout 15s, LLM retry 3x

**Quy Mô & Độ Phức Tạp:**
- Miền chính: Full-stack (Web + Mobile + Backend + AI/ML)
- Mức độ phức tạp: CAO
- Số thành phần kiến trúc ước tính: ~15-20 thành phần chính

### Ràng Buộc Kỹ Thuật & Phụ Thuộc

1. **Stack Nền Tảng (Đã Xác Định):**
   - Backend: Java Spring Boot (REST API)
   - Web: Next.js (App Router, SSR/CSR hybrid)
   - Mobile: React Native (Expo)
   - Lưu trữ: S3-compatible object storage

2. **Phụ Thuộc Bên Ngoài:**
   - OCR API (dịch vụ ngoài)
   - LLM API (dịch vụ ngoài)
   - Dịch vụ push notification (FCM/APNs)

3. **Ràng Buộc Tuân Thủ:**
   - NĐ 13/2023/NĐ-CP (Bảo vệ dữ liệu cá nhân Việt Nam)
   - Hướng dẫn app sức khỏe App Store / Google Play

4. **Ràng Buộc Hiệu Năng:**
   - Pipeline OCR phải xử lý async với timeout fallback
   - Phản hồi LLM phải được cache để kiểm soát chi phí và độ trễ

### Các Mối Quan Tâm Xuyên Suốt Đã Xác Định

1. **Bảo Mật & Quyền Riêng Tư** — Mã hóa, audit logging, RBAC, quản lý đồng ý
2. **Xử Lý Lỗi & Khả Năng Phục Hồi** — OCR fallback, LLM retry, degradation offline
3. **Chiến Lược Cache** — LLM cache theo hash, cache dữ liệu tham chiếu, SQLite offline
4. **Đồng Bộ Dữ Liệu** — Mobile ↔ Backend ↔ Web đồng bộ real-time
5. **Khả Năng Quan Sát** — Audit logs, analytics, error tracking
6. **Thiết Kế API** — Kiến trúc API-first phục vụ cả web và mobile clients

## Đánh Giá Template Khởi Tạo

### Miền Công Nghệ Chính

**Ứng Dụng Healthcare Full-stack** với 3 nền tảng:
- Backend API (Java Spring Boot)
- Web Dashboard (Next.js)
- Mobile App (React Native/Expo)

### Các Tùy Chọn Starter Đã Đánh Giá

| Nền Tảng | Starter | Phiên Bản | Trạng Thái |
|----------|---------|-----------|------------|
| Backend | Spring Initializr | Spring Boot 4.0.3 | ✅ Đã Chọn |
| Web | create-next-app | Next.js 16.x | ✅ Đã Chọn |
| Mobile | create-expo-app | Expo SDK 55 | ✅ Đã Chọn |

### Starter Đã Chọn & Lệnh Khởi Tạo

#### Backend — Spring Boot 4.0.3

**Lý Do:** Spring Boot là tiêu chuẩn industry cho enterprise Java APIs, với tính năng bảo mật xuất sắc (Spring Security), hỗ trợ database (Spring Data JPA), và hệ sinh thái trưởng thành phù hợp cho healthcare applications.

**Lệnh Khởi Tạo:**

```bash
curl https://start.spring.io/starter.zip \
  -d type=gradle-project-kotlin \
  -d language=java \
  -d bootVersion=4.0.3 \
  -d javaVersion=21 \
  -d groupId=com.healthlens \
  -d artifactId=healthlens-api \
  -d name=healthlens-api \
  -d packageName=com.healthlens.api \
  -d dependencies=web,data-jpa,postgresql,security,oauth2-resource-server,validation,actuator,lombok,devtools,flyway,data-redis,mail \
  -o healthlens-api.zip
```

**Dependencies Bao Gồm:**

| Dependency | Mục Đích |
|------------|----------|
| `web` | REST API endpoints với Spring MVC |
| `data-jpa` | ORM với Hibernate cho PostgreSQL |
| `postgresql` | JDBC driver cho PostgreSQL |
| `security` | Spring Security cho xác thực/phân quyền |
| `oauth2-resource-server` | Xác thực JWT token |
| `validation` | Bean Validation (JSR-380) |
| `actuator` | Health checks, metrics endpoints |
| `lombok` | Giảm boilerplate code |
| `devtools` | Hot reload trong development |
| `flyway` | Database migrations với audit trail |
| `data-redis` | Tích hợp Redis cho caching |
| `mail` | Gửi email (reset mật khẩu, thông báo) |

#### Web — Next.js 16.x

**Lý Do:** Next.js App Router cung cấp SSR cho landing page (SEO), CSR cho dashboard (tương tác), với hỗ trợ TypeScript xuất sắc và tích hợp Tailwind CSS.

**Lệnh Khởi Tạo:**

```bash
npx create-next-app@latest healthlens-web \
  --typescript \
  --tailwind \
  --eslint \
  --app \
  --src-dir \
  --import-alias "@/*" \
  --turbopack
```

**Dependencies Bổ Sung:**

```bash
# State & Data fetching
pnpm add @tanstack/react-query axios zustand

# UI Components
pnpm add @radix-ui/themes lucide-react

# Forms & Validation
pnpm add react-hook-form zod @hookform/resolvers

# Xử lý ngày tháng
pnpm add date-fns

# Biểu đồ (P2 - hiển thị xu hướng)
pnpm add recharts
```

#### Mobile — Expo SDK 55

**Lý Do:** Expo đơn giản hóa phát triển React Native với managed workflow, dễ dàng truy cập camera/file, và cập nhật OTA. SDK 55 là phiên bản ổn định mới nhất với hiệu năng cải thiện.

**Lệnh Khởi Tạo:**

```bash
npx create-expo-app@latest healthlens-mobile --template default@sdk-55
```

**Dependencies Bổ Sung:**

```bash
# Core Expo modules
npx expo install expo-router expo-camera expo-document-picker expo-image-picker

# Storage & Offline
npx expo install expo-sqlite expo-secure-store

# Notifications
npx expo install expo-notifications

# Chia sẻ với Web
pnpm add @tanstack/react-query axios zustand react-hook-form zod @hookform/resolvers
```

### Quyết Định Kiến Trúc Được Thiết Lập Bởi Starters

#### Ngôn Ngữ & Runtime

| Nền Tảng | Ngôn Ngữ | Runtime |
|----------|----------|---------|
| Backend | Java 21 (LTS) | JVM với GraalVM-ready |
| Web | TypeScript 5.x | Node.js 20+ |
| Mobile | TypeScript 5.x | Hermes engine |

#### Build Tooling

| Nền Tảng | Build Tool | Dev Server |
|----------|------------|------------|
| Backend | Gradle Kotlin DSL | Spring Boot DevTools |
| Web | Turbopack | Next.js dev server |
| Mobile | Metro bundler | Expo dev server |

#### Giải Pháp Styling

- **Web:** Tailwind CSS 4.x với Radix UI primitives
- **Mobile:** React Native StyleSheet với Expo theming

#### Tổ Chức Code

**Cấu Trúc Monorepo (Khuyến Nghị):**

```
healthlens/
├── apps/
│   ├── api/              # Spring Boot backend
│   │   ├── src/main/java/com/healthlens/api/
│   │   │   ├── config/       # Security, Redis, v.v.
│   │   │   ├── controller/   # REST endpoints
│   │   │   ├── service/      # Business logic
│   │   │   ├── repository/   # Data access
│   │   │   ├── entity/       # JPA entities
│   │   │   ├── dto/          # Request/Response DTOs
│   │   │   └── exception/    # Custom exceptions
│   │   └── src/main/resources/
│   │       ├── db/migration/ # Flyway migrations
│   │       └── application.yml
│   ├── web/              # Next.js web app
│   │   └── src/
│   │       ├── app/          # App Router pages
│   │       ├── components/   # React components
│   │       ├── hooks/        # Custom hooks
│   │       ├── lib/          # Utilities, API client
│   │       └── stores/       # Zustand stores
│   └── mobile/           # Expo mobile app
│       ├── app/              # Expo Router screens
│       ├── components/       # React Native components
│       ├── hooks/            # Shared hooks
│       ├── lib/              # Utilities, API client
│       └── stores/           # Zustand stores
├── packages/
│   └── shared/           # Shared TypeScript code
│       ├── types/            # API types, entities
│       ├── schemas/          # Zod validation schemas
│       └── constants/        # Shared constants
├── docker/
│   ├── docker-compose.yml
│   └── docker-compose.dev.yml
└── docs/
```

#### Thành Phần Cơ Sở Hạ Tầng

| Thành Phần | Công Nghệ | Mục Đích |
|------------|-----------|----------|
| Database | PostgreSQL 16 | Kho dữ liệu chính (JSONB cho health records) |
| Cache | Redis 7 | LLM response cache, session store, job queue |
| Object Storage | MinIO (dev) / S3 (prod) | Lưu trữ file PDF/ảnh |
| Message Queue | Redis Streams | Xử lý job OCR async |

#### Tích Hợp Dịch Vụ Bên Ngoài

| Dịch Vụ | Tùy Chọn Provider | Mục Đích |
|---------|-------------------|----------|
| OCR | Google Vision API, AWS Textract | Trích xuất văn bản từ tài liệu |
| LLM | OpenAI GPT-4, Anthropic Claude | Giải thích sức khỏe bằng tiếng Việt |
| Push Notifications | FCM (Android), APNs (iOS) | Thông báo mobile |
| Email | SendGrid, AWS SES | Email giao dịch |

### Trải Nghiệm Phát Triển

| Tính Năng | Backend | Web | Mobile |
|-----------|---------|-----|--------|
| Hot Reload | DevTools | Turbopack | Metro Fast Refresh |
| Type Safety | Lombok + Records | TypeScript strict | TypeScript strict |
| Linting | Checkstyle | ESLint | ESLint |
| Formatting | Spotless | Prettier | Prettier |
| Testing | JUnit 5 + Mockito | Vitest + Playwright | Jest + Detox |

**Lưu ý:** Khởi tạo dự án sử dụng các lệnh này nên là các stories triển khai đầu tiên trong Epic 0 (Thiết Lập Dự Án).

## Quyết Định Kiến Trúc Cốt Lõi

### Phân Tích Ưu Tiên Quyết Định

**Quyết Định Quan Trọng (Chặn Triển Khai):**
- Chiến lược xác thực (JWT)
- Cách tiếp cận schema database (Hybrid normalized + JSONB)
- Mẫu thiết kế API (REST + OpenAPI)
- Chiến lược upload file (Pre-signed URLs)

**Quyết Định Quan Trọng (Định Hình Kiến Trúc):**
- Mẫu phân quyền (RBAC + resource ownership)
- Quản lý state (TanStack Query + Zustand)
- Chiến lược offline (Read-only + sync queue)
- CI/CD pipeline (GitHub Actions)

**Quyết Định Hoãn Lại (Sau MVP):**
- Giám sát nâng cao (Prometheus/Grafana) — hoãn đến P2
- WebSocket real-time — polling chấp nhận được cho MVP
- Chi tiết rate limiting — triển khai khi cần

### Kiến Trúc Dữ Liệu

#### Cách Tiếp Cận Mô Hình Dữ Liệu

**Quyết Định:** Entity-centric với JPA Entities
**Lý Do:** Quy mô dự án phù hợp, team quen thuộc, có thể tiến hóa sang DDD sau nếu cần
**Ảnh Hưởng:** Tất cả backend entities, repository layer

#### Chiến Lược Lưu Trữ Health Record

**Quyết Định:** Hybrid (normalized + JSONB)
**Lý Do:** 
- Các trường cốt lõi (user_id, profile_id, exam_date, created_at) normalized cho indexing
- Health metrics lưu trong cột JSONB cho linh hoạt với nhiều loại chỉ số
- Tận dụng PostgreSQL JSONB indexing và query capabilities

**Ví Dụ Schema:**
```sql
CREATE TABLE health_records (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id),
    exam_date DATE NOT NULL,
    source_type VARCHAR(20) NOT NULL, -- 'ocr', 'manual', 'mixed'
    metrics JSONB NOT NULL, -- flexible health metrics
    raw_ocr_result JSONB, -- original OCR output for audit
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_health_records_metrics ON health_records USING GIN (metrics);
```

#### Chiến Lược Audit Logging

**Quyết Định:** Application-level với Spring AOP
**Lý Do:** Dễ triển khai, đủ cho tuân thủ NĐ 13/2023, capture được full context
**Triển Khai:**
- Custom `@Auditable` annotation
- AOP interceptor logs vào bảng `audit_logs`
- Các trường: user_id, action, resource_type, resource_id, old_value, new_value, ip_address, timestamp

### Xác Thực & Bảo Mật

#### Chiến Lược Xác Thực

**Quyết Định:** JWT với Access + Refresh tokens
**Phiên Bản:** Spring Security 6.x + jjwt 0.12.x
**Cấu Hình:**
- Access token: hết hạn 15 phút
- Refresh token: hết hạn 7 ngày, lưu trong HttpOnly cookie
- Refresh token rotation mỗi lần refresh (ngăn replay attacks)
- Token blacklist trong Redis cho logout

**Lý Do:** Stateless, scalable, Spring Security OAuth2 Resource Server đã có trong starter

#### Mẫu Phân Quyền

**Quyết Định:** RBAC với kiểm tra ownership cấp resource
**Roles:**
- `ROLE_USER` — Người dùng tiêu chuẩn
- `ROLE_ADMIN` — Quản trị viên hệ thống

**Resource Ownership:**
- Người dùng chỉ có thể truy cập profiles và health records của chính họ
- Chia sẻ gia đình: bảng `profile_shares` rõ ràng với permissions
- Admin chỉ có thể truy cập quản lý dữ liệu tham chiếu

#### Triển Khai MFA Admin

**Quyết Định:** TOTP (Time-based One-Time Password)
**Lý Do:** An toàn hơn SMS, không tốn chi phí, tiêu chuẩn industry
**Triển Khai:**
- Tương thích Google Authenticator / Authy
- Backup codes cho khôi phục
- MFA bắt buộc cho tất cả admin actions (NFR-S4)

#### Mã Hóa Dữ Liệu

**Quyết Định:** AES-256 at rest, TLS 1.2+ in transit (theo NFR-S1)
**Triển Khai:**
- PostgreSQL: extension `pgcrypto` cho sensitive fields (nếu cần field-level encryption)
- Redis: TLS enabled
- S3/MinIO: Server-side encryption enabled
- Tất cả API traffic qua HTTPS only

### Mẫu API & Giao Tiếp

#### Mẫu Thiết Kế API

**Quyết Định:** REST với OpenAPI 3.1
**Phiên Bản:** springdoc-openapi 2.x
**Lý Do:** Spring Boot hỗ trợ REST xuất sắc, OpenAPI spec generate client SDKs
**Triển Khai:**
- Base URL: `/api/v1/`
- OpenAPI spec auto-generated tại `/api/docs`
- Swagger UI tại `/api/swagger-ui`

#### Chiến Lược Versioning API

**Quyết Định:** URL versioning (`/api/v1/...`)
**Lý Do:** Rõ ràng, explicit, dễ test, mobile app có thể pin version

#### Định Dạng Error Response

**Quyết Định:** RFC 7807 Problem Details
**Triển Khai:**
```json
{
  "type": "https://healthlens.vn/errors/validation",
  "title": "Lỗi Validation",
  "status": 400,
  "detail": "Email format không hợp lệ",
  "instance": "/api/v1/auth/register",
  "timestamp": "2026-03-17T10:30:00Z",
  "errors": [
    {"field": "email", "message": "Email format không hợp lệ"}
  ]
}
```

#### Chiến Lược Upload File

**Quyết Định:** Pre-signed URLs với S3/MinIO
**Lý Do:** Giảm load backend, tốt hơn cho mobile (resume uploads)
**Quy Trình:**
1. Client yêu cầu pre-signed URL từ backend
2. Backend tạo URL với hết hạn 15 phút
3. Client upload trực tiếp đến S3/MinIO
4. Client thông báo backend hoàn thành
5. Backend kích hoạt pipeline OCR

### Kiến Trúc Frontend

#### Quản Lý State

**Quyết Định:** TanStack Query v5 + Zustand v4
**Lý Do:** 
- TanStack Query: server state, caching, background refetch, optimistic updates
- Zustand: client-only state (UI state, form drafts)
- Nhẹ, hỗ trợ TypeScript xuất sắc

**Phân Tách:**
- Server state (API data): TanStack Query
- Client state (UI, local preferences): Zustand
- Form state: React Hook Form (không cần global state)

#### Xử Lý Form

**Quyết Định:** React Hook Form v7 + Zod v3
**Lý Do:** Hiệu năng xuất sắc, Zod schemas chia sẻ với backend validation
**Triển Khai:**
- Zod schemas trong `packages/shared/schemas/`
- Type inference từ schemas
- Validation messages bằng tiếng Việt

#### Chiến Lược Offline Mobile

**Quyết Định:** Read-only offline với sync queue
**Lý Do:** Theo đặc tả PRD (NFR-I4)
**Triển Khai:**
- Expo SQLite: cache profiles, health records
- Sync queue: pending actions lưu local
- Auto-sync khi online
- Giải quyết xung đột: last-write-wins (profiles), append-only (health records)
- UI: offline indicator + thời gian sync cuối

### Cơ Sở Hạ Tầng & Triển Khai

#### Chiến Lược Container

**Quyết Định:** Docker Compose (dev) + Kubernetes-ready (prod)
**Lý Do:** Đơn giản cho local dev, khả năng mở rộng production cho 500 người dùng đồng thời
**Triển Khai:**
- `docker-compose.dev.yml`: PostgreSQL, Redis, MinIO, API
- Kubernetes manifests trong folder `/k8s/`
- Horizontal Pod Autoscaler cho API pods

#### CI/CD Pipeline

**Quyết Định:** GitHub Actions
**Lý Do:** Tích hợp với GitHub (nơi host source code), có sẵn cho public repositories, dễ cấu hình
**Cấu Hình:** `.github/workflows/`

**Pipeline Stages:**
```yaml
stages:
  - test
  - build
  - deploy

# Backend
api:test:
  stage: test
  script:
    - ./gradlew test

api:build:
  stage: build
  script:
    - ./gradlew bootBuildImage
    - docker push $CI_REGISTRY_IMAGE/api:$CI_COMMIT_SHA

# Web
web:test:
  stage: test
  script:
    - pnpm test

web:build:
  stage: build
  script:
    - pnpm build
    - docker build -t $CI_REGISTRY_IMAGE/web:$CI_COMMIT_SHA .

# Deploy
deploy:staging:
  stage: deploy
  script:
    - kubectl apply -f k8s/staging/
  environment: staging
  when: manual

deploy:production:
  stage: deploy
  script:
    - kubectl apply -f k8s/production/
  environment: production
  when: manual
  only:
    - main
```

#### Cấu Hình Môi Trường

**Quyết Định:** Mẫu 12-Factor App
**Triển Khai:**
- Environment variables cho tất cả config
- `.env.example` trong repo (không có secrets)
- GitHub Actions Secrets for environment variables
- Spring profiles: `dev`, `staging`, `prod`

#### Giám Sát & Quan Sát (MVP)

**Quyết Định:** Tối thiểu cho MVP, nâng cấp trong P2
**Triển Khai MVP:**
- Spring Actuator: `/actuator/health`, `/actuator/metrics`
- Structured JSON logging (Logback)
- Application-level audit logs trong database

**Đường Nâng Cấp P2:**
- Prometheus + Grafana cho metrics
- Loki cho log aggregation
- Jaeger cho distributed tracing

### Phân Tích Tác Động Quyết Định

#### Thứ Tự Triển Khai

1. **Thiết Lập Dự Án** — Monorepo, Docker Compose, GitHub Actions skeleton
2. **Schema Database** — Flyway migrations, core entities
3. **Xác Thực** — JWT, refresh tokens, Spring Security
4. **API Cốt Lõi** — User, Profile, Health Record CRUD
5. **Upload File** — Tích hợp S3, pre-signed URLs
6. **Pipeline OCR** — Xử lý async, Redis queue
7. **Tích Hợp LLM** — Tạo giải thích, caching
8. **Web Dashboard** — Next.js, TanStack Query
9. **Mobile App** — Expo, offline sync
10. **Admin Panel** — Dữ liệu tham chiếu, MFA

#### Phụ Thuộc Xuyên Thành Phần

```
┌─────────────────┐     ┌─────────────────┐
│   Mobile App    │────▶│                 │
└─────────────────┘     │                 │
                        │   Spring Boot   │────▶ PostgreSQL
┌─────────────────┐     │      API        │────▶ Redis
│   Web Dashboard │────▶│                 │────▶ S3/MinIO
└─────────────────┘     │                 │
                        └────────┬────────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
              ┌─────▼─────┐           ┌───────▼───────┐
              │  OCR API  │           │   LLM API     │
              │ (Ngoài)   │           │   (Ngoài)     │
              └───────────┘           └───────────────┘
```

**Phụ Thuộc Chia Sẻ:**
- `packages/shared/types/` — API types dùng bởi Web, Mobile, và backend DTOs
- `packages/shared/schemas/` — Zod schemas dùng bởi Web, Mobile forms
- `packages/shared/constants/` — Error codes, status enums

## Mẫu Triển Khai & Quy Tắc Nhất Quán

### Các Loại Mẫu Đã Định Nghĩa

**Điểm Xung Đột Quan Trọng Đã Xác Định:** 15+ khu vực nơi AI agents có thể đưa ra lựa chọn khác nhau, giờ đã được chuẩn hóa.

### Mẫu Đặt Tên

#### Quy Ước Đặt Tên Database (PostgreSQL)

| Phần Tử | Quy Ước | Ví Dụ |
|---------|---------|-------|
| Bảng | `snake_case`, số nhiều | `users`, `health_records`, `audit_logs` |
| Cột | `snake_case` | `user_id`, `created_at`, `exam_date` |
| Primary Keys | `id` (UUID) | `id UUID PRIMARY KEY` |
| Foreign Keys | `{bảng_số_ít}_id` | `user_id`, `profile_id` |
| Indexes | `idx_{bảng}_{cột}` | `idx_users_email`, `idx_health_records_profile_id` |
| Constraints | `{bảng}_{loại}_{cột}` | `users_uk_email`, `profiles_fk_user_id` |

#### Quy Ước Đặt Tên API (REST)

| Phần Tử | Quy Ước | Ví Dụ |
|---------|---------|-------|
| Endpoints | số nhiều, kebab-case | `/api/v1/health-records`, `/api/v1/users` |
| Path params | camelCase | `/api/v1/profiles/{profileId}` |
| Query params | camelCase | `?sortBy=createdAt&limit=10` |
| JSON fields | camelCase | `{ "userId": "...", "createdAt": "..." }` |
| Custom headers | `X-{Tên}` | `X-Request-Id`, `X-Correlation-Id` |

#### Quy Ước Đặt Tên Code

**Java Backend:**

| Phần Tử | Quy Ước | Ví Dụ |
|---------|---------|-------|
| Classes | PascalCase | `HealthRecordService`, `UserController` |
| Methods | camelCase, động từ đầu | `findByProfileId()`, `createHealthRecord()` |
| Variables | camelCase | `userId`, `healthRecord` |
| Constants | SCREAMING_SNAKE | `MAX_UPLOAD_SIZE`, `JWT_EXPIRY_MINUTES` |
| Packages | lowercase | `com.healthlens.api.service` |

**TypeScript Frontend:**

| Phần Tử | Quy Ước | Ví Dụ |
|---------|---------|-------|
| Components | PascalCase | `HealthRecordCard`, `ProfileList` |
| Files (components) | PascalCase.tsx | `HealthRecordCard.tsx` |
| Files (utils) | camelCase.ts | `apiClient.ts`, `formatDate.ts` |
| Hooks | camelCase, tiền tố use | `useHealthRecords`, `useProfile` |
| Types/Interfaces | PascalCase | `HealthRecord`, `User` |
| Zustand stores | camelCase + Store | `useAuthStore`, `useProfileStore` |

### Mẫu Cấu Trúc

#### Cấu Trúc Backend (Spring Boot)

```
apps/api/src/main/java/com/healthlens/api/
├── config/                    # Các class cấu hình
│   ├── SecurityConfig.java
│   ├── RedisConfig.java
│   └── OpenApiConfig.java
├── controller/                # REST controllers (mỏng)
│   ├── AuthController.java
│   ├── ProfileController.java
│   └── HealthRecordController.java
├── service/                   # Business logic
│   ├── AuthService.java
│   ├── ProfileService.java
│   └── OcrService.java
├── repository/                # Data access (JPA repositories)
│   ├── UserRepository.java
│   └── HealthRecordRepository.java
├── entity/                    # JPA entities
│   ├── User.java
│   ├── Profile.java
│   └── HealthRecord.java
├── dto/                       # Request/Response DTOs
│   ├── request/
│   │   └── CreateProfileRequest.java
│   └── response/
│       └── ProfileResponse.java
├── exception/                 # Custom exceptions + handlers
│   ├── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
├── mapper/                    # Entity ↔ DTO mappers
│   └── ProfileMapper.java
└── util/                      # Utilities
    └── JwtUtil.java
```

#### Cấu Trúc Frontend (Next.js Web)

```
apps/web/src/
├── app/                       # App Router pages
│   ├── (auth)/               # Nhóm auth (login, register)
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── (dashboard)/          # Nhóm dashboard được bảo vệ
│   │   ├── layout.tsx        # Dashboard layout với sidebar
│   │   ├── page.tsx          # Dashboard home
│   │   └── profiles/
│   │       ├── page.tsx
│   │       └── [id]/page.tsx
│   └── layout.tsx            # Root layout
├── components/
│   ├── ui/                   # UI components chung
│   │   ├── Button.tsx
│   │   └── Input.tsx
│   ├── features/             # Components theo tính năng
│   │   ├── auth/
│   │   ├── profiles/
│   │   └── health-records/
│   └── layouts/              # Layout components
│       └── DashboardLayout.tsx
├── hooks/                    # Custom hooks
│   ├── useAuth.ts
│   └── useHealthRecords.ts
├── lib/                      # Utilities
│   ├── api/                  # API client
│   │   ├── client.ts
│   │   └── endpoints.ts
│   ├── utils/
│   └── constants.ts
├── stores/                   # Zustand stores
│   └── authStore.ts
└── types/                    # Local types (mở rộng shared)
```

#### Quy Ước Vị Trí Test

| Nền Tảng | Quy Ước | Ví Dụ |
|----------|---------|-------|
| Backend | `src/test/java/...` phản chiếu main | `src/test/java/.../service/ProfileServiceTest.java` |
| Web | Co-located `*.test.tsx` | `components/ProfileCard.test.tsx` |
| Mobile | Co-located `*.test.tsx` | `components/ProfileCard.test.tsx` |
| E2E | Folder `/e2e/` | `e2e/auth.spec.ts` |

### Mẫu Định Dạng

#### Định Dạng API Response

**Success Response:**
```json
{
  "data": { ... },
  "meta": {
    "timestamp": "2026-03-17T10:30:00Z",
    "requestId": "uuid"
  }
}
```

**Paginated Response:**
```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 100,
    "totalPages": 5
  },
  "meta": { ... }
}
```

**Error Response (RFC 7807):**
```json
{
  "type": "https://healthlens.vn/errors/validation",
  "title": "Lỗi Validation",
  "status": 400,
  "detail": "Dữ liệu không hợp lệ",
  "instance": "/api/v1/profiles",
  "timestamp": "2026-03-17T10:30:00Z",
  "errors": [
    { "field": "email", "message": "Email không hợp lệ" }
  ]
}
```

#### Tiêu Chuẩn Định Dạng Ngày/Giờ

| Ngữ Cảnh | Định Dạng | Ví Dụ |
|----------|-----------|-------|
| API (JSON) | ISO 8601 UTC | `"2026-03-17T10:30:00Z"` |
| Database | TIMESTAMP WITH TIME ZONE | `2026-03-17 10:30:00+07` |
| Hiển Thị (VN) | `dd/MM/yyyy HH:mm` | `17/03/2026 17:30` |
| Chỉ ngày | `yyyy-MM-dd` | `2026-03-17` |

#### Sử Dụng HTTP Status Code

| Tình Huống | Status Code |
|------------|-------------|
| Thành công (GET) | 200 OK |
| Đã tạo (POST) | 201 Created |
| Không có nội dung (DELETE) | 204 No Content |
| Request không hợp lệ | 400 Bad Request |
| Chưa xác thực | 401 Unauthorized |
| Bị cấm | 403 Forbidden |
| Không tìm thấy | 404 Not Found |
| Xung đột | 409 Conflict |
| Lỗi validation | 422 Unprocessable Entity |
| Lỗi server | 500 Internal Server Error |

### Mẫu Giao Tiếp

#### Đặt Tên Event (Redis Streams)

```
# Định dạng: {domain}.{entity}.{action}
health-record.created
health-record.ocr-completed
health-record.ocr-failed
profile.shared
user.deleted
```

#### Cấu Trúc Event Payload

```json
{
  "eventId": "uuid",
  "eventType": "health-record.created",
  "timestamp": "2026-03-17T10:30:00Z",
  "payload": {
    "healthRecordId": "uuid",
    "profileId": "uuid",
    "userId": "uuid"
  },
  "metadata": {
    "correlationId": "uuid",
    "source": "api"
  }
}
```

#### Mẫu Quản Lý State (Zustand)

```typescript
// Đặt tên Store: use{Domain}Store
// Actions: động từ + danh từ
// Selectors: get + danh từ

interface AuthStore {
  // State
  user: User | null;
  isAuthenticated: boolean;
  
  // Actions
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
}
```

### Mẫu Quy Trình

#### Mẫu Xử Lý Lỗi

**Backend (Spring Boot):**
```java
// GlobalExceptionHandler bắt tất cả exceptions
// Custom exceptions extend base ApiException
// Luôn trả về định dạng RFC 7807

@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND, 
        ex.getMessage()
    );
    problem.setType(URI.create("https://healthlens.vn/errors/not-found"));
    return ResponseEntity.status(404).body(problem);
}
```

**Frontend (React):**
```typescript
// Xử lý lỗi TanStack Query
const { data, error, isError } = useQuery({
  queryKey: ['profile', profileId],
  queryFn: () => api.getProfile(profileId),
});

// Error boundary cho lỗi không mong đợi
// Toast notifications cho lỗi hiển thị người dùng
```

#### Mẫu Loading State

```typescript
// TanStack Query cung cấp: isLoading, isFetching, isRefetching
// Dùng isLoading cho load ban đầu
// Dùng isFetching cho background updates

const { isLoading, isFetching } = useHealthRecords();

// Mẫu UI: Skeleton cho ban đầu, indicator tinh tế cho refetch
{isLoading ? <Skeleton /> : <Content />}
{isFetching && <RefreshIndicator />}
```

#### Mẫu Validation

```typescript
// Zod schemas trong packages/shared/schemas/
// Validate ở frontend TRƯỚC KHI submit
// Validate ở backend LUÔN LUÔN (không bao giờ tin client)

export const createProfileSchema = z.object({
  name: z.string().min(1, "Tên không được để trống").max(100),
  dateOfBirth: z.string().date("Ngày sinh không hợp lệ"),
  gender: z.enum(["male", "female", "other"]),
});

export type CreateProfileRequest = z.infer<typeof createProfileSchema>;
```

### Hướng Dẫn Thực Thi

#### Tất Cả AI Agents PHẢI:

1. **Tuân theo quy ước đặt tên chính xác** — không ngoại lệ cho database, API, hoặc code
2. **Dùng shared types từ `packages/shared/`** — không bao giờ duplicate định nghĩa type
3. **Trả về lỗi RFC 7807** — không bao giờ custom error formats
4. **Dùng ISO 8601 cho tất cả ngày trong API** — định dạng hiển thị chỉ ở UI layer
5. **Đặt tests co-located với source** — trừ E2E tests trong `/e2e/`
6. **Dùng TanStack Query cho server state** — không bao giờ useState cho API data
7. **Validate với Zod schemas** — cả frontend submission và backend receipt

#### Anti-Patterns (KHÔNG BAO GIỜ LÀM)

```typescript
// ❌ SAI: snake_case trong TypeScript
const user_id = response.user_id;

// ✅ ĐÚNG: camelCase trong TypeScript
const userId = response.userId;

// ❌ SAI: Custom error format
return { error: "Không tìm thấy", code: 404 };

// ✅ ĐÚNG: RFC 7807
return { type: "...", title: "...", status: 404, detail: "..." };

// ❌ SAI: useState cho server data
const [profiles, setProfiles] = useState([]);
useEffect(() => fetchProfiles().then(setProfiles), []);

// ✅ ĐÚNG: TanStack Query
const { data: profiles } = useQuery({ 
  queryKey: ['profiles'], 
  queryFn: fetchProfiles 
});

// ❌ SAI: Duplicate định nghĩa type qua các apps
// Trong web/types/profile.ts VÀ mobile/types/profile.ts

// ✅ ĐÚNG: Single source of truth
// Trong packages/shared/types/profile.ts, imported bởi cả hai apps
```

## Cấu Trúc Dự Án & Ranh Giới

### Cấu Trúc Thư Mục Dự Án Hoàn Chỉnh

```
healthlens/
├── README.md
├── package.json                    # Package root monorepo
├── pnpm-workspace.yaml             # Cấu hình pnpm workspace
├── turbo.json                      # Cấu hình Turborepo (tùy chọn)
├── .gitignore
├── .github/workflows/                  # Pipeline GitHub Actions/CD
├── .env.example                    # Template môi trường
├── docker-compose.yml              # Compose production
├── docker-compose.dev.yml          # Compose development
│
├── apps/
│   ├── api/                        # Spring Boot Backend
│   │   ├── build.gradle.kts
│   │   ├── settings.gradle.kts
│   │   ├── gradle/
│   │   │   └── wrapper/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/healthlens/api/
│   │   │   │   │   ├── HealthLensApiApplication.java
│   │   │   │   │   ├── config/
│   │   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   │   ├── RedisConfig.java
│   │   │   │   │   │   ├── S3Config.java
│   │   │   │   │   │   ├── OpenApiConfig.java
│   │   │   │   │   │   └── JwtConfig.java
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── AuthController.java
│   │   │   │   │   │   ├── UserController.java
│   │   │   │   │   │   ├── ProfileController.java
│   │   │   │   │   │   ├── HealthRecordController.java
│   │   │   │   │   │   ├── UploadController.java
│   │   │   │   │   │   ├── FamilySharingController.java
│   │   │   │   │   │   └── admin/
│   │   │   │   │   │       ├── AdminReferenceDataController.java
│   │   │   │   │   │       ├── AdminUserController.java
│   │   │   │   │   │       └── AdminAuditController.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   ├── AuthService.java
│   │   │   │   │   │   ├── UserService.java
│   │   │   │   │   │   ├── ProfileService.java
│   │   │   │   │   │   ├── HealthRecordService.java
│   │   │   │   │   │   ├── OcrService.java
│   │   │   │   │   │   ├── LlmService.java
│   │   │   │   │   │   ├── StorageService.java
│   │   │   │   │   │   ├── FamilySharingService.java
│   │   │   │   │   │   ├── ReferenceDataService.java
│   │   │   │   │   │   ├── AuditService.java
│   │   │   │   │   │   └── NotificationService.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   │   ├── ProfileRepository.java
│   │   │   │   │   │   ├── HealthRecordRepository.java
│   │   │   │   │   │   ├── ProfileShareRepository.java
│   │   │   │   │   │   ├── ReferenceDataRepository.java
│   │   │   │   │   │   ├── AuditLogRepository.java
│   │   │   │   │   │   └── RefreshTokenRepository.java
│   │   │   │   │   ├── entity/
│   │   │   │   │   │   ├── User.java
│   │   │   │   │   │   ├── Profile.java
│   │   │   │   │   │   ├── HealthRecord.java
│   │   │   │   │   │   ├── ProfileShare.java
│   │   │   │   │   │   ├── ReferenceData.java
│   │   │   │   │   │   ├── AuditLog.java
│   │   │   │   │   │   ├── RefreshToken.java
│   │   │   │   │   │   └── UserConsent.java
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── request/
│   │   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   │   │   ├── CreateProfileRequest.java
│   │   │   │   │   │   │   ├── CreateHealthRecordRequest.java
│   │   │   │   │   │   │   ├── ManualMetricInput.java
│   │   │   │   │   │   │   └── ShareProfileRequest.java
│   │   │   │   │   │   └── response/
│   │   │   │   │   │       ├── AuthResponse.java
│   │   │   │   │   │       ├── UserResponse.java
│   │   │   │   │   │       ├── ProfileResponse.java
│   │   │   │   │   │       ├── HealthRecordResponse.java
│   │   │   │   │   │       ├── MetricExplanationResponse.java
│   │   │   │   │   │       ├── UploadUrlResponse.java
│   │   │   │   │   │       └── PaginatedResponse.java
│   │   │   │   │   ├── exception/
│   │   │   │   │   │   ├── ApiException.java
│   │   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   │   ├── UnauthorizedException.java
│   │   │   │   │   │   ├── ForbiddenException.java
│   │   │   │   │   │   ├── ValidationException.java
│   │   │   │   │   │   ├── OcrProcessingException.java
│   │   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   │   ├── mapper/
│   │   │   │   │   │   ├── UserMapper.java
│   │   │   │   │   │   ├── ProfileMapper.java
│   │   │   │   │   │   └── HealthRecordMapper.java
│   │   │   │   │   ├── security/
│   │   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   │   │   └── TotpService.java
│   │   │   │   │   ├── aspect/
│   │   │   │   │   │   └── AuditAspect.java
│   │   │   │   │   ├── annotation/
│   │   │   │   │   │   └── Auditable.java
│   │   │   │   │   ├── job/
│   │   │   │   │   │   ├── OcrProcessingJob.java
│   │   │   │   │   │   └── DataRetentionJob.java
│   │   │   │   │   └── util/
│   │   │   │   │       ├── DateTimeUtil.java
│   │   │   │   │       └── ValidationUtil.java
│   │   │   │   └── resources/
│   │   │   │       ├── application.yml
│   │   │   │       ├── application-dev.yml
│   │   │   │       ├── application-staging.yml
│   │   │   │       ├── application-prod.yml
│   │   │   │       └── db/
│   │   │   │           └── migration/
│   │   │   │               ├── V1__init_schema.sql
│   │   │   │               ├── V2__create_users_table.sql
│   │   │   │               ├── V3__create_profiles_table.sql
│   │   │   │               ├── V4__create_health_records_table.sql
│   │   │   │               ├── V5__create_profile_shares_table.sql
│   │   │   │               ├── V6__create_reference_data_table.sql
│   │   │   │               ├── V7__create_audit_logs_table.sql
│   │   │   │               ├── V8__create_user_consents_table.sql
│   │   │   │               └── V9__create_refresh_tokens_table.sql
│   │   │   └── test/
│   │   │       └── java/com/healthlens/api/
│   │   │           ├── controller/
│   │   │           │   ├── AuthControllerTest.java
│   │   │           │   └── ProfileControllerTest.java
│   │   │           ├── service/
│   │   │           │   ├── AuthServiceTest.java
│   │   │           │   ├── ProfileServiceTest.java
│   │   │           │   └── OcrServiceTest.java
│   │   │           └── integration/
│   │   │               ├── AuthIntegrationTest.java
│   │   │               └── HealthRecordIntegrationTest.java
│   │   └── Dockerfile
│   │
│   ├── web/                        # Next.js Web Dashboard
│   │   ├── package.json
│   │   ├── next.config.js
│   │   ├── tailwind.config.js
│   │   ├── tsconfig.json
│   │   ├── .env.local.example
│   │   ├── src/
│   │   │   ├── app/
│   │   │   │   ├── globals.css
│   │   │   │   ├── layout.tsx
│   │   │   │   ├── page.tsx                    # Trang landing (SSR)
│   │   │   │   ├── (auth)/
│   │   │   │   │   ├── layout.tsx
│   │   │   │   │   ├── login/page.tsx
│   │   │   │   │   ├── register/page.tsx
│   │   │   │   │   ├── forgot-password/page.tsx
│   │   │   │   │   └── reset-password/page.tsx
│   │   │   │   ├── (dashboard)/
│   │   │   │   │   ├── layout.tsx              # Layout được bảo vệ với sidebar
│   │   │   │   │   ├── page.tsx                # Trang chủ dashboard
│   │   │   │   │   ├── profiles/
│   │   │   │   │   │   ├── page.tsx            # Danh sách hồ sơ
│   │   │   │   │   │   ├── [id]/
│   │   │   │   │   │   │   ├── page.tsx        # Chi tiết hồ sơ
│   │   │   │   │   │   │   └── health-records/
│   │   │   │   │   │   │       └── page.tsx    # Timeline kết quả khám
│   │   │   │   │   │   └── new/page.tsx        # Tạo hồ sơ
│   │   │   │   │   ├── health-records/
│   │   │   │   │   │   ├── page.tsx            # Xem tất cả kết quả
│   │   │   │   │   │   └── [id]/page.tsx       # Chi tiết + giải thích
│   │   │   │   │   ├── family/
│   │   │   │   │   │   ├── page.tsx            # Quản lý chia sẻ gia đình
│   │   │   │   │   │   └── shared-with-me/page.tsx
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   ├── page.tsx            # Cài đặt chung
│   │   │   │   │   │   ├── account/page.tsx
│   │   │   │   │   │   └── privacy/page.tsx    # Xóa dữ liệu, đồng ý
│   │   │   │   │   └── upload/page.tsx         # Upload web (chỉ PDF)
│   │   │   │   └── admin/
│   │   │   │       ├── layout.tsx              # Layout admin
│   │   │   │       ├── page.tsx                # Dashboard admin
│   │   │   │       ├── reference-data/
│   │   │   │       │   ├── page.tsx
│   │   │   │       │   └── [id]/page.tsx
│   │   │   │       ├── users/page.tsx
│   │   │   │       ├── audit-logs/page.tsx
│   │   │   │       └── analytics/page.tsx
│   │   │   ├── components/
│   │   │   │   ├── ui/                         # UI chung (Radix-based)
│   │   │   │   │   ├── Button.tsx
│   │   │   │   │   ├── Input.tsx
│   │   │   │   │   ├── Card.tsx
│   │   │   │   │   ├── Modal.tsx
│   │   │   │   │   ├── Toast.tsx
│   │   │   │   │   ├── Skeleton.tsx
│   │   │   │   │   ├── Badge.tsx
│   │   │   │   │   └── Table.tsx
│   │   │   │   ├── features/
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── LoginForm.tsx
│   │   │   │   │   │   ├── RegisterForm.tsx
│   │   │   │   │   │   └── ConsentCheckbox.tsx
│   │   │   │   │   ├── profiles/
│   │   │   │   │   │   ├── ProfileCard.tsx
│   │   │   │   │   │   ├── ProfileList.tsx
│   │   │   │   │   │   ├── ProfileForm.tsx
│   │   │   │   │   │   └── ProfileSelector.tsx
│   │   │   │   │   ├── health-records/
│   │   │   │   │   │   ├── HealthRecordCard.tsx
│   │   │   │   │   │   ├── HealthRecordTimeline.tsx
│   │   │   │   │   │   ├── MetricDisplay.tsx
│   │   │   │   │   │   ├── MetricExplanation.tsx
│   │   │   │   │   │   ├── TrendChart.tsx      # P2
│   │   │   │   │   │   └── AbnormalAlert.tsx
│   │   │   │   │   ├── upload/
│   │   │   │   │   │   ├── FileDropzone.tsx
│   │   │   │   │   │   ├── UploadProgress.tsx
│   │   │   │   │   │   └── ManualInputForm.tsx
│   │   │   │   │   ├── family/
│   │   │   │   │   │   ├── ShareInviteForm.tsx
│   │   │   │   │   │   ├── SharedProfileCard.tsx
│   │   │   │   │   │   └── FamilyDashboard.tsx
│   │   │   │   │   └── admin/
│   │   │   │   │       ├── ReferenceDataTable.tsx
│   │   │   │   │       ├── ReferenceDataForm.tsx
│   │   │   │   │       ├── AuditLogTable.tsx
│   │   │   │   │       └── AnalyticsChart.tsx
│   │   │   │   └── layouts/
│   │   │   │       ├── AuthLayout.tsx
│   │   │   │       ├── DashboardLayout.tsx
│   │   │   │       ├── AdminLayout.tsx
│   │   │   │       ├── Sidebar.tsx
│   │   │   │       └── Header.tsx
│   │   │   ├── hooks/
│   │   │   │   ├── useAuth.ts
│   │   │   │   ├── useProfile.ts
│   │   │   │   ├── useHealthRecords.ts
│   │   │   │   ├── useFamilySharing.ts
│   │   │   │   └── useUpload.ts
│   │   │   ├── lib/
│   │   │   │   ├── api/
│   │   │   │   │   ├── client.ts               # Axios instance + interceptors
│   │   │   │   │   ├── auth.ts
│   │   │   │   │   ├── profiles.ts
│   │   │   │   │   ├── healthRecords.ts
│   │   │   │   │   ├── upload.ts
│   │   │   │   │   └── admin.ts
│   │   │   │   ├── utils/
│   │   │   │   │   ├── formatDate.ts
│   │   │   │   │   └── formatMetric.ts
│   │   │   │   └── constants.ts
│   │   │   ├── stores/
│   │   │   │   ├── authStore.ts
│   │   │   │   ├── profileStore.ts
│   │   │   │   └── uiStore.ts
│   │   │   ├── providers/
│   │   │   │   ├── QueryProvider.tsx
│   │   │   │   └── AuthProvider.tsx
│   │   │   └── middleware.ts                   # Bảo vệ route
│   │   ├── public/
│   │   │   ├── favicon.ico
│   │   │   └── images/
│   │   ├── e2e/                                # Tests E2E Playwright
│   │   │   ├── auth.spec.ts
│   │   │   ├── profile.spec.ts
│   │   │   └── health-record.spec.ts
│   │   └── Dockerfile
│   │
│   └── mobile/                     # React Native Expo App
│       ├── package.json
│       ├── app.json
│       ├── tsconfig.json
│       ├── babel.config.js
│       ├── metro.config.js
│       ├── app/
│       │   ├── _layout.tsx                     # Root layout (Expo Router)
│       │   ├── index.tsx                       # Splash/redirect
│       │   ├── (auth)/
│       │   │   ├── _layout.tsx
│       │   │   ├── login.tsx
│       │   │   ├── register.tsx
│       │   │   └── onboarding.tsx              # Đồng ý người dùng mới
│       │   ├── (tabs)/
│       │   │   ├── _layout.tsx                 # Navigation tab
│       │   │   ├── index.tsx                   # Trang chủ (kết quả gần đây)
│       │   │   ├── upload.tsx                  # Màn hình camera/upload
│       │   │   ├── profiles.tsx                # Quản lý hồ sơ
│       │   │   └── settings.tsx                # Cài đặt
│       │   ├── profiles/
│       │   │   ├── [id].tsx                    # Chi tiết hồ sơ
│       │   │   └── new.tsx                     # Tạo hồ sơ
│       │   ├── health-records/
│       │   │   ├── [id].tsx                    # Chi tiết kết quả + giải thích
│       │   │   └── manual-input.tsx            # Nhập chỉ số thủ công
│       │   └── upload/
│       │       ├── camera.tsx                  # Chụp camera
│       │       ├── preview.tsx                 # Xem trước + xác nhận
│       │       └── processing.tsx              # Màn hình xử lý OCR
│       ├── components/
│       │   ├── ui/
│       │   │   ├── Button.tsx
│       │   │   ├── Input.tsx
│       │   │   ├── Card.tsx
│       │   │   ├── Toast.tsx
│       │   │   └── LoadingSpinner.tsx
│       │   ├── features/
│       │   │   ├── auth/
│       │   │   │   ├── LoginForm.tsx
│       │   │   │   └── ConsentScreen.tsx
│       │   │   ├── profiles/
│       │   │   │   ├── ProfileCard.tsx
│       │   │   │   └── ProfileForm.tsx
│       │   │   ├── health-records/
│       │   │   │   ├── HealthRecordCard.tsx
│       │   │   │   ├── MetricDisplay.tsx
│       │   │   │   ├── MetricExplanation.tsx
│       │   │   │   └── AbnormalAlert.tsx
│       │   │   └── upload/
│       │   │       ├── CameraCapture.tsx
│       │   │       ├── ImagePreview.tsx
│       │   │       ├── OcrProgress.tsx
│       │   │       └── ManualInputForm.tsx
│       │   └── layouts/
│       │       ├── SafeAreaLayout.tsx
│       │       └── TabBar.tsx
│       ├── hooks/
│       │   ├── useAuth.ts
│       │   ├── useProfile.ts
│       │   ├── useHealthRecords.ts
│       │   ├── useCamera.ts
│       │   ├── useOfflineSync.ts
│       │   └── useUpload.ts
│       ├── lib/
│       │   ├── api/
│       │   │   ├── client.ts
│       │   │   ├── auth.ts
│       │   │   ├── profiles.ts
│       │   │   ├── healthRecords.ts
│       │   │   └── upload.ts
│       │   ├── storage/
│       │   │   ├── secureStore.ts              # Expo SecureStore wrapper
│       │   │   └── sqlite.ts                   # Database offline
│       │   ├── utils/
│       │   │   └── formatDate.ts
│       │   └── constants.ts
│       ├── stores/
│       │   ├── authStore.ts
│       │   ├── profileStore.ts
│       │   ├── offlineStore.ts
│       │   └── syncStore.ts
│       ├── providers/
│       │   ├── QueryProvider.tsx
│       │   ├── AuthProvider.tsx
│       │   └── OfflineProvider.tsx
│       └── assets/
│           ├── images/
│           └── fonts/
│
├── packages/
│   └── shared/                     # Code TypeScript Chia Sẻ
│       ├── package.json
│       ├── tsconfig.json
│       ├── src/
│       │   ├── index.ts
│       │   ├── types/
│       │   │   ├── user.ts
│       │   │   ├── profile.ts
│       │   │   ├── healthRecord.ts
│       │   │   ├── metric.ts
│       │   │   ├── familySharing.ts
│       │   │   ├── referenceData.ts
│       │   │   ├── api.ts                      # API response types
│       │   │   └── index.ts
│       │   ├── schemas/
│       │   │   ├── auth.ts                     # loginSchema, registerSchema
│       │   │   ├── profile.ts                  # createProfileSchema
│       │   │   ├── healthRecord.ts             # createHealthRecordSchema
│       │   │   ├── metric.ts                   # manualMetricSchema
│       │   │   └── index.ts
│       │   └── constants/
│       │       ├── errorCodes.ts
│       │       ├── metricTypes.ts
│       │       ├── roles.ts
│       │       └── index.ts
│       └── dist/                               # Output compiled
│
├── docker/
│   ├── api/
│   │   └── Dockerfile.prod
│   ├── web/
│   │   └── Dockerfile.prod
│   └── nginx/
│       └── nginx.conf
│
├── k8s/                            # Kubernetes manifests
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── api-deployment.yaml
│   │   ├── api-service.yaml
│   │   ├── web-deployment.yaml
│   │   ├── web-service.yaml
│   │   ├── ingress.yaml
│   │   └── secrets.yaml
│   ├── staging/
│   │   └── kustomization.yaml
│   └── production/
│       └── kustomization.yaml
│
├── scripts/
│   ├── setup-dev.sh                # Thiết lập môi trường dev
│   ├── generate-types.sh           # Tạo TS types từ OpenAPI
│   └── seed-reference-data.sh      # Seed dữ liệu tham chiếu ban đầu
│
└── docs/
    ├── api/
    │   └── openapi.yaml            # OpenAPI spec (auto-generated base)
    ├── architecture/
    │   └── README.md
    └── deployment/
        └── README.md
```

### Ranh Giới Kiến Trúc

#### Ranh Giới API

**Public REST API (`/api/v1/`):**
| Nhóm Endpoint | Đường Dẫn | Controller | Yêu Cầu Auth |
|---------------|-----------|------------|--------------|
| Xác Thực | `/api/v1/auth/*` | AuthController | Không (login/register) |
| Người Dùng | `/api/v1/users/*` | UserController | Có |
| Hồ Sơ | `/api/v1/profiles/*` | ProfileController | Có + Ownership |
| Kết Quả Khám | `/api/v1/health-records/*` | HealthRecordController | Có + Ownership |
| Upload | `/api/v1/upload/*` | UploadController | Có |
| Chia Sẻ Gia Đình | `/api/v1/family/*` | FamilySharingController | Có |

**Admin API (`/api/v1/admin/*`):**
| Nhóm Endpoint | Đường Dẫn | Controller | Yêu Cầu Auth |
|---------------|-----------|------------|--------------|
| Dữ Liệu Tham Chiếu | `/api/v1/admin/reference-data/*` | AdminReferenceDataController | Admin + MFA |
| Quản Lý Người Dùng | `/api/v1/admin/users/*` | AdminUserController | Admin + MFA |
| Audit Logs | `/api/v1/admin/audit-logs/*` | AdminAuditController | Admin + MFA |

**Ranh Giới Service Nội Bộ:**
- **OcrService** — Cô lập cuộc gọi API ngoài, logic retry, xử lý timeout
- **LlmService** — Cô lập cuộc gọi API ngoài, caching response (Redis)
- **StorageService** — Abstraction S3/MinIO, tạo pre-signed URL

#### Ranh Giới Component

**Mẫu Giao Tiếp Frontend:**

```
┌──────────────────────────────────────────────────────────────┐
│                      React Component                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────┐  │
│  │   Zustand   │◄───│ TanStack    │◄───│   API Client    │  │
│  │ (UI State)  │    │   Query     │    │  (Axios + Auth) │  │
│  └─────────────┘    │(Server State)│   └────────┬────────┘  │
│                     └─────────────┘             │            │
└─────────────────────────────────────────────────┼────────────┘
                                                  │
                                                  ▼
                                        ┌─────────────────┐
                                        │  Spring Boot    │
                                        │     REST API    │
                                        └─────────────────┘
```

**Ranh Giới Quản Lý State:**
- **TanStack Query** — Server state (profiles, health records, dữ liệu tham chiếu)
- **Zustand** — Client-only state (selectedProfileId, sidebar đóng/mở, form drafts)
- **React Hook Form** — Form state (cô lập theo form, không global)

#### Ranh Giới Service (Backend)

```
┌───────────────────────────────────────────────────────────────────┐
│                          Controller Layer                          │
│   Controllers mỏng — validation, kiểm tra auth, delegate to service│
└───────────────────────────────────┬───────────────────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────┐
│                          Service Layer                             │
│   Business logic, ranh giới transaction, cuộc gọi cross-service    │
│                                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ProfileService│  │HealthRecord │  │ OcrService   │             │
│  │              │  │   Service    │  │ (Ngoài)      │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
└───────────────────────────────────┬───────────────────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────────────────────────────────────┐
│                         Repository Layer                           │
│   Chỉ data access — không business logic, JPA repositories         │
└───────────────────────────────────┬───────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────┐
                    │      PostgreSQL       │
                    │        Redis          │
                    │       S3/MinIO        │
                    └───────────────────────┘
```

#### Ranh Giới Dữ Liệu

**Ranh Giới Schema Database:**

| Nhóm Schema/Bảng | Mục Đích | Mẫu Truy Cập |
|------------------|----------|--------------|
| `users`, `refresh_tokens` | Xác thực | Chỉ AuthService |
| `profiles`, `user_consents` | Hồ sơ người dùng | ProfileService |
| `health_records` | Dữ liệu sức khỏe | HealthRecordService + ProfileService |
| `profile_shares` | Chia sẻ gia đình | FamilySharingService |
| `reference_data` | Ngưỡng y tế | ReferenceDataService (đọc nhiều) |
| `audit_logs` | Tuân thủ | AuditService (ghi nhiều, append-only) |

**Ranh Giới Caching (Redis):**

| Mẫu Cache Key | TTL | Mục Đích |
|---------------|-----|----------|
| `token:blacklist:{jti}` | 15 phút | JWT blacklist |
| `llm:explanation:{hash}` | 24h | LLM response cache |
| `ref:thresholds:{metricType}` | 1h | Cache dữ liệu tham chiếu |
| `session:{userId}` | 30 phút | Session data |

### Ánh Xạ Yêu Cầu Đến Cấu Trúc

#### User Journey → Ánh Xạ Component

**J1 — Bà Lan (Upload + Giải Thích):**
| Năng Lực | Vị Trí Mobile | Vị Trí Web | API Endpoint |
|----------|---------------|------------|--------------|
| Chụp camera | `app/upload/camera.tsx` | N/A | N/A |
| Upload file | `components/features/upload/` | `components/features/upload/` | `POST /api/v1/upload/presigned-url` |
| Xử lý OCR | `app/upload/processing.tsx` | `components/features/upload/UploadProgress.tsx` | `POST /api/v1/health-records` |
| Hiển thị chỉ số | `components/features/health-records/MetricDisplay.tsx` | Giống | `GET /api/v1/health-records/{id}` |
| Giải thích LLM | `components/features/health-records/MetricExplanation.tsx` | Giống | Nhúng trong health record response |
| Xem timeline | `app/(tabs)/index.tsx` | `app/(dashboard)/profiles/[id]/health-records/page.tsx` | `GET /api/v1/profiles/{id}/health-records` |

**J2 — Anh Minh (Chia Sẻ Gia Đình Web):**
| Năng Lực | Vị Trí Web | API Endpoint |
|----------|------------|--------------|
| Dashboard gia đình | `app/(dashboard)/family/page.tsx` | `GET /api/v1/family/shared-with-me` |
| Xem hồ sơ chia sẻ | `components/features/family/SharedProfileCard.tsx` | `GET /api/v1/profiles/{id}` (với share permission) |
| Biểu đồ xu hướng | `components/features/health-records/TrendChart.tsx` | `GET /api/v1/profiles/{id}/health-records?metrics=HbA1c` |
| Mời chia sẻ | `components/features/family/ShareInviteForm.tsx` | `POST /api/v1/family/invitations` |

**J3 — Chị Thu (OCR Fallback):**
| Năng Lực | Vị Trí Mobile | Vị Trí Backend |
|----------|---------------|----------------|
| Xử lý lỗi OCR | `app/upload/processing.tsx` | `service/OcrService.java` |
| Form nhập thủ công | `app/health-records/manual-input.tsx` | `controller/HealthRecordController.java` |
| Gắn thẻ nguồn dữ liệu | N/A (tự động) | `entity/HealthRecord.java` (trường sourceType) |

**J4 — Admin (Dữ Liệu Tham Chiếu):**
| Năng Lực | Vị Trí Web | API Endpoint |
|----------|------------|--------------|
| Dashboard admin | `app/admin/page.tsx` | `GET /api/v1/admin/analytics` |
| CRUD dữ liệu tham chiếu | `app/admin/reference-data/` | `GET/POST/PUT/DELETE /api/v1/admin/reference-data` |
| Xem audit log | `app/admin/audit-logs/page.tsx` | `GET /api/v1/admin/audit-logs` |
| Thực thi MFA | `components/features/admin/` + backend | `POST /api/v1/auth/mfa/verify` |

#### Nhóm FR → Ánh Xạ Thư Mục

| Nhóm FR | Package Backend | Folder Features Frontend |
|---------|-----------------|--------------------------|
| FR1-FR8 (Tài Khoản) | `controller/AuthController`, `controller/UserController` | `features/auth/` |
| FR5-FR6 (Đa Hồ Sơ) | `controller/ProfileController`, `service/ProfileService` | `features/profiles/` |
| FR9-FR17 (Upload/OCR) | `controller/UploadController`, `service/OcrService` | `features/upload/` |
| FR18-FR23 (Giải Thích) | `service/LlmService`, `service/HealthRecordService` | `features/health-records/` |
| FR24-FR28 (Lịch Sử) | `controller/HealthRecordController` | `features/health-records/` |
| FR29-FR32 (Gia Đình) | `controller/FamilySharingController` | `features/family/` |
| FR33-FR38 (Admin) | `controller/admin/*` | `features/admin/` |

#### Mối Quan Tâm Xuyên Suốt → Ánh Xạ Vị Trí

| Mối Quan Tâm | Vị Trí Backend | Vị Trí Frontend |
|--------------|----------------|-----------------|
| Xác Thực | `config/SecurityConfig.java`, `security/*` | `hooks/useAuth.ts`, `stores/authStore.ts`, `middleware.ts` |
| Phân Quyền | `security/JwtAuthenticationFilter.java` | `middleware.ts`, `providers/AuthProvider.tsx` |
| Audit Logging | `aspect/AuditAspect.java`, `annotation/Auditable.java` | N/A (chỉ backend) |
| Xử Lý Lỗi | `exception/GlobalExceptionHandler.java` | `lib/api/client.ts` (interceptor) |
| Caching | `config/RedisConfig.java`, service layer | TanStack Query (tự động) |
| Validation | `dto/request/*`, Bean Validation | Zod schemas trong `packages/shared/schemas/` |

### Điểm Tích Hợp

#### Giao Tiếp Nội Bộ

**Frontend → Backend:**
- REST API qua HTTPS
- JWT trong Authorization header (access token)
- Refresh token trong HttpOnly cookie
- Request/Response: JSON với camelCase keys

**Backend → Dịch Vụ Ngoài:**
- **OCR API:** HTTP client với retry (3x), timeout (15s)
- **LLM API:** HTTP client với retry (3x), timeout (30s), response caching
- **S3/MinIO:** AWS SDK, pre-signed URLs cho client uploads

**Backend → Database:**
- JPA/Hibernate với connection pooling (HikariCP)
- Flyway cho migrations
- Read replicas cho hoạt động đọc nặng (P2)

#### Tích Hợp Bên Ngoài

| Dịch Vụ | Điểm Tích Hợp | Xử Lý Lỗi |
|---------|---------------|-----------|
| OCR API (Google Vision) | `service/OcrService.java` | Timeout → fallback nhập thủ công |
| LLM API (OpenAI/Claude) | `service/LlmService.java` | Timeout → cached response hoặc giải thích generic |
| S3/MinIO | `service/StorageService.java` | Pre-signed URL hết hạn → tạo lại |
| FCM/APNs | `service/NotificationService.java` | Thất bại im lặng, log để retry |
| Email (SendGrid) | `service/NotificationService.java` | Queue để retry |

#### Luồng Dữ Liệu

**Luồng Upload:**
```
Mobile/Web → [1] Yêu cầu pre-signed URL → Backend
          ← [2] Response pre-signed URL
          → [3] Upload file trực tiếp → S3/MinIO
          → [4] Thông báo upload hoàn thành → Backend
          → [5] Kích hoạt job OCR async → Redis Queue
          → [6] Xử lý OCR → External OCR API
          → [7] Giải thích LLM → External LLM API
          → [8] Lưu health record → PostgreSQL
          → [9] Trả kết quả → Mobile/Web
```

**Luồng Dữ Liệu Chia Sẻ Gia Đình:**
```
Chủ Sở Hữu (Mobile) → [1] Mời chia sẻ hồ sơ → Backend
                   → [2] Tạo profile_share record → PostgreSQL
                   → [3] Gửi thông báo → FCM/Email

Người Xem (Web) → [4] Yêu cầu hồ sơ chia sẻ → Backend
               → [5] Xác thực quyền chia sẻ → PostgreSQL
               ← [6] Trả hồ sơ + health records
```

### Mẫu Tổ Chức File

#### File Cấu Hình

| File | Mục Đích | Theo Môi Trường |
|------|----------|-----------------|
| `apps/api/src/main/resources/application.yml` | Cấu hình base | Không |
| `apps/api/src/main/resources/application-{env}.yml` | Override môi trường | Có |
| `apps/web/.env.local` | Môi trường web | Có (không commit) |
| `apps/mobile/app.json` | Cấu hình Expo | Một phần |
| `docker-compose.dev.yml` | Development local | Không |
| `.github/workflows/` | Pipeline CI/CD | Không |

#### Tổ Chức Source

**Backend (Theo Domain):**
```
controller/     → HTTP layer (mỏng, delegate đến service)
service/        → Business logic (ranh giới transaction ở đây)
repository/     → Data access (chỉ JPA repositories)
entity/         → Domain entities (JPA annotations)
dto/            → API contracts (request/response)
mapper/         → Chuyển đổi Entity ↔ DTO
exception/      → Custom exceptions + global handler
config/         → Các class cấu hình Spring
security/       → JWT, authentication filters
aspect/         → Mối quan tâm xuyên suốt (audit)
```

**Frontend (Theo Features):**
```
app/            → Pages/routes (Next.js App Router / Expo Router)
components/
  ui/           → UI components generic, tái sử dụng
  features/     → Components theo domain (nhóm theo feature)
  layouts/      → Page layouts, navigation
hooks/          → Custom React hooks
lib/
  api/          → API client và endpoints
  utils/        → Helper functions
stores/         → Zustand state stores
providers/      → React context providers
```

#### Tổ Chức Test

| Loại Test | Vị Trí Backend | Vị Trí Frontend |
|-----------|----------------|-----------------|
| Unit Tests | `src/test/java/.../service/` | Co-located `*.test.tsx` |
| Integration Tests | `src/test/java/.../integration/` | N/A |
| E2E Tests | N/A | `apps/web/e2e/`, `apps/mobile/__tests__/` |
| API Tests | `src/test/java/.../controller/` | N/A |

#### Tổ Chức Assets

**Web:**
```
apps/web/public/
  favicon.ico
  images/
    logo.svg
    og-image.png
```

**Mobile:**
```
apps/mobile/assets/
  images/
    icon.png
    splash.png
  fonts/
    (custom fonts nếu cần)
```

### Tích Hợp Quy Trình Phát Triển

#### Cấu Trúc Development Server

```bash
# Terminal 1: Backend
cd apps/api && ./gradlew bootRun

# Terminal 2: Web
cd apps/web && pnpm dev

# Terminal 3: Mobile
cd apps/mobile && pnpm start

# Terminal 4: Infrastructure
docker-compose -f docker-compose.dev.yml up
```

**URLs Local:**
- Backend API: `http://localhost:8080`
- Web Dashboard: `http://localhost:3000`
- Mobile Expo: `exp://localhost:8081`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- MinIO Console: `http://localhost:9001`

#### Cấu Trúc Quy Trình Build

| App | Lệnh Build | Output |
|-----|------------|--------|
| Backend | `./gradlew bootBuildImage` | Docker image |
| Web | `pnpm build` | `apps/web/.next/` |
| Mobile | `eas build` | APK/AAB/IPA |
| Shared | `pnpm build` | `packages/shared/dist/` |

#### Cấu Trúc Triển Khai

**GitHub Actions Pipeline Stages:**
```
test → build → deploy:staging → deploy:production
```

**Cấu Trúc Kubernetes:**
```
k8s/
  base/              → Manifests chung
  staging/           → Overlays staging (Kustomize)
  production/        → Overlays production (Kustomize)
```

**Thăng Tiến Môi Trường:**
```
Feature Branch → Staging (tự động) → Production (phê duyệt thủ công)
```

## Kết Quả Xác Thực Kiến Trúc

### Xác Thực Tính Nhất Quán ✅

**Tương Thích Quyết Định:**
Tất cả technology choices đã được kiểm tra tương thích:
- ✅ Spring Boot 4.0.3 + Java 21 — Phiên bản LTS, kết hợp ổn định
- ✅ Next.js 16.x + TypeScript 5.x — App Router trưởng thành, Turbopack ổn định
- ✅ Expo SDK 55 + React Native — Ổn định mới nhất, hỗ trợ Expo Router tốt
- ✅ PostgreSQL 16 + Redis 7 — Spring Data JPA/Redis tương thích đầy đủ
- ✅ JWT (jjwt 0.12.x) + Spring Security 6.x — Tích hợp native
- ✅ TanStack Query v5 + Zustand v4 — Không xung đột, mục đích bổ sung
- ✅ Zod v3 + React Hook Form v7 — Tích hợp resolver chính thức

**Không phát hiện xung đột quyết định.** Tất cả versions đã được chọn là stable releases với tài liệu tương thích.

**Tính Nhất Quán Mẫu:**
- ✅ Quy ước đặt tên thống nhất: snake_case (DB) → camelCase (API/TS) → PascalCase (classes/components)
- ✅ Định dạng lỗi RFC 7807 áp dụng toàn cục — backend GlobalExceptionHandler + frontend API client interceptor
- ✅ Mẫu quản lý state nhất quán: TanStack Query (server) + Zustand (client) + React Hook Form (forms)
- ✅ Mẫu cấu trúc folder thống nhất giữa web và mobile (features/, hooks/, lib/, stores/)

**Căn Chỉnh Cấu Trúc:**
- ✅ Cấu trúc monorepo hỗ trợ shared code qua `packages/shared/`
- ✅ Cấu trúc backend (controller → service → repository) phù hợp Spring Boot conventions
- ✅ Cấu trúc frontend (App Router + features) phù hợp Next.js 16 best practices
- ✅ Vị trí test co-located (frontend) + mirrored (backend) theo conventions

### Xác Thực Độ Phủ Yêu Cầu ✅

**Độ Phủ Yêu Cầu Chức Năng (38 FRs):**

| Nhóm FR | FRs | Hỗ Trợ Kiến Trúc | Trạng Thái |
|---------|-----|------------------|------------|
| Tài Khoản & Hồ Sơ (FR1-FR8) | FR1-FR8 | AuthController, UserController, ProfileController, SecurityConfig, UserConsent entity | ✅ Phủ |
| Upload & Xử Lý (FR9-FR17) | FR9-FR17 | UploadController, OcrService, StorageService, pre-signed URLs, luồng nhập thủ công | ✅ Phủ |
| Hiển Thị & Giải Thích (FR18-FR23) | FR18-FR23 | LlmService, ReferenceDataService, MetricExplanation components, ngưỡng theo ngữ cảnh | ✅ Phủ |
| Lịch Sử & Theo Dõi (FR24-FR28) | FR24-FR28 | HealthRecordController, Timeline components, Expo SQLite offline, syncStore | ✅ Phủ |
| Chia Sẻ Gia Đình (FR29-FR32) | FR29-FR32 | FamilySharingController, ProfileShare entity, polling web dashboard | ✅ Phủ |
| Quản Trị (FR33-FR38) | FR33-FR38 | Admin controllers, TotpService (MFA), AuditService, Analytics endpoints | ✅ Phủ |

**Độ Phủ Yêu Cầu Phi Chức Năng (16 NFRs):**

| NFR | Yêu Cầu | Hỗ Trợ Kiến Trúc | Trạng Thái |
|-----|---------|------------------|------------|
| NFR-P1 | OCR ≤10s | OcrService async + timeout 15s, Redis queue | ✅ Phủ |
| NFR-P2 | Tải trang ≤2s | Next.js SSR/CSR hybrid, TanStack Query caching | ✅ Phủ |
| NFR-P3 | LLM ≤5s | LlmService + Redis cache (24h TTL) | ✅ Phủ |
| NFR-P4 | Cập nhật dashboard ≤30s | Chiến lược polling (P1), đường nâng cấp WebSocket (P2) | ✅ Phủ |
| NFR-S1 | Mã hóa at rest/transit | AES-256 (PostgreSQL pgcrypto), TLS 1.2+ | ✅ Phủ |
| NFR-S2 | Session timeout 30 phút | Redis session với 30 phút TTL | ✅ Phủ |
| NFR-S3 | Audit logging | AuditAspect + @Auditable annotation | ✅ Phủ |
| NFR-S4 | Admin MFA | TotpService (TOTP) | ✅ Phủ |
| NFR-S5 | Quyền xóa 72h | DataRetentionJob, endpoints xóa explicit | ✅ Phủ |
| NFR-SC1 | 500 người dùng đồng thời | Kubernetes HPA, connection pooling (HikariCP) | ✅ Phủ |
| NFR-SC2 | 50 job OCR song song | Redis Streams queue, xử lý async | ✅ Phủ |
| NFR-A1 | WCAG 2.1 AA | Radix UI (primitives accessible), semantic HTML | ✅ Phủ |
| NFR-A2 | Font tối thiểu 16px | Cấu hình Tailwind CSS, design system | ✅ Phủ |
| NFR-R1 | 99% uptime (6-22 ICT) | Kubernetes deployment, health checks | ✅ Phủ |
| NFR-R2 | OCR timeout fallback | OcrService timeout → UX nhập thủ công | ✅ Phủ |
| NFR-R3 | LLM retry 3x | Logic retry LlmService | ✅ Phủ |

**Độ Phủ User Journey:**
- ✅ J1 (Bà Lan) — Luồng upload được ánh xạ đầy đủ đến components và API endpoints
- ✅ J2 (Anh Minh) — Web dashboard chia sẻ gia đình với biểu đồ xu hướng (P2)
- ✅ J3 (Chị Thu) — OCR fallback và luồng nhập thủ công được ghi nhận
- ✅ J4 (Admin) — Admin panel với MFA, CRUD dữ liệu tham chiếu, audit logs

### Xác Thực Sẵn Sàng Triển Khai ✅

**Đầy Đủ Quyết Định:**
- ✅ Tất cả quyết định quan trọng có versions cụ thể (Spring Boot 4.0.3, Next.js 16.x, v.v.)
- ✅ Lệnh starter đầy đủ cho cả 3 nền tảng
- ✅ Chiến lược schema database rõ ràng (hybrid normalized + JSONB)
- ✅ Luồng xác thực chi tiết (access/refresh tokens, rotation, blacklist)

**Đầy Đủ Cấu Trúc:**
- ✅ Cây thư mục hoàn chỉnh với ~200+ files/folders được định nghĩa
- ✅ Tất cả controllers, services, repositories được đặt tên cụ thể
- ✅ Components frontend tổ chức theo features
- ✅ Types và schemas package shared được định nghĩa

**Đầy Đủ Mẫu:**
- ✅ Quy ước đặt tên toàn diện cho DB, API, Java, TypeScript
- ✅ Mẫu xử lý lỗi với code examples
- ✅ Mẫu quản lý state với Zustand examples
- ✅ Mẫu validation với Zod examples
- ✅ Phần anti-patterns để tránh common mistakes

### Kết Quả Phân Tích Gaps

**Critical Gaps:** Không phát hiện

**Important Gaps (Không Chặn):**
1. **Rate Limiting** — Chưa định nghĩa chi tiết, có thể triển khai khi cần
2. **Monitoring Stack** — Tối thiểu cho MVP (chỉ Actuator), đường nâng cấp đến Prometheus/Grafana đã ghi nhận
3. **WebSocket** — Hoãn đến P2, polling chấp nhận được cho MVP

**Nice-to-Have Gaps:**
1. **OpenAPI Schema** — Auto-generated từ Spring Boot, có thể thêm custom schema file
2. **E2E Test Framework** — Playwright cho web, Detox cho mobile (đề cập nhưng chưa chi tiết)
3. **Performance Testing** — Có thể thêm k6 scripts cho load testing

### Vấn Đề Xác Thực Đã Giải Quyết

**Vấn Đề #1: GitHub Actions vs GitHub Actions**
- **Đã Giải Quyết:** Đã chuyển từ GitHub Actions sang GitHub Actions theo preference người dùng
- **Tác Động:** `.github/workflows/` thay thế `.github/workflows/`

**Vấn Đề #2: Ước Lượng Thời Gian**
- **Đã Giải Quyết:** Đã loại bỏ toàn bộ ước lượng thời gian theo hướng dẫn BMAD
- **Tác Động:** Architecture tập trung vào cấu trúc, không lập lịch

### Checklist Hoàn Thành Kiến Trúc

**✅ Phân Tích Yêu Cầu**
- [x] Bối cảnh dự án được phân tích kỹ lưỡng (38 FRs, 16 NFRs)
- [x] Quy mô và độ phức tạp được đánh giá (Độ phức tạp CAO, healthcare full-stack)
- [x] Ràng buộc kỹ thuật được xác định (NĐ 13/2023, phụ thuộc OCR/LLM ngoài)
- [x] Mối quan tâm xuyên suốt được ánh xạ (bảo mật, caching, audit, offline sync)

**✅ Quyết Định Kiến Trúc**
- [x] Quyết định quan trọng được ghi nhận với versions
- [x] Technology stack được chỉ định đầy đủ (Java 21, Spring Boot 4.0.3, Next.js 16, Expo 55)
- [x] Mẫu tích hợp được định nghĩa (REST, pre-signed URLs, Redis Streams)
- [x] Xem xét hiệu năng được giải quyết (caching, xử lý async)

**✅ Mẫu Triển Khai**
- [x] Quy ước đặt tên được thiết lập (6 bảng bao phủ tất cả khu vực)
- [x] Mẫu cấu trúc được định nghĩa (backend theo domain, frontend theo feature)
- [x] Mẫu giao tiếp được chỉ định (TanStack Query, Zustand, đặt tên event)
- [x] Mẫu quy trình được ghi nhận (xử lý lỗi, loading states, validation)

**✅ Cấu Trúc Dự Án**
- [x] Cấu trúc thư mục hoàn chỉnh được định nghĩa (~200+ files)
- [x] Ranh giới component được thiết lập (ranh giới API, service, data)
- [x] Điểm tích hợp được ánh xạ (nội bộ + bên ngoài)
- [x] Ánh xạ yêu cầu đến cấu trúc hoàn thành (4 user journeys + 6 nhóm FR)

### Đánh Giá Sẵn Sàng Kiến Trúc

**Trạng Thái Tổng Thể:** ✅ SẴN SÀNG CHO TRIỂN KHAI

**Mức Độ Tin Cậy:** CAO

**Điểm Mạnh Chính:**
1. **Độ Phủ Toàn Diện** — 38 FRs + 16 NFRs đều có hỗ trợ kiến trúc
2. **Lựa Chọn Công Nghệ Cụ Thể** — Versions cụ thể, không mơ hồ
3. **Ranh Giới Rõ Ràng** — Ranh giới service, data, API rõ ràng
4. **Mẫu Nhất Quán** — Quy ước đặt tên + anti-patterns giúp AI agents triển khai nhất quán
5. **Cấu Trúc Monorepo** — Shared code, single source of truth cho types/schemas
6. **Sẵn Sàng Tuân Thủ** — Yêu cầu NĐ 13/2023 được giải quyết (đồng ý, quyền xóa, audit)

**Khu Vực Cải Thiện Tương Lai:**
1. **Tính Năng P2** — WebSocket, biểu đồ xu hướng, thông báo cần thêm chi tiết khi triển khai
2. **Giám Sát Nâng Cao** — Stack Prometheus/Grafana có thể ghi nhận chi tiết hơn
3. **Tinh Chỉnh Hiệu Năng** — Database indexes, query optimization có thể refine sau MVP

### Bàn Giao Triển Khai

**Hướng Dẫn AI Agent:**
1. Tuân theo tất cả quyết định kiến trúc chính xác như ghi nhận
2. Sử dụng mẫu triển khai nhất quán qua tất cả components
3. Tôn trọng cấu trúc dự án và ranh giới
4. Sử dụng shared types từ `packages/shared/` — không bao giờ duplicate
5. Trả về lỗi RFC 7807 — không bao giờ custom formats
6. Validate với Zod schemas — cả frontend và backend
7. Tham khảo tài liệu này cho tất cả câu hỏi kiến trúc

**Ưu Tiên Triển Khai Đầu Tiên:**
```bash
# 1. Khởi tạo cấu trúc monorepo
mkdir -p healthlens/{apps,packages,docker,k8s,scripts,docs}
cd healthlens && pnpm init

# 2. Tạo pnpm-workspace.yaml
echo 'packages:\n  - "apps/*"\n  - "packages/*"' > pnpm-workspace.yaml

# 3. Khởi tạo Spring Boot backend
cd apps && curl https://start.spring.io/starter.zip \
  -d type=gradle-project-kotlin \
  -d language=java \
  -d bootVersion=4.0.3 \
  -d javaVersion=21 \
  -d groupId=com.healthlens \
  -d artifactId=api \
  -d name=healthlens-api \
  -d packageName=com.healthlens.api \
  -d dependencies=web,data-jpa,postgresql,security,oauth2-resource-server,validation,actuator,lombok,devtools,flyway,data-redis,mail \
  -o api.zip && unzip api.zip -d api && rm api.zip

# 4. Khởi tạo Next.js web app
npx create-next-app@latest web \
  --typescript --tailwind --eslint --app --src-dir --import-alias "@/*" --turbopack

# 5. Khởi tạo Expo mobile app
npx create-expo-app@latest mobile --template default@sdk-55

# 6. Khởi tạo shared package
mkdir -p packages/shared/src/{types,schemas,constants}

# 7. Thiết lập Docker Compose cho development local
cp docker/docker-compose.dev.yml docker-compose.dev.yml
docker-compose -f docker-compose.dev.yml up -d
```

**Thứ Tự Triển Khai:**
1. Thiết Lập Dự Án (monorepo, Docker Compose, skeleton GitHub Actions)
2. Schema Database (Flyway migrations V1-V9)
3. Xác Thực (JWT, refresh tokens, Spring Security)
4. API Cốt Lõi (User, Profile, Health Record CRUD)
5. Upload File (tích hợp S3, pre-signed URLs)
6. Pipeline OCR (xử lý async, Redis queue)
7. Tích Hợp LLM (tạo giải thích, caching)
8. Web Dashboard (Next.js, TanStack Query)
9. Mobile App (Expo, offline sync)
10. Admin Panel (dữ liệu tham chiếu, MFA)
