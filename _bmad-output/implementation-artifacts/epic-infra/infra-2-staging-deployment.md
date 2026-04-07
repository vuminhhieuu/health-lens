---
story_id: "infra-2"
epic: "infra"
story_key: "infra-2-staging-deployment"
title: "Staging Deployment với Vercel + Railway + Neon"
status: "ready-for-dev"
priority: "P3"
created_date: "2026-04-01"
input_artifacts: ["prd.md", "architecture.md", "epics.md"]
---

# Story: Staging Deployment với Vercel + Railway + Neon

## User Story Statement

As a nhóm phát triển,
I want có môi trường staging để test trước khi production,
So that có thể validate changes trong environment giống production trước khi release.

## Business Value

- Test changes trong production-like environment
- Phát hiện sớm issues trước khi release
- Stakeholders có thể review features trước production
- Separation between dev và prod environments

## Technical Context

### Current Architecture (Staging)
- **Web**: Vercel (auto-deploy from `dev` branch)
- **API**: Railway (auto-deploy from `dev` branch)
- **Database**: Neon staging branch
- **Vector DB**: Qdrant Cloud staging cluster
- **AI**: Groq API (shared với dev)

### Staging vs Production

| Component | Staging | Production |
|-----------|---------|------------|
| Web | Vercel (free) | Vercel (pro) hoặc K8s |
| API | Railway ($5/mo) | Railway hoặc K8s |
| Database | Neon staging branch | Neon main branch |
| Domain | staging.healthlens.app | healthlens.app |
| SSL | Auto (Vercel) | Let's Encrypt |

## Requirements

### Functional Requirements
1. **FR-Infra.2.1**: Vercel project configured cho web staging
2. **FR-Infra.2.2**: Railway project configured cho API staging
3. **FR-Infra.2.3**: Neon staging branch created và configured
4. **FR-Infra.2.4**: Environment variables configured for staging
5. **FR-Infra.2.5**: Auto-deploy on `dev` branch push
6. **FR-Infra.2.6**: Custom domain staging.healthlens.app configured
7. **FR-Infra.2.7**: Health monitoring for staging services

### Non-Functional Requirements
- **NFR-Infra.2.1**: Uptime >= 99%
- **NFR-Infra.2.2**: Deployment time < 5 minutes
- **NFR-Infra.2.3**: Staging data isolated from production

## Acceptance Criteria

### Given
Developer push code to `dev` branch

### When
Auto-deploy triggers

### Then
- [ ] Web deploys to Vercel staging
- [ ] API deploys to Railway staging
- [ ] Database connects to Neon staging branch
- [ ] All services healthy
- [ ] Accessible at staging.healthlens.app

### Given
Staging environment

### When
Testing performed

### Then
- [ ] All features work like production
- [ ] Performance similar to production
- [ ] Integration với external services working

## Implementation Details

### Infrastructure Setup

```
┌─────────────────────────────────────────────────────────────┐
│                    STAGING ARCHITECTURE                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   GitHub (dev branch)                                       │
│         ↓                                                    │
│   GitHub Actions                                            │
│         ↓                                                    │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│   │  Vercel    │    │  Railway   │    │    Neon     │     │
│   │  (Web)     │    │   (API)    │    │  (Staging)  │     │
│   └─────────────┘    └─────────────┘    └─────────────┘     │
│         ↓                ↓                  ↓              │
│   staging.web    →  staging.api  →  staging.db            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Environment Configuration

```yaml
# Staging Environment Variables
NODE_ENV=staging
API_BASE_URL=https://staging.api.healthlens.app
DATABASE_URL=postgresql://...@ep-staging-xxx.neon.tech/staging
QDRANT_HOST=https://staging-qdrant.cloud.qdrant.io
```

### Vercel Configuration

```yaml
# vercel.json
{
  "buildCommand": "pnpm build",
  "outputDirectory": "apps/web/out",
  "framework": "nextjs",
  "devCommand": "pnpm dev"
}
```

### Railway Configuration

```yaml
# railway.json
{
  "build": {
    "builder": "GRAALVM_NATIVE"
  },
  "deploy": {
    "numReplicas": 1,
    "restartPolicyType": "ON_FAILURE"
  }
}
```

## Dependencies

- **Pre-requisite**: GitHub Actions workflow (Infra.4) created
- **Pre-requisite**: Vercel account
- **Pre-requisite**: Railway account
- **Pre-requisite**: Neon account

## Testing Checklist

- [ ] Auto-deploy triggers on dev branch push
- [ ] All services accessible
- [ ] Database migrations work
- [ ] Environment variables configured correctly
- [ ] Custom domain works

## Notes

- Staging environment có thể dùng free tiers của Vercel
- Railway cần $5/mo credit - đủ cho staging
- Neon cho phép tạo multiple branches