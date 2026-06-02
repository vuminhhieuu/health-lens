# 🧩 HealthLens — Review Gap Analysis

> **Ngày**: 2026-05-28
> **Mục đích**: Liệt kê các **mảng review còn thiếu** sau 3 file đã có (`project_review.md`, `deep_code_review.md`, `folder_organization_solution.md`).
> **Cách đọc**: Mỗi mục gồm (1) **Hiện trạng quan sát được** từ code, (2) **Mức độ trưởng thành** (Stub / Partial / Production-ready), (3) **Risk / Action**.

---

## 🎯 Tóm tắt: 3 review đã cover gì, còn thiếu gì?

| Khía cạnh | project_review | deep_code_review | folder_org_solution | **CẦN BỔ SUNG** |
|-----------|:-:|:-:|:-:|:-:|
| Folder structure | ✅ | — | ✅✅ | — |
| SOLID violations | — | ✅ | — | — |
| God classes / components | ✅ | ✅ | — | — |
| Constants duplication | ✅ | — | ✅ | ⚠️ **Per-file deep audit** chưa có |
| **Feature maturity (chức năng đạt mức nào)** | — | — | — | 🔴 **THIẾU HOÀN TOÀN** |
| **DB / Migrations** | — | — | — | 🔴 **THIẾU HOÀN TOÀN** |
| **Scripts (CI, docker, infisical, package.json)** | — | — | — | 🔴 **THIẾU HOÀN TOÀN** |
| **Email templates inconsistency** | — | ✅ (mục 1.1) | — | ⚠️ Cần liệt kê cụ thể |
| **Stub / TODO code in production paths** | — | — | — | 🔴 **THIẾU** |
| **Mobile app real state** | ✅ (folder only) | — | ✅ (folder only) | 🔴 **Functional state thiếu** |
| **Test coverage strategy** | — | — | — | 🔴 **THIẾU** |
| **Observability / Monitoring** | — | — | — | 🔴 **THIẾU** |
| **Performance / N+1 / Caching** | ✅ (1 caching note) | — | — | ⚠️ Sơ sài |
| **Security depth (auth, rate limit, secrets, SSRF)** | — | — | — | 🔴 **THIẾU** |
| **Accessibility (a11y) + i18n** | — | — | — | 🔴 **THIẾU** |
| **Next.js conventions (error/loading/not-found)** | — | — | — | 🔴 **THIẾU** |
| **Dependency audit** | — | — | — | 🔴 **THIẾU** |
| **Docs consistency vs code** | — | — | — | 🔴 **THIẾU** |

---

## 1. 🚦 Feature Maturity — Trạng thái thực tế từng chức năng

Cái này **chưa có trong cả 3 review** — chỉ mới review code chất lượng, chưa review **chức năng đang ở mức nào**.

### 1.1 Map feature ↔ controller ↔ web page

