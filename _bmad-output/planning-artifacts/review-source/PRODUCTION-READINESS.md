# Disposition: REVIEW-FULL-v2 vs BMAD Output + Source Code

Ngày cập nhật: 2026-05-13

**Source of truth**:
- `_bmad-output/**` (PRD/Epics/Stories/Acceptance Criteria)
- Source code hiện tại

**Input để kiểm chứng**:
- `docs/REVIEW-FULL-v2.md`

Mục tiêu tài liệu này: đi **item-by-item** trong `REVIEW-FULL-v2` và quyết định:
- `Agree` (đúng và còn tồn tại)
- `Partly` (đúng một phần / framing chưa chuẩn / mức ưu tiên cần chỉnh)
- `Disagree` (không đúng hoặc đã được giải quyết)

Mỗi item sẽ kèm:
- Priority đề xuất (P0/P1/P2) theo **production scope** bạn yêu cầu (deployment/monitoring/backup/SLA + PII/PHI).
- Mapping sang `_bmad-output` story (nếu có).
- Mapping sang code (file/path).
- “Decision needed” nếu thiếu quyết định sản phẩm/ops/compliance.

## Status

- [ ] Đang xử lý: Section 1, 2, 11, 12 (các mục P0/P1)
- [ ] Chưa xử lý: Section còn lại trong `REVIEW-FULL-v2`

## Disposition (Batch 1)

### 1. Dashboard Home (`/home`)

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 1.1 StatCard hardcoded | Partly | P1 | “Fake data” đúng nếu đang hiển thị số giả; nhưng analytics/admin metrics nằm ở Epic 8, còn Home user-side không nhất thiết phải analytics “real”. Cần chốt: Home có bắt buộc lấy data thật từ latest record không? | PRD: user insight; có thể map sang Epic 5/4 tùy hướng | TBD: `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.2 `recordStatusLabel` fallback "Chưa xác thực" | Agree | P2 | Đúng: `recordStatusLabel` chỉ map `normal/attention/abnormal/error/failed/ocr_failed`, còn status khác (ví dụ `"unverified"`, `"review_required"`, `"processing"`) sẽ hiện “Chưa xác thực” -> gây nhầm. Nên map rõ theo contract từ API. | Epic 5.2 history + Epic 3 status | `apps/web/src/app/(dashboard)/home/page.tsx#recordStatusLabel` |
| 1.3 Family shared profiles display | Agree | P2 | Đúng: Home chỉ gọi `GET /profiles` (owned profiles). Shared profiles là endpoint khác (`ProfileService.getSharedProfiles`) và Home chưa hiển thị. Nếu production yêu cầu “xem được hồ sơ được chia sẻ từ family ngay trên home”, đây là gap. | Epic 6 family sharing | Web: `apps/web/src/app/(dashboard)/home/page.tsx`, API: `apps/api/src/main/java/com/healthlens/api/service/ProfileService.java#getSharedProfiles` |
| 1.4 Warning banner abnormal metrics | Agree | P2 | Hiện không có banner/CTA nổi bật khi có chỉ số bất thường ở record gần nhất. Đây là UX enhancement hơn là correctness. | Epic 4 insights | Home UI |
| 1.5 "Chăm sóc sức khỏe chủ động" text generic | Agree | P2 | Đúng: section này hardcode copy. Có thể thay bằng insight dựa trên latest record hoặc để “placeholder” nhưng nên tránh claim mang tính y khoa nếu không dựa dữ liệu. | Epic 4 recommendations | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.6 Action tiles disabled | Agree | P2 | Đúng: `Đặt lịch khám`, `Liên hệ bác sĩ`, `Trợ giúp` đang `disabled`. Production nên ẩn hoặc implement. | Product decision | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.7 Upload CTA trực tiếp trên home | Disagree | P3 | Home đã có `ActionTile` “Tải kết quả” link sang `/health-records?...&openUpload=1` (upload nhanh). | Epic 3 upload | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.8 Recent records fetch `refetchInterval:30000` | Partly | P2 | Home đang poll `home-profiles` mỗi 30s; recent records query không poll. Vấn đề chính: polling cố định (không pause khi tab hidden) và có thể dùng `staleTime` thay vì interval. | Perf polish | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.9 `ENSURE_DEFAULT` fire-and-forget không cleanup | Agree | P2 | Đúng: `useEffect` gọi `void apiClient.post(...)` không cancel/cleanup. Low risk nhưng vẫn là pattern xấu (dễ tạo request leak khi route change). | Web tech debt | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.10 `[, setPendingAccessUpdates]` unused state | Agree | P3 | Đúng: state value không bao giờ read; chỉ dùng setter để delete key. Có thể thay bằng `useRef` hoặc remove. | Web tech debt | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.11 Optimistic revoke không rollback | Disagree | P2 | Code hiện tại chỉ cập nhật cache trong `onSuccess` (sau khi API thành công), không phải optimistic update trước API call nên không có “rollback bug” như mô tả. | Epic 6 (sharing) | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.12 Profiles query không cache `staleTime` | Agree | P3 | Đúng: `useQuery` không set `staleTime` => default 0, nên mount lại sẽ refetch ngay. Không phải correctness issue. | Perf polish | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.13 `alert()` cho success/error mutation | Agree | P2 | Đúng: mutation dùng `alert()` cho success/error. Nên thay bằng toast để không block UI và consistent. | UX polish | `apps/web/src/app/(dashboard)/home/page.tsx` |
| 1.14 `extractApiDetail` duplicate | Agree | P2 | Đúng: `extractApiDetail` đang copy-paste ở nhiều pages. Nên đưa vào shared util. | Web tech debt | `apps/web/src/app/(dashboard)/home/page.tsx`, `apps/web/src/app/(dashboard)/health-records/page.tsx`, `apps/web/src/app/(dashboard)/profiles/page.tsx` |

### 2. Xem kết quả chi tiết (`/health-records/review/[recordId]`)

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 2.1 Nút "Chia sẻ" disabled | Agree | P1 | UI dead button: hoặc implement share, hoặc remove. Không nằm trong launch scope PRD P1 bắt buộc; có thể để P1/P2 tùy roadmap. | PRD “Chia sẻ” là core capability nhưng export/share link chưa có story rõ ở Epic 1-8 | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.2 Nút "Tải PDF" disabled | Agree | P2 | Export PDF là P2 trong PRD. Hiện disabled => nên ẩn cho production MVP hoặc implement ở phase growth. | PRD P2 “Xuất PDF tóm tắt” | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.3 AI Recommendations sai thứ tự | Agree | P2 | Đây là UX polish (không blocker). Story 4.4 mô tả recommendations ở cuối (UX-DR5) nên “đưa lên đầu” là quyết định UX mới, không phải bug. | Epic 4.4 | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.4 "Giải thích chi tiết từng chỉ số" redundant | Partly | P2 | Nếu trang đang render 2 chỗ giải thích giống nhau thì đúng; nhưng bản thân `HealthMetricCard` đã có expand “Giải thích”. Cần quyết định: 1 chỗ duy nhất và pattern progressive disclosure (Epic 4.5). | Epic 4.5 + 4.6 | `apps/web/src/components/ui/HealthMetricCard.tsx` |
| 2.5 Thiếu preview document trong done view | Agree | P0 | Đúng: branch `isDoneView` không render “Hồ sơ gốc” (file preview) trong khi review/edit view có. Với requirement giữ file gốc và giảm tranh chấp “OCR sai”, đây là P0 UX gap. | Epic 5.2 view original + Epic 3 review | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.6 `compactMetricPercent` fallback 60% | Agree | P2 | Đúng: nếu không có reference range hoặc value không parse được thì bar hiển thị 60% mặc định -> misleading. Nên dùng “N/A” state hoặc neutral bar. | Epic 4.6 metric UI | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx#compactMetricPercent` |
| 2.7 Card metric grid thiếu explanation | Disagree | P2 | `HealthMetricCard` đã có expand/collapse và fetch explanation khi mở rộng. Nếu grid dùng component khác thì sẽ đổi lại disposition sau khi đối chiếu page. | Epic 4.6 | `apps/web/src/components/ui/HealthMetricCard.tsx` |
| 2.8 AI recommendations fetch không staleTime hợp lý | Disagree | P3 | Query recommendations đã set `staleTime: 5 * 60 * 1000` và chỉ enable khi `status === "done"`. Không thấy “fetch mỗi mount” như mô tả (trừ khi data đã stale). | Epic 4.4 | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.9 Mất preview khi processing | Agree | P0 | Đúng: state `processing` chỉ render loader, không render file gốc. Với requirement “xem lại file original” và UX hỗ trợ upload lớn, nên hiển thị preview (read-only) ngay cả khi đang OCR. | Epic 3 upload + Epic 5.2 view original | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.10 Mất preview khi OCR failed | Agree | P0 | Đúng: khi `ocr_failed` + not manual mode, render `OcrFailureScreen` và screen này không có viewer/thumbnail file gốc. | Epic 3.5 | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`, `apps/web/src/components/features/upload/OcrFailureScreen.tsx` |
| 2.11 `showFullDoc` modal raw JSX | Agree | P2 | Đúng: fullscreen modal implement inline trong page; nên tách component để reuse + test + giảm độ dài file. | Web tech debt | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.12 `showConfirmModal` raw JSX | Agree | P2 | Đúng: confirm modal inline. Nên tách component và unify style với modal system. | Web tech debt | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.13 Add metric dialog cần modal riêng + auto-complete | Partly | P2 | Add dialog đã có (inline JSX) và có dropdown từ `referenceMetrics`, nhưng chưa có autocomplete/search và chưa tách component. | Epic 4.6 ref metrics | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.14 Inline edit table thiếu validation UX | Agree | P2 | Đúng: validation lỗi dồn vào `saveError` chung; input fields không highlight/inline error per field. | UX polish | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.15 Array index làm React key | Agree | P2 | Đúng: edit table dùng `key={idx}`; metric list khác chỗ dùng `${metric.name}-${idx}`. Khi reorder/insert/delete, key theo index có thể làm sai reconciliation. | Web correctness | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.16 1000+ line component | Agree | P2 | Đúng: file rất dài (nhiều modal + layout + logic) -> khó maintain/test. | Web tech debt | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.17 Empty catch blocks (retry upload/delete) | Agree | P1 | Đúng: `handleRetryUpload` và `handleDeleteRecord` catch không đọc server `detail/status` => mất thông tin hỗ trợ. Không phải security nhưng ảnh hưởng support. | Epic 3.5 retry + Epic 5 delete record | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.18 Polling không cleanup khi unmount | Disagree | P3 | `refetchInterval` do TanStack Query quản lý; khi component unmount thì polling dừng. Không thấy leak như mô tả. | N/A | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.19 `fileUrl` empty → broken iframe/img | Partly | P2 | `fileUrl` default `""`; modal/preview render dùng `src={fileUrl}`. Thực tế user chỉ mở modal khi click preview; nếu API không trả fileUrl hoặc empty thì sẽ broken. Nên guard `if (!fileUrl)` và show message. | UX robustness | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.20 `analyzerModel`, `testMethod`, `labSite` không render | Agree | P3 | Đúng: fields có trong type và payload nhưng UI không hiển thị. Nếu PRD yêu cầu, cần add; nếu không thì nên remove khỏi type để tránh confusion. | Product decision | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx` |
| 2.21 Thiếu preview document (OCR failed screen) | Agree | P0 | Đúng: `OcrFailureScreen` chỉ có placeholder “Ảnh tải lên gốc” chứ không render file/thumbnail; user không thể quyết định “Giữ một phần” dựa trên ảnh gốc. | Epic 3.5 OCR failure recovery | `apps/web/src/components/features/upload/OcrFailureScreen.tsx` |
| 2.22 Retry upload mất context | Disagree | P3 | Flow hiện tại dùng `retryRecordId` và backend giữ nguyên `recordId`; frontend `router.replace` về đúng record đó, không mất context như mô tả. (Vẫn có thể improve: giữ scroll position). | Epic 3.5 | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx#handleRetryUpload`, backend `HealthRecordService.createUploadUrl` |

