---
stepsCompleted:
  - bmad-help-routing
  - step-01-validate-prerequisites-adapted
  - step-02-design-epics-adapted
  - step-03-create-stories-adapted
project_name: health-lens
user_name: ie303
date: "2026-05-16"
workflowType: review-remainder-epics
documentPurpose: "Backlog bổ sung cho các review findings còn lại sau khi tách core OCR/AI/RAG/infrastructure improvements"
sourceDocuments:
  - _bmad-output/planning-artifacts/core-review-docs-coverage-audit.md
  - _bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md
  - _bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md
  - _bmad-output/planning-artifacts/review-source/PRODUCTION-READINESS.md
  - _bmad-output/planning-artifacts/review-source/REVIEW-PRODUCTION-MASTER.md
  - _bmad-output/planning-artifacts/review-source/production-review/p0-gates-checklist.md
  - _bmad-output/planning-artifacts/review-source/production-review/epic-8-analytics-spec.md
---

# Remaining Production Review Epics And Stories

## Purpose

Tài liệu này gom các finding còn lại từ review docs sau khi đã tách riêng backlog `epic-core-improvements` cho OCR/LLM/RAG/provider/infrastructure/security baseline.

Không thay thế:

- `_bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md`
- `_bmad-output/implementation-artifacts/epic-core-improvements/`

Tài liệu này là backlog candidate để bạn chốt scope trước khi dùng `bmad-create-story` tạo implementation story files.

## Priority Legend

- **P0**: production blocker hoặc có rủi ro security/data loss cao.
- **P1**: nên làm trong improve phase trước khi go-live web production.
- **P2**: polish/cleanup/deferred.

## Epic List

1. **Web Production Readiness & Screen Correctness**
2. **Auth Token And Email-Link Hardening**
3. **Family Sharing Lifecycle Correctness**
4. **Admin Analytics Instrumentation**
5. **Mobile Deferred Readiness**
6. **Cleanup, Accessibility & Frontend Maintainability**

---

## Epic 1: Web Production Readiness & Screen Correctness

### Epic Goal

Đưa các màn hình web chính từ trạng thái "feature exists" lên trạng thái production-ready: dữ liệu thật, state đầy đủ, error/loading rõ ràng, không mất preview/file gốc, và không có optimistic update sai.

### Story 1.1: Dashboard Uses Real Latest Health Data

**Priority:** P0

As a user opening the dashboard, I want to see real latest health data instead of hardcoded stats, so that the home screen reflects my actual health record state.

**Acceptance Criteria**

1. Dashboard stat cards use query-backed data, not hardcoded fake values.
2. Empty/new-user state shows useful next action without fake health metrics.
3. Latest record summary links to the correct record/profile.
4. Loading/error states do not show misleading health status.

### Story 1.2: Health Record Review Detail State And Preview Correctness

**Priority:** P0

As a user reviewing a health record, I want the original document and extracted AI/metric context to remain visible across processing, failed, review, and done states, so that I can verify medical values before trusting them.

**Acceptance Criteria**

1. Original document preview is available in `processing`, `ocr_failed`, `review_required`, and `done` states where file permissions allow it.
2. Share action is removed from contexts where sharing a pending review result is unsafe or confusing.
3. AI explanation is positioned consistently with the intended review flow.
4. Explanation sections are not duplicated or fragmented.
5. Empty catch blocks are replaced with user-visible errors and telemetry.

### Story 1.3: Health Records Hub Mapping, Sorting And Status Fixes

**Priority:** P1

As a user browsing health records, I want record statuses, timestamps, and filters to be accurate, so that I can find and interpret records reliably.

**Acceptance Criteria**

1. `lastUpdated` and status mappings match backend record state.
2. Sorting is deterministic and handles missing dates safely.
3. Filter labels use standardized Vietnamese with diacritics.
4. Empty/loading/error states use shared components from the UI consistency backlog.

### Story 1.4: Profile And History Screen Type Safety And Edge Cases

**Priority:** P1

As a user managing profiles and history, I want profile/history screens to handle edge cases safely, so that shared or deleted profile state does not show broken data.

**Acceptance Criteria**

