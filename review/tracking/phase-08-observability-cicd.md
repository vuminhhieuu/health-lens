# Phase 8 — Observability + CI/CD Hardening

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 8](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 14h (tuần 12)
> **Goal**: Production observability + CI/CD security + API consistency.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/08-observability`
- **PR(s)**: — (4 PRs)

---

## ✅ Pre-flight Checklist

- [ ] Đã đọc `review_gaps.md` §7, §13
- [ ] Đã đọc `runtime_security_and_correctness_audit.md` §6.4, §7
- [ ] Sentry account ready
- [ ] Branch: `git checkout -b phase/08-observability main`

---

## 🎯 Tasks

### 8.1 Micrometer Custom Metrics (2h)
- [ ] `@Timed` cho hot path (OCR pipeline, healthrecord query)
- [ ] Counter cho OCR provider success/failure
- [ ] Gauge cho pending OCR jobs
- [ ] Histogram cho LLM response latency

### 8.2 Prometheus Endpoint (30m)
- [ ] Add `io.micrometer:micrometer-registry-prometheus`
- [ ] Expose `/actuator/prometheus`
- [ ] Restrict access (internal network) trong SecurityConfig
- [ ] Test: `curl /actuator/prometheus | grep healthlens_`

### 8.3 Sentry SDK (2h)
- [ ] Java: `sentry-spring-boot-starter-jakarta` + DSN env
- [ ] Web: `@sentry/nextjs` + DSN
- [ ] Filter sensitive data trước gửi
- [ ] Test error capture

### 8.4 JaCoCo Coverage (2h)
- [ ] Config JaCoCo trong `build.gradle.kts`
- [ ] Threshold 60% (ramp up over time)
- [ ] Upload Codecov hoặc internal
- [ ] CI fail nếu < threshold

### 8.5 Trivy Docker Scan (1h)
- [ ] Trivy step trong `.github/workflows/deploy.yml`
- [ ] Scan API + OCR images
- [ ] Fail on HIGH/CRITICAL CVEs
- [ ] Whitelist false positives

### 8.6 Lighthouse CI (1h)
- [ ] `.github/workflows/lighthouse.yml`
- [ ] Run trên 5 key pages
- [ ] Assert: a11y ≥ 90, perf ≥ 70

### 8.7 Staging Deploy Workflow (1h)
- [ ] `.github/workflows/staging-deploy.yml`
- [ ] Trigger: push to `staging` branch
- [ ] Separate Infisical env

### 8.8 OpenAPI Codegen (2h)
- [ ] Gradle task `generateOpenApi` export to `docs/openapi.json`
- [ ] npm script generate TS client
- [ ] CI verify: no drift
- [ ] (Later) eliminate manual API route sync

### 8.9 ApiResponse<T> Envelope (2h) ⚠️ NEW
- [ ] Define record:
  ```java
  public record ApiResponse<T>(T data, Pagination pagination, Map<String, Object> meta) {
      public static <T> ApiResponse<T> of(T data) { ... }
      public static <T> ApiResponse<T> paged(T data, Pagination p) { ... }
  }
  ```
- [ ] Migrate 5 controllers (Profile, HealthRecord, Auth, Notification, AdminAnalytics)
- [ ] Web `apiClient` parse `.data`
- [ ] Document `docs/api-contracts.md`

### 8.10 Data Retention Policy (decision + 4h) ⚠️ NEW
- [ ] Stakeholder decision → [ADR-004](DECISIONS.md#adr-004):
  - A: Auto-delete N năm
  - B: User-controlled only
  - C: Tiered
- [ ] Nếu A:
  - Flyway migration: `archived_at` column
  - Scheduler (với ShedLock từ Phase 1.2): mark records ≥ N năm
  - Email notification 30 ngày trước
  - Update privacy policy
- [ ] Document `docs/data-retention-policy.md`

---

## 🧪 Verification

```bash
# Metrics
curl /actuator/prometheus | grep "healthlens_"

# Sentry
# Trigger /dev/throw-test → Sentry dashboard

# Coverage
./gradlew jacocoTestReport
# HTML report ≥ 60%

# Trivy
trivy image healthlens-api:latest

# Lighthouse
gh workflow run lighthouse.yml

# API envelope
curl /api/profiles | jq '.data, .pagination'
```

---

## 🏁 Phase Completion Checklist

- [ ] 10 task `[x]`
- [ ] `/actuator/prometheus` returns metrics
- [ ] Sentry receives test event
- [ ] JaCoCo ≥ 60% CI
- [ ] Trivy in deploy workflow
- [ ] Lighthouse CI on PR
- [ ] Staging deploy separate
- [ ] OpenAPI spec auto-exported
- [ ] ApiResponse in 5+ controllers
- [ ] ADR-004 logged
- [ ] Update DASHBOARD

---

## 📝 Retrospective

### Effort
| Task | Est | Actual |
|------|:-:|:-:|
| 8.1–8.3 Metrics + Sentry | 4.5h | — |
| 8.4–8.7 CI hardening | 5h | — |
| 8.8 OpenAPI | 2h | — |
| 8.9 Envelope | 2h | — |
| 8.10 Retention | 4h | — |
| **Total** | **17.5h** | — |

_Note: Plan est 14h — adjust based on actual_