### 11. OCR Pipeline

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 11.1 AWS Textract fallback là stub | Partly | P1 | Đúng là stub (không gọi thật), nhưng mô tả “throws not implemented” không chính xác: hiện trả `textract-stub` khi disabled. Đây vẫn là gap production nếu bạn muốn provider ngoài cho PDF/image. | Epic 3.1 + 4.6 Task 6 (OCR routing theo môi trường) | `apps/api/src/main/java/com/healthlens/api/service/AwsTextractClient.java` |
| 11.2 Python OCR service: sync HTTP trong async handler | Agree | P1 | Đúng: FastAPI endpoint `async def` nhưng dùng `requests.get()` blocking. Dưới tải có thể nghẽn event loop. | Epic 1.9 / OCR service | `services/ocr-service/app.py` |
| 11.3 No retry cho EasyOCR call | Agree | P2 | Đúng: `OcrService.processImage()` thử provider theo thứ tự nhưng **không có retry/backoff** cho cùng provider (easyocr/gcv/textract). Có thể OK cho MVP nhưng với SLA nên có retry có giới hạn + circuit breaker. | Epic 3 OCR pipeline + Epic 8.3 | `apps/api/src/main/java/com/healthlens/api/service/OcrService.java` |
| 11.4 No auth / rate limiting trên OCR endpoint | Partly | P1 | Với **API**: `/api/ocr/*` hiện không `permitAll()` trong `SecurityConfig` và có `@RequiresConsent` => không public. Nhưng **microservice** `services/ocr-service` expose `POST /ocr` không auth/rate limit, nếu deploy public sẽ là rủi ro. | Epic 3 | `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`, `apps/api/src/main/java/com/healthlens/api/controller/OcrController.java`, `services/ocr-service/app.py` |
| 11.5 Language detection heuristic | TBD | P3 | v2 nói có heuristic phát hiện ngôn ngữ; phía Spring chỉ forward `language_detected` từ microservice. Cần review thêm `services/ocr-service` nếu muốn nâng accuracy multilingual. | OCR quality | `services/ocr-service/*`, `apps/api/src/main/java/com/healthlens/api/service/OcrService.java` |
| 11.6 LLM prompt là Java string literal | Agree | P2 | Đúng: prompt OCR->LLM đang inline trong Java, khó maintain/version/test. Nên externalize template + versioning. | OCR/LLM maintainability | `apps/api/src/main/java/com/healthlens/api/service/OcrService.java` |
| 11.7 Regex fallback parser fragile | Agree | P2 | Đúng: `parseMetricsByRegex` dựa regex/heuristics line-based; dễ fail với layout khác. Cần test corpus + hardening. | OCR quality | `apps/api/src/main/java/com/healthlens/api/service/OcrService.java#parseMetricsByRegex` |
| 11.8 LLM JSON truncation repair heuristic fragile | Agree | P2 | Đúng: repair truncated JSON hiện “đóng ngoặc” rất đơn giản; dễ tạo JSON invalid. Nên dùng JSON repair library hoặc retry/reprompt. | OCR quality | `apps/api/src/main/java/com/healthlens/api/service/OcrService.java` |

### 12. LLM / AI / RAG

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 12.1 Populate Qdrant với medical data | Disagree | P0 | Outdated: Story 4.6 đã mô tả ingestion pipeline; code có `MetricExplanationIngestionJob/Service` và dataset `ai/metric-explanations.vi.json`. Vẫn còn câu hỏi quality/coverage corpus, nhưng “chưa có ingestion” là sai. | Epic 4.6 | `apps/api/src/main/java/com/healthlens/api/service/MetricExplanationIngestionJob.java`, `apps/api/src/main/resources/ai/metric-explanations.vi.json` |
| 12.2 RAG knowledge base documents | Partly | P1 | Có curated dataset, nhưng nếu mục tiêu RAG “đủ rộng” thì vẫn thiếu corpus chuẩn hóa + quy trình review/versioning. Cần quyết định mức coverage cho launch. | Epic 4.6 + Admin ref data | Dataset + ingestion code như trên |
| 12.8 `HealthMetricCard` query key gồm `value` + `status` | Agree | P2 | Đúng: queryKey đang gồm `value,status`, khiến edit nhỏ invalidates cache và refetch. Không phải blocker nhưng tốn cost/latency. | Epic 4.6 | `apps/web/src/components/ui/HealthMetricCard.tsx` |

