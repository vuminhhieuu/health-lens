# Phase 2 — Tooling & Conventions

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 2](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 10h
> **Goal**: Setup formatter + lint + git hooks **trước refactor lớn** → format noise không lẫn diff thật.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/02-tooling`
- **PR(s)**: —

---

## ✅ Pre-flight Checklist

- [ ] Phase 1 `🟢 Done`
- [ ] Đã đọc `final_misc_audit.md` §1, §2
- [ ] Đã đọc `repo_hygiene_audit.md` §4
- [ ] Branch: `git checkout -b phase/02-tooling main`

---

## 🎯 Tasks

### 2.A — Format & Lint (3h)

- [ ] **2.1** `.editorconfig` ở root — 5m
  - indent_style, indent_size, end_of_line=lf, charset=utf-8, trim_trailing_whitespace, insert_final_newline
- [ ] **2.2** Prettier + `.prettierrc` + `.prettierignore` (web + shared) — 20m
  - Add `format` script vào root `package.json`
- [ ] **2.3** Husky + lint-staged (pre-commit) — 30m
  - `pnpm add -D husky lint-staged`
  - `npx husky init`
  - `.husky/pre-commit` chạy `pnpm exec lint-staged`
  - Config `lint-staged` trong `package.json`
- [ ] **2.4** Spotless plugin — 30m
  - `apps/api/build.gradle.kts`: `id("com.diffplug.spotless") version "6.25.0"`
  - Config: `googleJavaFormat("1.22.0")`, target `src/**/*.java`
- [ ] **2.5** Ruff config — 15m
  - `services/ocr-service/pyproject.toml` section `[tool.ruff]`
- [ ] **2.6** **Format-all sweep** — 1h
  - `pnpm format`, `./gradlew spotlessApply`, `ruff format .`
  - Commit `chore: apply formatters (no behavior change)`
  - Tạo `.git-blame-ignore-revs` chứa commit hash
  - `git config blame.ignoreRevsFile .git-blame-ignore-revs`
- [ ] **2.7** CI format check — 30m
  - `.github/workflows/ci.yml` add: `prettier --check`, `spotlessCheck`, `ruff check`

### 2.B — GitHub Conventions (1h)

- [ ] **2.8** PR template — 15m
  - `.github/PULL_REQUEST_TEMPLATE.md`
  - Sections: Summary, Test plan, Migration impact, Docs, Linked issue
- [ ] **2.9** Issue templates — 20m
  - `.github/ISSUE_TEMPLATE/bug_report.md` + `feature_request.md`
- [ ] **2.10** CODEOWNERS — 10m
  - `.github/CODEOWNERS`
  - Owners: `db/migration/*`, `SecurityConfig.java`, `auth/`
- [ ] **2.11** Dependabot — 15m
  - `.github/dependabot.yml`
  - Ecosystems: npm, gradle, pip, docker. Weekly schedule

### 2.C — Standard Files (1h)

- [ ] **2.12.A** `LICENSE` (chọn MIT/Apache 2.0/proprietary) — 15m
- [ ] **2.12.B** `CONTRIBUTING.md` (workflow, Conventional Commits, PR checklist) — 20m
- [ ] **2.12.C** `SECURITY.md` (disclosure email) — 10m
- [ ] **2.12.D** `.nvmrc` (Node 20.18.0) — 5m
- [ ] **2.12.E** `CODE_OF_CONDUCT.md` (optional) — 10m

### Buffer (5h)
- [ ] Test formatters end-to-end (commit → hook → CI)
- [ ] Verify `.git-blame-ignore-revs` đúng

---

## 🧪 Verification

```bash
# Pre-commit hook
echo "  badly  formatted  " >> apps/web/src/test.ts
git add apps/web/src/test.ts
git commit -m "test"
# Expect: hook auto-formats hoặc reject

# CI passes
gh workflow run ci.yml

# Dependabot active
gh api repos/$OWNER/$REPO/contents/.github/dependabot.yml
```

---

## 🏁 Phase Completion Checklist

- [ ] 16 task `[x]`
- [ ] Format sweep commit merged
- [ ] CI có 3 format check jobs
- [ ] Pre-commit hook chạy
- [ ] 5 standard files added
- [ ] PR + issue templates active
- [ ] CODEOWNERS auto-assign reviewer
- [ ] Update DASHBOARD

---

## 📝 Retrospective

### Format sweep
- LoC changed: ___
- Files touched: ___

### Effort
| Section | Est | Actual |
|---------|:-:|:-:|
| 2.A Format | 3h | — |
| 2.B GitHub | 1h | — |
| 2.C Standard | 1h | — |
| Buffer | 5h | — |
| **Total** | **10h** | — |
