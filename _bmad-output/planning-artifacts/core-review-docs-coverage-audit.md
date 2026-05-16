---
stepsCompleted:
  - bmad-help-routing
  - review-docs-inventory
  - core-improvements-coverage-check
  - synthesis-location-correction
project_name: health-lens
date: "2026-05-16"
documentPurpose: "Audit coverage between docs review files and BMad core improvement artifacts"
canonicalBacklog:
  - _bmad-output/planning-artifacts/core-feature-infra-improvement-epics-and-stories.md
  - _bmad-output/implementation-artifacts/epic-core-improvements/
canonicalSynthesis:
  - _bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md
---

# Core Review Docs Coverage Audit

## Kết Luận Ngắn

Nên rà soát thêm một lần nữa, và kết quả rà soát cho thấy:

- Các vấn đề **core feature/infrastructure** đã bàn trong đoạn chat này đã được gom khá đầy đủ vào `epic-core-improvements`.
- File synthesis trước đó đặt ở `docs/core-feature-infra-issues-synthesis.md` là chưa đúng vị trí theo quy ước BMad. File này đã được chuyển sang `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md`.
- Vẫn còn nhiều finding trong `_bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md` và các production review docs **không thuộc core-improvements hoặc chưa được story hóa chi tiết**. Những phần này không nên bị coi là đã xử lý xong chỉ vì `epic-core-improvements` đã có 35 stories.

## Review Docs Đã Kiểm Tra

- `_bmad-output/planning-artifacts/review-source/REVIEW-FULL-v2.md`
- `_bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md`
- `_bmad-output/planning-artifacts/review-source/PRODUCTION-READINESS.md`
- `_bmad-output/planning-artifacts/review-source/REVIEW-PRODUCTION-MASTER.md`
- `_bmad-output/planning-artifacts/review-source/production-review/p0-gates-checklist.md`
- `_bmad-output/planning-artifacts/review-source/production-review/audit-logging-mini-adr.md`
- `_bmad-output/planning-artifacts/review-source/production-review/epic-8-analytics-spec.md`
- `docs/STAGING_DEPLOYMENT.md`

Ghi chú: `_bmad-output/planning-artifacts/review-source/PRODUCTION-READINESS.md` và `_bmad-output/planning-artifacts/review-source/REVIEW-DISPOSITION.md` có nội dung/heading gần như cùng vai trò disposition review. Khi chuẩn hóa tài liệu sau này nên chọn một bản canonical hoặc ghi rõ bản nào là bản hiện hành.

## Phần Đã Cover Tốt Trong Epic Core Improvements

| Nhóm review | Coverage hiện tại | Artifact chính |
| --- | --- | --- |
| OCR PDF/image mismatch | Đã cover rõ bằng MIME-aware router, OCR contract, provider registry, PDF/image routing | Stories `1.1`, `1.2`, `2.2` |
| OCR reliability | Đã cover retry, idempotency, DLQ, confidence gate, review-required state | Stories `1.3`, `1.4` |
| PaddleOCR vs EasyOCR | Đã cover bằng benchmark/adaptor, không đổi provider theo cảm tính | Story `1.5` |
| External OCR fallback | Đã cover consent, retention mode, kill switch, audit metadata | Story `1.6` |
| Provider switching | Đã cover LLM/OCR/embedding/vector provider abstraction và runbook | Epic 2 |
| LLM safety | Đã cover prompt versioning, structured output, validation, rollback | Stories `3.1`, `3.2` |
| RAG readiness | Đã cover corpus governance, curated/internal RAG, online trusted-source RAG | Stories `3.3`, `3.4`, `3.5` |
| Observability/ops | Đã cover correlation id, metrics/alerting, deploy topology, backup/restore | Epic 4 |
| Security baseline | Đã cover JWT/CORS/CSRF fail-fast, SSRF, admin session hardening, right-to-delete cleanup | Epic 5 |
| Toast/language/UI consistency | Đã cover toast foundation, replace alert/local toast, Vietnamese normalization, loading/error states | Epic 6 |
| Code organization | Đã cover backend AI/OCR/RAG boundary, review page split, shared hook, email template cleanup | Epic 7 |
| CI/CD quality gates | Đã cover security scan, OCR regression corpus, provider/infra smoke tests | Epic 8 |

## Phần Review Còn Chưa Được Đụng Tới Đầy Đủ

Các phần dưới đây xuất hiện trong review docs nhưng chưa được story hóa đủ chi tiết trong `epic-core-improvements`. Một số có thể đã nằm trong epic gốc, nhưng chưa có artifact đồng bộ trong core improvement backlog.

### 1. UX Theo Màn Hình Từ REVIEW-FULL-v2

Chưa cover đủ:

