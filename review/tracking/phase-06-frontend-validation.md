# Phase 6 — Frontend Refactor + Validation Wiring

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 6](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 38h (tuần 9–10)
> **Goal**: FSD-lite migration, split god components, wire Zod schemas, add data export endpoint.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/06-frontend/{sub}`
- **PR(s)**: — (6–7 PRs)

---

## ✅ Pre-flight Checklist

- [ ] Phase 3 `🟢 Done` (shared foundation)
- [ ] Đã đọc `folder_organization_solution.md` §3 (FSD)
- [ ] Đã đọc `deep_code_review.md` §6 (Component decomposition)
- [ ] Đã đọc `review_gaps.md` §10 (Next.js conventions)
- [ ] Đã đọc `runtime_security_and_correctness_audit.md` §6.3, §10
- [ ] Branch: `git checkout -b phase/06-frontend main`

---

## 🎯 Tasks

### 6.1 Next.js Conventions (2h)
- [ ] `apps/web/src/app/not-found.tsx`
- [ ] `apps/web/src/app/(dashboard)/loading.tsx`
- [ ] `apps/web/src/app/(dashboard)/error.tsx`
- [ ] `apps/web/src/app/(auth)/error.tsx`
- [ ] Migrate 30+ `useState(isLoading)` → Suspense

### 6.2 PWA manifest (30m)
- [ ] `apps/web/src/app/manifest.ts`
- [ ] Test installable from supported web browsers

### 6.3 FSD-lite Migration (6h)
- [ ] Skeleton: `src/features/{health-records,upload,profiles,auth,admin,...}/`, `src/shared/`, `src/layouts/`
- [ ] Move components theo `folder_organization_solution.md` §3.3
- [ ] Split `src/lib/` → `src/shared/lib/` + `src/features/*/lib/`
- [ ] Update tsconfig path aliases
- [ ] Verify build

### 6.4 Split ReviewRecordPage (10h) ⚠️ CRITICAL
- [ ] `features/health-records/components/`:
  - [ ] `MetricsTable.tsx`
  - [ ] `MetricExplanation.tsx`
  - [ ] `RecordHeader.tsx`
  - [ ] `RecordActions.tsx`
  - [ ] `RecommendationsPanel.tsx`
  - [ ] `DocumentPreview.tsx`
  - [ ] `ShareDialog.tsx`
- [ ] `features/health-records/hooks/`:
  - [ ] `useHealthRecordDetail.ts`
  - [ ] `useMetricEditing.ts`
  - [ ] `useRecordActions.ts`
- [ ] Shell `review/[recordId]/page.tsx` ≤ 100 dòng
- [ ] Migrate 30+ useState → useReducer hoặc hooks

### 6.5 Split admin/reference-data (6h)
- [ ] `admin/reference-data/page.tsx` (1,025 LoC) → 3 sub-components
- [ ] `admin/reference-data/approvals/page.tsx` (1,098 LoC) → 3 sub-components

### 6.6 Split home + admin/login (4h)
- [ ] `(dashboard)/home/page.tsx` (744) → 4 feature sections
- [ ] `admin/login/page.tsx` (691) → 2 sub-components

### 6.7 Extract inline types (1h)
- [ ] Move 7 interfaces trong `review/page.tsx` → `features/health-records/types.ts`

### 6.8 Domain hooks (2h)
- [ ] `useHealthRecordDetail`
- [ ] `useMetricEditing`
- [ ] `useFollowUpReminders`
- [ ] `useReferenceData`

### 6.9 Email Templates Thymeleaf (4h)
- [ ] `templates/email/password-reset.html`
- [ ] `templates/email/deletion-cancelled.html`
- [ ] `templates/email/deletion-completed.html`
- [ ] `templates/email/health-record-invitation.html`
- [ ] `templates/email/follow-up-reminder.html`
- [ ] Update `EmailService.java` dùng Thymeleaf cho 5 method

### 6.10 Wire Zod Schemas (6h) ⚠️ NEW
- [ ] Audit: `grep -rn ".safeParse\|.parse(" apps/web/src` → 12 hiện tại
- [ ] Wire vào API client: parse input trước POST
- [ ] Wire vào react-hook-form với `zodResolver`
- [ ] CI check: schema declared phải có usage

### 6.11 User Data Export (4h) ⚠️ NEW
- [ ] Backend `GET /api/users/me/export`
  - ZIP gồm: `profile.json`, `health-records.json`, `metrics.json`, originals
  - Rate limit: 1/hour/user
- [ ] FE button trong `settings/privacy` → download
- [ ] Test all data present

---

## 🧪 Verification

```bash
pnpm -F @healthlens/web build
pnpm -F @healthlens/web test

# Component sizes
wc -l apps/web/src/features/health-records/components/*.tsx
# Expect: no file > 400 LoC

# Zod usage
grep -rn ".safeParse\|.parse(" apps/web/src | wc -l
# Expect: ≥ 50 (from 12)

# Data export
curl -H "Authorization: Bearer $TOKEN" /api/users/me/export -o export.zip
unzip -l export.zip
```

---

## 🏁 Phase Completion Checklist

- [ ] 11 task `[x]`
- [ ] `ReviewRecordPage` ≤ 100 LoC shell
- [ ] No component > 400 LoC
- [ ] Zod wired ≥ 80% forms
- [ ] `/users/me/export` returns valid ZIP
- [ ] Email templates: 9/9 Thymeleaf (was 4/9)
- [ ] Next.js conventions 5+ routes
- [ ] PWA installable
- [ ] E2E smoke pass
- [ ] Update DASHBOARD

---

## 📝 Retrospective

| | Before | After |
|--|:-:|:-:|
| ReviewRecordPage LoC | 2,783 | — |
| Zod usages | 12 | — |
| Inline email | 5/9 | 0/9 |

### Effort
| Task | Est | Actual |
|------|:-:|:-:|
| 6.1–6.2 | 2.5h | — |
| 6.3 FSD | 6h | — |
| 6.4 ReviewRecord | 10h | — |
| 6.5–6.6 | 10h | — |
| 6.7–6.8 | 3h | — |
| 6.9 Email | 4h | — |
| 6.10 Zod | 6h | — |
| 6.11 Export | 4h | — |
| **Total** | **38h** | — |
