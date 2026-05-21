# HealthLens — Production Web Version Master Review (User + Admin + Ops/Compliance)

Ngày cập nhật: 2026-05-13 (Asia/Ho_Chi_Minh)

## 0) Mục tiêu & Nguồn tham chiếu

Mục tiêu: tạo **1 tài liệu review duy nhất** đủ chi tiết để:
- Chốt production scope cho **Web version** (bao gồm Admin).
- Liệt kê đầy đủ các vấn đề cần hoàn thiện/cải tiến/tối ưu/thêm/bỏ.
- Dùng làm “input source” cho việc tạo user story bằng BMAD skill sau đó.

**Source of truth** (theo yêu cầu):
- `_bmad-output/**` (PRD/Epics/Stories/Acceptance Criteria)
- Source code hiện tại (monorepo `apps/web`, `apps/api`, `services/ocr-service`, `packages/shared`)

**Tài liệu dùng để kiểm chứng/tham khảo**:
- [REVIEW-FULL-v2.md](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/docs/REVIEW-FULL-v2.md)
- [REVIEW-DISPOSITION.md](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/docs/REVIEW-DISPOSITION.md)
- [PRODUCTION-READINESS.md](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/docs/PRODUCTION-READINESS.md)
- `_bmad-output/planning-artifacts/prd.md` (scope, compliance: Nghị định 13/2023/NĐ-CP)

## 1) Các quyết định scope đã chốt trong cuộc trao đổi

1. **Lưu file gốc**: Giữ “original document” (PDF/image) và cho phép user xem lại file gốc.
2. **Địa bàn**: Việt Nam, dữ liệu y tế = dữ liệu cá nhân nhạy cảm (Nghị định 13/2023/NĐ-CP).
3. **Input hồ sơ**: không bắt buộc upload PDF, nhưng production phải hỗ trợ cả:
   - manual input
   - upload image
   - upload PDF
   Có **giới hạn dung lượng** (hiện là 20MB; xem `packages/shared/constants/index.ts`).
4. **OCR mục tiêu**: chính xác tối đa có thể cho “các chỉ số + thông tin cần thiết” để giảm sửa tay.
5. **OCR provider ngoài**:
   - **Được phép** dùng provider ngoài cho OCR (cả PDF và image)
   - Điều kiện: có consent rõ ràng + kiểm soát retention (tối thiểu “no-retention / immediate deletion” nếu provider hỗ trợ) + cơ chế kill-switch.
6. **Admin analytics**: production web version yêu cầu **đủ Epic 8.1, 8.2, 8.3** (các thành viên khác đang làm).
7. **Deliverable**: 1 file đầy đủ, chi tiết (tài liệu này).

## 2) Nhận định cốt lõi (để tránh “đốt công sức”)

### 2.1 Production web version có bắt buộc hỗ trợ upload PDF không?

Theo PRD/Epics: FR9 = “upload PDF kết quả khám” thuộc **P1-Core (Launch)** trong PRD, và Epic 3 có story `3-1-upload-pdf-image-library`. Nghĩa là:
- Nếu bám đúng PRD làm “production web version”: **nên xem PDF support là P0** (tối thiểu là upload + lưu + xem file gốc + pipeline OCR hoạt động).
- Nếu muốn “launch sớm” chỉ với image/manual: phải **đổi PRD/epics** để tránh “ship trái spec”.

Khuyến nghị mình chốt theo hướng: **PDF + image + manual là P0**, vì tại VN rất nhiều cơ sở trả file PDF; cắt PDF sẽ làm UX kém và tăng manual sửa.

### 2.2 Nếu chọn “OCR A (EasyOCR)” thì sau này đổi sang AWS Textract/service khác làm sao?

Không nên thiết kế “A = provider cố định”. Thiết kế production đúng là:
- 1 **OCR Contract** chuẩn hóa output (text + pages/blocks/lines + confidence + diagnostics)
- 1 **Router** chọn provider theo `mimeType` + fallback chain
- N provider adapters (Textract/GCV/Azure/EasyOCR/…)
- Parser metrics tách riêng khỏi OCR (để khi đổi provider không rewrite parsing)