1. Unsafe casts in history/profile mapping are replaced with validated mapping functions.
2. Profile delete/set-default behavior is aligned with backend capabilities.
3. Profile CRUD forms do not reset before API success.
4. Shared profile access state is rendered consistently.

### Story 1.5: User Avatar Upload And Profile Photo Management

**Priority:** P2

As a HealthLens user, I want to upload and manage my own avatar, so that my account/profile identity is recognizable without relying on hardcoded or external avatar services.

**Acceptance Criteria**

1. User can upload a profile avatar from the profile settings page using an image-only control with clear loading, success, error, and keyboard-accessible states.
2. Backend stores avatar metadata for the authenticated user and exposes `avatarUrl` or an equivalent signed/display URL through `GET /api/v1/users/me`.
3. Uploaded avatar validation rejects unsupported content types, oversized files, empty files, and unsafe filenames before persisting metadata.
4. The profile settings page and profile cards stop using the hardcoded `ui-avatars.com` URL; fallback uses local initials/icon rendering derived from the user/profile name.
5. Replacing or removing an avatar cleans up the previous storage object where feasible and never blocks account/profile data updates.
6. Tests cover upload success, validation errors, storage failure, avatar replacement/removal, user response mapping, and frontend fallback rendering.

---

## Epic 2: Auth Token And Email-Link Hardening

### Epic Goal

Loại bỏ token/PII leak trong URL/history/referrer, bổ sung rate limiting/audit cho public auth endpoints, và chuẩn hóa UX cho các link nhạy cảm qua email.

### Story 2.1: Verify Email Token Hygiene And Rate Limiting

**Priority:** P0/P1

As a user verifying email, I want the verification link to avoid leaking token state, so that account verification remains safe even through browser history and logs.

**Acceptance Criteria**

1. Verification token is removed from URL immediately after extraction.
2. Referrer behavior prevents leaking token to third parties.
3. Verify-email endpoint has IP/email-based rate limiting.
4. User-facing messages do not reveal whether token exists, expired, or was already used.
5. Verify attempts are audit logged without raw token.

### Story 2.2: Verify Email Frontend Resilience And Accessibility

**Priority:** P2

As a user opening a verification link, I want clear loading, retry, and accessibility behavior, so that slow or failed verification is understandable.

**Acceptance Criteria**

1. Suspense/loading state uses a skeleton or stable loading component.
2. Request uses cleanup/abort behavior on unmount.
3. Expired/used token state offers a resend-verification path where supported.
4. Async state changes use `aria-live`/`role="status"` where appropriate.
5. Axios/backend errors are parsed with runtime-safe guards.

### Story 2.3: Cancel Deletion Link And Token Flow Hardening

**Priority:** P0/P1

As a user cancelling account deletion, I want cancellation to be secure and accurately explained, so that I do not lose data because of URL leaks or client-side expiry bugs.

**Acceptance Criteria**

1. Email and sensitive token values are not exposed in durable URLs, logs, or browser history beyond unavoidable one-time link constraints.
2. Backend remains source of truth for token expiry; client-side expiry never blocks a valid backend cancellation.
3. `409`, `401`, `403`, `429`, and `500` outcomes map to distinct frontend states.
4. Token normalization matches backend encoding behavior.
5. Cancellation flow has tests for happy path, expired token, replay, 409, 429, 500, missing token, loading, and double-submit.

### Story 2.4: Public Auth Endpoint Rate Limit And Error Semantics

**Priority:** P0/P1

As a security owner, I want public auth endpoints to be rate-limited and to avoid leaking sensitive state, so that brute-force and enumeration risks are reduced.

**Acceptance Criteria**

1. Register, verify-email, forgot-password, invite accept, cancel deletion, and OCR trigger endpoints have documented rate limits.
2. Rate-limit responses are consistently mapped in the web UI.
3. Forgot-password does not silently swallow email provider failures without telemetry.
4. Register handles `429` and invitation token propagation correctly.
5. Pending-deletion auth text matches backend error semantics.

### Story 2.5: Refresh Token Rotation Race Hardening

**Priority:** P0

As a user with an active session, I want refresh token rotation to be race-safe, so that stolen or concurrently reused tokens cannot silently preserve access.