### 13. Backend: Transaction & Race Condition

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 13.1 Audit writes trong read-only transaction | Agree | P0 | Đúng và là blocker: read paths có side effects (audit inserts) -> khó scale, khó reason, có thể fail dưới readOnly semantics. | Epic 7.5 (audit log viewer) + cross-cutting audit | `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java` + `ReferenceDataService` |
| 13.2 `purgeSoftDeletedRecords()` không transactional | Partly | P0 | Đúng: best-effort, không atomic. Nhưng retention 30 ngày đã có (khớp decision). Vấn đề còn lại: idempotency/compensation/locking để tránh zombie/mất file. | Compliance/right-to-delete + ops | `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java#purgeSoftDeletedRecords` |
| 13.3 `createProfile()` race condition vượt giới hạn | Agree | P1 | Đúng: `countByUserId` rồi `save` có thể vượt giới hạn 10 nếu concurrent. Cần DB-level guard (constraint/lock) hoặc retry. Không phải security, nhưng là integrity/abuse. | Epic 2.2 create profile | `apps/api/src/main/java/com/healthlens/api/service/ProfileService.java` |
| 13.4 Refresh token rotation race | Agree | P0 | Đúng: 2 refresh concurrent dùng cùng refresh token có thể đều pass `revokedAt is null` trước khi commit, tạo 2 refresh token mới mà không phát hiện replay. Với production scope (security/PII), đây là P0. | Epic 1.3 secure session | `apps/api/src/main/java/com/healthlens/api/service/AuthService.java#refreshWithConsent` + `RefreshTokenRepository` |
| 13.5 Stream ack không đồng bộ DB (`OcrJobConsumer`) | Agree | P0 | Đúng: consumer `ack` message sau khi gọi service; nếu DB write fail nhưng ack thành công => mất job. Nếu DB success nhưng ack fail => job chạy lại (double-processing). Cần idempotency + retry + DLQ, hoặc transactional outbox pattern. | Epic 3 OCR pipeline + Epic 8.3 SLA metrics | `apps/api/src/main/java/com/healthlens/api/service/OcrJobConsumer.java` |
| 13.6 Không có DLQ cho OCR | Agree | P0 | Đúng: hiện exception path cũng `ack` luôn; fail cases không có retry policy/poison-queue -> ảnh hưởng SLA và analytics 8.3 (failure reason). | Epic 3.5 OCR failure recovery + Epic 8.3 | `OcrJobConsumer` + Redis streams |
| 13.7 Deletion request/cancel race | TBD | P1 | Có guard trong `cancelDeletionRequest`: chỉ cancel khi user `PENDING_DELETION` và request còn `PENDING`. Scheduler `executeDataDeletion` cũng check status `PENDING`. Tuy vậy chưa thấy locking để tránh race rất sát (cancel vs execute). Cần review isolation/locking và test. | Epic 1.6 deletion | `apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java` |
| 13.8 Shared editor modify User entity | Agree | P0 | Đúng: `ProfileService.updateProfile()` nếu profile default thì sync sang `User` (fullName/birthDate/gender), và **shared editor có quyền edit** profile -> có thể thay đổi thông tin chủ tài khoản. Đây là nghiêm trọng. | Epic 6 sharing + Epic 2 profile | `apps/api/src/main/java/com/healthlens/api/service/ProfileService.java` |
| 13.9 Duplicate ProfileShare accept concurrent | Agree | P1 | Có check `existsBy...` rồi `save` (check-then-insert) => race. Cần unique constraint `(profile_id, viewer_id, revoked_at is null?)` hoặc locking. | Epic 6 | `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java` |
| 13.10 `@Auditable` không có Aspect | Disagree | P1 | Sai về fact: codebase có `AuditableAspect`. Tuy vậy cần kiểm chứng coverage và PII logging. | Epic 7.5 | `apps/api/src/main/java/com/healthlens/api/aspect/AuditableAspect.java` |
| 13.11 Thiếu audit logs cho share lifecycle | Agree | P1 | Đúng: hiện mới thấy audit cho `revokeShare` (`ProfileShareService.writeRevokeAuditLog`). Các action còn lại (invite/create, resend, accept, reject, cancel) chưa ghi audit log domain-level => khó trace trong audit viewer/incident response. | Epic 7.5 (audit log viewer) + Epic 6 sharing | `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java` + `ProfileShareAuditLogRepository` |
| 13.12 Audit per-metric gây growth lớn | Partly | P2 | Có cơ chế audit trong `ReferenceDataService.classifyMetric(...)` và `HealthRecordService.enrichMetric(..., persistAudit=true)` gọi theo từng metric. Nếu audit viewer cần “ai xem record nào khi nào” thì 1 event/record view đủ; per-metric có thể làm bảng audit phình nhanh. Cần chốt audit granularity. | Epic 7.5 | `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java#enrichMetric`, `apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java` |
| 13.13 Xoá user không cleanup share/invitation/audit artefacts | Partly | P0 | DB có FK + `ON DELETE CASCADE` cho `profile_shares`/`profile_invitations` theo `profiles` và `users`, nên khi xoá **profiles owned** thì share/invitation của các profile đó sẽ cascade. Tuy nhiên `DataDeletionService` **không xoá user row** (chỉ anonymise), nên các artefacts gắn với user theo vai trò khác (ví dụ user là `viewer_id` trên profile của người khác, hoặc audit logs FK `user_id -> users`) vẫn còn. Với right-to-delete, cần quyết định: có xóa/revoke các shares mà user là viewer không; và audit logs có được giữ lại hay phải scrub. | Epic 1.6 right-to-delete + Epic 7.5 | `apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java`, migrations `V015/V016/V017/V027` |
| 13.14 Email verification “lost khi Redis down” | Disagree | P2 | Sai về fact: flow verify-email dùng DB table `email_verification_tokens`; `register()` tạo token và publish email event, không phụ thuộc Redis để “giữ trạng thái xác thực”. Redis down không làm mất token. | Epic 1.3 verify email | `apps/api/src/main/java/com/healthlens/api/service/AuthService.java#register`, `EmailVerificationTokenRepository` |
| 13.15 `forgotPassword` silent swallow error | Partly | P2 | Đúng là nuốt lỗi và luôn return success; nhưng đây là chủ ý để chống user enumeration. Vấn đề cần bổ sung cho production là observability/alerting khi email gửi fail (chứ không phải đổi behavior user-facing). | Epic 1.4 password reset | `apps/api/src/main/java/com/healthlens/api/service/AuthService.java#forgotPassword` |
| 13.16 S3 deletion best-effort khi xoá user | Partly | P1 | Đúng: `StorageService.deleteObjectsByPrefix(...)` catch mọi exception và return best-effort count để DB deletion vẫn chạy. Với compliance, cần cơ chế “reconciliation/cleanup job” để retry xoá object nếu lần đầu fail. | Epic 1.6 right-to-delete + ops | `apps/api/src/main/java/com/healthlens/api/service/StorageService.java#deleteObjectsByPrefix`, `DataDeletionService.executeDataDeletion` |
| 13.17 `cancelInvitation()` hard-delete | Agree | P2 | Đúng: `cancelInvitation` đang `profileInvitationRepository.delete(inv)` (hard delete). Nếu muốn auditability/consistency với soft-delete pattern, nên chuyển sang status `cancelled` + audit. Nếu không cần thì keep nhưng phải rõ “invitations are ephemeral”. | Epic 6 sharing | `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java#cancelInvitation` |
| 13.18 Cleanup shares/invitations khi user là viewer/invitee | Agree | P1 | Với right-to-delete, nếu user từng được share profile của người khác (`profile_shares.viewer_id=userId`) thì `DataDeletionService` hiện **không revoke/delete** các dòng này (vì không xoá user row). Tương tự, `profile_invitations.invitee_email` có thể chứa email thật của user đã bị anonymise -> cần scrub/cancel. | Epic 1.6 + Epic 6 | `DataDeletionService.executeDataDeletion`, `ProfileShareService`, migrations `V015/V017` |
| 13.19 Gender normalization drop non-standard values | Partly | P2 | Đúng: `ReferenceDataService.normalizeGender()` chỉ map `male|female` (và vài alias “nam/nữ”), còn “other” hoặc free-text sẽ -> `null`. Tác động: chọn reference range “gender-agnostic”. Nếu product cho phép `other`, cần quyết định logic: treat “other” như null hay cần ranges riêng. | Epic 2 profile + Epic 4.6 classification | `apps/api/src/main/java/com/healthlens/api/service/ReferenceDataService.java#normalizeGender`, DTO validation `UpdateUserRequest` |
| 13.20 Same-class `@Transactional` self-invocation proxy bypass | Disagree | P3 | Finding này không áp dụng theo code hiện tại: `markOcrCompleted(3 args)` gọi overload `markOcrCompleted(4 args)` nhưng **cả hai** đều `@Transactional`, và call nội bộ vẫn nằm trong transaction của method ngoài. Không thấy “proxy bypass” tạo behavioral bug ở đây. | Epic 3 OCR | `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java#markOcrCompleted` |
| 13.21 `confirmUpload()` retry race overwrites `fileKey` | Partly | P2 | Có thể xảy ra nếu client tạo nhiều upload reservations cho cùng `recordId` (đặc biệt retry) rồi confirm song song: `confirmUpload` lấy reservation hiện tại, set `record.fileKey`, enqueue job, delete reservation. Nếu trước đó đã upload vào “fileKey cũ”, object sẽ orphan. Cần chốt invariant: 1 recordId chỉ có 1 fileKey, và flow retry phải tạo record mới hoặc cleanup keys cũ. | Epic 3.1/3.5 upload & retry | `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java#persistUploadReservation`, `#confirmUpload` |

