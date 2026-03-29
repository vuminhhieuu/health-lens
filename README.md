# HealthLens Monorepo

Monorepo cho HealthLens gồm backend Spring Boot, web Next.js và mobile Expo.

## Cấu trúc

```text
apps/
  api/      Spring Boot 4.0.3
  web/      Next.js 16
  mobile/   Expo SDK 55
packages/
  shared/   shared types/schemas/constants
docker/
  docker-compose.dev.yml
```

## Yêu cầu môi trường

- Java 21 (khuyến nghị cho `apps/api`)
- Node.js 20+
- pnpm 10+
- Docker Compose

## Thiết lập nhanh

```bash
pnpm install
docker compose -f docker/docker-compose.dev.yml up -d
```

## Chạy dev

```bash
# API
cd apps/api && ./gradlew bootRun --args='--spring.profiles.active=dev'

# Web
cd apps/web && pnpm dev
```

## Mobile (Phase 2)

Story hiện đã scaffold thư mục `apps/mobile` theo Expo SDK 55. Khi bắt đầu Phase 2:

```bash
pnpm install
cd apps/mobile
npx expo start
```
