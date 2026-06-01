# 🎨 HealthLens — Final Misc Audit: Style, Tooling, Assets, Localization

> **Ngày**: 2026-05-28
> **Phạm vi**: Phần "đuôi" cuối cùng — những góc nhỏ mà 5 review trước chưa cover.
> **Mục tiêu**: Đóng gói report cuối → đi đến **plan tổng hợp**.

---

## 1. 🎨 Code Style & Formatting — ZERO config

### 1.1 Hiện trạng

| Tool config | Trạng thái |
|-------------|-----------|
| `.editorconfig` | ❌ **Không có** |
| `apps/web/.prettierrc*` / `prettier.config.*` | ❌ **Không có** |
| `apps/web/eslint.config.mjs` | ✅ Có (Flat config) |
| `apps/mobile/eslint.config.js` | ✅ Có |
| Spotless / Checkstyle / google-java-format (Java) | ❌ **Không có** trong `build.gradle.kts` |
| Git hooks (Husky / Lefthook / simple-git-hooks) | ❌ **Không có** |
| `lint-staged` | ❌ **Không có** |
| Ruff / Black (Python OCR service) | ❌ **Không có** trong `requirements.txt` |

### 1.2 Hậu quả

1. **Tab/space drift** giữa contributors — code commit có thể mix 2 spaces, 4 spaces, tabs tùy editor mỗi người.
2. **Java code style không enforce** — 87 test files + ~200 main files chỉ dựa vào IDE convention từng người.
3. **Pre-commit không chặn lỗi** — ESLint chỉ chạy ở CI, không chạy ở client-side commit → push code broken → CI fail → cycle dài.
4. **Python OCR svc không format** — `app.py` 455 dòng có thể có format drift.

### 1.3 Action — Effort thấp, ROI cao

```
1. Add .editorconfig (root)                                     ← 5 min
2. Add Prettier + .prettierrc + .prettierignore (web + mobile)  ← 20 min
3. Add Husky + lint-staged                                      ← 30 min
4. Add Spotless plugin (apps/api/build.gradle.kts)              ← 30 min
5. Add Ruff config (services/ocr-service/pyproject.toml)        ← 15 min
6. CI step: spotless check + prettier check + ruff check        ← 30 min
```

Total: **~2h** cho cả style infrastructure.

---

## 2. 🐙 GitHub Repo Conventions — Missing

### 2.1 Hiện trạng

| File chuẩn | Trạng thái |
|------------|-----------|
| `.github/PULL_REQUEST_TEMPLATE.md` | ❌ |
| `.github/ISSUE_TEMPLATE/bug_report.md` | ❌ |
| `.github/ISSUE_TEMPLATE/feature_request.md` | ❌ |
| `.github/CODEOWNERS` | ❌ |
| `.github/dependabot.yml` | ❌ |
| `renovate.json` | ❌ |
| `.github/workflows/ci.yml` | ✅ |
| `.github/workflows/deploy.yml` | ✅ |

### 2.2 Hậu quả

- PR không có checklist → reviewer phải nhớ verify gì (test, doc, migration safety…).
- Issue không structured → bug report thiếu repro steps, env info.
- Không CODEOWNERS → auto-request review không enforced (đặc biệt risky cho `migration/`, `SecurityConfig.java`).
- Không dependabot → CVE alerts bị bỏ lỡ (đã flag review_gaps §8.1).

### 2.3 Action

```
1. Add .github/PULL_REQUEST_TEMPLATE.md (checklist: tests, docs, migration)  ← 15 min
2. Add .github/ISSUE_TEMPLATE/{bug_report,feature_request}.md                ← 20 min
3. Add .github/CODEOWNERS (DB migrations, security, billing paths)           ← 10 min
4. Add .github/dependabot.yml (pnpm + gradle + pip + docker)                 ← 15 min
```

Total: **~1h**.

---

## 3. 🖼️ Assets Audit

### 3.1 Web `apps/web/public/`