- Dashboard home: hardcoded/fake stats, latest real metrics, glanceable status.
- Health record review/detail: xóa nút chia sẻ sai ngữ cảnh, đưa AI explanation lên đầu, gộp explanation, preview bị mất ở `processing`, `ocr_failed`, `done`.
- Health records hub: lastUpdated mapping, filter/sort records, status display.
- Profiles/history: unsafe casts, delete/set-default/profile management edge cases.
- Settings: change password, notification preferences, 2FA, page-specific dead code.

Đánh giá: đây là nhóm **product/web UX improvement**, không nên trộn hết vào core infrastructure nếu mục tiêu của `epic-core-improvements` là OCR/AI/infra/security. Nên tạo một backlog riêng kiểu `epic-web-product-polish` hoặc mở rộng Epic 6 nếu muốn gom vào cùng nhánh.

### 2. Verify Email Findings

Chưa có story riêng đủ granular cho:

- Token nằm trong URL/referrer/history.
- Rate limiting verify-email endpoint.
- Error message leak token validity.
- TOCTOU concurrent verify.
- Audit logging verify-email.
- Frontend raw backend `detail`.
- Unsafe Axios error casting.
- Missing loading skeleton, resend action, AbortController, `aria-live`, status differentiation.

Đánh giá: nên tạo một story riêng nếu production scope vẫn yêu cầu email verification hardening. Có thể đặt trong Epic 5 hoặc một epic auth-security riêng.

### 3. Invitation Accept Findings

Chưa có story riêng đủ granular cho:

- Invitation token trong URL query param.
- `sessionStorage` access không try/catch.
- Expired token silent redirect.
- `AcceptResult.outcome` mismatch.
- Rate limiting accept endpoint.
- Accept/revoke race.
- Missing 409 handling.
- Audit log thiếu cho invitation accept.
- Wrong revoke fallback id.
- No timeout, no query invalidation, no owner notification.
- Hardcoded language/i18n và metadata.

Đánh giá: token hygiene và storage hardening đã được nhắc trong Epic 5/Epic 6, nhưng nhóm invitation lifecycle cần story riêng vì có race, auth, audit, UX và notification cùng lúc.

### 4. Cancel Deletion Findings

Đã cover phần lõi right-to-delete trong Story `5.4`, nhưng chưa cover đủ UI/API edge cases:

- Client-side expiry gate có thể block cancellation hợp lệ.
- 409/401/403/500/429 mapping.
- Token normalization mismatch.
- `parseTimestamp` fragile timezone regex.
- Missing initial missing-token state.
- Countdown background-tab lag.
- 72-hour fallback undocumented.
- Loading state thay toàn page.
- `role="alert"` / `aria-live`.
- Test suite thiếu case.

Đánh giá: Story `5.4` nên được bổ sung hoặc tạo story con `Cancel Deletion UX/API Hardening` để tránh story quá lớn.

### 5. Admin Analytics / Audit Viewer Cụ Thể

Đã cover audit spine/correlation trong Story `4.1`, nhưng `_bmad-output/planning-artifacts/review-source/production-review/epic-8-analytics-spec.md` yêu cầu cụ thể hơn:

- `/admin/analytics` DB-backed.
- Events: `USER_REGISTERED`, `UPLOAD_STARTED`, `UPLOAD_CONFIRMED`, `OCR_COMPLETED`, `OCR_FAILED`.
- Charts 8.1/8.2/8.3: user growth, WAU/upload volume, upload success/failure rate.
- Runtime logs hiện chưa đủ làm analytics source.

Đánh giá: Epic 8 gốc có stories `8.1`, `8.2`, `8.3`, nhưng nếu muốn đồng bộ hoàn toàn với review production thì cần bảo đảm các story đó có event instrumentation và DB-backed acceptance criteria. Core improvement Story `4.1` chỉ là nền audit/event spine, không thay thế analytics feature story.

### 6. Family Sharing Edge Cases

Review còn nêu:

- Block self-invite.
- Duplicate ProfileShare khi accept concurrent.
- Accept + revoke race.
- Share lifecycle audit logs.
- Owner notification khi invitation accepted.
- Enforcement sau revoke.

Đánh giá: đây là nhóm production correctness quan trọng. Một phần liên quan audit đã được cover bởi Story `4.1`, nhưng lifecycle/race/notification nên có story riêng trong epic sharing hoặc security.

### 7. Auth Và Session Ngoài Admin

Đã cover admin session storage, nhưng review còn có:

- Refresh token rotation race/stolen token.
- Register SSR/window access issue.
- Register 429 handling, inviteToken gửi API.
- Forgot password silent swallow email error.
- Pending deletion text matching.
- Public endpoint rate limit: register, verify-email, invite accept, cancel deletion, OCR trigger.