### 14. Backend: Security & Data Integrity

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 14.1 JWT secret hardcoded dev | Partly | P0 | Đúng là có **default secret** cho base/dev profiles. Staging/prod yêu cầu env (`?JWT_SECRET is required`). Rủi ro P0 nằm ở misconfig: deploy sai profile hoặc quên set env sẽ chạy với secret mặc định. | Infra + Security | `apps/api/src/main/resources/application.yml` (`jwt.secret`) |
| 14.2 Spring Boot 4.0.3 pre-release risk | Partly | P1 | Rủi ro stability là thật; nhưng “P0” phụ thuộc bạn chốt policy dùng stable-only hay chấp nhận milestone. Với scope production, mình đề xuất P1 (đổi về stable) hoặc “dependency locking + soak tests”. | Infra | `apps/api/build.gradle.kts` |
| 14.3 Email lộ trong URL cancellation | Agree | P0 | Đúng: cancellation link hiện build dạng `...?token=...&email=...&expiresAt=...&requestedAt=...`. Với PHI/PII scope, email trong URL là P0 (leak qua logs/history/referer). Email phải derive server-side từ token hoặc trả về trong response (không nằm URL). | Epic 1.6 deletion + security hardening | `apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java#buildCancellationLink` |
| 14.4 MinIO credentials hardcoded | Partly | P1 | Đúng: có default creds `minioadmin/minioadmin` trong config. Acceptable cho local dev; production cần enforce env secret và CI guard để không deploy với default. | Infra hardening | `apps/api/src/main/resources/application.yml`, `docker/compose*.yml` |
| 14.7 No pagination getRecordsByProfile | Agree | P1 | Đúng: `GET /health-records/profiles/{profileId}` trả **list all** (không page/limit). Có endpoint `getProfileHistory(page,limit)` nhưng route này vẫn có thể gây memory/latency nếu profile có nhiều records. | Epic 5.1/5.2 | `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java`, `HealthRecordService.getRecordsByProfile` |
| 14.9 Admin TOTP brute-force không rate limit | Disagree | P2 | Outdated: admin auth đã có `AdminAuthRateLimiter` (3 attempts, lock 30 phút) và `AdminAuthService.login()` gọi `checkLocked/recordFailure` cho cả password và TOTP. | Epic 7.1 | `apps/api/src/main/java/com/healthlens/api/security/AdminAuthRateLimiter.java`, `AdminAuthService.java` |
| 14.6 Stored XSS potential (`diagnosis`, `hospitalName`) | Disagree | P2 | Frontend React render text node mặc định sẽ escape HTML; không thấy `dangerouslySetInnerHTML`/markdown renderer => stored XSS theo kiểu “inject `<script>`” không xảy ra. Vẫn cần validation length + charset để tránh log/UX issues. | Epic 3/5 | `apps/web/src/app/(dashboard)/health-records/review/[recordId]/page.tsx`, `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 14.5 Thiếu `@Valid` trên confirmRecord endpoint | Agree | P1 | Đúng: `HealthRecordController.confirmRecord` nhận `@RequestBody(required=false)` và không `@Valid`. Service validate metrics nếu có, nhưng các field khác không có bean validation. | Epic 3.3 review & save | `apps/api/src/main/java/com/healthlens/api/controller/HealthRecordController.java` |
| 14.10 User-facing 500 cho IllegalArgumentException | Disagree | P2 | Outdated: `GlobalExceptionHandler` map `IllegalArgumentException` -> 400 ProblemDetail. | Cross-cutting | `apps/api/src/main/java/com/healthlens/api/exception/GlobalExceptionHandler.java` |
| 14.8 5s cache TTL cho status quá ngắn | Agree | P1 | Đúng: `getStatus()` cache Redis TTL 5 giây. Với polling 1-2s có thể OK, nhưng dưới tải sẽ vẫn hit DB thường xuyên; nên tăng TTL (ví dụ 15-30s) hoặc cache theo state (processing vs done). | Epic 3 upload polling + Epic 8.3 | `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java#getStatus` |

Ngoài ra: `application.yml` có default MinIO creds (`minioadmin/minioadmin`). Đây là acceptable cho local dev nhưng **P0/P1** nếu prod misconfig.

### 19. CI / CD & Docker

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 19.x Web tests không chạy trong CI | Agree | P1 | CI hiện chạy `pnpm --filter web lint` và `pnpm --filter web build`, nhưng **không chạy `pnpm --filter web test`** (vitest). Với production scope, nên có web tests gate tối thiểu. | Infra/devops | `.github/workflows/ci.yml`, `apps/web/package.json` |
| 19.1 Web Dockerfile dùng pnpm@9 | Agree | P1 | Đúng: Dockerfile `corepack prepare pnpm@9` trong khi CI/env dùng pnpm `10.13.1`. Có thể gây lockfile mismatch/bug khó debug. | Infra/devops | `apps/web/Dockerfile` |
| 19.9 OCR Dockerfile redundant builder stage | Agree | P3 | Đúng: stage `builder` cài lại requirements y hệt `deps` nhưng không dùng ở runner. Không ảnh hưởng runtime nhưng là nợ kỹ thuật. | Infra/devops | `services/ocr-service/Dockerfile` |
| 19.2 PostgreSQL missing từ base `compose.yml` | Partly | P2 | Đúng là base compose không có Postgres, nhưng comment trong file nói base chỉ là “shared services”; Postgres nằm ở `compose.dev.yml`. Nếu mục tiêu là “compose base = đầy đủ local stack” thì nên đưa Postgres vào base; nếu không thì đây là quyết định, không phải bug. | Infra/devops | `docker/compose.yml`, `docker/compose.dev.yml` |

### 15-17. Frontend (Anti-patterns / Dead Code / Error Handling)

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 15.2 `useEffect` missing deps (UploadButton) | Disagree | P3 | Outdated: effect auto-open đã có deps `[profileId, searchParams]`. | Epic 3.1 | `apps/web/src/components/features/upload/UploadButton.tsx` |
| 16.1 `sessionStorage` không try/catch (7 locations) | Partly | P1 | Đúng ở ít nhất invitations/admin; chưa đối chiếu đủ “7 locations” nhưng pattern tồn tại. P1 reliability. | Cross-cutting | web pages/components |
| 16.2 No-op union type `"view" | "edit" | string` | Agree | P2 | Đúng: type bị widen thành `string`, mất type safety. | Epic 6 | `apps/web/src/components/features/profiles/InviteMemberModal.tsx` (và các nơi khác) |
| 16.3 `as unknown as` double assertion (history) | Agree | P1 | Đúng: `resolveHistoryStatus` dùng cast để đọc field không tồn tại trong type. Nên fix type hoặc runtime validation. | Epic 5.1 | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 17.7 UploadButton “empty catch” | Partly | P1 | Catch không trống nhưng không phân loại error theo bước; không log/correlation id; khó support. | Epic 3.1/3.5 | `apps/web/src/components/features/upload/UploadButton.tsx` |
| 17.8 PUT success nhưng confirm fail không retry | Agree | P0 | Đúng: nếu `PUT` xong nhưng `confirm-upload` fail, file nằm ở storage nhưng record không process; UI chỉ báo lỗi chung. Production cần retry/“resume confirm” + job reconciliation. | Epic 3.5 + Epic 8.3 | `apps/web/src/components/features/upload/UploadButton.tsx`, backend confirm endpoint |
| 17.2 API Client 403 redirect hardcoded `/dashboard` | Agree | P1 | Đúng: interceptor redirect về `/dashboard` trong khi app dùng `/home`. | Cross-cutting | `apps/web/src/lib/api/apiClient.ts` |
| 17.3 API Client thiếu AbortController | Agree | P2 | Đúng: axios client không có cancellation strategy cho route change/unmount; không critical nhưng tốt cho UX. | Cross-cutting | `apps/web/src/lib/api/apiClient.ts` |
| 17.4 API Client không retry transient 5xx | Agree | P2 | Đúng: chỉ có retry logic cho 401 refresh; 5xx/network không retry/backoff. | Cross-cutting | `apps/web/src/lib/api/apiClient.ts` |

### 18. Mobile App

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 18.5 Missing `@healthlens/shared` dependency | Agree | P1 | Đúng: mobile `tsconfig` có path alias `@healthlens/shared` nhưng `apps/mobile/package.json` không khai báo dependency workspace. Sẽ gây lỗi build/CI mobile. | Epic 9 (mobile) | `apps/mobile/package.json`, `apps/mobile/tsconfig.json` |
| 18.1 “ZERO implemented” mobile | Partly | P2 | Repo có mobile app skeleton (`apps/mobile/src` có ~18 files) nhưng mức độ tính năng so với PRD (10+ screens) có thể chưa đạt. Không thuộc Phase 1 web MVP nên không P0. | Epic 9 | `apps/mobile/src` |
| 18.7 State management empty exports | Agree | P2 | Đúng: `apps/mobile/stores/index.ts` và `apps/mobile/hooks/index.ts` export `{}`. | Epic 9 | `apps/mobile/stores/index.ts`, `apps/mobile/hooks/index.ts` |

### 20. Cross-cutting

| ID (v2) | Disposition | Priority | Ghi chú |
|---|---|---:|---|
| 20.1 `alert()` thay toast | Agree | P2 | UX polish; không blocker production nếu có kế hoạch thay dần. |
| 20.2 Loading skeleton nhiều pages | Agree | P2 | Improve perceived performance; không phải correctness. |
| 20.3 Exam date validate tương lai | TBD | P2 | Cần đối chiếu validation ở web form + backend parse; hiện backend parse examDate có warning. |
| 20.4 Global exception handler handle 405 | TBD | P2 | `GlobalExceptionHandler` chưa có handler riêng cho `HttpRequestMethodNotSupportedException`; cần xác nhận response thực tế có rơi về HTML error page hay không (tùy spring config). |
| 20.5 A11y htmlFor/id | Agree | P3 | A11y backlog. |
| 20.6 no-img-element disables | Agree | P3 | Tech debt/Next best practice. |
| 20.7 Radix themes tree-shake | Agree | P3 | Perf micro-opt. |
| 20.8 Error boundary auth pages | Agree | P2 | Resilience UX. |
| 20.9 Offline detection | Agree | P2 | Resilience UX. |
| 20.10 Request deduplication | Partly | P3 | TanStack Query đã dedup theo queryKey, nhưng “multiple components fetch same endpoint with different keys” vẫn có thể xảy ra. |

### Admin Analytics (Epic 8 / FR38)

Các finding “Admin dashboard metrics stub/hardcoded” được xem theo source of truth:
- `_bmad-output/implementation-artifacts/epic-8/8-1...md`, `8-2...md`, `8-3...md` đều `ready-for-dev`.
- Bạn xác nhận **production bắt buộc có đủ 8.1/8.2/8.3** => đánh P0 cho “missing implementation”.

| Area | Disposition | Priority | Ghi chú | Story mapping |
|---|---|---:|---|---|
| Admin analytics chưa implement (8.1/8.2/8.3) | Agree | P0 | Đây là gap planned-vs-built. Không nhất thiết là bug của code hiện tại, nhưng là thiếu scope production. | Epic 8 (FR38) |

