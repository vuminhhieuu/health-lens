# Story 1.1: Thiết lập dự án ban đầu từ starter template

Status: ready-for-dev

## Execution scope

**Phase 1 — Web MVP:** Story thuộc Epic 1–8 — hoàn thiện **web trước**. Các task **Mobile** trong story (nếu có) là **Phase 2**, chỉ làm sau khi Web MVP đóng.

## Stitch — giao diện tham chiếu (Google Stitch)

- **Dự án:** HealthLens-Web-MVP — `projectId`: `578519912546445367`
- **Chỉ mục đầy đủ:** [STITCH-SCREEN-LINKS.md](../STITCH-SCREEN-LINKS.md)

Không có screen Stitch tương ứng trong project Web MVP này (story kỹ thuật / mobile phase 2).

*Ghi chú:* Link HTML prototype và screenshot tải từ Google Stitch có thể hết hạn. Làm mới snapshot: MCP `list_screens` (project HealthLens-Web-MVP, `projectId` trên) rồi ghi đè `_data/stitch-screens.json`, sau đó chạy lại script này.

## Story

As a nhóm phát triển sản phẩm,
I want khởi tạo monorepo từ starter template chuẩn kiến trúc,
so that **Phase 1** có thể triển khai nhất quán **web và backend**; **mobile (Phase 2)** bám cùng kiến trúc monorepo sau khi Web MVP đóng.

## Acceptance Criteria

1. **Given** repository trống hoặc chưa chuẩn hóa cấu trúc, **When** thực hiện setup theo starter template đã chọn trong architecture, **Then** tạo đầy đủ `apps/api`, `apps/web`, `packages/shared` cùng cấu hình build mặc định **và** thư mục `apps/mobile` (placeholder hoặc scaffold tối thiểu; Expo đầy đủ có thể hoãn tới Phase 2 — Task 4).
2. **Given** monorepo đã khởi tạo, **When** chạy lệnh dev, **Then** `apps/api` và `apps/web` khởi động thành công ở môi trường dev (**điều kiện đóng Phase 1**). Ứng dụng Expo trong `apps/mobile` khởi động được khi Task 4 (Phase 2) đã làm; nếu Task 4 chưa làm, ghi rõ trong README cách bổ sung mobile sau.
3. **Given** cấu trúc monorepo, **When** kiểm tra các app, **Then** `packages/shared/types/`, `packages/shared/schemas/`, `packages/shared/constants/` đã tồn tại với cấu trúc TypeScript hợp lệ.
4. **Given** project đã setup, **When** chạy **GitHub Actions** workflow skeleton (CI), **Then** stage `test` và `build` pass mà không có lỗi.
5. **Given** môi trường dev, **When** chạy `docker-compose -f docker/docker-compose.dev.yml up`, **Then** PostgreSQL 16, Redis 7, và MinIO đều khởi động thành công.

## Tasks / Subtasks

