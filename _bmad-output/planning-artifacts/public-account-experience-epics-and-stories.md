---
stepsCompleted:
  - step-01-validate-prerequisites-adapted
  - step-02-design-epics-adapted
  - step-03-create-stories-adapted
  - step-04-final-validation-adapted
  - consolidated-single-epic-2026-05-19
project_name: health-lens
user_name: ie303
date: "2026-05-19"
workflowType: public-account-experience
documentPurpose: "Một epic duy nhất — trang chủ công khai, cài đặt, thông báo (đã lược trùng với backlog hiện có)"
sourceDocuments:
  - _bmad-output/planning-artifacts/prd.md
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/ux-design-specification.md
  - _bmad-output/design-artifacts/SCREENS-FULL-LIST-VI.md
  - _bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md
  - _bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md
canonicalImplementationRoot: _bmad-output/implementation-artifacts/public-account-experience/
storyKeyPrefix: pae
storyCount: 12
---

# Epic: Public Account Experience (PAE)

## Epic Goal

Hoàn thiện trải nghiệm **trước và sau đăng nhập**: landing + SEO, pháp lý public, hub cài đặt, đổi mật khẩu, trung tâm thông báo — mà không trùng lặp backlog `remaining-production-review` đã có.

## Stories Loại Bỏ (trùng — không tạo file PAE)

| Đã lược | Thay bằng | Lý do |
|---------|-----------|-------|
| ~~Auth link map + `/support` public + forgot-password UX~~ | `remaining-2-6-forgot-password-page-refactor-and-public-auth-link-consistency` | AC trùng: footer `#`, public help, forgot-password layout |
| ~~Owner notification on share accept~~ | `remaining-3-3-sharing-lifecycle-audit-and-owner-notification` | Cùng scope email owner khi accept invite |

**Thứ tự phụ thuộc:** `pae-5` (privacy/terms) nên **done** trước hoặc song song đầu `remaining-2-6` (2.6 cần route `/privacy`, `/terms` tồn tại).

## Story List (12 stories — một epic)

| # | Key | Priority | Status file |
|---|-----|----------|-------------|
| 1 | `pae-1-settings-hub-and-navigation-fix` | P0 | ready-for-dev |
| 2 | `pae-2-marketing-route-group-and-root-routing` | P1 | backlog |
| 3 | `pae-3-public-landing-page-ssr` | P1 | backlog |
| 4 | `pae-4-seo-foundation-metadata-sitemap-robots` | P1 | backlog |
| 5 | `pae-5-public-privacy-and-terms-pages` | P0 | backlog |
| 6 | `pae-6-change-password-api-and-settings-ui` | P1 | backlog |
| 7 | `pae-7-settings-privacy-tab-consent-and-legal-links` | P2 | backlog |
| 8 | `pae-8-settings-about-tab-version-and-support` | P2 | backlog |
| 9 | `pae-9-profile-settings-stub-cleanup` | P2 | backlog |
| 10 | `pae-10-notification-inbox-aggregate-api` | P1 | backlog |
| 11 | `pae-11-notification-center-ui-and-bell-wiring` | P1 | backlog |
| 12 | `pae-12-notification-email-preferences` | P2 | backlog |

**Implement khuyến nghị:** `1` → `5` → (`remaining-2-6`) → `2` → `3` → `4` → `6` → `10` → `11` → `7` → `8` → `9` → `12` → (`remaining-3-3`)

---

### Story 1: Settings Hub And Navigation Fix

**Key:** `pae-1-settings-hub-and-navigation-fix` | **P0**

As a signed-in user, I want `/settings` to open a settings hub, so that navigation from sidebar and mobile tab works.

**Acceptance Criteria:** Hub 200 với links Profile, Security, Privacy, Notifications, About, Delete account; nav "Cài đặt" không 404; stub routes cho sections chưa build; Vitest smoke.

**File:** `public-account-experience/pae-1-settings-hub-and-navigation-fix.md`

---

### Story 2: Marketing Route Group And Root Routing

**Key:** `pae-2-marketing-route-group-and-root-routing` | **P1**

As a visitor, I want `/` to show public content without forced login redirect, so that marketing and SEO pages are reachable.

**Acceptance Criteria:** `(marketing)` route group; `/` không redirect `/home`; `/home` vẫn auth-guard; auth "Quay lại trang chủ" → `/`; `lang="vi"` + bỏ metadata boilerplate root.

