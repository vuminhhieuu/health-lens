# Phase 9 — Docs + BMAD Cleanup

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 9](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 10h (tuần 13)
> **Goal**: Restructure BMAD output theo v6 + update outdated docs + add missing.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/09-docs`
- **PR(s)**: — (2 PRs)

---

## ✅ Pre-flight Checklist

- [ ] Phase 5 `🟢 Done`
- [ ] Đã đọc `repo_hygiene_audit.md` §1, §2
- [ ] Pause story creation 1 ngày
- [ ] Branch: `git checkout -b phase/09-docs main`

---

## 🎯 Tasks

### 9.1 BMAD Output Restructure (4h)
- [ ] Tạo `_bmad-output/planning-artifacts/epics/`
- [ ] Move 27 epic về flat `epic-01-foundation/` → `epic-27-infra/`:
  - `implementation-artifacts/epic-1..11/` → `planning-artifacts/epics/epic-01..11-…/`
  - `implementation-artifacts/epic-core-improvements/epic-1..8-…/` → `epic-12..19-…/`
  - `implementation-artifacts/remaining-production-review/epic-1..6-…/` → `epic-20..25-…/`
  - `implementation-artifacts/public-account-experience/` → `epic-26-…/`
  - `implementation-artifacts/epic-infra/` → `epic-27-infra/`
- [ ] Tạo `planning-artifacts/proposals/` + move sprint-change-proposals
- [ ] Tạo `planning-artifacts/reports/` + move validation/readiness reports
- [ ] Rename `_data/` → `data/`
- [ ] Update cross-reference links (grep + sed)

### 9.2 sprint-status.yaml (30m)
- [ ] Tạo `_bmad-output/implementation-artifacts/sprint-status.yaml` per BMAD v6
- [ ] Initial entries cho 27 epic

### 9.3 Rename STAGING_DEPLOYMENT.md (5m)
- [ ] `docs/STAGING_DEPLOYMENT.md` → `docs/staging-deployment.md`
- [ ] Update links

### 9.4 Merge project-context vs project-overview (30m)
- [ ] Diff
- [ ] Merge → `docs/project-overview.md` canonical
- [ ] Delete duplicate

### 9.5 Add Missing Docs (4h)
- [ ] `docs/security.md` — threat model, CVE process
- [ ] `docs/observability.md` — metrics catalog, dashboards, alerting
- [ ] `docs/database-migrations.md` — Flyway convention, rollback, shedlock
- [ ] `docs/code-style.md` — Java/TS conventions, comment policy
- [ ] `docs/data-retention-policy.md` — từ ADR-004 + procedure

### 9.6 Update Outdated Docs (1h)
- [ ] `docs/source-tree-analysis.md` reflect package-by-feature
- [ ] `docs/architecture.md` update 19 controllers + 37 entities
- [ ] `docs/llm-prompt-templates.md` document v4 + v9
- [ ] Auto-gen `docs/api-contracts.md` từ OpenAPI (sau Phase 8.8)
- [ ] Verify `docs/index.md` link tới mọi file

---

## 🧪 Verification

```bash
# No broken links
grep -rn "STAGING_DEPLOYMENT" docs/  # Expect: 0
grep -rn "project-context" docs/      # Expect: 0

# BMAD layout
ls _bmad-output/planning-artifacts/epics/
# Expect: epic-01 → epic-27

# sprint-status valid
python -c "import yaml; yaml.safe_load(open('_bmad-output/implementation-artifacts/sprint-status.yaml'))"
```

---

## 🏁 Phase Completion Checklist

- [ ] 6 tasks `[x]`
- [ ] 27 epics flat
- [ ] `sprint-status.yaml` valid
- [ ] 5 new docs added
- [ ] 4 docs updated
- [ ] No broken refs
- [ ] Update DASHBOARD

---

## 📝 Retrospective

### Files touched
- BMAD: ~110 moved
- Docs: 5 added, 4 updated

### Effort
| Task | Est | Actual |
|------|:-:|:-:|
| 9.1 BMAD | 4h | — |
| 9.2 sprint-status | 30m | — |
| 9.3–9.4 | 35m | — |
| 9.5 Add | 4h | — |
| 9.6 Update | 1h | — |
| **Total** | **10h** | — |