Đánh giá: cần một story auth hardening riêng nếu chưa có trong backlog hiện tại. Story `5.1` mới tập trung config fail-fast, chưa đủ cho token rotation/rate-limit UX/API behavior.

### 8. Mobile Scope

Review có nhóm mobile:

- Dashboard, upload OCR, results, profiles.
- Settings, family sharing, camera OCR.
- Missing `@healthlens/shared` dependency.
- Mobile CI/EAS Build.

Đánh giá: `epic-core-improvements` gần như không đụng mobile. Nếu mobile vẫn là scope release thì cần backlog riêng; nếu web production là scope hiện tại thì ghi rõ mobile deferred.

### 9. CI/CD Và Deployment Chi Tiết

Đã cover security scan và release smoke tests, nhưng review còn:

- Staging deployment workflow.
- E2E tests setup.
- Frontend test coverage.
- Mobile CI/EAS Build.
- Docker cleanup: pnpm version, base compose, OCR Dockerfile redundant builder stage.

Đánh giá: Story `8.1`-`8.3` là baseline tốt, nhưng chưa thay thế toàn bộ checklist CI/CD trong review. Nên thêm story riêng cho E2E/staging nếu release gate yêu cầu.

### 10. Smaller Code Cleanup / UI Debt

Chưa cover rõ:

- Empty barrel files.
- `API_TIMEOUT` dead re-export.
- `@radix-ui/themes` tree-shake.
- `@next/next/no-img-element`.
- `console.error` redundant.
- Accessibility `htmlFor/id`.
- Reusable HealthMetricsGrid/ReferenceRangeIndicator.
- Empty state illustration.

Đánh giá: đây là P2/P3 cleanup. Không cần đưa vào core improvements nếu mục tiêu là production blockers, nhưng nên có backlog riêng để không mất dấu.

## P0 Gate Coverage Check

| P0 gate | Status trong core improvements | Nhận xét |
| --- | --- | --- |
| PDF/image/manual core record flow | Partial/Good | OCR flow đã cover, nhưng UI preview/detail/history E2E chưa cover đủ trong core improvements |
| Account, consent, delete | Partial | Right-to-delete lõi đã cover; verify-email/auth/cancel-delete UI edge cases còn thiếu story riêng |
| Family sharing | Partial | Token/race/audit findings chưa được story hóa đủ |
| External OCR controls | Good | Story `1.6` cover consent/retention/kill-switch/audit |
| Token hygiene | Partial | Admin storage và deletion token đã cover; invite/verify/cancel URL token details cần story riêng |
| Right-to-delete purge | Good/Partial | Backend cleanup/race covered; cancel-deletion frontend/API edge cases chưa đủ |
| Telemetry/alerting | Good | Story `4.2` cover baseline |
| Backup/restore/runbook | Good | Story `4.4` cover baseline |
| SLO/deletion SLA | Partial | Có ops baseline nhưng SLO target/reporting cần cụ thể khi implement |
| Admin auth/RBAC | Good/Partial | Admin storage covered; MFA/RBAC end-to-end cần đối chiếu với epic gốc |
| Reference-data governance | Mostly outside core improvements | Đã có epic gốc 7.2-7.5; không nên coi core improvements thay thế |
| Audit log + analytics | Partial | Audit spine covered; analytics feature instrumentation/charts cần story gốc hoặc bổ sung |

## Khuyến Nghị Tiếp Theo

1. Giữ `epic-core-improvements` là backlog canonical cho OCR/AI/RAG/provider/infrastructure/security baseline.
2. Không tạo thêm story vào `epic-core-improvements` cho mọi P2/P3 review finding nếu chúng không thuộc core/infra. Làm vậy sẽ làm epic bị phình và khó sprint planning.
3. Tạo thêm một backlog/epic riêng cho phần còn lại từ `REVIEW-FULL-v2`, ví dụ:
   - `epic-web-production-readiness`
   - `epic-auth-token-flow-hardening`
   - `epic-sharing-and-email-link-hardening`
   - `epic-admin-analytics-instrumentation`
   - `epic-mobile-deferred-readiness`
4. Nếu muốn production web go-live, ưu tiên tạo story tiếp theo cho:
   - Verify-email hardening.
   - Invitation accept/revoke lifecycle hardening.
   - Cancel-deletion UX/API hardening.
   - Admin analytics DB-backed instrumentation.
   - Review/detail page state/preview correctness.

## Artifact Location Correction

Đã chuyển synthesis từ:

- `docs/core-feature-infra-issues-synthesis.md`

sang:

- `_bmad-output/planning-artifacts/core-feature-infra-issues-synthesis.md`

Các reference trong `core-feature-infra-improvement-epics-and-stories.md` và các story files dưới `epic-core-improvements` đã được cập nhật để trỏ về vị trí mới.
