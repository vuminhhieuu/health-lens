# Story 8.1: CI Security Scan Baseline

Status: done

## Execution Scope

**Area:** CI/CD security gates — secret scan, dependency scan (PR); container image scan (deploy `main`)  
**Priority:** P1  
**Linear:** LIN-167

## Story

As a release owner, I want automated security scans in CI, so that high-risk issues (secrets, vulnerable dependencies, vulnerable images) are caught before deployment.

## Acceptance Criteria

| # | Criterion | Implementation |
|---|-----------|----------------|
| 1 | Scan results visible on PR checks | Workflow **Security** on PR: `secret-scan`, `dependency-scan (java-api \| node-monorepo \| python-ocr)` |
| 2 | Critical/High vulnerability → CI fails | Trivy FS gate: `--severity CRITICAL,HIGH`, `--ignore-unfixed`, `--exit-code 1` |
| 3 | Docker image → critical/high reported | Job **`scan-images`** in `deploy.yml` on `main` (Trivy image scan on GHCR tags after `build`) |
| 4 | Secret commit → block per policy | Job **`secret-scan`** (Gitleaks + `.gitleaks.toml`) |

## Design decisions (LIN-167)

PR workflow is intentionally **lightweight** for daily dev (~4–6 min):

- **On PR / push to `dev`:** `secret-scan` + `dependency-scan` only.
- **Not on PR:** SAST (Semgrep) and per-PR Docker build+image scan — removed to avoid 15–45 min runs and overlap with dependency scan.
- **On `main` deploy:** `scan-images` gates production deploy (API / Web / OCR images in GHCR).

Tools: OSS only (Gitleaks, Trivy). No Sonar/Snyk tokens.

## Tasks / Subtasks

- [x] Add dependency scanning for Java / Node (pnpm monorepo root) / Python (OCR service).
- [x] Add secret scanning (Gitleaks).
- [x] Configure severity gates (CRITICAL, HIGH) and SARIF artifacts.
- [x] Add Docker image scan for API / Web / OCR on **`main`** deploy pipeline (not on every PR).
- [x] Harden Dockerfiles (`apk upgrade` / `apt-get upgrade`, pip bumps for OCR).
- [x] Align vulnerable dependencies (axios, next, Netty, pnpm overrides).
- [x] Remove stale `apps/web/pnpm-lock.yaml` (monorepo uses root `pnpm-lock.yaml` only).

## Workflows

### `.github/workflows/security.yml`

| Job | Tool | Target |
|-----|------|--------|
| `secret-scan` | Gitleaks | Full repo history (`fetch-depth: 0`) |
| `dependency-scan (java-api)` | Trivy FS | `apps/api` |
| `dependency-scan (node-monorepo)` | Trivy FS | `.` (root lock; skips `apps/mobile`) |
| `dependency-scan (python-ocr)` | Trivy FS | `services/ocr-service` |

Triggers: `pull_request` (path filters), `push` to `main` / `develop` / `dev`, `workflow_dispatch`.

### `.github/workflows/deploy.yml`

| Job | When | Role |
|-----|------|------|
| `scan-images` | `main` only, after `build` | Trivy image scan on GHCR; fail on CRITICAL/HIGH |
| `deploy-production` | `main` | `needs: [build, scan-images]` |

## Config files

| File | Purpose |
|------|---------|
| `.gitleaks.toml` | Secret scan rules + allowlist (`.env.example`, `_bmad-output`, docs) |
| `.trivy.yaml` | Shared policy for local runs (`trivy fs --config .trivy.yaml <path>`) |
| `.trivyignore` | Optional CVE suppressions (empty baseline) |

## Dependency / lockfile notes

- Single lockfile: **`pnpm-lock.yaml`** at repo root (`pnpm-workspace.yaml`).
- Root `package.json` `pnpm.overrides`: `axios@1.15.2`, `next@16.2.6`, `@xmldom/xmldom@0.8.13`.
- Do **not** commit `apps/web/pnpm-lock.yaml` — causes Trivy false positives (outdated axios/next).

## Dev Notes

- Supersedes old story `epic-10/10-9-ci-cd-security-integration.md`.
- `ci.yml`: extended path triggers (`services/**`, `.gitleaks.toml`, `.trivy.yaml`).
- Java: `build.gradle.kts` pins Netty `4.2.13.Final` and `grpc-netty-shaded` `1.75.0` for scan alignment.

## References

- Old source: `_bmad-output/implementation-artifacts/epic-10/10-9-ci-cd-security-integration.md`
- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md` Story 8.1

## Dev Agent Record

### Agent Model Used

Composer

### Completion Notes List

- Added `.github/workflows/security.yml`: PR/dev gates — Gitleaks + Trivy filesystem (Java, pnpm monorepo, Python).
- Extended `.github/workflows/deploy.yml`: `scan-images` on `main` before `deploy-production`; updated `notify-failure`.
- Extended `.github/workflows/ci.yml` path triggers for OCR/services and security config files.
- Added `.gitleaks.toml`, `.trivy.yaml`, `.trivyignore`.
- Upgraded web deps (`axios`, `next`), root pnpm overrides, refreshed `pnpm-lock.yaml`.
- Removed duplicate `apps/web/pnpm-lock.yaml`.
- Dockerfile hardening (api, web, ocr-service); API Gradle resolution for Netty/gRPC.
- Fixed invalid `${{ env.* }}` in workflow matrix (use literal `skip_dirs` in matrix).
- SAST (Semgrep) and PR-time container build+scan **not** shipped — container coverage via deploy `scan-images` on `main`.

### File List

- `.github/workflows/security.yml`
- `.github/workflows/deploy.yml`
- `.github/workflows/ci.yml`
- `.gitleaks.toml`
- `.trivy.yaml`
- `.trivyignore`
- `package.json`
- `pnpm-lock.yaml`
- `apps/web/package.json`
- `apps/mobile/package.json`
- `apps/api/build.gradle.kts`
- `apps/api/Dockerfile`
- `apps/web/Dockerfile`
- `services/ocr-service/Dockerfile`

### Local verification (optional)

```bash
gitleaks detect --source . --config .gitleaks.toml

trivy fs --severity CRITICAL,HIGH --ignore-unfixed apps/api
trivy fs --severity CRITICAL,HIGH --ignore-unfixed .
trivy fs --severity CRITICAL,HIGH --ignore-unfixed services/ocr-service
```