```
apple-touch-icon.png    32 KB    ✅ OK
brand/                 756 KB    ✅ Brand assets
icon-192.png            40 KB    ✅ OK (PWA candidate)
icon-512.png           264 KB    ✅ OK (PWA candidate)
file.svg                4 KB     ❌ Next.js template default
globe.svg               4 KB     ❌ Next.js template default
next.svg                4 KB     ❌ Next.js template default
vercel.svg              4 KB     ❌ Next.js template default
window.svg              4 KB     ❌ Next.js template default
```

→ **5 SVG file của Next.js template** vẫn còn — chiếm 20 KB nhưng không dùng. **DELETE**.

### 3.2 PWA — Có icon nhưng KHÔNG có manifest

```
✅ icon-192.png + icon-512.png    ← Đã có (Web App Manifest icons)
❌ apps/web/public/manifest.json   ← Không có
❌ apps/web/src/app/manifest.ts    ← Không có (Next.js 13+ convention)
```

→ Đã có sẵn 2 icon PWA nhưng **không có manifest** → trình duyệt không nhận đây là PWA, không cho "Add to Home Screen". Cần tạo `src/app/manifest.ts`:

```typescript
import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "HealthLens",
    short_name: "HealthLens",
    icons: [
      { src: "/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png" },
    ],
    theme_color: "...",
    background_color: "...",
    display: "standalone",
    start_url: "/",
  };
}
```

### 3.3 Mobile assets

```
apps/mobile/assets/images/
├── android-icon-background.png    ✅ OK
├── android-icon-foreground.png    ✅
├── android-icon-monochrome.png    ✅
├── expo-badge.png                 ❌ Expo template
├── expo-badge-white.png           ❌ Expo template
├── expo-logo.png                  ❌ Expo template
├── favicon.png                    ✅
├── icon.png                       ✅
├── logo-glow.png                  ✅
├── react-logo.png                 ❌ Expo template
├── react-logo@2x.png              ❌ Expo template
├── react-logo@3x.png              ❌ Expo template
├── splash-icon.png                ✅
└── tutorial-web.png               ❌? Verify còn dùng
```

→ **6+ expo template assets** chưa dọn. Vì mobile = placeholder (đã flag review_gaps §1.1), những asset này 100% chưa dùng.

### 3.4 API `apps/api/src/main/resources/fonts/Arial.ttf` — 🔴 LICENSING RISK

```
-rw-rw-r-- Arial.ttf  773 KB
```

**Arial** là proprietary font của **Monotype**, KHÔNG free cho redistribution. Embed trong production app → vi phạm bản quyền.

→ **Action**: Thay bằng font free tương đương:
- **DejaVu Sans** (free, supports Vietnamese)
- **Liberation Sans** (metric-compatible với Arial)
- **Noto Sans** (Google, free, full Unicode)

Đây là **medical app produce PDF** — PDF có thể bị distribute → high exposure risk.

### 3.5 `apps/api/src/main/resources/static/` empty

Folder rỗng. Hoặc xóa, hoặc add `.gitkeep` + document purpose.

---

## 4. 🌐 Localization Strategy

### 4.1 Quan sát

