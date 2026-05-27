## Deferred from: code review of 1-1-starter-template-setup.md (2026-03-29)

- Bổ sung persistence volumes cho Postgres/MinIO trong compose dev (`docker/docker-compose.dev.yml`) để giảm rủi ro mất dữ liệu local khi recreate container.

## Deferred from: code review of 1-7-groq-api-setup.md (2026-04-03)

- **F6** — Input format injection risk trong `String.format()` tại `LlmService.java:100`. `metricName`, `value`, `unit` dùng trực tiếp trong format string. Nên validate/sanitize input trước khi đưa vào prompt builder khi có user-provided data. (_Low priority, data hiện đến từ trusted source_)
- **F7** — `EmbeddingService.embedBatch()` không validate từng item trong list (null/blank strings). Thêm filter hoặc validation loop trước khi call API. (_Low priority, batch chưa có consumer trong codebase_)
- **F8** — AC-2: Embedding model thực tế khác spec. Spec yêu cầu `groq/embed-multilingual-v3` nhưng Groq không cung cấp embedding API. Quyết định embedding provider chính thức (Jina AI / OpenAI / self-hosted) sẽ được thực hiện trong **Story 1.8 (Qdrant Cloud Setup)** để đảm bảo vector dimensions khớp với Qdrant collection config.

## Deferred from: code review of 3-1-upload-pdf-image-library.md (2026-04-20)

- Schema `health_records` chưa có một số cột trong phần Dev Notes (`exam_date`, `ocr_confidence`) tại `apps/api/src/main/resources/db/migration/V010__create_health_records_table.sql`. Deferred vì hiện tại không chặn AC chính của story upload/confirm/enqueue.

## Deferred from: code review of 6-3-vietnamese-language-normalization-and-message-catalog.md (2026-05-17)

- Cancel-deletion client treats every backend 409 as cancellation success at `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx:148`. Deferred because it appears pre-existing and outside the direct 6.3 diff, but it can mislead users into believing deletion cancellation succeeded when backend refused it.

## Deferred from: code review of 3-5-trusted-online-rag-source-adapter-with-citation-and-cache.md (2026-05-19T14:40:36+07:00)

- AC4 admin/audit inspection surface is ambiguous — Defer admin/audit inspection surface to a follow-up story because this story should finish adapter-level retrieval, citation metadata, persistence, and cache hardening first; answer-linked admin/audit API/UI needs separate scope. Story created: `_bmad-output/implementation-artifacts/epic-core-improvements/epic-3-llm-rag-governance/3-6-online-rag-citation-audit-inspection-surface.md` (`core-3-6-online-rag-citation-audit-inspection-surface`).

## Deferred from: code review of 7-10-unify-email-event-delivery.md (2026-05-20T15:20:00+07:00)

- Sprint status includes unrelated `core-7-9-confirm-and-document-event-driven-architecture` status movement at `_bmad-output/implementation-artifacts/sprint-status.yaml:226`. Deferred because it appears to belong to the prior Story 7.9 workflow rather than the Story 7.10 email delivery implementation.

## Deferred from: code review of 11-2-user-two-factor-authentication-totp.md (2026-05-22)

- Thiếu audit `USER_TOTP_SETUP` khi bắt đầu setup (admin có `ADMIN_TOTP_SETUP`) — cải thiện observability, không chặn AC.

## Deferred from: code review of pae-2-marketing-route-group-and-root-routing.md (2026-05-20)

- Trùng lặp logic `logout` và query `currentUser` giữa `MarketingHeader` và `(dashboard)/layout` — tech debt nhỏ sau refactor header; có thể gom hook dùng chung sau.
- Story File List chưa phản ánh ~16 file mới/sửa ngoài phạm vi ghi nhận ban đầu — cập nhật khi commit.

## Deferred from: code review of pae-3-public-landing-page-ssr.md (2026-05-20)

- Test landing chỉ assert chuỗi trong file nguồn (`marketing-landing.test.ts`) — Cùng pattern `routing-policy.test.ts`; bổ sung render test khi có harness RTL cho marketing pages.
- Footer trùng `/support` và `/help` — Cả hai link cùng đích sau redirect; gọn navigation khi có trang support thật (remaining-2-6 / pae-8).

## Deferred from: code review of pae-4-seo-foundation-metadata-sitemap-robots.md (2026-05-20)

- SVG mặc định Next (`next.svg`, `vercel.svg`) vẫn còn trong `public/` — không ảnh hưởng runtime; dọn ở story dọn dẹp assets sau.

## Deferred from: code review of 7-11-introduce-application-stream-event-boundary.md (2026-05-20T21:45:00+07:00)