**Acceptance Criteria**

1. Concurrent refresh requests cannot both mint valid sessions from the same old token.
2. Reuse of an already-rotated token is detected and invalidates the affected session family according to policy.
3. Token reuse events are audit logged without raw token.
4. Tests cover normal rotation, concurrent rotation, stolen-token replay, and logout.

### Story 2.6: Forgot Password Page Refactor And Public Auth Link Consistency

**Priority:** P2

As a user recovering access to my account, I want the forgot-password flow to be visually and behaviorally consistent with login and public support links, so that I can recover safely and reach privacy, terms, and help information without dead links.

**Acceptance Criteria**

1. Forgot-password page uses the same auth-page layout conventions as login/register: stable header, restrained card radius, no decorative background blobs, consistent button/input sizing, and no layout shift between form and success state.
2. Forgot-password submission keeps enumeration-safe behavior while giving clear, accessible status text for success, network failure, generic server failure, and `429` rate limit.
3. The forgot-password flow does not desynchronize user/auth state: unauthenticated users remain unauthenticated, existing logged-in state is not mutated, and any post-reset/login guidance returns to the correct public auth route.
4. Login footer links `Quy định bảo mật`, `Điều khoản sử dụng`, and `Trợ giúp` point to real routes instead of `#`; forgot-password uses the same link map and labels.
5. Public legal/help routes exist or links target existing routes, with safe behavior for unauthenticated users. Dashboard-only `/help` must not be linked directly from public auth pages if it redirects to login.
6. Header help icons on login, register, forgot-password, and reset-password either navigate to the same public help route or are removed if no public support route exists.
7. Tests cover link hrefs, forgot-password success/error/rate-limit states, accessible live regions, and absence of broken `#`/missing-route links on public auth pages.

---

## Epic 3: Family Sharing Lifecycle Correctness

### Epic Goal

Đảm bảo family sharing đúng quyền, không leak token, không race giữa accept/revoke, và lifecycle event có audit/notification đầy đủ.

### Story 3.1: Invitation Accept Token And Storage Hygiene

**Priority:** P0/P1

As an invited family member, I want invitation acceptance to avoid token leaks and storage crashes, so that accepting access is safe across browsers.

**Acceptance Criteria**

1. Invitation token is removed from URL/history as early as possible.
2. `sessionStorage` access is wrapped with safe fallback behavior.
3. Expired/invalid invitation state does not silently redirect to a confusing page.
4. Accept outcome type matches backend response contract.
5. Accept endpoint has rate limiting.

### Story 3.2: Accept/Revoke Race And Share Enforcement

**Priority:** P0

As a profile owner, I want revoke and accept operations to be race-safe, so that revoked access cannot become active again.

**Acceptance Criteria**

1. Concurrent accept and revoke result in one valid final state.
2. Duplicate `ProfileShare` rows cannot be created by concurrent accepts.
3. Revoked users lose access immediately across API and UI.
4. Wrong revoke fallback ID behavior is fixed and tested.
5. Tests cover owner/viewer invite, accept, revoke, concurrent accept, concurrent revoke, and post-revoke access.

### Story 3.3: Sharing Lifecycle Audit And Owner Notification

**Priority:** P1

As a profile owner, I want sharing lifecycle events to be auditable and visible, so that I know who gained or lost access to my health profile.

**Acceptance Criteria**

1. Invite, accept, reject, cancel, revoke, and failed access attempts produce audit events.
2. Owner receives notification when invitation is accepted if product policy requires it.
3. Audit metadata includes actor, owner, viewer, profile, invitation, correlation id, and outcome.
4. No raw invitation token or full email is logged.

---

## Epic 4: Admin Analytics Instrumentation

### Epic Goal

Đảm bảo admin analytics không phải stub/runtime log mà là DB-backed, event-backed, filterable, và đủ cho các charts 8.1/8.2/8.3.

### Story 4.1: Product Event Instrumentation For Admin Analytics

**Priority:** P1

As an admin, I want product events recorded in a queryable store, so that analytics charts reflect real usage.

**Acceptance Criteria**