### 7. Admin (Security/Session)

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 7.1 Admin Dashboard metrics (stub) | Agree | P0 | Đúng: admin dashboard page đang hiển thị placeholder “Sẽ implement Story 8.x”. Bạn đã chốt production phải đủ 8.1/8.2/8.3. | Epic 8 | `apps/web/src/app/admin/page.tsx`, `_bmad-output/implementation-artifacts/epic-8/*` |
| 7.2 Audit log display (stub) | **Resolved** | — | **2026-05-21:** Audit log viewer shipped; decomposed modules (remaining-6-4 / core-7-5). | Epic 7.5 + remaining-6-4 | `apps/web/src/app/admin/audit-log/page.tsx`, `components/admin/audit-log/` |
| 7.3 CSV import / PDF export (reference data) | TBD | P2 | Chưa thấy flow import/export. Đây là scope mở rộng: nếu admin cần bulk ops thì làm; nếu không thì postpone. | Epic 7.x | Admin ref data UI + API TBD |
| 7.4 Approval history timeline (change set approvals) | Partly | P2 | Có trang approvals list change sets và status, nhưng “timeline/history đầy đủ” (ai approve khi nào, reasons, prior revisions) chưa thấy như một audit viewer tổng quát. Cần chốt yêu cầu. | Epic 7.4/7.5 | `apps/web/src/app/admin/reference-data/approvals/page.tsx` |
| 7.5 Admin token trong `sessionStorage` | Agree | P0 | Đúng và là P0 trong production: admin token trong JS storage => XSS là mất quyền admin. Planning (7.1) mong muốn “admin session TTL 15 phút”; nên align cơ chế với user (refresh token HttpOnly cookie / server session). | Epic 7.1 (MFA) + security hardening | `apps/web/src/lib/api/adminApiClient.ts`, `apps/web/src/app/admin/login/page.tsx`, `apps/web/src/app/admin/layout.tsx` |
| 7.6 `sessionStorage` không try/catch | Agree | P1 | Đúng: trong một số môi trường (private browsing/quota) có thể throw và crash. Không phải data breach nhưng ảnh hưởng reliability/UX. | Cross-cutting | nhiều file web (admin + invitations accept) |
| 7.7 `Number(range.minValue)` không validate (NaN) | Partly | P1 | Đúng: UI build payload dùng `Number(...)` có thể ra `NaN` nếu user nhập text; backend có validation range nhưng UX nên chặn sớm và highlight field lỗi. | Epic 7.2 | `apps/web/src/app/admin/reference-data/page.tsx` |
| 7.8 Submit/Publish change set endpoints “dead” | Agree | P2 | Đúng: backend có `/change-sets/{id}/submit` và `/publish`, nhưng frontend hiện không thấy gọi. Nếu multi-admin mode cần submit, đây là gap. | Epic 7.4 | `apps/api/src/main/java/com/healthlens/api/controller/AdminReferenceDataController.java`, `apps/web/src/app/admin/reference-data/page.tsx` |
| 7.9 `new Date()` không validate trong `formatDate` | Agree | P3 | Đúng: `formatDate(dateStr)` gọi `new Date(dateStr)` rồi format, không guard `Invalid Date`. Nếu `createdAt` malformed/null sẽ render “Invalid Date”. | Admin UX polish | `apps/web/src/app/admin/reference-data/approvals/page.tsx#formatDate` |