- Consumer group bootstrap can skip the first real event if a stream is created with `_init`, a real event is appended before `createGroup(..., ReadOffset.latest())`, and the group starts after that event. Deferred because the same bootstrap race existed in the previous email/OCR consumer setup and was not introduced by this boundary refactor.
- OCR retry enqueue publishes to Redis before the queued DB state is saved, so a save failure after publish can leave DB state behind the stream event. Deferred because the same publish-before-save ordering existed before this boundary refactor; a later reliability story should decide whether to change retry transaction/outbox semantics.

## Deferred from: code review of 2-6-forgot-password-page-refactor-and-public-auth-link-consistency (2026-05-21)

- Nút ngôn ngữ trên login vẫn là `button` không có hành động — tồn tại trước story 2.6; cần story i18n/language switcher riêng.

## Deferred from: code review of pae-6-change-password-api-and-settings-ui.md (2026-05-21)

- `AdminAuditLogService` chưa có nhãn hiển thị cho audit action `CHANGE_PASSWORD` — ngoài AC story; bổ sung khi làm màn audit auth.

## Deferred from: code review of pae-8-settings-about-tab-version-and-support.md (2026-05-21)

- AC4: profile vẫn card “Độ hoàn thiện hồ sơ” 85% cứng — xử lý trong `pae-9`.
- Gom `ResourceLinkRow` / `AboutMetricTile` dùng chung với privacy settings.
- Thay khối liên hệ about bằng `SettingsSupportCard` + mailto có version.

## Deferred from: code review of pae-7-settings-privacy-tab-consent-and-legal-links.md (2026-05-21)

- Test chỉ đọc source tĩnh, không mock API/React Query — pattern giống các story PAE khác; bổ sung khi có harness component test dùng chung.
- Lặp cấu hình `DashboardPageShell` 3 lần (loading/error/success) — có thể extract helper sau khi ổn định UI settings.

## Deferred from: code review of 11-3-personal-and-family-health-context-fields.md (2026-05-22)

- `notes` max 1000 (create) vs 500 (update) vẫn lệch giữa Zod/backend — pre-existing; story chỉ yêu cầu thống nhất `displayName`.
- Thiếu `UserController` WebMvc test cho `PUT /me/health-context` — coverage gap, không chặn AC.

## Deferred from: code review of pae-12-notification-email-preferences.md (2026-05-22)

- AC5 ghi `EmailService`/`FollowUpReminderScheduler` nhưng preference check nằm ở `EmailConsumer` (đường gửi email thực tế qua Redis stream). Chức năng đạt; có thể thêm guard trong `EmailService` nếu sau này có gọi SMTP trực tiếp.

## Deferred from: code review of 11-4-shared-dashboard-workflow-components.md (2026-05-22)

- AC#5 full `pnpm test` fail — `privacy-settings.page.test.ts` expect `label: "Riêng tư"` nhưng page dùng `breadcrumbFromSettings("Riêng tư")` — pre-existing, không do story 11.4.

## Deferred from: code review of r1-1-capture-refactor-baseline-and-quality-gates.md (2026-05-27)

- Refactor story status tracking is not represented in central `sprint-status.yaml`; R1.1 is tracked only in the refactor story file because the existing sprint status does not contain refactor story keys.

## Deferred from: code review of r1-3-move-automation-scripts-to-root-structure.md (2026-05-27)

- Non-interactive `down.sh` deletes volumes without explicit confirmation at `scripts/docker/down.sh:58`; deferred because the same behavior existed in `docker/scripts/down.sh` before the move.
- Documentation still points to deleted script paths such as `README.md:88`; deferred because Story R1.3 explicitly leaves docs to the later docs story R1.6.
- Mobile reset can fail partially but still exit successfully at `scripts/mobile/reset-project.js:94`; deferred because the same catch-and-log behavior existed before the move.

## Deferred from: code review of r1-4-extract-shared-docker-script-utilities.md (2026-05-27)

- Non-interactive `down.sh` destructive mode bypasses explicit confirmation at `scripts/docker/down.sh:31`; deferred because the behavior existed before R1.4 and was already recorded from R1.3.
- Symlinked Docker script launchers resolve `_common.sh` relative to the symlink directory at `scripts/docker/up.sh:28`; deferred because supported invocation remains the documented repo path and symlink launchers were not introduced as a requirement.
- `cleanup.sh --ci` without `--force` can no-op and still finish successfully at `scripts/docker/cleanup.sh:156`; deferred because the confirmation behavior predates this utility extraction.
- `cleanup.sh --dry-run` and `--analyze` still require Docker daemon preflight at `scripts/docker/cleanup.sh:152`; deferred because the preflight order predates this utility extraction.
- `NO_COLOR` only disables colors when set to `1` at `scripts/docker/_common.sh:23`; deferred because this preserves the pre-existing script behavior, though a future cleanup can adopt the broader NO_COLOR convention.
- `format_duration` does not normalize non-numeric input at `scripts/docker/_common.sh:59`; deferred because current callers pass numeric `date +%s` deltas and this is inherited helper behavior.