| # | Feature | Backend (controller) | Web pages | Mobile | Mức trưởng thành | Gap |
|---|---------|----------------------|-----------|--------|:----------------:|-----|
| 1 | **Authentication** (login/register/reset/verify) | `AuthController` | 5 pages (`(auth)/*`) | ❌ | 🟢 Production-ready | OK |
| 2 | **User profile & TOTP** | `UserController`, `UserTotpController` | `settings/profile`, `settings/security` | ❌ | 🟢 Production-ready | OK |
| 3 | **Patient profiles** (multi-patient per user) | `ProfileController`, `SharedProfileController` | `profiles`, `profiles/[id]/history` | ❌ | 🟢 Production-ready | OK |
| 4 | **Profile sharing / invitations** | `InvitationController`, `HealthRecordInvitationController` | `(auth)/invitations/accept`, `(auth)/health-record-invitations/accept` | ❌ | 🟢 Production-ready | OK |
| 5 | **Health record upload + OCR** | `HealthRecordController`, `OcrController` | `health-records`, `health-records/review/[id]` | ❌ | 🟡 **Partial** | God-service (1,710 dòng); review page (2,782 dòng) |
| 6 | **OCR provider chain** | `OcrService` + 4 providers | — | — | 🟡 **Partial** | `AwsTextractClient` là **STUB** (TODO comment) — review #2 không bắt được |
| 7 | **LLM metric explanation (RAG)** | `MetricExplanationRetrievalService` | review page modal | ❌ | 🟢 Production-ready | Đã có corpus version, citations |
| 8 | **Recommendations (LLM)** | `LlmService` | review page section | ❌ | 🟢 Production-ready | Hardcoded prompt v8 |
| 9 | **PDF export** | `HealthRecordPdfService` | review page button | ❌ | 🟢 Production-ready | — |
| 10 | **Follow-up reminders** | `FollowUpReminderController` | `follow-up-reminders` | ❌ | 🟢 Production-ready | — |
| 11 | **Notifications** (in-app + email) | `NotificationController` | NotificationBell + `(dashboard)/*` | ❌ | 🟢 Production-ready | Email service consistency |
| 12 | **Consent management** | `ConsentController` | ConsentModal | ❌ | 🟢 Production-ready | — |
| 13 | **Account deletion (GDPR-like)** | `DataDeletionService` | `settings/delete-account`, `cancel-deletion` | ❌ | 🟢 Production-ready | — |
| 14 | **Admin: Analytics** | `AdminAnalyticsController` | `admin/analytics` | — | 🟢 Production-ready | — |
| 15 | **Admin: Reference data CRUD** | `AdminReferenceDataController` + workflow | `admin/reference-data` + approvals + import | — | 🟢 Production-ready | God-component (1,098 + 1,025 LoC) |
| 16 | **Admin: Audit log** | `AdminAuditLogController` | `admin/audit-log` | — | 🟢 Production-ready | — |
| 17 | **Admin: Online RAG citations** | `AdminOnlineRagCitationController` | — | — | 🟡 **Partial** | Backend có, web UI chưa có (?) |
| 18 | **Visit summary** | (?) | `visit-summary` | ❌ | 🟡 **Partial** | Cần verify backend coverage |
| 19 | **Guide / FAQ / Marketing** | — | `guide`, `(marketing)/*` | ❌ | 🟢 Production-ready | Static |
| 20 | **Mobile app** | — | — | ✅ chỉ có **placeholder** | 🔴 **Stub** | `app/index.tsx` chỉ render "HealthLens mobile placeholder" |

### 1.2 Phát hiện đáng chú ý

#### 🔴 Mobile app = chỉ là vỏ Expo

```bash
$ cat apps/mobile/app/index.tsx
export default function Home() {
  return (
    <View>
      <Text>HealthLens mobile placeholder</Text>
    </View>
  );
}
```

→ Mobile **chưa có chức năng**, nhưng đã có **54 dependencies** (`expo-camera`, `expo-document-picker`, `react-navigation`, `react-query`...) — tức là đã setup infra nhưng feature code = 0. Review #1 chỉ nói về folder, **chưa flag được rằng mobile = stub**.

#### 🔴 `AwsTextractClient` là **stub** trong production code path

```java
// apps/api/src/main/java/com/healthlens/api/service/AwsTextractClient.java:15
 * <p>TODO: Implement actual AWS Textract integration khi có AWS credentials.
```

Nhưng `OcrProviderRegistry` vẫn route đến `TextractOcrProvider` dựa trên config. Nếu provider này được pick mà chưa impl → request fail. Risk này **3 review trước không có**.

#### 🔴 `DevController` có endpoint dev — verify gating

```java
@RestController
@RequestMapping(ApiRoutes.DEV_BASE)
@Profile({"docker", "dev"})
public class DevController { … }
```

OK — đã gate bằng `@Profile`, nhưng:
- `"docker"` profile được dùng cho cả prod không? → Check `application-docker.yml`
- Risk: production deploy với profile `docker` → endpoint dev bị leak.

### 1.3 Action items

