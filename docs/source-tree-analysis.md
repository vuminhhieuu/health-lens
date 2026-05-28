# Source Tree Analysis

**Last updated:** 2026-05-16

```text
health-lens/
├── apps/
│   ├── api/                  # Spring Boot backend API
│   │   ├── src/main/java/com/healthlens/api/
│   │   │   ├── annotation/   # Auditing and consent annotations
│   │   │   ├── aspect/       # Auditable/consent enforcement aspects
│   │   │   ├── config/       # Security, OCR, AI, storage, scheduler config
│   │   │   ├── constants/    # Backend route constants
│   │   │   ├── controller/   # REST controllers
│   │   │   ├── dto/          # API DTOs and OCR/reference-data payloads
│   │   │   ├── entity/       # JPA entities
│   │   │   ├── exception/    # Domain and global exception handling
│   │   │   ├── repository/   # Spring Data repositories
│   │   │   ├── security/     # JWT, rate limiters, account status cache
│   │   │   ├── service/      # Business logic and integrations
│   │   │   └── util/         # JWT utilities
│   │   └── src/main/resources/
│   │       ├── db/migration/ # Flyway migrations V001-V027
│   │       ├── ai/           # Vietnamese metric explanation seed data
│   │       └── templates/    # Email templates
│   ├── web/                  # Next.js frontend
│   │   └── src/
│   │       ├── app/          # App Router pages and layouts
│   │       ├── components/   # Feature, layout, and UI components
│   │       ├── hooks/        # Client hooks
│   │       ├── lib/          # API client and consent helpers
│   │       └── stores/       # Zustand auth state
│   └── mobile/               # Expo mobile app, currently lighter than web
│       ├── app/              # Expo Router entry
│       └── src/              # Mobile routes, components, hooks, theme
├── packages/
│   └── shared/               # Shared TypeScript constants, schemas, types
├── services/
│   └── ocr-service/          # FastAPI EasyOCR microservice
├── docker/
│   ├── compose.yml           # Base Redis/MinIO services
│   └── compose.dev.yml       # Local API/web/Postgres/Mailhog/OCR stack
├── scripts/
│   ├── db/                   # DB analysis and migration helper scripts
│   └── docker/               # up/logs/down/cleanup helper scripts
├── docs/                     # Project knowledge for humans and AI agents
├── _bmad-output/             # BMad planning and implementation artifacts
└── .github/workflows/        # CI and deployment workflows
```

## Critical Entry Points

| Area | Entry point |
| --- | --- |
| Repository scripts | `package.json` |
| Web app | `apps/web/src/app/layout.tsx`, `apps/web/src/app/page.tsx` |
| Web API client | `apps/web/src/lib/api/apiClient.ts` |
| Shared API paths | `packages/shared/constants/api.ts` |
| API app | `apps/api/src/main/java/com/healthlens/api/HealthLensApplication.java` |
| API route registry | `apps/api/src/main/java/com/healthlens/api/constants/ApiRoutes.java` |
| API config | `apps/api/src/main/resources/application.yml` |
| OCR service | `services/ocr-service/app.py` |
| Local stack | `docker/compose.yml`, `docker/compose.dev.yml`, `scripts/docker/up.sh` |

## Generated/Excluded Paths

Avoid treating these as source of truth during code analysis:

- `node_modules/`
- `.next/`
- `.gradle/`
- `apps/api/build/`
- `services/ocr-service/__pycache__/`