### Invitations Accept / Cancel Deletion (token in URL)

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| IA-01 Invitation token in URL query param | Agree | P0 | Đúng: token trong URL có risk leakage (history, logs, referer). Với PII/PHI scope, cần sửa. | Epic 6 (sharing) + security hardening | `apps/web/src/app/(auth)/invitations/accept/page.tsx` + backend accept |
| IA-02 `sessionStorage` accessed without try/catch | Agree | P1 | Đúng: `sessionStorage.getItem/setItem` có thể throw (private browsing/quota) => crash. Ở production nên bọc try/catch và degrade gracefully. | Cross-cutting | `apps/web/src/app/(auth)/invitations/accept/page.tsx` + các chỗ admin |
| IA-03 Expired token silently redirects | Agree | P1 | Đúng: backend có thể trả `outcome=expired` + `redirectUrl`, nhưng frontend không check `outcome` và luôn `window.location.replace(redirectUrl)` -> UX khó hiểu (user bị đá sang `/profiles`). Nên hiển thị error state rõ ràng và CTA. | Epic 6 sharing | `apps/web/src/app/(auth)/invitations/accept/page.tsx`, `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java#acceptInvitation` |
| IA-04 Outcome type mismatch (`require-register` vs `require-login`) | Agree | P1 | Đúng: backend trả `"require-login"` khi anonymous; frontend type liệt kê `"require-register"`. Hiện code không branch theo outcome (đi thẳng `redirectUrl`), nhưng type mismatch làm sai contract/maintainability. | Epic 6.1 invite member | Backend: `ProfileShareService.acceptInvitation` | 
| IA-08 `window.location.replace` used for all outcomes | Partly | P2 | Đúng là đang replace luôn; nhưng đây là chủ ý “full navigation” để bootstrap auth/session. Tuy vậy nên xử lý riêng `expired` (show message thay vì redirect). | Epic 6 | `apps/web/src/app/(auth)/invitations/accept/page.tsx` |
| IA-09 No audit log for invitation accept | Agree | P1 | Đúng: accept path không ghi audit (trong `ProfileShareService.acceptInvitation`). Với compliance/ops, nên có audit cho share lifecycle. | Epic 7.5 (audit viewer) + sharing | `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java` |
| IA-05 No rate limiting on permitAll accept endpoint | Agree | P1 | Đúng: endpoint accept là `permitAll()` trong `SecurityConfig`, hiện chưa thấy rate limiter riêng cho accept. Nên thêm rate limit + response blinding để giảm enumeration oracle. | Epic 6 + security hardening | `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java` + `InvitationController` |
| IA-06 Accept + revoke race condition | Partly | P2 | Có thể xảy ra cạnh tranh giữa `revokeShare()` (owner revoke) và `acceptInvitation()` (viewer accept) nếu link mời còn hợp lệ: accept có thể tạo/khôi phục share ngay sau khi owner revoke. Cần chốt invariant: revoke phải invalidate token + accept phải check “revoked after invite created” hoặc use token single-use + audit. | Epic 6 sharing | `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java#acceptInvitation`, `#revokeShare` |
| IA-07 Missing 409 handling (contract/UX) | Agree | P2 | Frontend accept page hiện không branch riêng cho `409` (conflict: already accepted/expired/illegal state), dẫn tới message generic. Không blocker nhưng làm support khó. | UX polish | `apps/web/src/app/(auth)/invitations/accept/page.tsx` |
| IA-10 `revokeShare` fallback uses wrong ID | Agree | P1 | Đúng: fallback `profileShareRepository.findById(viewerId)` là sai semantics (viewerId != shareId) và có thể dẫn tới behavior bất ngờ/bug khó debug. | Epic 6 sharing | `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java#revokeShare` |
| IA-11 403 fallback message misleading | Partly | P2 | Đúng một phần: 403 hiện default “Email đăng nhập không khớp…” nhưng thực tế 403 còn có thể do account pending deletion/consent/permission khác. Nên dựa `errorCode/type` để message chính xác hơn. | UX hardening | `apps/web/src/app/(auth)/invitations/accept/page.tsx` |
| IA-12 Unsafe type cast without runtime validation | Agree | P2 | Đúng: `response.data?.data as AcceptResult` không validate shape. Nên dùng zod/guard để tránh crash khi contract đổi. | Web robustness | `apps/web/src/app/(auth)/invitations/accept/page.tsx` |
| IA-13 No timeout / cancellation on API request | Agree | P2 | Đúng: không timeout/AbortController; network hang -> spinner vô hạn. | Web robustness | `apps/web/src/app/(auth)/invitations/accept/page.tsx` |
| IA-14 Incoming invitations query not invalidated after accept | TBD | P3 | v2 đề cập banner invitation còn hiển thị đến lần refetch tiếp theo. Cần đối chiếu trang `/profiles` hiện tại có query `incomingInvitations` và invalidate sau accept hay chưa. | Epic 6 | `apps/web/src/app/(dashboard)/profiles/page.tsx` (cần verify) |
| IA-15 No notification to owner on accept | Agree | P2 | Đúng: accept hiện không gửi email/push cho owner. Với audit/ops, nên có ít nhất audit log + optional email notify. | Epic 6 + Epic 7.5 | `apps/api/src/main/java/com/healthlens/api/service/ProfileShareService.java#acceptInvitation` |
| IA-16 useEffect missing cleanup (setState on unmounted) | Partly | P3 | Có thể xảy ra nếu user rời trang nhanh; low impact nhưng đúng pattern. | Web tech debt | `apps/web/src/app/(auth)/invitations/accept/page.tsx` |
| IA-17 Token visible in address bar until redirect | Agree | P3 | Đúng: token vẫn ở URL từ lúc load đến khi redirect; có thể `replaceState` ngay khi đọc token để giảm leak. (Không thay thế được backend fix IA-01, chỉ giảm rủi ro). | Security polish | `apps/web/src/app/(auth)/invitations/accept/page.tsx` |
| IA-18 No i18n on accept page | Agree | P3 | Nếu app target 1 ngôn ngữ thì không cần; nếu roadmap multi-locale thì đây là backlog. | Product decision | Web accept page |
| IA-19 No page title / OG metadata | Agree | P3 | Email preview/SEO polish. | UX polish | Web accept page |
| IA-20 Theoretical unhandled promise rejection | Disagree | P3 | `void run()` trong effect không gây unhandled rejection vì promise được handled trong `run()` try/catch. | N/A | `apps/web/src/app/(auth)/invitations/accept/page.tsx` |
| IA-21 Component re-mount | Disagree | P3 | Không phải bug actionable. | N/A | N/A |
| CD-01 Email exposed in URL | Agree | P0 | Đúng: email trong URL là PII leak. | Epic 1.6 account deletion request | `apps/web/src/app/cancel-deletion/*` + backend email link generation |
| CD-02 Cancellation token in URL | Agree | P0 | Đúng: cancellation token trong URL query param là sensitive; leak tương tự invitation token. | Epic 1.6 | `DataDeletionService` link build + CancelDeletionClient |
| CD-03 Token replay mitigation incomplete | Partly | P2 | Token replay hiện bị chặn gián tiếp vì request status chuyển `CANCELLED` nên lần sau sẽ fail `!isPending()`. Nhưng thiếu audit detail (IP/user-agent, correlation id), và không notify owner “ai vừa cancel”. Với PHI, nên tăng audit. | Epic 1.6 + Epic 7.5 | `apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java#cancelDeletionRequest` |
| CD-04 Client-side expiry gate blocks backend cancellation | Agree | P1 | Đúng: frontend disable nút theo `isExpired` local time; backend mới là authority. Nên cho phép submit, backend trả lỗi nếu quá hạn. | Epic 1.6 | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx` |
| CD-05 Test coverage critically insufficient | Agree | P2 | Đúng: hiện test coverage cho CancelDeletion UI rất ít; production cần cover happy path + error matrix + double click + countdown. | QA | `apps/web/src/app/cancel-deletion/CancelDeletionClient.test.tsx` |
| CD-06 409 treated as identical success | Agree | P2 | Đúng theo v2: cần xác nhận code hiện tại xử lý 409 thế nào (nhiều khả năng đang show success luôn). Nếu có khác thì sẽ chỉnh. | UX hardening | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx` |
| CD-07 401/403/500/429 all generic error | Agree | P2 | Đúng: page hiện không branch theo status code một cách rõ; nên map status->message/action. | UX hardening | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx` |
| CD-08 Token normalization mismatch (`URLEncoder` vs `decodeURIComponent`) | Disagree | P3 | Token hiện là Base64 URL-safe (không có `+`/space) và backend encode bằng `URLEncoder` (`%xx`), frontend decode bằng `decodeURIComponent` là tương thích. Finding này có thể áp dụng nếu token format đổi. | N/A | `DataDeletionService.generateCancellationToken`, `CancelDeletionClient.normalizeCancellationToken` |
| CD-09 `parseTimestamp` fragile timezone regex | Partly | P2 | Backend đang gửi `Instant.toString()` (ISO-8601) nên `new Date(...)` parse được; regex “space HH:MM -> +HH:MM” là thừa và có thể tạo hành vi lạ nếu input không chuẩn. Nên restrict nhận ISO-8601 và fail closed. | Web robustness | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx#parseTimestamp`, backend `DataDeletionService` |
| CD-10 No initial check for missing token | Agree | P2 | Đúng: thiếu `useEffect` set error khi token null; hiện user thấy UI ready rồi click mới biết lỗi. | UX polish | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx` |
| CD-11 `maskedEmail` memo re-runs every render | Agree | P3 | Đúng: dependency `searchParams` là object mới mỗi render. Extract `email` string sớm để dependency ổn định. | Perf/clarity | `apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx` |
| CD-12 Tautological test assertion | TBD | P3 | Cần mở `CancelDeletionClient.test.tsx` để xác nhận finding; nếu đúng thì test đang không assert behavior. | QA | `apps/web/src/app/cancel-deletion/CancelDeletionClient.test.tsx` |
| CD-13 No mock reset between tests | TBD | P3 | Cần mở test file để xác nhận; pattern `vi.fn()` leak là phổ biến. | QA | `apps/web/src/app/cancel-deletion/CancelDeletionClient.test.tsx` |
| CD-14 No rate limiting on public cancel endpoint | Agree | P1 | Đúng: cancel deletion là public (`permitAll` via `USERS_DELETION_PATTERN`) và chưa thấy rate limit riêng. Với compliance/abuse prevention, cần limit theo IP + token hash. | Epic 1.6 | `apps/api/src/main/java/com/healthlens/api/config/SecurityConfig.java`, `UserController.cancelDeletion` |
| CD-15 Race window cancellation/deletion scheduler | Partly | P3 | Có guard `isOverdue()` + check user status `PENDING_DELETION`, và scheduler re-check `status==PENDING`. Tuy vậy vẫn nên có tests cho các edge-case concurrent. | Epic 1.6 | `DataDeletionService.processDeletionRequests/executeDataDeletion/cancelDeletionRequest` |
| CD-16 Countdown timer lags in background tab | Agree | P3 | Browser throttling là thật; chấp nhận được hoặc improve theo visibility API. | UX polish | `CancelDeletionClient.tsx` |
| CD-17 72-hour fallback undocumented | Agree | P3 | UI tự tính 72h; tốt hơn là hiển thị từ server/config để không lệch nếu policy đổi. | Product/ops | `CancelDeletionClient.tsx`, `DataDeletionService` |
| CD-18 Loading state replaces whole page | Agree | P3 | UX polish; không correctness. | UX polish | `CancelDeletionClient.tsx` |
| CD-19 Missing aria-live / role alert | Agree | P3 | A11y enhancement. | A11y | `CancelDeletionClient.tsx` |
| CD-20 catch uses `as AxiosError` instead of runtime check | Agree | P3 | Đúng: nên dùng `axios.isAxiosError` để tránh crash. | Web tech debt | `CancelDeletionClient.tsx` |

### Verify Email (`/verify-email`)

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| VE-001 Token leaked in URL referrer/history | Agree | P1 | Đúng: token ở query param; frontend không xóa khỏi URL nên có leakage qua history/screenshot và có thể qua `Referer` khi người dùng click link khác trong trang. | Epic 1.3 | `apps/web/src/app/(auth)/verify-email/page.tsx` |
| VE-002 No rate limiting verify-email endpoint | Agree | P1 | Đúng: `POST /api/v1/auth/verify-email` gọi thẳng `authService.verifyEmail` không rate limit theo IP/token. Nên có limiter để giảm brute-force token. | Security hardening | `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java` |
| VE-003 Error messages leak token validity enumeration | Agree | P2 | Đúng: service phân biệt "invalid" vs "expired/used". Nên unify message để giảm oracle. (Tradeoff: UX debug vs security). | Security hardening | `apps/api/src/main/java/com/healthlens/api/service/AuthService.java#verifyEmail` |
| VE-004 TOCTOU race on concurrent verify | Agree | P2 | Đúng: `verifyEmail()` load token rồi check `usedAt`, sau đó mới set `usedAt` và save. 2 request song song có thể cùng pass check trước khi commit. Nên dùng “atomic update where used_at is null” hoặc optimistic lock. | Epic 1.3 | `apps/api/src/main/java/com/healthlens/api/service/AuthService.java#verifyEmail` |
| VE-005 No audit logging on verify-email attempts | Agree | P2 | Đúng: không có audit log cho attempt/success/failure. Với ops/compliance, nên log structured (không log token raw) + correlate IP/user-agent nếu cần. | Epic 7.5 audit viewer + security | `apps/api/src/main/java/com/healthlens/api/service/AuthService.java#verifyEmail` |
| VE-006 Frontend shows backend `detail` verbatim | Agree | P2 | Đúng: frontend lấy `resp.data?.detail` và show thẳng; message backend hiện phân biệt invalid/expired => biến thành oracle ở UI. Nên map sang message fixed ở frontend. | Security/UX hardening | `apps/web/src/app/(auth)/verify-email/page.tsx` |
| VE-007 Unsafe type assertion for Axios error | Agree | P3 | Đúng: cast `error.response as { data?: { detail?: string } }` không check shape. Nên dùng `axios.isAxiosError` + type guard/zod. | Web tech debt | `apps/web/src/app/(auth)/verify-email/page.tsx` |
| VE-008 Frontend leaks token state via message | Agree | P2 | Đúng (hệ quả VE-006 + VE-003): UI có thể hiển thị “đã hết hạn/đã dùng” khác với “không hợp lệ” => enumeration. | Security hardening | `apps/web/src/app/(auth)/verify-email/page.tsx` + backend `AuthService.verifyEmail` |
| VE-009 No loading skeleton in Suspense fallback | Agree | P3 | Đúng: fallback là màn hình trống (background). Không ảnh hưởng correctness, chỉ UX. | UX polish | `apps/web/src/app/(auth)/verify-email/page.tsx` |
| VE-010 No resend-verification action | Agree | P3 | Đúng: nếu token expired/used, user không có CTA “gửi lại email xác thực”. Đây là UX/support improvement. | Epic 1.3 | Web + API (resend endpoint) TBD |
| VE-011 useEffect no cleanup/AbortController | Partly | P3 | Đúng: không có abort/cancel, nhưng request rất ngắn; rủi ro setState on unmount thấp. Có thể fix khi làm chung batch “request cancellation”. | Web tech debt | `apps/web/src/app/(auth)/verify-email/page.tsx` |
| VE-012 Missing aria-live for async status | Agree | P3 | A11y enhancement; không blocker. | A11y | `apps/web/src/app/(auth)/verify-email/page.tsx` |
| VE-013 Token persists in URL after success | Agree | P2 | Đúng: frontend đọc `token` từ query và không `replaceState` để xóa. Đây là privacy issue (token leak via screenshots/history). | Epic 1.3 secure login/session | `apps/web/src/app/(auth)/verify-email/page.tsx` |
| VE-014 Re-verify already verified shows error | Agree | P2 | Đúng theo code: `AuthService.verifyEmail` fail nếu token used/expired, không trả success idempotent. Không phải blocker nhưng UX kém. | Epic 1.3 | `apps/api/src/main/java/com/healthlens/api/service/AuthService.java#verifyEmail` |
| VE-015 Frontend catch không phân biệt status | Agree | P3 | Đúng: verify-email page hiển thị chung message cho mọi lỗi; có thể phân biệt 429 vs 400 vs 500. | UX polish | `apps/web/src/app/(auth)/verify-email/page.tsx` |
| VE-016 `withCredentials: true` unnecessary for verify-email | Disagree | P3 | `apiClient` bật `withCredentials` globally để gửi refresh cookie, verify-email không cần nhưng không gây lỗi/overhead đáng kể. Có thể giữ cho consistency. | N/A | `apps/web/src/lib/api/apiClient.ts` |