1. Trước khi tuyên bố feature OCR "done", **fix `AwsTextractClient` stub** hoặc remove khỏi registry.
2. Verify profile naming: phải có rõ ràng `dev`, `staging`, `prod`. Profile `docker` mơ hồ.
3. Quyết định mobile roadmap: build full hay drop dependencies (54 deps × hidden bloat).

---

## 2. 🔑 Constants — Per-file Deep Audit

Review #1 đã list 6 vấn đề constants (Gender, Pagination, FileSize, Status, Consent, Routes). **Còn thiếu**: audit nội dung từng file constants để tìm constant **vô dụng**, **không nhất quán naming**, hoặc **giá trị nghi vấn**.

### 2.1 Inventory constants files

| File | LoC | Constants count (≈) | Vấn đề riêng |
|------|-----|---------------------|--------------|
| `packages/shared/constants/api.ts` | 253 | ~40 | Trùng PAGINATION với env.ts (đã flag) |
| `packages/shared/constants/status.ts` | 196 | ~25 | `HealthRecordStatus` không khớp backend (đã flag) |
| `packages/shared/constants/error-codes.ts` | 230 | ~60 | **Định nghĩa nhưng backend không dùng** (đã flag review #1 §6.1) |
| `packages/shared/constants/index.ts` | 38 | ~10 | `UPLOAD_MAX_SIZE_BYTES` conflict (đã flag) |
| `packages/shared/constants/consent.ts` | 5 | 1 | `CONSENT_VERSION='1.0'` sync comment smell (đã flag) |
| `packages/shared/config/env.ts` | 158 | ~25 | Duplicate PAGINATION + OCR_CONFIG (đã flag) |
| `apps/api/.../constants/ConsentConstants.java` | 12 | 1 | Mirror với consent.ts (đã flag) |
| `apps/api/.../constants/SecurityConstants.java` | 13 | ~3 | OK |
| `apps/api/.../constants/ApiRoutes.java` | (?) | ~50 | Đối chiếu với `api.ts` — có khớp? |

### 2.2 Audit gap còn lại (chưa được review)

#### (a) Cross-language string convention
- TS shared: `lowercase` cho enum values (`'male'`, `'processing'`)
- Java entity: **mixed** — có chỗ `"processing"` (DB value), có chỗ `MALE` (Java enum constant)
- → **Chưa có quy ước**: TS dùng kebab/snake, Java dùng UPPER_SNAKE. Cần convention doc.

#### (b) Naming inconsistency cùng package
- `API_PAGINATION` vs `PAGINATION` (cùng giá trị, khác tên)
- `UPLOAD_MAX_SIZE_BYTES` vs `MAX_FILE_SIZE` vs `MAX_IMAGE_SIZE_BYTES`
- `OCR_CONFIG.MAX_FILE_SIZE` lồng nhau (object) vs phẳng

#### (c) Magic numbers ngoài constants
Cần grep:
```bash
grep -rn "Duration.ofSeconds\|Duration.ofMinutes\|Duration.ofHours" apps/api/src/main/java
```
→ Nhiều cache TTL, presigned URL TTL được hardcode inline (review #1 §4.2 đã nêu cache 5s).

#### (d) Locale strings — chưa có file message bundle nào
```
apps/api/src/main/resources/  ← KHÔNG có messages_vi.properties hay i18n bundle
```
→ Mọi Vietnamese string đều hardcoded trong code (review #2 §5.1 nêu 88 chỗ).

### 2.3 Action items bổ sung

1. Tạo **CODESTYLE.md** (hoặc trong CLAUDE.md) ghi rõ convention naming constants giữa Java và TS.
2. Audit Duration/TTL — extract sang `TtlConstants.java` + `ttl.ts`.
3. Đặt nền cho i18n bundle (Vietnamese) — kể cả chưa multi-locale, bundle hóa cũng giúp dễ sửa text.

---

## 3. 🗄️ Database / Migrations — Hoàn toàn chưa review

### 3.1 Hiện trạng

- **50 migrations** (`V001` → `V050`), Flyway versioning
- File naming `V###__snake_case.sql` ✅ chuẩn
- **22/50** migrations có FK với `ON DELETE`
- **58 `CREATE INDEX`** statements
- **0 repeatable migrations** (`R__*`) — chưa dùng (có thể OK)

### 3.2 Vấn đề chưa được review

#### (a) Migration sprawl — 50 file trong 10 tháng phát triển
Có **những migration "patch" rất nhỏ**:
- `V022__add_backup_codes_to_admin_totp.sql`
- `V023__add_last_record_at_to_profiles.sql`
- `V029__add_access_level_to_health_record_share_tables.sql`
- `V030__add_pdf_download_audit_metadata.sql`
- `V036__add_user_avatar_metadata.sql`

→ Pattern: nhiều migration `ADD COLUMN` lẻ tẻ. Không phải sai, nhưng cần verify:
- Mỗi `ADD COLUMN` có default + `NOT NULL` cho rollback safety?
- Có migration nào **block** trên production table lớn? (PostgreSQL `ADD COLUMN NOT NULL DEFAULT x` rewrite table → lock dài)

#### (b) Không có rollback strategy
Flyway free version không hỗ trợ undo migrations natively. Đã có chiến lược rollback (forward-only) chưa? **Không thấy doc.**

#### (c) Index quality
58 CREATE INDEX — cần verify:
- Có index "vô dụng" không? (column low cardinality, không nằm trong WHERE thực tế)
- Có index thiếu cho slow query? (cần lấy slow log)
- Composite index có đúng thứ tự column không?

#### (d) `IF NOT EXISTS` overuse — masks bugs
`V049` dùng `ADD COLUMN IF NOT EXISTS` → nếu schema drift, Flyway sẽ silent skip. Production usually `ADD COLUMN` không IF NOT EXISTS để fail fast.

#### (e) Naming inconsistency
- `V016__add_health_record_soft_delete_and_audit.sql` (compound name)
- `V050__backfill_ocr_terminal_activity_events.sql` (action: backfill)
- `V045__notification_inbox_read_snapshot.sql` (no verb)
→ Cần convention: bắt đầu bằng verb (`create_`, `add_`, `alter_`, `backfill_`, `drop_`).

#### (f) Backfill in same migration as schema change
`V050__backfill_*` là dedicated backfill migration ✅ tốt.
Nhưng nếu backfill chạy SLOW trên prod → block deploy. Cần:
- Threshold time check
- Batched backfill pattern (LIMIT/OFFSET hoặc cursor)

### 3.3 Action items

1. **Audit slowest queries** trên staging DB → đối chiếu index.
2. **Verify mỗi ADD COLUMN NOT NULL** có default phù hợp, không gây table rewrite.
3. **Document rollback procedure** trong `docs/operations-runbook.md`.
4. **Migration naming convention** doc.

---

## 4. 🛠️ Scripts — Audit toàn bộ

### 4.1 Inventory

| Location | Files | Purpose | Status |
|----------|-------|---------|--------|
| `package.json` (root) | 3 scripts (`dev`, `build`, `test`) | Workspace runner | 🟢 OK, minimal |
| `apps/web/package.json` | 5 scripts | next dev/build/start/lint/test | 🟢 OK |
| `apps/mobile/package.json` | 6 scripts | expo start/android/ios/web/lint/reset | 🟢 OK |
| `apps/web/scripts/` | `smoke-sitemap-urls.mjs` | SEO smoke test | 🟢 OK |
| `apps/mobile/scripts/` | `reset-project.js` | Expo template helper | 🟢 OK |
| `docker/scripts/` | `up.sh`, `down.sh`, `logs.sh`, `cleanup.sh` | Compose wrappers | ⚠️ Verify |
| `infisical/scripts/` | `infisical.sh` | Secret manager wrapper | ⚠️ Verify |

### 4.2 Vấn đề chưa được review

#### (a) Không có script `test:e2e` hoặc `test:integration`
- Web: `test` chỉ chạy `vitest` (unit tests)
- API: Gradle có `test` task nhưng không có Testcontainers-only task
- → Không có script chạy E2E end-to-end (API + web + DB thật)

#### (b) Không có script `format` / `prettier`
- Root `package.json` thiếu `format` script
- Mỗi sub-app có ESLint nhưng không có Prettier wiring
- Risk: code style drift giữa contributors

#### (c) Mobile thiếu `test`
```json
"scripts": {
  "start": "expo start",
  "android": "expo start --android",
  ...
  "lint": "expo lint"
  // ❌ Không có "test"
}
```
→ Root-level `pnpm test` chạy `-r --if-present test` → mobile bị skip.

#### (d) Docker scripts có handle multi-env?
`docker/scripts/up.sh` đọc gì? `compose.yml` + override? Cần verify từng script.

#### (e) Không có script `db:migrate`, `db:reset`, `db:seed`
- Migration tự chạy lúc Spring Boot start (Flyway auto-migrate)
- Nhưng local dev nếu cần reset DB → phải `docker compose down -v` → mất data
- Cần script tiện ích: `pnpm db:reset`, `pnpm db:seed`

### 4.3 Action items

1. Thêm `test:integration`, `test:e2e` scripts.
2. Add Prettier + `pnpm format`.
3. Mobile: add Vitest/Jest + `test` script.
4. Tạo `db:reset`, `db:seed` (có dùng `pg_dump`/`pg_restore` cho fixture).

---

## 5. 📧 Email Templates — Inconsistency Cụ Thể

Review #2 §1.1 đã nói "EmailService 550 dòng HTML hardcoded". Bổ sung **map cụ thể**:

| Method | Template file | Cách render |
|--------|---------------|-------------|
| `sendVerificationEmail` | `templates/email/verification.html` | Thymeleaf ✅ |
| `sendProfileInvitationEmail` | `templates/email/profile-invitation.html` | Thymeleaf ✅ |
| `sendProfileShareAcceptedEmail` | `templates/email/profile-share-accepted.html` | Thymeleaf ✅ |
| `sendDeletionConfirmationEmail` | `templates/email/deletion-request.html` | Thymeleaf ✅ |
| **`sendPasswordResetEmail`** | ❌ | Inline `.formatted()` |
| **`sendCancellationConfirmationEmail`** | ❌ | Inline `.formatted()` |
| **`sendDeletionCompletionEmail`** | ❌ | Inline `.formatted()` |
| **`sendHealthRecordInvitationEmail`** | ❌ | Inline `.formatted()` |
| **`sendFollowUpReminderEmail`** | ❌ | Inline `.formatted()` |

→ **5/9 methods chưa migrate sang Thymeleaf**. Hành động cụ thể (5 file template cần tạo):
- `templates/email/password-reset.html`
- `templates/email/deletion-cancelled.html`
- `templates/email/deletion-completed.html`
- `templates/email/health-record-invitation.html`
- `templates/email/follow-up-reminder.html`

---

## 6. 🧪 Test Coverage — Strategy Gap

### 6.1 Số liệu

| Module | Test files | Notes |
|--------|-----------|-------|
| `apps/api` | **87** Java test files | Có Testcontainers setup |
| `apps/web` | **22** colocated `.test.ts(x)` | Vitest |
| `apps/mobile` | **0** | ❌ Không có test infra |
| `services/ocr-service` | **1** (sai chỗ trong `__pycache__/`) | Không có pytest folder |

### 6.2 Vấn đề chưa được review

#### (a) Web 22 test vs 38 pages = ~58% pages có test?
Cần đo coverage thực sự. 22 test có thể là util tests, không phải page tests.

#### (b) Không thấy E2E test (Playwright/Cypress)
Critical flows như "upload → OCR → review → confirm" cần E2E. Hiện không có.

#### (c) Contract test giữa shared package ↔ Java backend
`@healthlens/shared` định nghĩa types/constants. Backend không import được TS → không có cách enforce contract khớp ở compile time.
- Đề xuất: contract test bằng OpenAPI generation + diff.

#### (d) API có 87 test nhưng `HealthRecordService` 1,710 dòng — test cover hết case?
Cần đo coverage:
```bash
./gradlew jacocoTestReport
```
Nếu coverage < 70% trên god-service → risk khi refactor.

### 6.3 Action items

1. Add Playwright E2E setup → cover top 5 critical flows.
2. Mobile: chọn Jest Expo hoặc Vitest + setup.
3. Generate JaCoCo report, set threshold trong CI.
4. OpenAPI codegen từ Java → TS client → eliminate `routes.ts` drift.

---

## 7. 📡 Observability — Hoàn toàn chưa review

### 7.1 Hiện trạng

```bash
$ grep -rn "@Counted\|@Timed\|Micrometer\|Observation" apps/api/src/main/java
(no results)
```

→ **Không có custom metrics** mặc dù Spring Boot Actuator bundled.

### 7.2 Gap

| Concern | Hiện trạng | Cần |
|---------|------------|-----|
| **Health check endpoints** | ✅ Actuator + OCR svc có `/health` | OK |
| **Metrics** (request count, latency, errors) | ❌ Không có custom | Add `@Timed` cho hot path, expose `/actuator/prometheus` |
| **Distributed tracing** | ❌ Không thấy Sleuth/OpenTelemetry | Add OpenTelemetry SDK |
| **Correlation ID** | ✅ `CorrelationIdFilter` đã có | OK — verify propagate sang OCR svc |
| **Structured logging** | ⚠️ Cần verify JSON layout | Verify `logback-spring.xml` |
| **Error tracking** (Sentry/Rollbar) | ❌ Không thấy | Add Sentry SDK cho cả Java + web |
| **Alerting** | ❌ Không thấy doc | Document trong runbook |

### 7.3 Action items

1. Add Micrometer custom metrics cho OCR pipeline (provider success rate, latency).
2. Add Sentry/Rollbar cho production error tracking.
3. Verify CorrelationID propagate xuyên OCR service.

---

## 8. 🔒 Security Deep Audit

Có **6 rate limiter classes** + 20 endpoints rate-limited. Tốt. Nhưng:

### 8.1 Gap chưa review

| Area | Hiện trạng | Cần kiểm tra |
|------|-----------|--------------|
| **JWT secret rotation** | Static? | Verify rotation strategy |
| **CSRF protection** | (?) | Web có `csrfToken` cho mutations? |
| **CORS config** | ✅ `SecurityConfig` | Verify allowed origins per env |
| **SSRF protection** | ✅ OCR svc có `test_ssrf_protection.py` | Verify còn dùng — file đang ở sai chỗ |
| **SQL injection** | ✅ JPA + 53 `@Query` | Verify không có raw `String.format` query |
| **XSS** | (?) | Verify all user content escaped — đặc biệt PDF generation |
| **Presigned URL TTL** | (?) | Verify TTL phù hợp (1h cho upload, ngắn cho download) |
| **Secret in logs** | (?) | Verify `AuditPiiMasker` cover hết PII fields |
| **Dependency vulnerabilities** | ❌ Không thấy `snyk`, `npm audit`, `dependabot` setup | Cần thêm vào CI |
| **Rate limit cho admin endpoints** | (?) | Verify admin path có rate limit không |
| **2FA recovery codes** | ✅ V022 đã add backup codes | OK |

### 8.2 Action items

1. Add `dependabot.yml` hoặc `snyk` scan vào CI.
2. Verify CSRF flow trong web (Next.js → Spring Security).
3. Document threat model trong `docs/security.md` (chưa có).

---

## 9. 🌐 Web Accessibility (a11y) + i18n

### 9.1 Hiện trạng

```bash
$ grep -rln "aria-\|role=" apps/web/src | wc -l
68 files có a11y attributes
$ grep -rln "useTranslation\|i18next\|Intl\." apps/web/src
5 files dùng Intl (locale-aware date/number)
```

### 9.2 Gap

- **i18n library**: chưa có (next-intl, next-i18next). Tất cả text Vietnamese inline → hardcoded
- **A11y testing**: chưa có axe/Lighthouse trong CI
- **Color contrast**: chưa verify với WCAG
- **Keyboard navigation**: chưa có doc/test cho tab order trong modals
- **Screen reader**: 68/38+ pages có aria attr — random coverage

### 9.3 Action items

1. Quyết định locale strategy: single-locale (vi) hay multi-locale → nếu single, OK; nếu multi, cần setup ngay.
2. Add `eslint-plugin-jsx-a11y` strict rules.
3. Add Lighthouse CI vào `.github/workflows/ci.yml`.

---

## 10. 📑 Next.js Conventions Missing

```bash
$ find apps/web/src/app -name "loading.tsx" -o -name "not-found.tsx" -o -name "error.tsx"
(empty)
```

→ **38 pages, 0 file loading.tsx / not-found.tsx / error.tsx**.

Next.js App Router conventions không được dùng:
- `loading.tsx` → Suspense boundary cho UX skeleton
- `error.tsx` → Error boundary local
- `not-found.tsx` → 404 page
- `template.tsx` → re-mount on navigation

→ **Toàn bộ loading state + error state phải handle thủ công trong từng page** → 30+ useState đếm trong review #2 §6.

### 10.1 Action items

1. Add `(dashboard)/loading.tsx`, `(dashboard)/error.tsx` cho route group
2. Add root `not-found.tsx`
3. Migrate `isLoading` useState patterns → Suspense

---

## 11. 📦 Dependency Audit

### 11.1 Mobile = stub nhưng 54 dependencies

`apps/mobile/package.json` declare:
- `expo-camera`, `expo-document-picker`, `expo-image-picker`
- `@react-navigation/{bottom-tabs,elements,native}` × 3
- `@tanstack/react-query`, `axios`, `react-hook-form`, `zod`

Trong khi `app/index.tsx` chỉ render placeholder text. → **Phantom dependencies**, tăng install time + bundle size.

### 11.2 Web deps

- **`lucide-react@^1.7.0`** — phiên bản này không tồn tại (`lucide-react` hiện ở v0.x). Có thể typo, cần verify
- **`next@16.2.1`** + **`react@19.2.4`** — bleeding edge → risk breaking changes
- AGENTS.md có cảnh báo "This is NOT the Next.js you know" → confirm bleeding-edge risk

### 11.3 API deps (build.gradle.kts)

- Spring Boot 4.0.3, Spring AI 2.0.0-**M4** (Milestone, không phải GA) → unstable

### 11.4 Action items

1. Remove unused mobile deps cho đến khi feature thật sự được build.
2. Verify `lucide-react@^1.7.0` resolve thật → có thể là `^0.x` typo.
3. Document risk Spring AI Milestone version trong runbook.
4. Add `dependabot.yml` để auto-update + flag CVEs.

---

## 12. 📚 Docs Consistency Audit

17 docs files (`docs/*.md`), **1,546 LoC tổng**. Cần verify:

| Doc | Cần audit |
|-----|-----------|
| `architecture.md` (94 LoC) | Có khớp với code không? (hybrid layer hiện tại) |
| `data-models.md` (61 LoC) | Có 37 entities — doc có liệt kê hết? |
| `api-contracts.md` (139 LoC) | Có khớp với 19 controllers? |
| `event-driven-architecture.md` (96 LoC) | Khớp với 2 Redis stream events? |
| `provider-switching-runbook.md` (138 LoC) | OK — operational |
| `source-tree-analysis.md` (72 LoC) | **Likely outdated** — hybrid structure đã đề xuất refactor |
| `STAGING_DEPLOYMENT.md` (265 LoC) | Dài nhất — verify staging env vẫn vận hành |
| `llm-prompt-templates.md` (26 LoC) | Prompt v8 đã update? |

### 12.1 Action items

1. Đối chiếu mỗi doc với code thực tế → mark "Last verified: 2026-05-28".
2. Auto-generate API contracts từ Spring Doc OpenAPI (đã có `OpenApiConfig.java`) → eliminate drift.

---

## 13. 🚀 CI/CD Workflow Audit

### 13.1 `.github/workflows/ci.yml` jobs

```
- lint (web + mobile)
- api-test
- api-build
- web-build
```

### 13.2 `.github/workflows/deploy.yml` jobs

```
- build (Docker images)
- deploy-production
- notify-failure
```

### 13.3 Gap

| Concern | Status |
|---------|--------|
| Lint web/mobile | ✅ |
| Type check shared | ✅ |
| API unit test | ✅ |
| API build | ✅ |
| Web build | ✅ |
| **OCR service test** | ❌ Không có job |
| **OCR service build** | ⚠️ Build via Docker chỉ trong deploy.yml |
| **Mobile test** | ❌ Không có test |
| **Mobile build (EAS)** | ❌ Không có |
| **Dependency scan (CVE)** | ❌ |
| **DB migration validation** | ❌ Không có job Flyway validate |
| **E2E test** | ❌ |
| **Lighthouse / a11y** | ❌ |
| **Coverage report** | ❌ |
| **Docker image vuln scan** (Trivy/Grype) | ❌ |
| **Staging deploy** | ❌ chỉ có prod |

### 13.4 Action items

1. Add OCR service test job (pytest).
2. Add Trivy scan trong deploy workflow.
3. Add coverage report (JaCoCo + Codecov).
4. Tách workflow: `staging-deploy.yml` riêng cho staging.

---

## 14. 🎁 Bonus: Performance / Caching

### 14.1 Quan sát từ code

- **129 `@Transactional`** trong service layer
- **8 `FetchType.*`** explicit — verify default `EAGER` không bị lạm dụng
- **0 `@OneToMany` / `@ManyToMany`** — không có N+1 risk hiển nhiên (good!)
- **53 `@Query`** — verify projection nào dùng `JOIN FETCH`

### 14.2 Cache strategy

- Redis cache: TTL 5s (review #1 §4.2 — vô dụng)
- Spring `@Cacheable`: cần audit usage
- Browser cache: web có `Cache-Control` đúng? Cần verify

### 14.3 Action items

1. Audit `@Cacheable` annotations.
2. Tăng cache TTL từ 5s → 5min cho status response (loại bỏ presigned URL khỏi cache key).
3. Add APM (Datadog/New Relic) nếu prod traffic > 100 RPS.

---

## 📋 Priority Recommendations cho 4 review tiếp theo

| Review file đề xuất | Mô tả | Effort |
|---------------------|-------|--------|
| `feature_maturity_audit.md` | Per-feature status: stub/partial/prod-ready, gating risks | M |
| `db_migrations_audit.md` | 50 migrations review + index quality + rollback strategy | M |
| `scripts_and_devops_audit.md` | Scripts + CI/CD + Docker compose audit | M |
| `security_and_observability_audit.md` | Security threat model + monitoring strategy | L |

---

## 🎯 TL;DR — Cái cần làm tiếp

3 review đã có là **code-quality oriented**. Cần thêm **operational + product**:

1. 🔴 **Feature maturity matrix** — feature nào prod-ready, feature nào stub (đặc biệt: mobile = placeholder, AwsTextract = TODO).
2. 🔴 **DB migrations audit** — 50 file chưa được rà soát chất lượng.
3. 🔴 **Scripts + CI/CD gaps** — thiếu E2E, Trivy, dependabot, mobile test, coverage.
4. 🔴 **Observability roadmap** — chưa có custom metrics, tracing, error tracking.
5. 🔴 **Email template gap** — 5/9 method chưa migrate Thymeleaf, có template file cụ thể cần tạo.
6. 🟡 **Constants deep audit** — magic numbers (Duration) chưa bundled.
7. 🟡 **Next.js conventions** — 38 pages, 0 loading/error/not-found.tsx.
8. 🟡 **Dependency hygiene** — mobile 54 deps cho code placeholder; verify lucide-react version.

Sources:
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [Next.js App Router Conventions](https://nextjs.org/docs/app/api-reference/file-conventions)
- [Flyway Migration Best Practices](https://documentation.red-gate.com/fd/migrations-184127470.html)