Hiện code đang “couple” theo kiểu `OcrService.processImage(url)` và consumer gọi luôn cho mọi file type. Ví dụ: [OcrJobConsumer.java](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java) luôn gọi `ocrService.processImage(downloadUrl)` (không phân biệt PDF/image).

## 3) Production Definition of Done (gates)

Các gate dưới đây là “điều kiện cần” trước khi gọi production:

### 3.1 Functional completeness (User)
- Auth + consent + right-to-delete (happy path + edge cases).
- Profile management (create/update/list; default profile logic rõ).
- Upload (PDF/image) + manual flow.
- OCR -> review/edit -> confirm -> detail view -> history timeline.
- Family sharing: invite/accept/revoke và enforcement quyền truy cập.
- Xóa record, và xem file gốc (original) trong detail view.

### 3.2 Functional completeness (Admin)
- Admin login MFA + RBAC.
- Reference data CRUD + approve workflow.
- Audit log viewer (Story 7.5).
- Analytics dashboard Epic 8.1/8.2/8.3 (yêu cầu của bạn).

### 3.3 NFR: Security/Compliance (VN health PII/PHI)
- Consent: explicit, logged, versioned (what user consented to, when).
- Data retention policy: object storage + DB + logs.
- Encryption: in transit + at rest.
- Access logging/audit: ai làm gì (admin) + ai truy cập hồ sơ (tối thiểu record-level).
- Rate limiting cho public endpoints nhạy cảm (register/verify-email/invite accept/cancel deletion/ocr triggers).
- Secret management: không hardcode, rotation plan.
- Token handling: tránh query param token cho action nhạy cảm (invite accept, cancel deletion).

### 3.4 NFR: Reliability/Operations (deployment/monitoring/backup/SLA)
- Monitoring + alerting: API/OCR/Redis/DB/S3; metrics fail-rate, latency, queue depth.
- Centralized logging (PII redaction policy), trace/correlation id.
- Backup + restore drill (DB + object storage metadata).
- Runbook + incident response.
- SLA/SLO nội bộ (ví dụ: p95 latency, OCR completion time, deletion SLA 72h).

### 3.5 Quality gates (Engineering)
- CI xanh (lint/test/build).
- E2E tests tối thiểu cho luồng core (login -> upload -> review -> save -> view history).
- Migration & seed strategy rõ ràng (reference data, env drift).

## 4) Tình trạng hiện tại theo BMAD sprint-status (high level)

Theo `_bmad-output/implementation-artifacts/sprint-status.yaml` (last_updated 2026-05-10):
- Epic 1,2,3,4,5,6 phần lớn `done`/`review`.
- Epic 7: `7-3-import-reference-data-csv-json` done; `7-5-reference-data-audit-log-view` done (audit log tại `/admin/audit-log`).
- Epic 8: `8-1/8-2/8-3` đang `sprint-3` (chưa done).
- Mobile Epic 9 backlog (không thuộc scope web production hiện tại).

Điểm quan trọng: “done theo sprint-status” không đồng nghĩa “production-ready”. `REVIEW-FULL-v2` và scan code cho thấy có các P0/P1 blockers (OCR PDF, security token, transactional side-effects, ops gaps).

## 5) Gaps trọng yếu (P0) cần chốt trước khi nghĩ tới “fix UI”

### P0-A) OCR PDF pipeline đang mismatch (gần như “broken by design”)

Dấu hiệu code-level:
- Upload cho phép PDF (`accept="application/pdf"`): [UploadButton.tsx](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/web/src/components/features/upload/UploadButton.tsx)
- Backend generate upload url có path cho `application/pdf`: [HealthRecordService.java](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java)
- Nhưng OCR consumer không phân biệt filetype: [OcrJobConsumer.java](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java) line 110 gọi `ocrService.processImage(downloadUrl)`
- OCR microservice hiện tại là EasyOCR (image OCR), không phải PDF OCR (thường cần “document OCR” hoặc render PDF->image).