---

### Story 3: Public Landing Page SSR

**Key:** `pae-3-public-landing-page-ssr` | **P1**

As a prospective user, I want a Vietnamese landing explaining HealthLens, so that I understand value before registering.

**Acceptance Criteria:** Hero, 3-step how-it-works, compliance/disclaimer, features, CTAs; SSR/RSC; responsive; teal brand; no fake patient data.

---

### Story 4: SEO Foundation

**Key:** `pae-4-seo-foundation-metadata-sitemap-robots` | **P1**

As a marketing owner, I want sitemap/robots/metadata, so that public pages are indexable correctly.

**Acceptance Criteria:** `sitemap.ts`, `robots.ts`, OG/title/description landing, favicon, disallow dashboard/admin in robots.

---

### Story 5: Public Privacy And Terms Pages

**Key:** `pae-5-public-privacy-and-terms-pages` | **P0**

As an unauthenticated user, I want `/privacy` and `/terms`, so that I can read policies before consent.

**Acceptance Criteria:** Public Vietnamese content; medical + NĐ 13 aligned with ConsentModal; no auth; in sitemap; enables `remaining-2-6` footer links.

---

### Story 6: Change Password API And UI

**Key:** `pae-6-change-password-api-and-settings-ui` | **P1**

As a signed-in user, I want to change password in settings, so that I can rotate credentials without email reset.

**Acceptance Criteria:** `POST /api/v1/auth/change-password`; RHF+Zod UI; `notify.*`; profile link real route; Java + shared constants + tests.

---

### Story 7: Settings Privacy Tab

**Key:** `pae-7-settings-privacy-tab-consent-and-legal-links` | **P2**

As a user, I want to review consent status in settings, so that I understand data handling.

**Acceptance Criteria:** `/settings/privacy` shows Consent API status; links `/privacy`, `/terms`; link delete-account; ConsentModal unchanged on login.

---

### Story 8: Settings About Tab

**Key:** `pae-8-settings-about-tab-version-and-support` | **P2**

As a user, I want version and support info in settings, so that I can get help.

**Acceptance Criteria:** `/settings/about` with version from env/build; legal + support links (reuse constants from `remaining-2-6` when done).

---

### Story 9: Profile Settings Stub Cleanup

**Key:** `pae-9-profile-settings-stub-cleanup` | **P2**

As a user, I want settings to hide non-functional stubs, so that I am not misled.

**Acceptance Criteria:** Remove/hide fake 2FA, hardcoded 85%, disabled health-info block or link to `/profiles`; support uses shared contact constants.

---

### Story 10: Notification Inbox Aggregate API

**Key:** `pae-10-notification-inbox-aggregate-api` | **P1**

As a signed-in user, I want one inbox API, so that the UI can show unified notifications.

**Acceptance Criteria:** `GET /api/v1/notifications/inbox`; types profile invite, record invite, optional reminders; reuse services; auth + tests.

---

### Story 11: Notification Center UI And Bell

**Key:** `pae-11-notification-center-ui-and-bell-wiring` | **P1**

As a signed-in user, I want the header bell to open notifications, so that I do not search across pages.

**Acceptance Criteria:** Drawer or `/notifications`; badge count; empty state; navigate to action; `aria-expanded`; depends on pae-10.

---

### Story 12: Notification Email Preferences

**Key:** `pae-12-notification-email-preferences` | **P2**

As a user, I want email toggles, so that I control non-security notifications.

**Acceptance Criteria:** DB + GET/PUT preferences; `/settings/notifications` UI; EmailService/scheduler respects toggles; migration + tests.

---

## FR Coverage (sau khi lược trùng)

| Requirement | Story |
|-------------|-------|
| Landing public | 2, 3 |
| SEO | 4 |
| Legal public | 5 (+ remaining-2-6 links) |
| Settings hub | 1 |
| Change password | 6 |
| Privacy tab | 7 |
| About | 8 |
| Inbox | 10, 11 |
| Email prefs | 12 |
| Auth public links | **remaining-2-6** |
| Owner notify accept | **remaining-3-3** |

## Sprint Integration

- Tất cả story files: `_bmad-output/implementation-artifacts/public-account-experience/pae-{N}-*.md`
- `sprint-status.yaml` → một epic `public-account-experience` + 12 keys `pae-1` … `pae-12`
