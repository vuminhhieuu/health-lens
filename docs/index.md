# HealthLens Project Knowledge

**Status:** Phase 4 - Implementation (Pre-code)  
**Last Updated:** March 28, 2026

---

## Project Overview

HealthLens là nền tảng giúp người dùng Việt Nam chuyển đổi kết quả xét nghiệm y tế (PDF, ảnh) thành thông tin sức khỏe dễ hiểu, sử dụng OCR + LLM explanation + family sharing network.

---

## Planning Artifacts

Tất cả planning documents được generate bởi BMad workflows:


| Document            | Mô tả                                  | Link                                                                                        |
| ------------------- | -------------------------------------- | ------------------------------------------------------------------------------------------- |
| **PRD**             | Product Requirements (38 FRs, 16 NFRs) | [prd.md](../_bmad-output/planning-artifacts/prd.md)                                         |
| **Architecture**    | Technical Architecture (8 ADRs)        | [architecture.md](../_bmad-output/planning-artifacts/architecture.md)                       |
| **Epics & Stories** | 8 Epics, 40+ User Stories              | [epics.md](../_bmad-output/planning-artifacts/epics.md)                                     |
| **UX Design**       | 40+ Screen Specifications              | [ux-design-specification.md](../_bmad-output/planning-artifacts/ux-design-specification.md) |
| **PRD Validation**  | Validation Report                      | [prd-validation-report.md](../_bmad-output/planning-artifacts/prd-validation-report.md)     |


---

## Implementation Artifacts

Story files và sprint tracking:


| Document          | Mô tả                      | Link                                                                                  |
| ----------------- | -------------------------- | ------------------------------------------------------------------------------------- |
| **Sprint Status** | Current sprint tracking    | [sprint-status.yaml](../_bmad-output/implementation-artifacts/sprint-status.yaml)     |
| **Dev Handoff**   | Developer onboarding guide | [dev-handoff-guide.md](../_bmad-output/implementation-artifacts/dev-handoff-guide.md) |
| **Story Files**   | Individual story specs     | [implementation-artifacts/](../_bmad-output/implementation-artifacts/)                |


---

## Design Artifacts

UX/Design specifications và implementation guides:


| Document             | Mô tả                       | Link                                                                                   |
| -------------------- | --------------------------- | -------------------------------------------------------------------------------------- |
| **Design System**    | Component library & tokens  | [DESIGN-SYSTEM-PLAN.md](../design-artifacts/DESIGN-SYSTEM-PLAN.md)                     |
| **Tokens Reference** | Colors, typography, spacing | [TOKENS-QUICK-REFERENCE.md](../design-artifacts/TOKENS-QUICK-REFERENCE.md)             |
| **Screens List**     | All 40+ screens catalog     | [SCREENS-FULL-LIST-VI.md](../design-artifacts/SCREENS-FULL-LIST-VI.md)                 |
| **Phase 1 Guide**    | Implementation roadmap      | [PHASE-1-IMPLEMENTATION-GUIDE.md](../design-artifacts/PHASE-1-IMPLEMENTATION-GUIDE.md) |


---

## Tech Stack

### Backend

- **Framework:** Spring Boot 4.0.3 (Java 21)
- **Database:** PostgreSQL 16 + Redis 7
- **Storage:** AWS S3 / MinIO

### Web

- **Framework:** Next.js 16 (TypeScript)
- **UI:** Tailwind CSS + Radix UI
- **State:** TanStack Query + Zustand

### Mobile

- **Framework:** Expo SDK 55 (React Native)
- **Storage:** SQLite (offline) + Secure Store

---

## Code Documentation

> **Note:** Phần này sẽ được populate sau khi có code bằng `bmad-document-project` và `bmad-generate-project-context`.

- `project-context.md` - (Sẽ tạo sau) LLM-optimized rules & patterns
- `architecture/` - (Sẽ tạo sau) Code architecture docs
- `api/` - (Sẽ tạo sau) API documentation
- `guides/` - (Sẽ tạo sau) Developer guides

---

## Quick Links

- [Repository Structure](../FINAL-MERGED-REPOSITORY-STRUCTURE.md)
- [GitHub Pages](./_pages/) - Project website
- [BMad Config](../_bmad/bmm/config.yaml)
