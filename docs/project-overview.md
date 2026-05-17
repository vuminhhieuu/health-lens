# HealthLens Project Overview

**Last updated:** 2026-05-16

## Purpose

HealthLens is a Vietnamese healthcare document processing platform. Users upload medical documents, the system extracts text and health metrics through OCR, stores normalized health records, and returns AI-assisted explanations that are easier for non-specialists to understand.

## Repository Type

Monorepo with separate deployable parts:

| Part | Path | Type | Main stack |
| --- | --- | --- | --- |
| Web app | `apps/web` | Next.js frontend | Next.js 16, React 19, TypeScript, Tailwind CSS, Radix UI, TanStack Query, Zustand |
| Mobile app | `apps/mobile` | Expo app | Expo SDK 55, React Native 0.83, Expo Router, TypeScript |
| API | `apps/api` | Backend API | Spring Boot 4, Java 21, Gradle, JPA, Redis, Flyway, Spring Security |
| OCR service | `services/ocr-service` | Python microservice | FastAPI, EasyOCR, Pillow, NumPy |
| Shared package | `packages/shared` | Shared TS contracts | TypeScript, Zod |
| Infrastructure | `docker`, `.github/workflows` | Local/dev/CI support | Docker Compose, shell scripts, GitHub Actions |

## Core Capabilities

- Authentication, email verification, refresh-token sessions, password reset, account deletion flow.
- User and family profile management.
- Profile invitation and sharing with access revocation audit paths.
- Health record upload through presigned object-storage URLs.
- OCR extraction and metric confirmation flow.
- Reference metric/range management and admin approval workflow.
- AI explanations using OpenAI-compatible chat configuration plus optional vector retrieval.
- Local development stack for PostgreSQL, Redis, MinIO, Mailhog, API, web, and optional OCR service.

## High-Level Flow

1. User authenticates through the web app.
2. Web app calls the Spring API through `NEXT_PUBLIC_API_BASE_URL`.
3. API persists users, profiles, health records, reference data, audit logs, and workflow state in PostgreSQL.
4. API uses Redis for cache/streams and account/session supporting behavior.
5. Uploaded files are stored in MinIO locally or S3-compatible storage in deployed environments.
6. OCR is handled either through provider integrations in the API or the FastAPI EasyOCR microservice.
7. AI explanations use Spring AI with an OpenAI-compatible chat provider and optional embedding/vector retrieval.

## Primary Documentation

- [Architecture](./architecture.md)
- [API Contracts](./api-contracts.md)
- [Data Models](./data-models.md)
- [Component Inventory](./component-inventory.md)
- [Development Guide](./development-guide.md)
- [Deployment Guide](./deployment-guide.md)
- [Source Tree Analysis](./source-tree-analysis.md)
- [Project Context](./project-context.md)