1. Events exist for `USER_REGISTERED`, `UPLOAD_STARTED`, `UPLOAD_CONFIRMED`, `OCR_COMPLETED`, and `OCR_FAILED`.
2. Events include required dimensions: user/profile/record/file type/provider/confidence/failure reason where applicable.
3. Events are queryable without scanning raw logs.
4. Event writes do not block user-critical flow unnecessarily and have failure telemetry.

### Story 4.2: Admin Analytics Dashboard DB-Backed Charts

**Priority:** P1

As an admin, I want analytics charts backed by stored events, so that user growth, WAU/upload volume, and OCR success/failure rates are trustworthy.

**Acceptance Criteria**

1. `/admin/analytics` renders DB-backed user growth.
2. `/admin/analytics` renders WAU and upload volume.
3. `/admin/analytics` renders upload/OCR success and failure rate.
4. Charts support date range and empty/error states.
5. Stub/static chart data is removed.

---

## Epic 5: Mobile Deferred Readiness

### Epic Goal

Làm rõ mobile có thuộc release scope không. Nếu có, đóng các blocker tối thiểu; nếu không, đánh dấu deferred rõ trong sprint/status/docs.

### Story 5.1: Mobile Scope Decision And Deferred Work Tracking

**Priority:** P1 if mobile is in scope, otherwise P2

As a product owner, I want mobile scope explicitly decided, so that production readiness does not silently assume mobile is ready.

**Acceptance Criteria**

1. Mobile release scope is documented as in-scope or deferred.
2. Deferred mobile findings are linked from sprint/deferred-work docs.
3. Missing `@healthlens/shared` dependency is resolved or explicitly deferred.
4. Mobile CI/EAS Build status is documented.

### Story 5.2: Mobile Minimum Functional Surface

**Priority:** P1 if mobile is in release scope

As a mobile user, I want the basic HealthLens flows to work on mobile, so that dashboard, upload/OCR, results, profiles, settings, and family sharing are usable.

**Acceptance Criteria**

1. Mobile dashboard, upload OCR, results, profiles, settings, and family sharing routes are implemented or explicitly scoped out.
2. Camera OCR path has permission/error handling.
3. Offline-readonly history behavior is aligned with existing mobile epic.
4. Mobile CI covers build and smoke tests.

---

## Epic 6: Cleanup, Accessibility & Frontend Maintainability

### Epic Goal

Gom các P2/P3 cleanup không nên lẫn vào core improvements nhưng vẫn cần track để giảm nợ kỹ thuật và cải thiện accessibility.

### Story 6.1: Frontend Dead Code And Export Cleanup

**Priority:** P2

As a developer, I want dead exports and unused code removed, so that the frontend codebase is easier to maintain.

**Acceptance Criteria**

1. Empty barrel files are removed or populated with intentional exports.
2. Dead `API_TIMEOUT` re-export is removed or documented.
3. Unused imports and redundant `console.error` calls are cleaned where safe.
4. Lint rules catch reintroduced obvious unused exports.

### Story 6.2: Accessibility And Shared UI Components Cleanup

**Priority:** P2

As a user relying on assistive technology, I want forms and dynamic states to be accessible, so that HealthLens remains usable beyond visual interaction.

**Acceptance Criteria**

1. Form labels have correct `htmlFor/id`.
2. Async status/error states use appropriate live regions.
3. Shared HealthMetricsGrid and ReferenceRangeIndicator are extracted where duplication exists.
4. Loading skeleton and empty state components are reused consistently.

### Story 6.3: Frontend Bundle And Image Rule Cleanup

**Priority:** P3

As a maintainer, I want frontend bundle and image warnings addressed, so that build output remains clean and predictable.

**Acceptance Criteria**

1. `@radix-ui/themes` usage is reviewed for tree-shaking impact.
2. `@next/next/no-img-element` warnings are resolved or explicitly justified.
3. Bundle-impact changes are measured before/after where practical.

## Recommended Next Step

Chốt một trong hai hướng:

1. **Production web first**: tạo implementation story files cho Epic 1-4 trước, defer mobile/cleanup.
2. **Full review cleanup**: tạo implementation story files cho toàn bộ Epic 1-6, nhưng sprint planning sẽ lớn hơn và cần chia phase rõ.