Production impact:
- PDF upload sẽ cho ra OCR kém hoặc fail, kéo theo parsing sai, recommendations sai.

Decision đề xuất (bạn đã đồng ý hướng provider ngoài):
- Thiết kế **provider-agnostic OCR**, route theo `mimeType`, có provider PDF (Textract/GCV Doc OCR/Azure Form Recognizer) + provider image.

### P0-B) Transaction correctness: read-only nhưng có write side-effect (audit)

`REVIEW-FULL-v2` nêu rõ P0: audit writes trong read-only transaction.
Production impact:
- GET endpoint gây write -> khó scale, khó cache, gây deadlock/lock escalation, surprise behavior.
Action cần chốt: audit granularity (per-record hay per-metric), và tách audit khỏi read path.

### P0-C) Admin auth token đang lưu trong sessionStorage (XSS -> takeover)

Code: `apps/web/src/lib/api/adminApiClient.ts`, `apps/web/src/app/admin/login/page.tsx`, `apps/web/src/app/admin/layout.tsx` (đã được nêu trong PRODUCTION-READINESS).
Production impact:
- Chỉ cần 1 XSS là mất admin token.
Scope decision:
- Nếu production yêu cầu “real ops/admin”: nên chuyển admin auth sang HttpOnly cookie + session/refresh rotation (và CSRF strategy).

### P0-D) Token nhạy cảm nằm trong URL query params (invite accept, cancel deletion)

Theo `REVIEW-FULL-v2`:
- Invitation accept token ở query param (IA-01 P0).
- Cancel deletion token + email trong query param (CD-01/CD-02 P1).
Production impact:
- Token leak qua browser history, logs, Referer header, proxy logs.

### P0-E) Ops baseline chưa đủ cho “production” (monitoring/backup/SLA)

Hiện có story “infra-devops-foundation” và staging guide, nhưng production scope bạn yêu cầu bao gồm:
- monitoring/metrics dashboard
- backup/restore
- SLA/SLO
- audit log viewer (admin)
=> Đây là phần phải được đưa vào review backlog rõ ràng, không thể để “tự nhiên sẽ có”.

## 6) Gaps UI/UX (User) theo màn hình (đối chiếu REVIEW-FULL-v2 + PRD/UX)

Phần này tập trung vào “đúng chức năng” và “dead UI”.

### 6.1 `/health-records/review/[recordId]` (Xem kết quả chi tiết)

Các nhận định bạn nêu là đúng theo `REVIEW-FULL-v2`:
- Nút “Chia sẻ” / “Tải PDF” disabled: hoặc implement, hoặc remove để tránh dead UI.
- “AI khuyến nghị” nên đưa lên đầu (progressive disclosure: summary/insight trước).
- “Giải thích chi tiết từng chỉ số” đang redundant: gom explanation về 1 nơi (grid card expand/collapse) và bỏ section trùng.
- Thiếu preview document/original file trong done view (cần giữ để user verify).

Bổ sung production-ready acceptance:
- “Manual correction” phải có validation mạnh (unit, range, numeric parsing).
- Hiển thị rõ “nguồn chỉ số”: OCR vs manual (FR15).
- Status mapping theo contract rõ ràng (processing/review_required/failed).

### 6.2 `/home` (Dashboard)

Rủi ro chính:
- dữ liệu “fake/hardcoded” gây hiểu nhầm nếu user tưởng là thật.
- polling/refetch interval cần tối ưu (pause khi tab hidden).

Decision cần chốt:
- Home có cần “latest real metrics” hay chỉ là “CTA hub”? (PRD có định hướng “glanceable status”, nên mình nghiêng về “latest record summary phải là thật”.)

### 6.3 Profiles (`/profiles`, `/profiles/[id]/history`)

