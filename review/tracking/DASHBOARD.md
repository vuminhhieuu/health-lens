# 📊 HealthLens Refactor — Dashboard

> **Last updated**: 2026-06-01 (initial)
> **Plan**: [INTEGRATED_MASTER_PLAN.md](../INTEGRATED_MASTER_PLAN.md)
> **Total estimated**: ~173h | **Actual**: 0h | **% complete**: 0%

---

## 🎯 Current Focus

**Active phase**: — (chưa bắt đầu)
**Active branch**: `phase/00-safety-net` (worktree created)
**Next decision gate**: Phase 0 sign-off

---

## 📈 Phase Status

| # | Phase | Status | Effort (est/act) | Started | Done | PR(s) |
|:-:|-------|:-:|:-:|---------|------|-------|
| 0 | [Critical Safety Net](phase-00-critical-safety-net.md) | 🔴 Not Started | 14h / — | — | — | — |
| 1 | [Runtime Hardening](phase-01-runtime-hardening.md) | 🔴 Not Started | 10h / — | — | — | — |
| 2 | [Tooling & Conventions](phase-02-tooling-conventions.md) | 🔴 Not Started | 10h / — | — | — | — |
| 3 | [Shared Foundation](phase-03-shared-foundation.md) | 🔴 Not Started | 12h / — | — | — | — |
| 4 | [Exceptions & Error Codes](phase-04-exceptions-error-codes.md) | 🔴 Not Started | 10h / — | — | — | — |
| 5 | [Backend Package-by-Feature](phase-05-backend-package-by-feature.md) | 🔴 Not Started | 40h / — | — | — | — |
| 6 | [Frontend + Validation](phase-06-frontend-validation.md) | 🔴 Not Started | 38h / — | — | — | — |
| 7 | [Mobile + OCR Service](phase-07-mobile-ocr-service.md) | 🔴 Not Started | 14h / — | — | — | — |
| 8 | [Observability + CI/CD](phase-08-observability-cicd.md) | 🔴 Not Started | 14h / — | — | — | — |
| 9 | [Docs + BMAD](phase-09-docs-bmad.md) | 🔴 Not Started | 10h / — | — | — | — |
| 10 | [AI Tooling](phase-10-ai-tooling.md) | 🔴 Not Started | 1h / — | — | — | — |

---

## 🚦 Decision Gates

| Gate | Phase | Status | Approved by | Date |
|------|:-:|:-:|------------|------|
| Safety Net sign-off | 0 | ⏸️ Pending | — | — |
| ShedLock multi-pod test | 1.2 | ⏸️ Pending | — | — |
| LLM prompt injection verified | 1.1 | ⏸️ Pending | — | — |
| Coverage baseline | 5 | ⏸️ Pending | — | — |
| Smoke E2E sau Phase 5+6 | 5, 6 | ⏸️ Pending | — | — |
| Mobile build vs drop | 7A | ⏸️ Pending | — | — |
| Data retention | 8.10 | ⏸️ Pending | — | — |

→ Chi tiết: [DECISIONS.md](DECISIONS.md)

---

## 🚨 Active Blockers

_Chưa có blocker._

---

## 📅 Sprint Timeline

```
Tuần 1 (06-02 → 06-08): Phase 0 — Critical Safety Net
Tuần 2 (06-09 → 06-15): Phase 1 — Runtime Hardening
Tuần 3 (06-16 → 06-22): Phase 2 — Tooling
Tuần 4 (06-23 → 06-29): Phase 3 — Shared Foundation
Tuần 5 (06-30 → 07-06): Phase 4 — Exceptions
Tuần 6 (07-07 → 07-13): Phase 5A — healthrecord/
Tuần 7 (07-14 → 07-20): Phase 5B — ocr/ + llm/
Tuần 8 (07-21 → 07-27): Phase 5C — others
Tuần 9 (07-28 → 08-03): Phase 6 — Frontend refactor
Tuần 10 (08-04 → 08-10): Phase 6 — Validation wiring
Tuần 11 (08-11 → 08-17): Phase 7 — Mobile + OCR
Tuần 12 (08-18 → 08-24): Phase 8 — Observability + CI/CD
Tuần 13 (08-25 → 08-31): Phase 9 — Docs + BMAD
Parallel: Phase 10 — AI tooling
```

---

## 📊 Quick-Win Path (~22h)

- [ ] Phase 0 toàn bộ (14h)
- [ ] Phase 1.1 — LLM prompt injection (4h)
- [ ] Phase 1.2 — ShedLock (2.5h)
- [ ] Phase 3.1–3.3 — Status enum (6h, overlap)

→ Cover 8 critical issues với 13% effort.

---

## 🏆 Definition of Done

### Runtime Correctness
- [ ] HTTP security headers mọi response
- [ ] LLM prompts wrap user content + PromptSanitizer
- [ ] @Scheduled jobs có @SchedulerLock
- [ ] HealthRecord có @SQLDelete + @Where
- [ ] Magic-byte MIME validation
- [ ] DataDeletionService purge Redis + S3
- [ ] /users/me/export hoạt động
- [ ] 0 side effect trong @Transactional(readOnly)
- [ ] All controllers typed DTO + @Valid
- [ ] Zod schemas wired ≥ 80%

### Code Quality
- [ ] Coverage ≥ 70%
- [ ] Lighthouse a11y ≥ 90
- [ ] Zero IllegalArgumentException cho business
- [ ] Zero magic status strings
- [ ] HealthRecordService ≤ 400 dòng
- [ ] ReviewRecordPage ≤ 100 dòng shell
- [ ] Sentry production
- [ ] /actuator/prometheus exposed
- [ ] _bmad-output/ BMAD v6
- [ ] Zero Arial.ttf
- [ ] PWA installable