### 10. Consent / GDPR

| ID (v2) | Disposition | Priority | Ghi chú | Story mapping | Code refs |
|---|---|---:|---|---|---|
| 10.1 Dedicated `/consent` page | Agree | P3 | Hiện consent implement dạng modal trong `app/layout.tsx`, không có page route riêng. Nếu UX spec yêu cầu trang consent dedicated thì đây là missing scope. | Epic 1.5 consent | `apps/web/src/app/layout.tsx`, `apps/web/src/components/features/consent/ConsentModal.tsx` |
| 10.2 Consent PDF download | Agree | P3 | Không thấy chức năng tải PDF consent/policy. | Epic 1.5 consent | Web consent modal |
| 10.3 OCR controller consent check silent skip | Agree | P1 | Đúng theo code: `ConsentAspect` **return** nếu unauthenticated/anonymous. Nếu endpoint nào `permitAll()` mà vẫn gắn `@RequiresConsent` thì consent sẽ bị “skip”. Hiện `/api/ocr/*` không permitAll nhưng invitation/cancel endpoints permitAll nên cần tránh dán `@RequiresConsent` nhầm; hoặc sửa aspect fail-closed cho unauthenticated khi annotation xuất hiện. | Epic 1.5 consent | `apps/api/src/main/java/com/healthlens/api/aspect/ConsentAspect.java` |
| 10.4 X-Forwarded-For spoofing | Agree | P1 | Đúng: consent controller lấy IP đầu tiên từ `X-Forwarded-For` không có trusted proxy allowlist => attacker spoof IP trong audit. | Epic 1.5 consent | `apps/api/src/main/java/com/healthlens/api/controller/ConsentController.java` |
| 10.5 Toast ID dùng `Date.now()` | Agree | P3 | Đúng: `ConsentModal.addToast()` dùng `Date.now()` làm id; collision trong cùng ms là low probability nhưng có thể. | Web tech debt | `apps/web/src/components/features/consent/ConsentModal.tsx` |
| 10.6 `setTimeout` trong `handleReject` không cleanup | Agree | P2 | Đúng: `handleReject` setTimeout redirect 1s trong `finally` không cleanup; `addToast` cũng setTimeout remove toast không cleanup. | Web tech debt | `apps/web/src/components/features/consent/ConsentModal.tsx` |

### 6. Auth (Login/Register/Forgot/Reset)

| ID (v2) | Disposition | Priority | Ghi chú | Code refs |
|---|---|---:|---|---|
| 6.1 "Remember me" checkbox | Agree | P3 | Chưa có UI/behavior “remember me”. (Hiện auth dựa refresh cookie, nên “remember me” thực chất là TTL policy decision). | Web auth UX |
| 6.2 Change password page/backend missing | Agree | P1 | Đúng theo story list: hiện có forgot/reset, chưa thấy change-password endpoint/UX. | Web settings/auth | (tìm endpoint sau) |
| 6.3 Thiếu rate limiting trên register | Agree | P0 | Đúng: `AuthService.register()` không rate limit; endpoint register có thể bị flood. | `apps/api/src/main/java/com/healthlens/api/service/AuthService.java#register` |
| 6.4 Thiếu rate limiting trên verify-email | Agree | P0 | Đúng: `AuthController.verifyEmail` không limiter; token brute-force/abuse. | `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java` |
| 6.5 Register không real-time validation | Disagree | P3 | Register form dùng `react-hook-form` + `zodResolver` với `mode: "onChange"` nên có validation realtime. | `apps/web/src/app/(auth)/register/page.tsx` |
| 6.6 `window` access SSR crash (register) | Disagree | P2 | Register page là client component và có guard `typeof window !== "undefined"`, nên không crash SSR build theo mô tả. | `apps/web/src/app/(auth)/register/page.tsx` |
| 6.7 Register: Empty catch block | Partly | P1 | Catch không trống nhưng nuốt chi tiết lỗi (không phân biệt 409/429/5xx) và không dùng `apiClient`. | `apps/web/src/app/(auth)/register/page.tsx` |
| 6.8 inviteToken display nhưng không gửi API | Agree | P1 | Đúng: đọc query `inviteToken` nhưng request body không gửi; backend cũng chưa thấy handling. Nếu flow invite-register là requirement, đây là gap. | `apps/web/src/app/(auth)/register/page.tsx` |
| 6.9 Register: Không handle 429 rate limit | Agree | P1 | Đúng: `catch { setSubmitError(\"Đăng ký thất bại...\") }` không branch 429/retryAfterSeconds như login/forgot-password. | `apps/web/src/app/(auth)/register/page.tsx` |
| 6.10 Register: Dùng raw `axios.post` không qua `apiClient` | Agree | P2 | Đúng: bypass interceptor/consistent error handling (ví dụ correlation id, base URL, timeout). | `apps/web/src/app/(auth)/register/page.tsx` |
| 6.11 Login: Pending deletion detection fragile | Agree | P2 | Đúng: login page detect pending deletion bằng text matching (`detail/title/type`) + normalize tiếng Việt. Nên dùng `errorCode`/`type` ổn định từ backend. | `apps/web/src/app/(auth)/login/page.tsx` |
| 6.12 Reset password: `setTimeout` không cleanup | Agree | P2 | Đúng: sau success gọi `setTimeout(() => router.push(...), 3000)` không clear nếu unmount. | `apps/web/src/app/(auth)/reset-password/page.tsx` |
| 6.13 Auth pages thiếu error boundary | Agree | P3 | Nếu component throw (storage quota, unexpected shape), không có error boundary -> blank/crash. Đây là resilience UX. | Web auth pages |
| 6.14 Login `<Suspense>` fallback blank div | Agree | P3 | Fallback hiện chỉ là nền trống; không phải loading skeleton meaningful. | `apps/web/src/app/(auth)/login/page.tsx` |

### 3-5. Profiles & History (Web)

