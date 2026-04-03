---
story_id: "infra-4"
epic: "infra"
story_key: "infra-4-ci-cd-pipeline"
title: "CI/CD Pipeline với GitHub Actions"
status: "ready-for-dev"
priority: "P2"
created_date: "2026-04-01"
input_artifacts: ["prd.md", "architecture.md"]
---

# Story: CI/CD Pipeline với GitHub Actions

## User Story Statement

As a nhóm phát triển,
I want có CI/CD pipeline tự động với GitHub Actions,
So that mỗi lần push code sẽ tự động chạy tests, build, và deploy đến môi trường phù hợp.

## Business Value

- Tự động hóa quy trình release
- Giảm human error trong deployment
- Nhanh chóng phát hiện lỗi qua automated tests
- Traceability rõ ràng từ code đến deployment

## Technical Context

### Current Git Branch Strategy
- `main` → Production
- `dev` → Staging
- `feature/*` → Local dev

### Target Platforms
- **Web**: Vercel
- **API**: Railway
- **Database**: Neon (branch-based)
- **Container Registry**: GitHub Container Registry (GHCR)

## Requirements

### Functional Requirements
1. **FR-Infra.4.1**: Pipeline trigger on push to `main`, `dev`, và `feature/*`
2. **FR-Infra.4.2**: Run tests (unit, integration) trước khi build
3. **FR-Infra.4.3**: Build Docker images for API và Web
4. **FR-Infra.4.4**: Push images to GitHub Container Registry
5. **FR-Infra.4.5**: Deploy to staging on `dev` branch push
6. **FR-Infra.4.6**: Deploy to production on `main` branch push (với approval)
7. **FR-Infra.4.7**: Run security scan (Trivy) on Docker images
8. **FR-Infra.4.8**: Send deployment notifications (Slack/Discord)

### Non-Functional Requirements
- **NFR-Infra.4.1**: Pipeline execution time < 15 phút
- **NFR-Infra.4.2**: Test coverage requirement: > 70%
- **NFR-Infra.4.3**: Artifacts retained: 30 days

## Acceptance Criteria

### Given
Khi có push đến `dev` branch

### When
CI/CD pipeline triggers

### Then
- [ ] Tests run và pass
- [ ] Docker images build successfully
- [ ] Images pushed to GHCR
- [ ] Deploy to Railway staging automatic
- [ ] Notification sent

### Given
Khi có push đến `main` branch

### When
CI/CD pipeline triggers

### Then
- [ ] Tests run và pass
- [ ] Docker images build successfully
- [ ] Manual approval required trước deploy
- [ ] Deploy to production
- [ ] Deployment logged

### Given
Khi có pull request

### When
CI/CD pipeline triggers

### Then
- [ ] Run tests
- [ ] Build check
- [ ] Report status to PR

## Implementation Details

### GitHub Actions Workflows

```yaml
.github/
├── workflows/
│   ├── ci.yml           # Test & Build (runs on all branches)
│   ├── deploy-staging.yml  # Deploy to staging (on dev push)
│   └── deploy-production.yml # Deploy to production (on main push, manual approval)
└── README.md
```

### Pipeline Stages

```
┌─────────────────────────────────────────────────────────────┐
│                    CI/CD PIPELINE                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. LINT & ANALYZE                                          │
│     ├── ESLint (Web)                                        │
│     ├── Checkstyle (API)                                    │
│     └── Trivy (Security scan)                               │
│         ↓                                                    │
│  2. TEST                                                    │
│     ├── Unit Tests (Web + API)                              │
│     ├── Integration Tests                                   │
│     └── Coverage Report                                     │
│         ↓                                                    │
│  3. BUILD                                                   │
│     ├── Build Web (Next.js)                                 │
│     ├── Build API (Spring Boot)                             │
│     └── Build Docker Images                                │
│         ↓                                                    │
│  4. PUSH                                                    │
│     └── Push to GHCR (with tag)                             │
│         ↓                                                    │
│  5. DEPLOY                                                  │
│     ├── Staging: Auto-deploy on dev                        │
│     └── Production: Manual approval required               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Environment Variables Required

```yaml
# Secrets (GitHub Secrets)
NEON_DATABASE_URL: ${{ secrets.NEON_DATABASE_URL }}
NEON_STAGING_URL: ${{ secrets.NEON_STAGING_URL }}
RAILWAY_TOKEN: ${{ secrets.RAILWAY_TOKEN }}
VERCEL_TOKEN: ${{ secrets.VERCEL_TOKEN }}
DOCKER_REGISTRY_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Deployment Flow

| Branch | Trigger | Action |
|--------|---------|--------|
| `feature/*` | Push | Run tests only |
| `dev` | Push | Deploy to staging auto |
| `main` | Push | Deploy to production (approval) |
| PR | Opened | Run tests, report status |

## Dependencies

- **Pre-requisite**: GitHub repository
- **Pre-requisite**: GHCR enabled
- **Pre-requisite**: Vercel project connected
- **Pre-requisite**: Railway project connected

## Testing Checklist

- [ ] Pipeline triggers on correct branches
- [ ] Tests run và pass
- [ ] Images build successfully
- [ ] Staging deploy works
- [ ] Production deploy requires approval

## Notes

- Use GitHub Environments để protect production deployment
- Store secrets in GitHub Secrets, never in code
- Implement caching để speed up pipeline