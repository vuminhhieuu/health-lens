# HealthLens

**Nền tảng quản lý sức khỏe thông minh cho người Việt Nam**

Chuyển đổi kết quả xét nghiệm y tế (PDF, ảnh) thành thông tin sức khỏe dễ hiểu, sử dụng OCR + LLM explanation + family sharing network.

---

## Status

| Phase | Status |
|-------|--------|
| Planning | ✅ Complete |
| Architecture | ✅ Complete |
| Implementation | 🔄 In Progress |

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Spring Boot 4.0.3 (Java 21), PostgreSQL 16, Redis 7 |
| **Web** | Next.js 16, TypeScript, Tailwind CSS, Radix UI |
| **Mobile** | Expo SDK 55, React Native |
| **Infrastructure** | Docker, Kubernetes, GitHub Actions |

---

## Project Structure

```
health-lens/
├── _bmad/                      # BMad framework configuration
├── _bmad-output/               # Planning & implementation artifacts
│   ├── planning-artifacts/     # PRD, Architecture, Epics, UX
│   └── implementation-artifacts/
├── _pages/                     # GitHub Pages (project website)
├── design-artifacts/           # UX/Design specifications
├── docs/                       # Project knowledge for AI agents
├── apps/                       # (Coming) Application code
│   ├── api/                    # Spring Boot backend
│   ├── web/                    # Next.js web app
│   └── mobile/                 # Expo mobile app
├── packages/                   # (Coming) Shared TypeScript packages
└── libs/                       # (Coming) Shared backend libraries
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [Project Knowledge](./docs/index.md) | Navigation hub for all documentation |
| [Repository Structure](./FINAL-MERGED-REPOSITORY-STRUCTURE.md) | Complete monorepo structure definition |
| [PRD](/_bmad-output/planning-artifacts/prd.md) | Product Requirements Document |
| [Architecture](/_bmad-output/planning-artifacts/architecture.md) | Technical Architecture & ADRs |

---

## Quick Start

> **Note:** Project đang ở giai đoạn pre-implementation. Source code sẽ được thêm vào khi bắt đầu Phase 4.

```bash
# Clone repository
git clone <repo-url>
cd health-lens

# Xem documentation
open docs/index.md
```

---

## Development

### Prerequisites
- Java 21+
- Node.js 20+
- pnpm 9+
- Docker & Docker Compose

### Setup (Coming Soon)
```bash
# Install dependencies
pnpm install

# Start development environment
docker-compose -f docker/docker-compose.dev.yml up -d
pnpm dev
```

---

## Team

- **IE303** - UIT Software Engineering Course Project

---

## License

Private - All rights reserved