| ID (v2) | Disposition | Priority | Ghi chú | Code refs |
|---|---|---:|---|---|
| 3.1 Upload button trên hub | Agree | P2 | Hub hiện chỉ cho click vào history và có nút Share (secondaryAction). Nếu muốn “mỗi profile card có upload nhanh” thì cần thêm CTA upload. Không phải blocker nhưng tăng conversion. | `apps/web/src/app/(dashboard)/health-records/page.tsx`, `apps/web/src/components/features/profiles/ProfileCard.tsx` |
| 3.2 Pending invitations badge | Agree | P3 | Hiện hub không hiển thị số lời mời pending (incoming invitations) trên card. Nếu sharing là core, nên thêm badge hoặc tab riêng. | `apps/web/src/app/(dashboard)/health-records/page.tsx` |
| 3.3 Shared profile `lastUpdated` mapping mismatch | Disagree | P3 | Backend `SharedProfileResponse.lastUpdated` là `Instant`; frontend truyền vào `ProfileCard` và format bằng `new Date(lastUpdated)` là hợp lệ. Không thấy mismatch rõ ràng (trừ khi semantics muốn “last record time” thay vì “last updated”). | Web: `apps/web/src/app/(dashboard)/health-records/page.tsx`, API: `apps/api/src/main/java/com/healthlens/api/dto/response/SharedProfileResponse.java`, `ProfileService.getSharedProfiles` |
| 3.4 Empty state không illustration | Disagree | P3 | Empty state hiện có icon `Users` và layout khá đầy đủ; không phải “text đơn thuần”. Có thể polish thêm nhưng không phải gap. | `apps/web/src/app/(dashboard)/health-records/page.tsx` |
| 3.7 Optimistic revoke không rollback (hub) | Disagree | P2 | Revoke share mutation xóa cache trong `onSuccess` sau khi API thành công; không thấy optimistic update trước API call nên không cần rollback. | `apps/web/src/app/(dashboard)/health-records/page.tsx` |
| 3.5 Profiles fetch `refetchInterval:30000` | Agree | P2 | Đúng: có polling 30s. Nên dừng khi tab hidden hoặc tăng `staleTime`/refetch on focus. | `apps/web/src/app/(dashboard)/health-records/page.tsx` |
| 3.6 ENSURE_DEFAULT fire-and-forget không cleanup | Agree | P2 | Đúng: dùng `void apiClient.post(...)` trong effect mà không cancellation/cleanup. Rủi ro nhỏ nhưng có thể gây setState after unmount ở flow liên quan. | `apps/web/src/app/(dashboard)/health-records/page.tsx` |
| 3.8 `alert()` cho success/error | Agree | P2 | Đúng: hub dùng `alert()` cho invite/update/revoke. Nên thay toast. | `apps/web/src/app/(dashboard)/health-records/page.tsx` |
| 3.9 `mapSharedStatusToCardStatus` silent drop | Agree | P1 | Đúng: backend shared latestStatus có thể là `unverified` hoặc `error`, nhưng mapping chỉ nhận `normal/attention|warning/abnormal` -> UI rơi về “Chưa có cập nhật” (sai ngữ nghĩa). Nên map thêm `unverified/error` hoặc show trạng thái riêng. | `apps/web/src/app/(dashboard)/health-records/page.tsx#mapSharedStatusToCardStatus`, API `ProfileService.resolveLatestSharedStatus` |
| 3.10 `extractApiDetail` duplicate | Agree | P2 | Đúng: util copy-paste giống Home/Profiles. Nên shared util. | `apps/web/src/app/(dashboard)/health-records/page.tsx` |
| 4.1 Filter "Chưa xác thực" không hoạt động | Disagree | P3 | Hiện filter status dùng `resolveHistoryStatus(item)` và option có value `unverified`; mapping trả `unverified` cho `processing/review_required/pending` nên filter “Chưa xác thực” hoạt động theo code. | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx#resolveHistoryStatus` |
| 4.2 Không thể xoá profile (API) | Agree | P1 | Đúng: backend chưa có `DELETE /profiles/{profileId}` nên UX “xoá profile” nếu có sẽ không hoạt động. (Finding thuộc Section 5/Profiles hơn là History). | `apps/api/src/main/java/com/healthlens/api/controller/ProfileController.java` |
| 4.3 Preview nhanh khi click record | Agree | P3 | Chưa có hover/side panel preview; hiện click đi thẳng sang review page. Đây là UX enhancement. | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 4.4 Sort records (by date/status) | Partly | P3 | Backend đã sort theo `examDate desc, createdAt desc` trong `getProfileHistory`, nên “sort by date” cơ bản có. Nhưng UI chưa có user-controlled sorting/filter nâng cao theo status. | `apps/api/src/main/java/com/healthlens/api/service/HealthRecordService.java#getProfileHistory`, web history page |
| 4.5 Delete success dùng `alert()` | Disagree | P3 | Flow delete hiện không dùng `alert()`; đóng modal và update list. (Vẫn có thể thêm toast). | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 4.6 Infinite scroll "Tải thêm" button | Partly | P3 | Page đã có auto-load bằng `IntersectionObserver` nhưng vẫn giữ nút “Tải thêm” fallback. Đây không phải bug; có thể chọn 1 pattern thống nhất. | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 4.7 Optimistic delete không rollback (history) | Disagree | P3 | Theo code hiện tại, cache chỉ bị mutate trong `onSuccess` (sau khi API OK), không phải optimistic update trước call, nên không có vấn đề “rollback khi fail”. | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 4.8 `resolveHistoryStatus` unsafe type casting | Agree | P1 | Đúng: dùng `as unknown as` để đọc field không có trong type. | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 4.9 Infinite query không error state | Agree | P1 | Đúng: page handle `isLoading` nhưng không render error state nếu `historyQuery.isError` hoặc fetch page 2+ fail; user có thể thấy UI trống/đứng. | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 4.10 `deleteError` không clear khi mở modal item khác | Partly | P2 | `deleteError` clear khi cancel/success, nhưng nếu error xảy ra rồi user mở modal cho item khác mà không cancel trước, error vẫn hiển thị. Nên clear khi set `deleteTarget`. | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 4.11 `pendingAccessUpdates` state never read | Disagree | P3 | History page hiện không có state `pendingAccessUpdates` như mô tả. | `apps/web/src/app/(dashboard)/profiles/[profileId]/history/page.tsx` |
| 5.3 Xoá profile không có API | Agree | P1 | Đúng: `ProfileController` không có `DELETE /profiles/{profileId}`. | `apps/api/src/main/java/com/healthlens/api/controller/ProfileController.java` |
| 5.4 Set as default không có API | Agree | P1 | Đúng: không có endpoint set default profile; chỉ có `ensure-default`. | `ProfileController.java`, `ProfileService.java` |
| 5.1 Upload avatar/photo cho profile | Agree | P3 | UI `ProfileCard` có prop `avatarUrl` nhưng không có flow upload/avatar management; backend cũng không có endpoint storage/avatar. Nếu muốn production “profile photo”, cần thêm scope. | Web: `apps/web/src/components/features/profiles/ProfileCard.tsx`, API: (new endpoints) |
| 5.2 Existing conditions / allergies fields | Agree | P3 | Không thấy field này trong profile DTO/entity/UI. Nếu PRD/UX spec yêu cầu, đây là gap scope. | Product decision | Web profiles + API Profile fields |
| 5.5 Profile card không hiển thị age | Agree | P3 | Đúng: card không tính/hiển thị tuổi; hiện chỉ show name/relationship/notes/status/updated. UX polish. | `apps/web/src/components/features/profiles/ProfileCard.tsx` |
| 5.6 Validation inconsistency (create max=50 vs update max=100; notes max=1000 vs 500) | Agree | P2 | Đúng: backend `CreateProfileRequest.displayName max 50` nhưng `UpdateProfileRequest max 100`; notes max 1000 vs 500. Cần thống nhất để tránh 400 bất ngờ khi update hoặc create. | API consistency | `apps/api/src/main/java/com/healthlens/api/dto/request/CreateProfileRequest.java`, `UpdateProfileRequest.java` |
| 5.7 Form reset trước khi API complete | Agree | P1 | Đúng: `CreateProfileModal` gọi `reset()` ngay sau `onSubmit(formattedData)` (không await), nên nếu API fail thì user mất input. Nên chỉ reset khi `onSuccess`. | UX correctness | `apps/web/src/components/features/profiles/CreateProfileModal.tsx` |
| 5.8 `ProfileCard` missing `React.memo` | Agree | P3 | Có thể tối ưu nhưng không bắt buộc. Nếu list lớn, memo hoá giúp giảm rerender. | Perf polish | `apps/web/src/components/features/profiles/ProfileCard.tsx` |

### 8. Settings

| ID (v2) | Disposition | Priority | Ghi chú |
|---|---|---:|---|
| 8.1 Change password page | Agree | P1 | Nếu production user account cần self-service, đây là thiếu scope. |
| 8.2 User 2FA setup | Partly | P2 | PRD không bắt buộc cho launch; tốt cho hardening. |
| 8.3 Notification preferences | Agree | P3 | Không thấy settings page/feature cho notification prefs. Nếu PRD/UX spec yêu cầu thì đây là missing scope. |
| 8.4 Privacy settings tab | Agree | P3 | Không có privacy settings page/tab riêng (ngoài consent modal/flow). |
| 8.5 About / Support pages | Partly | P3 | Có block “Support” trong settings/profile nhưng không có trang About/Support đầy đủ. Nếu production cần helpdesk/FAQ thì bổ sung. |
| 8.6 `Link` import unused (`settings/profile`) | Disagree | P3 | `Link` đang được dùng để link sang `/settings/delete-account`. | 
| 8.7 `setTimeout` not cleaned up (`settings/profile`) | Agree | P2 | Đúng: `setTimeout(() => setSuccessMessage(null), 3000)` không cleanup nếu unmount. Low risk nhưng đúng pattern. |
| 8.8 Hardcoded avatar URL | Agree | P2 | Đúng: avatar dùng `ui-avatars.com` với `name=H+L` hardcoded. Production cần derive từ user name hoặc implement avatar upload; đồng thời cân nhắc privacy khi gọi external avatar service. |
| 8.9 Direct DOM manipulation (`delete-account`) | Partly | P3 | Có `document.body.style.overflow = \"hidden\"` khi modal overlay hiển thị và có restore cleanup. Đây không phải bug correctness, nhưng có thể thay bằng CSS `overflow-hidden` trên root layout để tránh side effects. |

### 9. Family Sharing

| ID (v2) | Disposition | Priority | Ghi chú |
|---|---|---:|---|
| 9.3 Self-invite không bị chặn | Agree | P1 | Đúng: `inviteByEmail` không chặn email = inviter email. Nên block để tránh spam/confusion. | 
| 9.4 Revoke share dùng sai ID fallback | Agree | P1 | Đúng: `revokeShare` fallback `findById(viewerId)` là sai semantics (viewerId != shareId). | 


## Open Decisions (cần chốt để ra production scope)

1. “Đặt ở VN” được hiểu là: compute chính ở VN, nhưng OCR/LLM provider ngoài có thể cross-border nếu có consent + retention control (bạn đã xác nhận).
2. Retention của original file: giữ theo vòng đời record; soft-delete purge sau **30 ngày** (đã chốt).
3. Admin analytics (Epic 8): **bắt buộc đủ 8.1/8.2/8.3** cho production (đã chốt).