Các gap phổ biến:
- delete profile / set default profile endpoints có thể chưa đầy đủ (theo v2).
- form validation inconsistency (max length).
- avatar/conditions/allergies: P2 theo PRD.

### 6.4 Family sharing

PRD FR29-FR32: bắt buộc cho web MVP.
Các gap theo v2 cần kiểm chứng kỹ:
- self-invite phải chặn.
- change access level: có cần không hay keep “re-invite” (product decision).
- family dashboard route riêng (`/family-dashboard`) có thể không bắt buộc nếu Home đã cover, nhưng PRD/UX có nêu UX-DR6.

## 7) Admin scope (FR33-FR38)

### 7.1 Epic 7 (Reference data)

Hiện trạng web (cập nhật 2026-05-21):
- Admin dashboard stub: [admin/page.tsx](/home/vmhieu/Workspace/UIT/IE303/Project/health-lens/apps/web/src/app/admin/page.tsx)
- **Audit log viewer:** shipped — compose-only route + `components/admin/audit-log/` (remaining-6-4, core-7-5); không còn placeholder.
- Reference data page có nhưng cần đối chiếu “approval workflow + import”.

Production must-have:
- Không có đường “bypass approval” (nếu approval là requirement).
- Audit log viewer usable (filter/search/pagination).

### 7.2 Epic 8 (Admin analytics) = Required theo bạn

Hiện trạng web:
- Admin dashboard hiện chỉ là placeholder “Story 8.x”.

Acceptance expectation (tối thiểu theo Epic 8.1-8.3):
- 8.1: user growth overview (all-time + theo tháng).
- 8.2: WAU + upload volume (all-time + theo ngày/tuần).
- 8.3: upload success/failure rate (đặc biệt quan trọng cho OCR quality + ops).

Lưu ý: Epic 8 muốn có data thì cần event tracking chuẩn (upload confirmed, ocr completed/failed, parse failed, manual saved).

## 8) OCR / LLM / RAG: “Production-grade” checklist

### 8.1 OCR provider routing (PDF vs image)

Yêu cầu tối thiểu:
- Job payload có `mimeType`/`fileType` + `fileKey` + `recordId`.
- Router quyết định provider:
  - PDF: Textract/GCV Doc OCR/Azure (primary), fallback PDF->image->image OCR (last resort).
  - Image: provider vision OCR + fallback EasyOCR.
- Output normalize: text + confidence + per-page blocks (để parser robust hơn).

Ràng buộc khi dùng OCR provider ngoài (theo scope bạn chốt):
- Consent: user phải “opt-in” rõ ràng cho việc gửi tài liệu y tế ra bên thứ ba (tách riêng khỏi consent chung nếu cần).
- Retention: cấu hình “no-retention / immediate deletion” nếu provider hỗ trợ; nếu không hỗ trợ thì phải có hợp đồng + SLA retention cụ thể và nêu rõ trong consent.
- Region/cross-border: ưu tiên region gần VN (ví dụ Singapore) để giảm latency và giảm rủi ro chuyển dữ liệu; cần kiểm tra điều khoản cross-border theo NĐ 13 (đây là phần nên có legal review).
- Encryption: TLS bắt buộc; data-at-rest của provider phải có chứng từ (SOC2/ISO) nếu có.
- Kill-switch: env flag để tắt provider ngoài và chuyển về fallback nội bộ khi có sự cố.
- Auditability: log “provider used + request id + retention mode” (không log raw document text).

### 8.2 Parser robustness (trích “chỉ số”)

Hiện trạng thường gặp:
- regex fragile, phụ thuộc layout lab.
Production expectation:
- Có test fixtures cho các mẫu xét nghiệm VN (ít nhất 10-20 mẫu).
- Có scoring/threshold: “review required” khi parsing mơ hồ.
- Store raw OCR để debug.

### 8.3 LLM output quality & safety

