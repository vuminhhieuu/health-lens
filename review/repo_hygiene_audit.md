# 🧹 HealthLens — Repo Hygiene & BMAD Artifact Audit

> **Ngày**: 2026-05-28
> **Phạm vi**: 4 mảng review CHƯA được cover ở 4 file review trước:
> 1. `docs/` — refactor/add/edit/delete
> 2. `_bmad/` + `_bmad-output/` — sắp xếp lại theo BMAD-METHOD v6 chuẩn
> 3. AI tooling dirs (`.agent`, `.agents`, `.codex`, `.cursor`, `.opencode`, `.github/skills`) — 6 nơi trùng lặp skills
> 4. Root config files (gitignored vs tracked, orphan files)

---

## 0. Tóm tắt phát hiện

| Mảng | Files affected | Severity | Mức độ effort |
|------|---------------|----------|---------------|
| docs/ inconsistency | 17 file | 🟡 Medium | S (1–2h) |
| `_bmad-output/` không khớp BMAD v6 | ~110 file, 5+ folder | 🔴 High | M (4–6h) |
| 6 dirs skills trùng lặp | 5 tool dirs + .github/skills | 🟡 Medium | S |
| `_bmad/` (gitignored) | OK, không cần action | 🟢 Low | — |
| `.opencode/node_modules` (gitignored) | 26 MB local | 🟢 Low | — |
| Root config sprawl (env files đã flag) | 6 files | (đã có ở review #1) | — |
| Missing standard repo files (LICENSE, CONTRIBUTING…) | 4–5 file | 🟡 Medium | S |

---

## 1. 📚 `docs/` — Refactor + Add + Edit + Delete

### 1.1 Inventory hiện tại (17 file, 1,546 LoC)

```
docs/
├── api-contracts.md              (139 LoC)
├── architecture.md               (94)
├── component-inventory.md        (58)
├── data-models.md                (61)
├── deployment-guide.md           (65)
├── development-guide.md          (96)
├── environment-reference.md      (124)
├── event-driven-architecture.md  (96)
├── index.md                      (95)
├── llm-prompt-templates.md       (26)
├── operations-runbook.md         (55)
├── project-context.md            (60)
├── project-overview.md           (52)
├── provider-switching-runbook.md (138)
├── source-tree-analysis.md       (72)
├── STAGING_DEPLOYMENT.md         (265)    ← UPPER_CASE outlier
└── testing-guide.md              (50)
```

### 1.2 Vấn đề

#### (a) Naming inconsistency
- **16/17 file**: kebab-case (`api-contracts.md`)
- **1/17 file**: SCREAMING_SNAKE (`STAGING_DEPLOYMENT.md`)
→ Rename `STAGING_DEPLOYMENT.md` → `staging-deployment.md`.

#### (b) Trùng lặp nội dung (cần verify thực tế khi đọc)
| Pair nghi vấn | Phân biệt? |
|---------------|------------|
| `project-context.md` (60 LoC) **vs** `project-overview.md` (52 LoC) | Tiêu đề gần giống. **Có thể merge** thành 1 file `project-overview.md` |
| `deployment-guide.md` (65 LoC) **vs** `staging-deployment.md` (265 LoC) | Staging dài hơn deployment chung — nghi vấn duplication |
| `architecture.md` (94 LoC) **vs** `source-tree-analysis.md` (72 LoC) | Có chồng lấn về cấu trúc — nhưng review #1 đã chỉ ra `source-tree-analysis.md` outdated (vẫn mô tả hybrid structure) |
| `event-driven-architecture.md` (96 LoC) **vs** `architecture.md` | Có khả năng event-driven là section của architecture, không phải file riêng |

#### (c) Thiếu document phục vụ contributor
| File chuẩn thiếu | Tại sao cần |
|------------------|-------------|
| `docs/security.md` | Threat model, security review process (review #4 đã flag) |
| `docs/monitoring.md` | Observability roadmap (chưa có metrics/tracing) |
| `docs/database-migrations.md` | 50 migrations + rollback strategy (review gaps đã flag) |
| `docs/contributing.md` HOẶC root `CONTRIBUTING.md` | Commit convention, PR checklist |
| `docs/code-style.md` | Convention giữa Java/TS naming (constants, enum case) |

#### (d) Outdated content
- `source-tree-analysis.md` mô tả cấu trúc hybrid (đã đề xuất refactor) → cần update sau khi migrate
- `architecture.md` cần verify khớp với 19 controllers + 37 entities thực tế
- `llm-prompt-templates.md` (26 LoC) — chỉ doc prompt v3 cho metric explanation, không doc prompt v8 recommendations

#### (e) Thiếu `index.md` rõ ràng làm landing
`docs/index.md` đã có (95 LoC) — verify nó **link tới mọi file khác** và có TOC.

### 1.3 Action plan cho `docs/`

#### Phase A — Cleanup (30 min)
1. Rename `STAGING_DEPLOYMENT.md` → `staging-deployment.md`
2. Đọc + diff `project-context.md` vs `project-overview.md` → merge nếu trùng, hoặc xác lập rõ scope từng file
3. Verify `index.md` link tới tất cả 16 file còn lại

#### Phase B — Add (1–2h)
4. Tạo `docs/security.md` (threat model + dependency CVE process)
5. Tạo `docs/observability.md` (metrics + tracing + alerting roadmap)
6. Tạo `docs/database-migrations.md` (Flyway convention + rollback)
7. Tạo `docs/code-style.md` (constants, naming, comment policy)
8. Tạo root `CONTRIBUTING.md` (PR workflow, commit format)

#### Phase C — Update (sau khi refactor code, 2h)
9. Update `source-tree-analysis.md` để khớp package-by-feature (sau khi áp dụng `folder_organization_solution.md`)
10. Update `architecture.md` — đối chiếu 19 controllers + 37 entities
11. Update `llm-prompt-templates.md` — doc prompt v8 mới
12. Verify `api-contracts.md` khớp Java OpenAPI generated spec
13. Verify `event-driven-architecture.md` khớp 2 Redis streams (OcrJob + Email)

#### Phase D — Auto-generate (ROI cao)
14. Generate `api-contracts.md` từ Spring Doc OpenAPI (đã có `OpenApiConfig.java`) — eliminate manual drift
15. Generate `data-models.md` từ JPA entity scan (custom Gradle task)

---

## 2. 🤖 `_bmad/` + `_bmad-output/` — Sắp xếp theo BMAD-METHOD v6

### 2.1 Trạng thái Git của BMAD

```bash
$ git check-ignore _bmad _bmad-output
_bmad        # ← gitignored ✅ (local install, không track)
              # ← _bmad-output KHÔNG ignored → TRACKED
```

→ Chỉ `_bmad-output/` cần audit. `_bmad/` là local install của BMAD CLI (tương tự `node_modules/`), không cần dọn.

### 2.2 BMAD-METHOD v6 chuẩn

Theo `docs.bmad-method.org`:

```
_bmad-output/
├── project-context.md            ← (1) AI conventions cho toàn dự án
├── planning-artifacts/           ← (2) Analyst + PM + Architect outputs
│   ├── PRD.md
│   ├── architecture.md
│   ├── ux-design-specification.md
│   ├── epics.md                  ← Epic catalog
│   └── epics/                    ← (POST-ARCHITECTURE) Stories per epic
│       ├── epic-1/
│       │   ├── 1-1-story-name.md
│       │   └── 1-2-story-name.md
│       └── epic-2/
└── implementation-artifacts/     ← (3) Dev artifacts
    └── sprint-status.yaml        ← Tracks all epics/stories execution state
```

### 2.3 Trạng thái hiện tại — Vi phạm convention

#### (a) Sai vị trí epics & stories
**BMAD chuẩn**: Stories ở `planning-artifacts/epics/epic-N/*.md`
**Hiện tại**: Stories ở `implementation-artifacts/epic-N/*.md`

```
❌ Hiện tại:
_bmad-output/
├── planning-artifacts/
│   ├── prd.md
│   ├── architecture.md
│   ├── epics.md
│   ├── research/                  ← (custom — chấp nhận được)
│   ├── review-source/             ← (custom — manual inputs)
│   └── + 16 file lẻ tẻ            ← Hỗn loạn
└── implementation-artifacts/
    ├── epic-1/                    ← (ĐÚNG RA THUỘC planning-artifacts/epics/)
    ├── epic-2/
    ├── ...epic-11
    ├── epic-core-improvements/    ← NESTED epics!
    │   ├── epic-1-ocr-reliability/
    │   ├── epic-2-ai-provider-abstraction/
    │   └── ...epic-8
    ├── remaining-production-review/  ← NESTED epics!
    │   ├── epic-1-web-production-readiness/
    │   └── ...epic-6
    ├── epic-infra/                ← Lonely epic
    ├── public-account-experience/ ← Container for epics
    ├── bugs/                      ← Bug reports — không chuẩn BMAD
    ├── _data/                     ← stitch-screens.json — orphan
    └── tools/                     ← apply_stitch_sections.py — scripts misplaced
```

#### (b) Nested epic groupings — 3 namespace cho cùng concept

| Group | Folder | Số sub-epics |
|-------|--------|--------------|
| Original epics | `epic-1/` … `epic-11/` + `epic-infra/` | 12 |
| Improvement initiative | `epic-core-improvements/epic-1-…/` … `epic-8-…/` | 8 |
| Production readiness | `remaining-production-review/epic-1-…/` … `epic-6-…/` | 6 |
| Public experience | `public-account-experience/` | 1 |

→ **27 epic** chia ở 4 namespace khác nhau → khó tracking, khó pick story tiếp theo.

#### (c) Thiếu `sprint-status.yaml`
BMAD v6 dùng file này để track sprint execution. Hiện tại không có → mọi PM tracking phải dùng Linear / GitHub Project bên ngoài.

#### (d) `planning-artifacts/` mixes nhiều concept
30+ file ở `planning-artifacts/` gồm:
- PRD, architecture, epics catalog (BMAD chuẩn)
- 2 file sprint-change-proposal (legitimate)
- 1 file smart-validation-report (legitimate)
- 5 file `*-epics-and-stories.md` (subset của epics.md — duplication?)
- 2 file `*-readiness-report-*` (operational)
- 1 file `core-improvements-duplicate-story-audit.md` (meta)
- `research/` (technical research outputs)
- `review-source/` (manual review docs)

### 2.4 Target structure (theo BMAD v6 + HealthLens extension)

```
_bmad-output/
├── project-context.md                        ← MOVE FROM docs/ (if applicable)
│
├── planning-artifacts/
│   ├── prd.md                                ← KEEP
│   ├── architecture.md                       ← KEEP
│   ├── epics.md                              ← KEEP (catalog of all epics)
│   ├── ux-design-specification.md            ← KEEP
│   ├── system_design.md                      ← KEEP
│   │
│   ├── epics/                                ← (NEW LOCATION FOR ALL STORIES)
│   │   ├── epic-01-foundation/               ← Was: epic-1/
│   │   ├── epic-02-…/
│   │   ├── ...epic-11-account-security/
│   │   ├── epic-12-ocr-reliability/          ← Flatten from: epic-core-improvements/epic-1-ocr-reliability/
│   │   ├── epic-13-ai-provider-abstraction/  ← Flatten epic-core-improvements/epic-2
│   │   ├── epic-14-llm-rag-governance/
│   │   ├── epic-15-production-infra-ops/
│   │   ├── epic-16-security-compliance/
│   │   ├── epic-17-frontend-consistency/
│   │   ├── epic-18-code-organization/
│   │   ├── epic-19-ci-cd-quality-gates/
│   │   ├── epic-20-web-production-readiness/
│   │   ├── epic-21-auth-token-email-hardening/
│   │   ├── epic-22-family-sharing-lifecycle/
│   │   ├── epic-23-admin-analytics-instrumentation/
│   │   ├── epic-24-mobile-deferred-readiness/
│   │   ├── epic-25-cleanup-accessibility/
│   │   ├── epic-26-public-account-experience/
│   │   └── epic-27-infra/
│   │
│   ├── proposals/                            ← (NEW) Sprint change proposals
│   │   ├── sprint-change-proposal-2026-03-28.md
│   │   └── sprint-change-proposal-2026-05-20.md
│   │
│   ├── reports/                              ← (NEW) Validation/readiness reports
│   │   ├── implementation-readiness-2026-03-28.md
│   │   ├── prd-validation-report.md
│   │   ├── smart-validation-report.md
│   │   ├── core-improvements-duplicate-story-audit.md
│   │   ├── core-feature-infra-issues-synthesis.md
│   │   └── core-review-docs-coverage-audit.md
│   │
│   ├── research/                             ← KEEP (technical research)
│   │   ├── technical-ocr-llms-2026-03-29.md
│   │   ├── technical-option-bplus-2026-04-01.md
│   │   └── ...
│   │
│   └── review-source/                        ← KEEP (manual review inputs)
│       └── production-review/
│
├── design-artifacts/                         ← (KEEP — BMAD WDS workflow output)
│   ├── INDEX.md
│   ├── MASTER-EXECUTION-GUIDE.md
│   ├── A-Product-Brief/
│   ├── B-Trigger-Map/
│   ├── C-UX-Scenarios/
│   ├── D-Design-System/
│   ├── E-PRD/
│   ├── F-Testing/
│   ├── G-Product-Development/
│   ├── design-system-plan.md                 ← Rename DESIGN-SYSTEM-PLAN.md
│   ├── phase-1-implementation-guide.md
│   ├── phase-2-implementation.md             ← Lose "-VI" suffix (Vietnamese)
│   ├── phase-2-stitch-prompt.md
│   ├── phase-3-stitch-prompt.md
│   ├── phase-4-stitch-prompt.md
│   ├── phase-2-4-summary.md
│   ├── screens-full-list.md
│   ├── step-by-step-stitch.md
│   ├── stitch-ai-specifications.md
│   └── tokens-quick-reference.md
│
└── implementation-artifacts/
    ├── sprint-status.yaml                    ← (NEW) BMAD v6 standard
    ├── bugs/                                 ← KEEP (HealthLens extension)
    │   ├── first-result-cta-does-not-open-upload.md
    │   ├── profile-invariant-mismatch.md
    │   └── review-record-save-and-edit-toggle.md
    ├── data/                                 ← RENAME from _data/ (no underscore)
    │   └── stitch-screens.json
    └── tools/                                ← KEEP (story-driven scripts)
        ├── apply_stitch_sections.py
        └── generate_linear_import_csv.py
```

### 2.5 Migration mapping (cụ thể)

| From | To |
|------|-----|
| `implementation-artifacts/epic-1/` → `epic-11-…/` | `planning-artifacts/epics/epic-01-foundation/` → `epic-11-account-security/` |
| `implementation-artifacts/epic-core-improvements/epic-1-…/` → `epic-8-…/` | `planning-artifacts/epics/epic-12-…/` → `epic-19-…/` |
| `implementation-artifacts/remaining-production-review/epic-1-…/` → `epic-6-…/` | `planning-artifacts/epics/epic-20-…/` → `epic-25-…/` |
| `implementation-artifacts/public-account-experience/` | `planning-artifacts/epics/epic-26-public-account-experience/` |
| `implementation-artifacts/epic-infra/` | `planning-artifacts/epics/epic-27-infra/` |
| `planning-artifacts/sprint-change-proposal-*.md` | `planning-artifacts/proposals/` |
| `planning-artifacts/*-validation-report.md`, `*-readiness-report-*.md`, `*-audit.md`, `*-synthesis.md` | `planning-artifacts/reports/` |
| `implementation-artifacts/_data/` | `implementation-artifacts/data/` (dấu `_` không cần) |
| Tạo mới | `implementation-artifacts/sprint-status.yaml` |

### 2.6 Action plan

#### Phase 1 — Restructure (M effort)
1. Tạo `planning-artifacts/epics/` folder
2. Move 27 epic folders về đó với numbering thống nhất `epic-01-…` đến `epic-27-…`
3. Tạo `planning-artifacts/proposals/` + `planning-artifacts/reports/` và move file tương ứng
4. Tạo `implementation-artifacts/sprint-status.yaml`
5. Rename `_data/` → `data/`

#### Phase 2 — Naming consistency (S effort)
6. Rename design-artifacts files thành kebab-case (bỏ SCREAMING_SNAKE và `-VI` suffix)
7. Convention: tất cả file dùng kebab-case `.md`

#### Phase 3 — Documentation
8. Update `_bmad-output/planning-artifacts/epics.md` (catalog) phản ánh layout mới
9. Document trong root `CONTRIBUTING.md`: "Stories live in `_bmad-output/planning-artifacts/epics/`"

#### ⚠️ Trước khi migrate
- `_bmad-output/` đang **TRACKED trong git** → migration sẽ tạo PR khổng lồ. Cân nhắc:
  - Strategy A: 1 PR cho cả move (clean diff, nhưng risk conflict cao với in-flight stories)
  - Strategy B: Move 1 epic mỗi PR (chậm nhưng safe)
- Verify mọi link cross-reference giữa stories không bị break sau move

---

## 3. 🛠️ AI Tooling Dirs — 6 nơi trùng lặp `skills/`

### 3.1 Hiện trạng (đã check `git check-ignore`)

| Directory | Tracked? | Nội dung | Size |
|-----------|----------|----------|------|
| `.agent/skills/` | gitignored (`.agent/`) | BMAD skills (workflow.yaml + customize.toml format) | ? |
| `.agents/skills/` | gitignored | BMAD skills (subset) | ? |
| `.codex/skills/` | gitignored | BMAD skills (workflow.md + SKILL.md + manifest.yaml) | ? |
| `.cursor/skills/` | gitignored | BMAD skills (same format as .codex) | ? |
| `.opencode/skills/` | gitignored | BMAD skills (same format as .codex) + 26 MB `node_modules` | 36 MB |
| `.github/skills/` | **TRACKED ✅** | BMAD skills (66 skill folders) | — |

→ Trong 6 chỗ, **chỉ `.github/skills/` được commit**. 5 dir còn lại là **per-IDE local skills exported** từ `_bmad/_config/ides/{cursor,opencode,…}.yaml`.

### 3.2 Vấn đề

#### (a) Per-developer pollution
5 thư mục `.{tool}/skills/` chỉ phục vụ tool cụ thể local. Repo path bị "noise" với 5 dirs gitignored mà không developer nào khác cần.

#### (b) Manifest tách rời
- `_bmad/_config/ides/cursor.yaml` controls `.cursor/skills/` generation
- `_bmad/_config/ides/opencode.yaml` controls `.opencode/skills/`
- `_bmad/_config/ides/github-copilot.yaml` controls `.github/skills/`
- `_bmad/_config/ides/antigravity.yaml` controls another?

→ User chỉ cần **chạy 1 IDE** nhưng có 4 manifest. Có thể consolidate.

#### (c) `.opencode/` có `package.json` + `node_modules` riêng (26 MB)
Bất thường — local tool không nên có dependency installation lồng trong project repo. Cần verify:
- `.opencode/package.json` có cần thiết?
- Hay là leftover từ install thử?

#### (d) `.github/skills/` (TRACKED — cần audit)
66 skill folders được tracked → ảnh hưởng:
- Clone size tăng
- Mọi developer kéo về phải tải 66 skills dù chỉ dùng 1–2

Verify: 66 skills này có thực sự dùng trong GitHub Copilot workflow? Hay là export thừa?

### 3.3 Action plan

#### Phase 1 — Audit (30 min)
1. Hỏi team: dev đang dùng IDE nào? (Cursor, Copilot, Codex, OpenCode, Antigravity)
2. Giữ lại MAX 2 export: `.github/skills/` (CI) + 1 local-IDE (per primary IDE)

#### Phase 2 — Cleanup (15 min)
3. Xóa thư mục local của IDE không dùng (gitignored nên không tạo PR)
4. Xóa `.opencode/node_modules/` (26 MB) — gitignored nhưng chiếm disk
5. Xóa `.opencode/package.json` nếu không cần

#### Phase 3 — Consolidation (1h)
6. Disable IDE exports không cần trong `_bmad/_config/ides/`
7. Document trong `CONTRIBUTING.md`: "Generate IDE skills bằng `bmad export <ide>`"

#### Phase 4 — Decision về `.github/skills/`
8. Audit 66 tracked skills — có dùng hết trong GH Copilot workspace không?
9. Nếu **không**: xóa khỏi git, thay bằng generate-on-demand
10. Nếu **có**: giữ nguyên + document

---

## 4. 📦 Root config files

### 4.1 Inventory

| File | Tracked | Size | Status |
|------|---------|------|--------|
| `.dockerignore` | ✅ | 1.1 KB | OK |
| `.env` | ❌ (ignored) | 5.3 KB | OK |
| `.env.example` | ✅ | 5.2 KB | OK |
| `.env.production` | ❌ (ignored via `.env.*`) | 4.8 KB | (đã flag review #1) |
| `.env.staging` | ❌ (ignored) | 1.4 KB | (đã flag) |
| `.env.staging.api` | ❌ (ignored) | 4.9 KB | (đã flag) |
| `.gitignore` | ✅ | 3.4 KB | OK |
| `.infisical.json` | ✅ | 134 B | OK |
| `package.json` | ✅ | 244 B | Quá nhỏ → chỉ 3 script, có thể bổ sung (review gaps đã flag) |
| `pnpm-lock.yaml` | ✅ | 509 KB | OK |
| `pnpm-workspace.yaml` | ✅ | 40 B | OK |
| `README.md` | ✅ | 5.5 KB | (cần audit) |
| `settings.gradle.kts` | ✅ | 106 B | OK |

### 4.2 Vấn đề chưa được review

#### (a) Thiếu files chuẩn open-source / team-collab
| File chuẩn thiếu | Mục đích |
|------------------|----------|
| `LICENSE` | Pháp lý — không có license rõ ràng cho code |
| `CONTRIBUTING.md` | Onboarding, PR conventions, commit format |
| `CODE_OF_CONDUCT.md` | Team conduct (nếu mở public hoặc có outside contributor) |
| `SECURITY.md` | Security disclosure policy |
| `CHANGELOG.md` | Release notes |
| `.editorconfig` | Editor-agnostic formatting (tab/space, line ending) |
| `.nvmrc` | Node version pin (web/mobile build) |
| `.tool-versions` (asdf) hoặc Volta `volta` field | Multi-tool version pin |

#### (b) `.gitignore` quality
3.4 KB là decent size. Cần verify có pattern cover:
- IDE-specific (.vscode, .idea — có)
- Test outputs (coverage/ — verify)
- Mobile (Expo: .expo/, dist/ — verify)
- AI tooling (.agent/, .agents/, .codex/, .cursor/, .opencode/ — verify)
- BMAD local (`_bmad/` — verify)

#### (c) `README.md` (5.5 KB) — chưa được audit
Cần verify:
- Quick start instructions còn đúng?
- Link tới `docs/index.md` còn live?
- Setup pre-requisites đầy đủ?

### 4.3 Action plan

#### Phase 1 — Add (1h)
1. Tạo `LICENSE` (chọn: MIT / Apache 2.0 / proprietary)
2. Tạo `CONTRIBUTING.md` — workflow, commit format (Conventional Commits?)
3. Tạo `SECURITY.md` — vulnerability disclosure email
4. Tạo `.editorconfig` — minimum: line ending, indent
5. Tạo `.nvmrc` (Node 20)

#### Phase 2 — Audit (30 min)
6. Verify `.gitignore` cover tất cả AI tooling dirs + `_bmad/`
7. Audit `README.md` — verify mọi link và instruction còn correct

#### Phase 3 — Optional
8. Setup auto-generated `CHANGELOG.md` (Release Please / changesets)
9. Add `pnpm` version pin trong `packageManager` field (đã có "pnpm@10.13.1" ✅)

---

## 5. 🧐 Các phát hiện nhỏ khác

### 5.1 `_bmad/memory/bmad-agent-commit-scribe-sidecar/`
Memory store của agent — gitignored OK, nhưng nếu không dùng agent đó → có thể xóa.

### 5.2 `_bmad/bmm/4-implementation/` empty
Folder rỗng trong BMAD module — không cần action (gitignored).

### 5.3 `node_modules/` ở ROOT (802 MB local)
Gitignored, nhưng note: pnpm workspace bình thường KHÔNG có top-level `node_modules` lớn — chỉ có symlinks. 802 MB chứng tỏ có **hoisted deps**. Verify `.npmrc` / pnpm config.

### 5.4 `build/` ở root (144 KB)
Gradle build output ở root level. Bình thường Gradle để build output ở `apps/api/build/`. Verify đây không phải artifact bị bỏ quên.

### 5.5 `infisical/ONBOARDING.md` + `README.md`
2 file MD trong `infisical/` — có thể move vào `docs/secrets-management.md` để consolidate.

### 5.6 Files không có ai dùng (verify)
- `apps/api/HELP.md` — Spring Boot CLI auto-generated. Verify có nội dung gì meaningful không, nếu không → xóa.
- `apps/api/gradlew.bat` — Windows wrapper. Nếu team toàn Linux/Mac → có thể xóa.
- `apps/mobile/expo-env.d.ts` — Expo auto-generated. OK.

---

## 6. 📊 Tổng kết: 5 file review đã tạo cover những gì

| Aspect | project_review | deep_code_review | folder_org | review_gaps | **repo_hygiene** (this file) |
|--------|:-:|:-:|:-:|:-:|:-:|
| Folder structure (code) | ✅ | — | ✅✅ | — | — |
| SOLID violations | — | ✅ | — | — | — |
| God classes | ✅ | ✅ | — | — | — |
| Constants | ✅ | — | ✅ | ✅ | — |
| Features maturity | — | — | — | ✅ | — |
| DB migrations | — | — | — | ✅ | — |
| Scripts + CI | — | — | — | ✅ | — |
| Observability | — | — | — | ✅ | — |
| Email templates | — | ✅ | — | ✅ | — |
| Performance | ✅ (nhỏ) | — | — | ✅ | — |
| Security | — | — | — | ✅ | — |
| **`docs/` content** | — | — | — | (flag only) | **✅** |
| **`_bmad-output/` BMAD compliance** | — | — | — | — | **✅** |
| **AI tooling dirs cleanup** | — | — | (folder only) | (flag only) | **✅** |
| **Standard repo files** (LICENSE, CONTRIBUTING…) | — | — | — | — | **✅** |
| **Naming consistency cross-repo** | — | — | — | — | **✅** |

---

## 7. 🎯 Còn vấn đề nào CHƯA review không?

Đã cover hết các mảng repo. Nhưng còn **một số góc nhỏ** có thể audit thêm khi có thời gian:

### 7.1 Code style / formatting
- Có `.editorconfig` không? — không
- Có Prettier config không? — verify `apps/web/`
- Có Spotless/Checkstyle cho Java không? — verify `build.gradle.kts`

### 7.2 Git hygiene
- Branch protection rules trên `main`? — verify GitHub settings
- Commit signing required? — verify
- PR template? — verify `.github/PULL_REQUEST_TEMPLATE.md` không thấy
- Issue template? — verify `.github/ISSUE_TEMPLATE/`

### 7.3 Localization deep
- Vietnamese vs English mix trong code comments
- AI prompts file `metric-explanation.v3.txt` — Vietnamese hay English?
- Email templates `*.html` — Vietnamese?

### 7.4 Asset management
- `apps/web/public/` (verify assets không có file rác / lớn unnecessary)
- `apps/api/src/main/resources/fonts/` — font files cho PDF? Bao nhiêu MB?
- Image assets: PNG/SVG optimization?

### 7.5 Build artifacts gitignored properly
- `.next/`, `dist/`, `build/`, `out/` — verify gitignored
- Test coverage outputs — verify gitignored
- IDE caches — verify gitignored

### 7.6 Monorepo concerns
- `pnpm-workspace.yaml` chỉ 40 B — verify cover đủ workspace packages
- Cross-package version conflicts (e.g., 3 react versions)
- Hoisting strategy (`.npmrc` config)

---

## 📋 Đề xuất 2 file review cuối cùng nếu muốn 100% coverage

| File | Nội dung |
|------|----------|
| `style_and_formatting_audit.md` | Editorconfig, Prettier, ESLint, Spotless, Git hooks |
| `assets_and_localization_audit.md` | Fonts, images, prompts, email templates languages |

→ 2 file này **không gấp**, có thể skip nếu effort là constraint.

---

## TL;DR — Cleanup priority order

1. 🔴 **`_bmad-output/` restructure** — 27 epic chia 4 namespace → migrate về `planning-artifacts/epics/` flat
2. 🔴 **Add standard files** — LICENSE, CONTRIBUTING.md, SECURITY.md, .editorconfig
3. 🟡 **`docs/` cleanup** — rename SCREAMING_CASE, merge duplicates, add missing docs (security, observability, db-migrations)
4. 🟡 **AI tooling cleanup** — giảm 6 dirs skills xuống 2 (1 IDE local + .github/skills)
5. 🟢 **Root config** — verify .gitignore + audit README

Tổng effort estimate: **8–12h** spread qua 2–3 PRs.

Sources:
- [BMAD-METHOD documentation](https://docs.bmad-method.org/tutorials/getting-started/) — output folder convention v6
- [BMAD-METHOD GitHub](https://github.com/bmad-code-org/bmad-method) — module structure
- [Feature-Sliced Design](https://feature-sliced.design/docs/get-started/overview) — đã reference ở file trước