- [ ] Task 1 — Khởi tạo monorepo root và Gradle Kotlin DSL wrapper (AC: #1)
  - [ ] Tạo thư mục root: `apps/api`, `apps/web`, `apps/mobile`, `packages/shared`, `docker/`, `docs/`
  - [ ] Tạo `settings.gradle.kts` tại root với các subprojects
  - [ ] Tạo `.gitignore` phù hợp với Gradle, Node, Expo và IntelliJ
- [ ] Task 2 — Khởi tạo Spring Boot 4.0.3 backend (AC: #1, #2)
  - [ ] Chạy lệnh Spring Initializr đặc biệt trong `apps/api/` với đúng dependencies
  - [ ] Xác minh `apps/api` `./gradlew bootRun` khởi động thành công
  - [ ] Cấu hình `application.yml` cho profile `dev` với datasource, Redis, và placeholder OCR/LLM keys
  - [ ] Tạo `apps/api/src/main/java/com/healthlens/api/HealthLensApplication.java`
- [ ] Task 3 — Khởi tạo Next.js 16.x web app (AC: #1, #2)
  - [ ] Chạy `npx create-next-app@latest` với đúng flags (TypeScript, Tailwind, App Router, Turbopack)
  - [ ] Cài thêm: `@tanstack/react-query`, `axios`, `zustand`, `@radix-ui/themes`, `lucide-react`, `react-hook-form`, `zod`, `@hookform/resolvers`, `date-fns`
  - [ ] Xác minh `pnpm dev` khởi động thành công
  - [ ] Tạo cấu trúc thư mục: `src/app/(auth)/`, `src/app/(dashboard)/`, `src/components/ui/`, `src/components/features/`, `src/hooks/`, `src/lib/api/`, `src/stores/`
- [ ] Task 4 — **Phase 2 — Mobile:** Khởi tạo Expo SDK 55 trong `apps/mobile/` (AC: #1, #2 khi triển khai mobile)
  - [ ] Chạy `npx create-expo-app@latest healthlens-mobile --template default@sdk-55` trong `apps/mobile/`
  - [ ] Cài thêm: `expo-router`, `expo-camera`, `expo-document-picker`, `expo-image-picker`, `expo-sqlite`, `expo-secure-store`, `expo-notifications`
  - [ ] Cài shared: `@tanstack/react-query`, `axios`, `zustand`, `react-hook-form`, `zod`, `@hookform/resolvers`
  - [ ] Xác minh `npx expo start` khởi động thành công
  - [ ] Tạo cấu trúc: `app/`, `components/`, `hooks/`, `lib/api/`, `stores/`
- [ ] Task 5 — Khởi tạo packages/shared (AC: #3)
  - [ ] Tạo `packages/shared/package.json` với TypeScript config
  - [ ] Tạo `packages/shared/types/index.ts` với placeholder types (User, Profile, HealthRecord)
  - [ ] Tạo `packages/shared/schemas/index.ts` với placeholder Zod schemas
  - [ ] Tạo `packages/shared/constants/index.ts` với status enums, error codes
  - [ ] Cấu hình tsconfig để web và mobile có thể import từ `@healthlens/shared`
- [ ] Task 6 — Setup Docker Compose dev environment (AC: #5)
  - [ ] Tạo `docker/docker-compose.dev.yml` với PostgreSQL 16, Redis 7, MinIO
  - [ ] Cấu hình PostgreSQL: database `healthlens_dev`, user `healthlens`, port 5432
  - [ ] Cấu hình Redis: port 6379, không password (dev only)
  - [ ] Cấu hình MinIO: port 9000 (API), 9001 (Console)
  - [ ] Thêm healthchecks cho từng service
- [ ] Task 7 — Setup GitHub Actions CI skeleton (AC: #4)
  - [ ] Tạo workflow (ví dụ `.github/workflows/ci.yml`) với jobs tương đương stages: `test`, `build`
  - [ ] Thêm job `api:test` — `./gradlew test`
  - [ ] Thêm job `web:test` — `pnpm test`
  - [ ] Thêm job `api:build` — `./gradlew bootBuildImage` (hoặc build jar/image tùy chọn)
  - [ ] (Tùy chọn) `deploy:staging` / `deploy:production` — manual hoặc environment protection trên GitHub
- [ ] Task 8 — Tạo README.md và .env.example (AC: #1)
  - [ ] `README.md` root với hướng dẫn setup local
  - [ ] `.env.example` với tất cả required env vars (DB, Redis, MinIO, OCR, LLM, JWT)
  - [ ] Tạo `docs/` folder với placeholder architecture doc

## Dev Notes

### Kiến Trúc Monorepo

Dự án sử dụng **monorepo structure** với Gradle Kotlin DSL cho backend và pnpm workspaces cho frontend:

```
healthlens/
├── apps/
│   ├── api/              # Spring Boot 4.0.3, Java 21, Gradle Kotlin DSL
│   ├── web/              # Next.js 16.x, TypeScript, Tailwind 4.x, App Router
│   └── mobile/           # Expo SDK 55, React Native, TypeScript
├── packages/
│   └── shared/           # Shared TypeScript types, Zod schemas, constants
├── docker/
│   ├── docker-compose.yml
│   └── docker-compose.dev.yml
└── docs/
```

### Lệnh Khởi Tạo Chính Xác

**Backend (chạy từ `apps/api/`):**
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
unzip healthlens-api.zip -d apps/api/
```

**Web (chạy từ `apps/web/`):**
```bash
npx create-next-app@latest . \
  --typescript --tailwind --eslint --app --src-dir \
  --import-alias "@/*" --turbopack
```

**Mobile (chạy từ `apps/mobile/`):**
```bash
npx create-expo-app@latest . --template default@sdk-55
```

### Package Structure — Shared

```typescript
// packages/shared/types/index.ts — Minimal stubs cho Story 1.1
export interface User { id: string; email: string; }
export interface Profile { id: string; userId: string; displayName: string; }
export interface HealthRecord { id: string; profileId: string; examDate: string; }
```

### Docker Compose Dev

Credentials chuẩn cho dev environment:
- PostgreSQL: `postgresql://healthlens:healthlens@localhost:5432/healthlens_dev`
- Redis: `redis://localhost:6379`
- MinIO: endpoint `http://localhost:9000`, access key `minioadmin`, secret key `minioadmin`

### GitHub Actions (CI)

Không cần Container Registry thực tế cho story này — chỉ cần workflow skeleton với các job `test` / `build` pass được trên PR.

### Project Structure Notes

- Root package: `com.healthlens.api` — **không thay đổi**
- App Router config: `src/app/` — **không thay đổi sang `pages/`**
- Expo Router: sử dụng file-based routing trong `app/` folder
- pnpm workspaces: cấu hình trong `pnpm-workspace.yaml` ở root

### References

- [Source: architecture.md#Đánh-Giá-Template-Khởi-Tạo]
- [Source: architecture.md#Tổ-Chức-Code]
- [Source: architecture.md#Cơ-Sở-Hạ-Tầng]
- [Source: architecture.md#CI/CD-Pipeline]
- [Source: epics.md#Story-1.1]

## Dev Agent Record

### Agent Model Used

_[To be filled by dev agent]_

### Debug Log References

### Completion Notes List

### File List