Production guardrails:
- Disclaimers rõ (Mức A/B theo PRD).
- Prompt versioning + rollback.
- Observability: capture prompt version + model + tokens + latency + outcome.
- Không để LLM “bịa ngưỡng chuẩn”: reference range phải lấy từ reference data.

### 8.4 RAG data readiness

`REVIEW-FULL-v2` đúng ở 12.1/12.2: Qdrant có config nhưng thiếu ingestion corpus.
Khuyến nghị production approach:
- Không “RAG everything” ngay.
- Bắt đầu bằng curated knowledge base (metrics explanations) có nguồn rõ ràng, versioned, admin reviewable.

## 9) Deployment/Monitoring/Backup/SLA (scope bạn yêu cầu)

Tình trạng hiện tại:
- Có Docker + staging guide + env separation (story `infra-devops-foundation`).
Nhưng để đạt production scope “real” cần bổ sung:

### 9.1 Monitoring/metrics
- API: request rate, p95 latency, error rate, DB pool usage.
- OCR pipeline: queue depth (Redis stream), job latency, fail rate theo provider, OCR confidence distribution.
- LLM: latency, error rate, cost proxy, low-quality rate.
- Storage: upload failures, signed-url failures, delete failures.

### 9.2 Logging
- Structured logs (JSON), correlation id end-to-end (upload -> ocr job -> parse -> save).
- PII redaction policy (không log email/token/document text thô ở INFO).

### 9.3 Backup & restore
- DB backups (daily + PITR nếu dùng managed DB).
- Object storage lifecycle/retention: original files + derived artifacts.
- “Restore drill” định kỳ (ít nhất 1 lần trước go-live).

### 9.4 SLA/SLO (nội bộ)
Đề xuất tối thiểu để vận hành:
- OCR completion: p95 <= X phút (tùy provider), alert nếu backlog tăng.
- Right-to-delete: xử lý trong 72h (theo PRD), có job + audit.
- Uptime target (API/OCR): ví dụ 99.5% trong MVP.

## 10) Backlog đề xuất (để tạo stories) theo nhóm

Mục này là “story seeds” (không viết full story ở đây), ưu tiên theo production.

## 10.1 Traceability Matrix (Issue -> BMAD Story -> Code)

Mục tiêu của bảng này: khi bạn dùng BMAD tạo user stories/plan, bạn có thể lấy trực tiếp “seed” theo từng issue.

| Nhóm | Issue (tóm tắt) | Priority | BMAD mapping | Code refs (điểm vào) |
|---|---|---:|---|---|
| OCR | PDF OCR mismatch (consumer gọi `processImage` cho mọi type) | P0 | Epic 3 (re-scope) + new story “OCR Router by mimeType” | `apps/api/.../OcrJobConsumer.java`, `apps/api/.../OcrService.java`, `services/ocr-service/app.py`, `apps/web/.../UploadButton.tsx` |
| OCR | Chuẩn hóa OCR output contract (pages/blocks/confidence) | P0 | New story (Epic 3) | `apps/api/.../OcrService.java` (+ DTO `OcrResult`) |
| OCR | Parser robustness + fixtures/tests theo mẫu VN | P0 | Epic 3.3/3.4 (extend) | `apps/api/.../OcrService.parseMetrics(...)` |
| AI/RAG | Qdrant có nhưng thiếu ingestion corpus (RAG data) | P0 | Epic 4.6 (extend) | `apps/api/.../MetricExplanationRetrievalService.java`, `apps/api/.../VectorStoreService.java`, `apps/api/.../EmbeddingService.java` |
| Backend | Read-only tx nhưng write audit (side effect) | P0 | New story (Backend hardening) | `apps/api/.../HealthRecordService.java`, `apps/api/.../ReferenceDataService.java` |
| Security | Admin token sessionStorage (XSS risk) | P0 | New story (Admin auth hardening) | `apps/web/src/lib/api/adminApiClient.ts`, `apps/web/src/app/admin/login/page.tsx`, `apps/web/src/app/admin/layout.tsx` |
| Security | Sensitive token ở URL query (invite accept) | P0 | Epic 6 (extend) | `apps/web/.../invitations/accept/page.tsx`, `apps/api/.../ProfileShareService.java` |
| Security | Sensitive token + email ở URL query (cancel deletion) | P1 | Epic 1.6 (extend) | `apps/web/.../CancelDeletionClient.tsx`, `apps/api/.../DataDeletionService.java` |
| Admin | Audit log viewer (Story 7.5) — đã có UI tại `/admin/audit-log` | — | Story 7.5 | `apps/web/src/app/admin/audit-log/page.tsx` |
| Admin | Analytics 8.1/8.2/8.3 (required) | P1 | Stories 8.1/8.2/8.3 | `apps/web/src/app/admin/page.tsx` (stub) |
| UI | Detail view dead buttons + reorder AI + merge explanation + giữ preview original | P1 | Epic 4/5 (extend) | `apps/web` route `/health-records/review/[recordId]` |
| Ops | Monitoring/metrics/backup/restore/SLA runbooks | P0/P1 | New “Ops readiness” story | `docs/STAGING_DEPLOYMENT.md`, `docker/*`, deploy manifests |

