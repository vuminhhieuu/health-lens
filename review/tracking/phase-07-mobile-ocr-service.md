# Phase 7 — Mobile + OCR Service

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 7](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 14h (tuần 11)
> **Goal**: Mobile decision gate + OCR service từ monolith app.py → modules.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/07-mobile-ocr`
- **PR(s)**: — (3 PRs)

---

## ✅ Pre-flight Checklist

- [ ] Đã đọc `folder_organization_solution.md` §4, §5
- [ ] Đã đọc `review_gaps.md` §1.1, §11.1
- [ ] **Decision needed** ([ADR-003](DECISIONS.md#adr-003)): mobile build vs drop
- [ ] Branch: `git checkout -b phase/07-mobile-ocr main`

---

## 🎯 Phase 7A — Mobile (~4h)

### 7A.1 Decision Gate
- [ ] Discuss với PM: mobile roadmap 6 tháng tới?
- [ ] Log decision → ADR-003
- [ ] Update DASHBOARD decision gate

### 7A.2 Nếu giữ mobile (3h)
- [ ] Unify `apps/mobile/src/` theo `folder_organization_solution.md` §4.2
- [ ] Move `app/components/*`, `app/hooks/*`, `app/lib/*`, `app/stores/*` → `src/`
- [ ] Update tsconfig paths

### 7A.3 Nếu giữ: add test (2h)
- [ ] Decide Jest Expo hoặc Vitest
- [ ] Config + smoke test
- [ ] CI job

### 7A.4 Nếu drop mobile (1h)
- [ ] Remove 50/54 unused deps
- [ ] Keep: `expo`, `expo-router`, `react`, `react-native`
- [ ] Document trong README

---

## 🎯 Phase 7B — OCR Service (~10h)

### 7B.1 Modularize app.py (5h)
- [ ] Tạo `services/ocr-service/src/`:
  - `main.py` (FastAPI app + middleware + routes)
  - `config.py` (pydantic-settings)
  - `ocr/router.py`, `ocr/schemas.py`, `ocr/service.py`, `ocr/dependencies.py`, `ocr/exceptions.py`
  - `image/downloader.py`, `image/validator.py`, `image/ssrf_guard.py`
  - `core/logging.py`, `core/middleware.py`, `core/health.py`
- [ ] Move logic từ `app.py` 455 dòng → modules
- [ ] Delete `app.py` (entry: `src/main.py`)
- [ ] Dockerfile entry: `CMD ["uvicorn", "src.main:app", ...]`

### 7B.2 Tests folder (30m)
- [ ] `services/ocr-service/tests/conftest.py`
- [ ] Move `test_ssrf_protection.py` → `tests/`
- [ ] Add `test_ocr_router.py`, `test_image_validator.py`

### 7B.3 Pytest + Trivy CI (1h)
- [ ] Job `ocr-test` trong `.github/workflows/ci.yml`
- [ ] Trivy scan cho OCR Docker image

### 7B.4 CorrelationID propagation (1h)
- [ ] Verify Java API send `X-Correlation-ID` → OCR svc
- [ ] OCR log đúng correlation ID
- [ ] Integration test

### 7B.5 pyproject.toml (1h)
- [ ] Convert `requirements.txt` → `pyproject.toml`
- [ ] Separate `requirements/{base,dev,prod}.txt`
- [ ] Update Dockerfile

### Buffer (1.5h)
- [ ] Integration testing + fixes

---

## 🧪 Verification

```bash
cd services/ocr-service && pytest -v
docker build -t healthlens-ocr ./services/ocr-service
trivy image healthlens-ocr

# CorrelationID
docker compose logs api | grep "correlation-id"
docker compose logs ocr | grep "correlation-id"
# Verify same ID

# Mobile (if kept)
pnpm -F @healthlens/mobile lint
pnpm -F @healthlens/mobile test
```

---

## 🏁 Phase Completion Checklist

### 7A
- [ ] ADR-003 logged
- [ ] Either: src/ unified + tests, OR deps slimmed

### 7B
- [ ] `app.py` 455 LoC → distributed modules
- [ ] `tests/` folder properly placed
- [ ] CI runs pytest
- [ ] CorrelationID end-to-end
- [ ] Trivy passes (or documented CVEs)

### Both
- [ ] Tests xanh
- [ ] Smoke E2E pass
- [ ] Update DASHBOARD

---

## 📝 Retrospective

### Mobile decision
- Outcome: build / drop / defer
- Reasoning: ___

### Effort
| Task | Est | Actual |
|------|:-:|:-:|
| 7A Mobile | 4h | — |
| 7B OCR | 10h | — |
| **Total** | **14h** | — |