| Layer | Language strategy |
|-------|-------------------|
| AI prompt `metric-explanation.v3.txt` | **English instructions → Vietnamese output** ("exactly 3 short lines in Vietnamese") |
| AI prompt `recommendations.v8-medical-disclaimer-vi.txt` | **Vietnamese instructions + Vietnamese output** ("Bạn là trợ lý sức khỏe…") |
| Email templates HTML | **Vietnamese hardcoded** (`<title>Xác thực Email - HealthLens</title>`) |
| Java exception messages | **Vietnamese hardcoded** (88 chỗ — đã flag review #1 §2.3) |
| TS UI strings | **Vietnamese hardcoded** (no i18n library — đã flag review_gaps §9) |

### 4.2 Inconsistency cần fix

#### (a) 2 prompt convention khác nhau
- `metric-explanation.v3.txt`: English meta + Vietnamese instruction output
- `recommendations.v8-…-vi.txt`: Vietnamese hết
→ Quyết định convention chung: prompt instruction English (dễ maintain), output Vietnamese (user-facing).

#### (b) Prompt versioning trong filename
- `metric-explanation.v3.txt` (v3)
- `recommendations.v8-medical-disclaimer-vi.txt` (v8)
→ Không có changelog. Cần `docs/llm-prompt-templates.md` (đã có 26 LoC) document version history.

### 4.3 Single-locale acknowledgement

Toàn bộ app target Vietnamese users. Nếu **chưa có kế hoạch multi-locale**, KHÔNG cần i18n library — nhưng cần document quyết định này trong `docs/code-style.md` để contributor mới không tự ý add i18n stack.

---

## 5. 🔧 Missing Infrastructure (lightweight observation)

### 5.1 No OpenAPI codegen

API có `OpenApiConfig.java` (Spring Doc) → có thể generate OpenAPI 3 spec ở `/v3/api-docs`. Nhưng:
- ❌ Không có Gradle task export spec ra `docs/openapi.json`
- ❌ Không có TS client generation từ spec
- ❌ Không có contract test giữa Java OpenAPI ↔ web `routes.ts`

→ **Drift opportunity** giữa API và web mỗi khi endpoint thay đổi.

### 5.2 No feature flag library

```
grep "feature.*flag\|FeatureFlag" → 0 results
```

→ Khi muốn release feature dần (canary, A/B), không có infra. Nếu kế hoạch dài hạn cần A/B test → consider `Unleash` self-hosted hoặc `Statsig`.

### 5.3 No analytics / product tracking

```
grep "gtag|posthog|plausible|amplitude" → 0 results
```

→ Không đo lường được user behavior (funnel upload → review → confirm), bounce rate, retention. Nếu là product:
- Plausible (privacy-friendly, GDPR-compliant)
- PostHog (self-hosted option, có funnel + session replay)

### 5.4 No cookie consent banner

```
grep "cookie.*consent|cookieconsent" → 0 results
```

→ App thu thập sức khỏe (high-sensitivity PII) + presumably target EU/CCPA-relevant users → **GDPR risk nếu deploy production**.

Cần cookie consent banner cho:
- Analytics tracking cookie
- (Có thể) session cookie nếu non-essential

---

## 📋 Tóm tắt 6 review đã có

| # | File | Cover |
|---|------|-------|
| 1 | `project_review.md` | Hybrid structure, god classes, constants base |
| 2 | `deep_code_review.md` | SOLID, decomposition, adapter pattern, exception hierarchy |
| 3 | `folder_organization_solution.md` | Package-by-feature chi tiết + FSD web/mobile + ocr-service modules |
| 4 | `review_gaps.md` | Feature maturity, DB migrations, scripts, CI/CD, observability, security, a11y, deps |
| 5 | `repo_hygiene_audit.md` | docs/, BMAD output, AI tooling, standard repo files |
| 6 | `final_misc_audit.md` (this) | Style/format, GitHub conventions, assets, localization, infra |

---

## 🎯 Trả lời thẳng: ĐÃ HẾT.

Sau 6 review covering: code architecture, SOLID, folder organization, features maturity, infrastructure, ops, repo hygiene, BMAD compliance, style/formatting, assets, localization — **không còn mảng nào cần audit riêng**.

Còn thì chỉ là **deep-dive cụ thể vào 1 file** (ví dụ "audit dòng-by-dòng HealthRecordService.java") — nhưng đó là **execution**, không phải **discovery**.

→ Sẵn sàng cho **implementation plan tổng hợp**.

Sources:
- [Next.js App Router Manifest](https://nextjs.org/docs/app/api-reference/file-conventions/metadata/manifest)
- [Spotless Gradle Plugin](https://github.com/diffplug/spotless)
- [Conventional Commits](https://www.conventionalcommits.org/)