### P0 Backlog (blockers)
- OCR: provider-agnostic contract + router theo mimeType + PDF OCR provider.
- Parser: fixtures + unit tests + confidence gating.
- Transaction: read endpoints không write; audit tách khỏi read path.
- Token hygiene: remove query-param tokens cho accept/cancel deletion; add referrer policy.
- Admin auth hardening: không dùng sessionStorage token; chuyển sang cookie/session + CSRF plan.
- Ops: monitoring+alerting baseline; backup/restore plan.

### P1 Backlog (should-have để vận hành tốt)
- Admin audit log viewer (Story 7.5): filter/search/paging.
- Epic 8.1-8.3 analytics: define events + endpoints + UI.
- Rate limiting cho các public endpoints nhạy cảm.
- Data deletion purge idempotency + retry/reconciliation job cho storage delete.
- Detail view UX: bỏ dead buttons hoặc implement; reorder AI; merge explanation sections; keep original preview.

### P2 Backlog (polish/growth)
- Dedicated consent page + consent PDF download (nếu muốn).
- Profile avatar, conditions/allergies.
- Family dashboard page riêng (nếu muốn tách khỏi Home).
- Export PDF/Share link (P2 trong PRD, không nên giả lập nút disabled).

## 11) Ý kiến về nhận định “Pig Pickle / REVIEW-FULL-v2”

Mình đã đọc hết `REVIEW-FULL-v2` và nhìn tổng thể:
- Phần lớn P0/P1 findings là **đúng hướng**, đặc biệt: OCR PDF mismatch, token in URL, read-only tx writes, admin token storage, thiếu RAG corpus.
- Một số finding thuộc dạng “UX/polish/tech debt”, cần re-prioritize theo production scope bạn chốt (Ops/Compliance/OCR quality trước, UI polish sau).
- Tài liệu v2 rất dài (154 findings). Để dùng làm production plan, cần “master triage” gắn với PRD/Epics/Stories và quy về backlog nhóm (mục 10) thay vì xử lý theo màn hình rời rạc.

## 12) “Bước tiếp theo” (không implement ngay, chỉ để chốt vấn đề)

Nếu bạn đồng ý, bước tiếp theo trong review/discovery nên là:
1. Chốt “P0 gates” cụ thể thành checklist có thể tick (mục 3), và xác định ai owner từng gate.
2. Chốt thiết kế OCR provider-agnostic (router + contract + retention/consent policy) ở mức “ADR 1 trang”.
3. Chốt audit granularity + dữ liệu nào bắt buộc log theo compliance/ops (admin actions, user access).
4. Chốt Epic 8 analytics: event definitions + nguồn dữ liệu (DB tables vs event log).

Sau khi 4 mục trên rõ, lúc đó mới dùng BMAD để generate stories, và team bắt đầu implement theo thứ tự.
