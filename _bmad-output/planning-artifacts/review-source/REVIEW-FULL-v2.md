# HealthLens — Full Review Report v2

> **Generated**: 2026-05-13
> **Methodology**: BMad Adversarial Review + Edge Case Hunter + Code Review + Data Flow Audit + Transaction/Race Analysis + TypeScript/Dead Code Audit  
> **Total findings**: ~154 (P0: 24, P1: 43, P2: 53, P3: 34)

---

## Mục lục

1. [Màn hình Dashboard Home](#1-dashboard-home-home)
2. [Màn hình Xem kết quả chi tiết](#2-xem-k%E1%BA%BFt-qu%E1%BA%A3-chi-ti%E1%BA%BFt-health-recordsreviewrecordid)
3. [Màn hình Health Records Hub](#3-health-records-hub-health-records)
4. [Màn hình Profile History](#4-profile-history-profilesprofileidhistory)
5. [Màn hình Profile Management](#5-profile-management-profiles)
6. [Màn hình Auth (Login, Register, Password)](#6-auth-login-register-forgotreset-password)
7. [Màn hình Admin](#7-admin)
8. [Màn hình Settings](#8-settings)
9. [Family Sharing](#9-family-sharing)
10. [Consent / GDPR](#10-consent--gdpr)
11. [OCR Pipeline](#11-ocr-pipeline)
12. [LLM / AI / RAG](#12-llm--ai--rag)
13. [Backend: Transaction & Race Condition](#13-backend-transaction--race-condition)
14. [Backend: Security & Data Integrity](#14-backend-security--data-integrity)
15. [Frontend: React Anti-patterns](#15-frontend-react-anti-patterns)
16. [Frontend: TypeScript & Dead Code](#16-frontend-typescript--dead-code)
17. [Frontend: Data Flow & Error Handling](#17-frontend-data-flow--error-handling)
18. [Mobile App](#18-mobile-app)
19. [CI / CD & Docker](#19-ci--cd--docker)
20. [Cross-cutting](#20-cross-cutting)
21. [Ma trận ưu tiên](#21-ma-tr%E1%BA%ADn-%C6%B0u-ti%C3%AAn)
22. [Màn hình Xác thực Email](#22-x%C3%A1c-th%E1%BB%B1c-email-verify-email)
23. [Màn hình Chấp nhận lời mời](#23-ch%E1%BA%A5p-nh%E1%BA%ADn-l%E1%BB%9Di-m%E1%BB%9Di-invitationsaccept)
24. [Màn hình Hủy yêu cầu xóa tài khoản](#24-h%E1%BB%A7y-y%C3%AAu-c%E1%BA%A7u-x%C3%B3a-t%C3%A0i-kho%E1%BA%A3n-cancel-deletion)

---

## 1. Dashboard Home (`/home`)

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 1.1 | **Sửa** | StatCard hardcoded | 4 chỉ số (120/80, 95, 200, 24.5) là fake data, không gọi API real | P0 |
| 1.2 | **Sửa** | `recordStatusLabel` fallback "Chưa xác thực" | Không có status mapping trigger label này vì type system hạn chế | P1 |
| 1.3 | **Thêm** | Family shared profiles display | Home không hiển thị health status của profiles được share từ family | P1 |
| 1.4 | **Thêm** | Warning banner abnormal metrics | Khi có chỉ số bất thường cần banner cảnh báo nổi bật | P1 |
| 1.5 | **Cải tiến** | "Chăm sóc sức khỏe chủ động" text generic | Nên dùng AI insight dựa trên metrics gần nhất thay vì text cứng | P2 |
| 1.6 | **Cải tiến** | Action tiles disabled | "Đặt lịch khám", "Liên hệ bác sĩ", "Trợ giúp" disabled → implement hoặc ẩn | P2 |
| 1.7 | **Cải tiến** | Upload CTA trực tiếp trên home | Không có nút upload nhanh, phải vào health-records | P2 |
| 1.8 | **Tối ưu** | Recent records fetch `refetchInterval:30000` | Polling 30s ngay cả khi tab không visible | P2 |
| 1.9 | **Sửa** | `ENSURE_DEFAULT` fire-and-forget không cleanup | Race condition khi unmount; dùng AbortController | P1 |
| 1.10 | **Sửa** | `[, setPendingAccessUpdates]` unused state | Destructured value không dùng, state setter chỉ dùng trong callback | P2 |
| 1.11 | **Sửa** | Optimistic revoke không rollback | `setQueryData` xoá member khỏi UI nhưng API fail → data không đồng bộ | P0 |
| 1.12 | **Tối ưu** | Profiles query không cache `staleTime` | Mặc định 0 → refetch mỗi lần mount | P2 |
| 1.13 | **Cải tiến** | `alert()` cho success/error mutation | Native browser dialog thay vì toast notification | P1 |
| 1.14 | **Tối ưu** | `extractApiDetail` duplicate | Hàm này copy-paste ở home, health-records, profiles → shared utility | P2 |

---

## 2. Xem kết quả chi tiết (`/health-records/review/[recordId]`)

### 2a. Done View

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 2.1 | **Sửa** | Nút "Chia sẻ" disabled | Xoá `<button disabled>Chia sẻ</button>` | P0 |
| 2.2 | **Sửa** | Nút "Tải PDF" disabled | Xoá hoặc implement export PDF | P0 |
| 2.3 | **Sửa** | AI Recommendations sai thứ tự | Section đang dưới metrics grid, cần đưa lên trên | P0 |
| 2.4 | **Sửa** | "Giải thích chi tiết từng chỉ số" redundant | Section bottom dùng HealthMetricCard trùng với metric cards grid | P0 |
| 2.5 | **Sửa** | Thiếu preview document trong done view | Sau khi lưu, mất hẳn panel hồ sơ gốc bên trái | P0 |
| 2.6 | **Cải thiện** | `compactMetricPercent` fallback 60% | Khi không có reference range, bar hiện 60% là sai | P1 |
| 2.7 | **Cải thiện** | Card metric grid thiếu explanation | Cần thêm expand/collapse explanation vào mỗi card | P0 |
| 2.8 | **Tối ưu** | AI recommendations fetch không staleTime hợp lý | `staleTime: 5 min` nhưng fetch mỗi mount | P2 |

### 2b. Review/Edit View

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 2.9 | **Sửa** | Mất preview khi processing | Status "processing" chỉ hiện loading, không thấy document gốc | P0 |
| 2.10 | **Sửa** | Mất preview khi OCR failed | `OcrFailureScreen` không có document viewer | P0 |
| 2.11 | **Sửa** | `showFullDoc` modal raw JSX | Không phải component riêng, không tái sử dụng | P1 |
| 2.12 | **Sửa** | `showConfirmModal` raw JSX | Confirm modal code inline trong page | P1 |
| 2.13 | **Thêm** | Add metric dialog là modal riêng | Không có auto-complete từ reference metrics | P1 |
| 2.14 | **Cải thiện** | Inline edit table thiếu validation UX | Lỗi hiện ở `saveError` chung, không highlight field | P2 |
| 2.15 | **Cải thiện** | Array index làm React key | `metrics.map((m, idx) => <tr key={idx}>)` → sai DOM reconciliation | P1 |
| 2.16 | **Cải thiện** | 1000+ line component | Cần tách thành sub-components | P2 |
| 2.17 | **Sửa** | Empty catch blocks | `handleRetryUpload` và `handleDeleteRecord` dùng `catch` không tham số → mất server error | P0 |
| 2.18 | **Sửa** | Polling không cleanup khi unmount | `refetchInterval: 3000` tiếp tục chạy sau khi navigate away | P1 |
| 2.19 | **Sửa** | `fileUrl` empty → broken iframe/img | `showFullDoc` modal render khi `fileUrl = ""` | P1 |
| 2.20 | **Sửa** | `analyzerModel`, `testMethod`, `labSite` không render | API fields defined trong type nhưng không dùng | P2 |

### 2c. OCR Failed Screen

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 2.21 | **Sửa** | Thiếu preview document | User không thấy ảnh gốc để quyết định "Giữ một phần" | P0 |
| 2.22 | **Cải tiến** | Retry upload mất context | Sau chọn file mới, redirect sang record mới, mất vị trí hiện tại | P2 |

---

## 3. Health Records Hub (`/health-records`)

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 3.1 | **Thêm** | Upload button trên hub | Profile card nào cũng nên có nhanh upload | P1 |
| 3.2 | **Thêm** | Pending invitations badge | Hiển thị số lời mời chưa xử lý trên profile card | P2 |
| 3.3 | **Sửa** | Shared profile `lastUpdated` mapping | `SharedProfile.lastUpdated` không khớp `ProfileCard.updatedAt` | P1 |
| 3.4 | **Cải tiến** | Empty state không illustration | "Chưa có hồ sơ nào" text đơn thuần | P2 |
| 3.5 | **Tối ưu** | Profiles fetch `refetchInterval:30000` | Polling 30s ngay cả khi tab không visible | P2 |
| 3.6 | **Sửa** | `ENSURE_DEFAULT` fire-and-forget không cleanup | Race condition khi unmount | P1 |
| 3.7 | **Sửa** | Optimistic revoke không rollback | Xoá member khỏi UI nhưng API fail | P0 |
| 3.8 | **Cải tiến** | `alert()` cho success/error | Native browser dialog | P1 |
| 3.9 | **Sửa** | `mapSharedStatusToCardStatus` silent drop | Status không mapping → mất status badge | P1 |
| 3.10 | **Tối ưu** | `extractApiDetail` duplicate (lần 3) | Copy-paste từ home | P2 |

---

## 4. Profile History (`/profiles/[profileId]/history`)

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 4.1 | **Sửa** | Filter "Chưa xác thực" không hoạt động | `resolveHistoryStatus` mapping sai | P1 |
| 4.2 | **Sửa** | Không thể xoá profile | ProfileController không có `@DeleteMapping` | P1 |
| 4.3 | **Thêm** | Preview nhanh khi click record | Hover preview hoặc side panel detail | P2 |
| 4.4 | **Thêm** | Sort records (by date, by status) | Hiện tại chỉ show mới nhất lên đầu | P2 |
| 4.5 | **Cải tiến** | Delete success dùng `alert()` | Cần toast notification | P1 |
| 4.6 | **Cải tiến** | Infinite scroll "Tải thêm" button | UX kém hơn auto-load khi scroll | P2 |
| 4.7 | **Sửa** | Optimistic delete không rollback | Xoá record khỏi UI nhưng API fail | P0 |
| 4.8 | **Sửa** | `resolveHistoryStatus` unsafe type casting | `as unknown as` bypass TypeScript safety | P1 |
| 4.9 | **Sửa** | Infinite query không error state | Nếu page 2+ load fail, user không thấy gì | P1 |
| 4.10 | **Sửa** | `deleteError` không clear khi mở modal với item khác | Giữ nguyên error message cũ | P2 |
| 4.11 | **Tối ưu** | `pendingAccessUpdates` state never read | Dead code | P2 |

---

## 5. Profile Management (`/profiles`)

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 5.1 | **Thêm** | Upload avatar/photo cho profile | Backend không có endpoint; `avatarUrl` prop không dùng được | P2 |
| 5.2 | **Thêm** | Existing conditions / allergies fields | UX spec có field này nhưng chưa implement | P2 |
| 5.3 | **Sửa** | Xoá profile không có API | `ApiPaths.PROFILES.DELETE` tồn tại nhưng backend chưa có endpoint | P1 |
| 5.4 | **Sửa** | "Set as default" không hoạt động | `ApiPaths.PROFILES.SET_DEFAULT` tồn tại nhưng backend chưa implement | P1 |
| 5.5 | **Cải tiến** | Profile card không hiển thị age | UX spec hiển thị age | P2 |
| 5.6 | **Sửa** | Validation inconsistency `name:max=50` vs `max=100` | Create cho phép 50, Update cho phép 100 | P2 |
| 5.7 | **Sửa** | Form reset trước khi API complete | `reset()` gọi synchronous; API fail → form đã trống | P1 |
| 5.8 | **Sửa** | `ProfileCard` missing `React.memo` | Re-render không cần thiết trong lists | P2 |

---

## 6. Auth (Login, Register, Forgot/Reset Password)

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 6.1 | **Thêm** | "Remember me" checkbox | UX spec có, chưa implement | P2 |
| 6.2 | **Thêm** | Change password page | API route `/api/v1/auth/change-password` defined nhưng backend chưa có controller | P1 |
| 6.3 | **Sửa** | Thiếu rate limiting trên register | Login có, register không → account creation flood | P0 |
| 6.4 | **Sửa** | Thiếu rate limiting trên verify-email | Cho phép brute-force verification token | P0 |
| 6.5 | **Cải tiến** | Register không real-time validation | Validation chỉ xảy ra khi submit | P2 |
| 6.6 | **Sửa** | `window` object access during SSR (register) | `new URLSearchParams(window.location.search)` crash SSR build | P0 |
| 6.7 | **Sửa** | Register: Empty catch block | Server error completely ignored | P1 |
| 6.8 | **Sửa** | Register: `inviteToken` display nhưng không gửi API | Query param display nhưng không dùng | P1 |
| 6.9 | **Sửa** | Register: Không handle 429 rate limit | Login handle, register không | P1 |
| 6.10 | **Sửa** | Register: Dùng raw `axios.post` không qua `apiClient` | Bypass interceptor, không đồng bộ | P2 |
| 6.11 | **Sửa** | Login: Pending deletion detection fragile | Text matching Vietnamese normalized string | P1 |
| 6.12 | **Sửa** | Reset password: `setTimeout` không cleanup | Redirect sau 3s nhưng không clear nếu unmount | P1 |
| 6.13 | **Cải tiến** | Auth pages thiếu error boundary | Login, Register silent crash | P2 |
| 6.14 | **Tối ưu** | Login: `<Suspense>` fallback blank div | Không meaningful loading state | P2 |

---

## 7. Admin

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 7.1 | **Thêm** | Admin Dashboard metrics | Đang stub "S? implement ? Story 8.x" | P1 |
| 7.2 | **Thêm** | Audit log display | Stub "S? implement ? Story 7.5" | P1 |
| 7.3 | **Thêm** | CSV import / PDF export | Cho reference data management | P2 |
| 7.4 | **Thêm** | Approval history timeline | Cho change set approvals | P2 |
| 7.5 | **Sửa** | Admin token trong sessionStorage | XSS vulnerability → nên dùng HttpOnly cookie | P1 |
| 7.6 | **Sửa** | sessionStorage không try/catch | 7 locations crash trong private browsing | P0 |
| 7.7 | **Sửa** | `Number(range.minValue)` không validate | User nhập text → NaN gửi lên API | P1 |
| 7.8 | **Sửa** | Admin `SUBMIT_CHANGE_SET`, `PUBLISH_CHANGE_SET` API defined nhưng frontend không gọi | Dead endpoints | P2 |
| 7.9 | **Sửa** | `new Date()` không validate trong `formatDate` | Malformed date → "Invalid Date" | P2 |

---

## 8. Settings

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 8.1 | **Thêm** | Change password page | Chỉ có forgot-password flow, không có change password khi đã login | P1 |
| 8.2 | **Thêm** | 2FA setup cho user account | Admin đã có TOTP, user chưa có | P2 |
| 8.3 | **Thêm** | Notification preferences | UX spec có, chưa implement | P2 |
| 8.4 | **Thêm** | Privacy settings tab | UX spec có | P2 |
| 8.5 | **Thêm** | About / Support pages | UX spec có | P3 |
| 8.6 | **Sửa** | `Link` import unused (`settings/profile`) | `import Link from "next/link"` không dùng | P2 |
| 8.7 | **Sửa** | `setTimeout` not cleaned up (`settings/profile`) | `setSuccessMessage(null)` sau 3s không cleanup | P1 |
| 8.8 | **Sửa** | Hardcoded avatar URL | `name=H+L` hardcoded thay vì user's real name | P2 |
| 8.9 | **Sửa** | Direct DOM manipulation (`delete-account`) | `document.body.style.overflow = "hidden"` | P2 |

---

## 9. Family Sharing

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 9.1 | **Thêm** | Family Dashboard (`/family-dashboard`) | UX spec có, chưa implement | P1 |
| 9.2 | **Thêm** | Family Sharing Management page | UX spec có dedicated page | P2 |
| 9.3 | **Sửa** | Self-invite không bị chặn | User có thể invite chính mình | P1 |
| 9.4 | **Sửa** | Revoke share dùng sai ID fallback | `viewerId` dùng làm `share.id` → logic sai | P1 |
| 9.5 | **Sửa** | Existing user không notify khi thay đổi access level | Email chỉ gửi cho user chưa có tài khoản | P2 |
| 9.6 | **Sửa** | Không có API direct change access level | Phải re-invite để thay đổi | P1 |
| 9.7 | **Sửa** | `InviteMemberModal` no-op union type | `"view" \| "edit" \| string` = `string` | P1 |
| 9.8 | **Sửa** | `cancelInvitation()` hard-delete thay vì soft-delete | Inconsistent với phần còn lại | P2 |

---

## 10. Consent / GDPR

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 10.1 | **Thêm** | Dedicated `/consent` page | UX spec có full page 5 sections, hiện tại là modal | P2 |
| 10.2 | **Thêm** | Consent PDF download | UX spec có "Tải xuống bản PDF" | P3 |
| 10.3 | **Sửa** | OCR controller consent check silent skip | `/api/ocr/` có `@RequiresConsent` nhưng không JWT → skip silently | P1 |
| 10.4 | **Sửa** | X-Forwarded-For spoofing | `extractClientIpAddress()` không filter trusted proxy | P2 |
| 10.5 | **Sửa** | Toast ID dùng `Date.now()` | 2 toast cùng ms → duplicate ID | P2 |
| 10.6 | **Sửa** | `setTimeout` trong `handleReject` không cleanup | ConsentModal redirect 1s sau không cleanup | P1 |

---

## 11. OCR Pipeline

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 11.1 | **Thêm** | AWS Textract fallback là stub | `AwsTextractClient` throws "not yet implemented" | P1 |
| 11.2 | **Sửa** | Python OCR service: sync HTTP trong async handler | `requests.get()` blocking event loop | P1 |
| 11.3 | **Sửa** | No retry cho EasyOCR call | Nếu service down, chuyển provider ngay | P1 |
| 11.4 | **Sửa** | No auth / rate limiting trên OCR endpoint | Endpoint `/ocr` public | P1 |
| 11.5 | **Cải tiến** | Language detection heuristic | Chỉ dựa vào dấu câu tiếng Việt, không chính xác với document hỗn hợp | P2 |
| 11.6 | **Cải tiến** | LLM prompt là Java string literal | Khó maintain, test, version | P1 |
| 11.7 | **Tối ưu** | Regex fallback parser fragile | Pattern giả định format line cố định | P2 |
| 11.8 | **Cải tiến** | LLM JSON truncation repair heuristic fragile | Chỉ check `[` không `]` → dễ tạo JSON invalid | P1 |

---

## 12. LLM / AI / RAG

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 12.1 | **Thêm** | Populate Qdrant với medical data | Vector store đã config nhưng chưa có data ingestion | P0 |
| 12.2 | **Thêm** | RAG knowledge base documents | Cần tài liệu medical reference | P0 |
| 12.3 | **Sửa** | LLM prompts duplicated | `FALLBACK_EXPLANATIONS`, `FALLBACK_EXPLANATIONS_BY_NORMALIZED_METRIC`, `METRIC_CONTEXTS` overlap | P2 |
| 12.4 | **Cải tiến** | `metricSpecificLifestyleAdvice` switch statement | Hardcode từng metric, cần external config (YAML) | P2 |
| 12.5 | **Cải tiến** | `isLowQualityRecommendations` heuristic | Keyword matching dễ false positive | P2 |
| 12.6 | **Cải tiến** | Prompt versioning manual | Cache invalidation phải bump version thủ công | P2 |
| 12.7 | **Tối ưu** | Redis failure silent swallow | Cache outage không metric/alert | P2 |
| 12.8 | **Cải tiến** | `HealthMetricCard` query key gồm `value` + `status` | Mỗi edit → cache invalidate → fetch lại explanation | P1 |

---

## 13. Backend: Transaction & Race Condition

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 13.1 | **Sửa** | Audit writes trong read-only transaction (`HealthRecordService`) | `getDetail()` + `getRecommendations()` `@Transactional(readOnly=true)` gọi `persistAuditLog()` (INSERT) | P0 |
| 13.2 | **Sửa** | `purgeSoftDeletedRecords()` không `@Transactional` | S3 + DB không atomic → mất file hoặc zombie records | P0 |
| 13.3 | **Sửa** | `createProfile()` race condition | 2 request concurrent → >10 profiles/user | P0 |
| 13.4 | **Sửa** | Refresh token rotation race | 2 request concurrent dùng 1 refresh token → không phát hiện stolen token | P0 |
| 13.5 | **Sửa** | Stream ack không đồng bộ DB (`OcrJobConsumer`) | DB commit xong ack fail → OCR chạy lại; ack xong DB fail → mất result | P0 |
| 13.6 | **Thêm** | Không có Dead Letter Queue cho OCR | Message lỗi bị ack và xoá, không retry | P0 |
| 13.7 | **Sửa** | Deletion request/cancel race (`DataDeletionService`) | Scheduler + cancel concurrent → data vẫn bị xoá | P0 |
| 13.8 | **Sửa** | Shared editor modify User entity (`ProfileService`) | Share (edit) → đổi `fullName`, `birthDate` của chủ tài khoản | P0 |
| 13.9 | **Sửa** | Duplicate `ProfileShare` khi accept concurrent | 2 accept cùng lúc → 2 active share rows | P1 |
| 13.10 | **Sửa** | `@Auditable` annotation không có Aspect implementation | `DELETE_HEALTH_RECORD` không bao giờ được audit | P1 |
| 13.11 | **Sửa** | Thiếu audit logs cho share lifecycle | invite, accept, reject, cancel không log | P1 |
| 13.12 | **Sửa** | `enrichMetric()` gọi `persistAuditLog()` mỗi lần `getDetail()` | Audit log unbounded growth | P1 |
| 13.13 | **Sửa** | Xoá user không cleanup `profile_shares`, `invitations`, `audit_logs` | Orphan records gây lỗi runtime | P0 |
| 13.14 | **Sửa** | Email verification lost khi Redis down (`AuthService`) | Silent catch → user register xong không login được | P1 |
| 13.15 | **Sửa** | `forgotPassword` silent swallow error | Luôn return success dù email không gửi được | P1 |
| 13.16 | **Sửa** | S3 deletion best-effort khi xoá user | Silent catch → file vẫn còn trên S3 | P1 |
| 13.17 | **Sửa** | `cancelInvitation()` hard-delete | Inconsistent với soft-delete pattern khác | P2 |
| 13.18 | **Sửa** | Orphaned `ProfileInvitation` khi viewer user deleted | Không cleanup khi user xoá tài khoản | P1 |
| 13.19 | **Sửa** | Gender normalization drop non-standard values | `null` → reference range không chính xác | P2 |
| 13.20 | **Sửa** | Same-class `@Transactional` self-invocation proxy bypass | `markOcrCompleted` 3-param gọi 4-param internal | P2 |
| 13.21 | **Sửa** | `confirmUpload()` retry race overwrites `fileKey` | 2 concurrent retry → orphan S3 object | P2 |

---

## 14. Backend: Security & Data Integrity

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 14.1 | **Sửa** | JWT secret hardcoded trong dev profile | `application.yml` dev profile có secret cố định → forge token | P0 |
| 14.2 | **Sửa** | Spring Boot 4.0.3 (pre-release/M3) | Chưa GA → rủi ro stability, API changes | P0 |
| 14.3 | **Sửa** | Email lộ trong URL cancellation | `?email=user@example.com` trong deletion cancellation link | P1 |
| 14.4 | **Sửa** | MinIO credentials hardcoded | Default `minioadmin/minioadmin` | P1 |
| 14.5 | **Sửa** | Thiếu `@Valid` trên `confirmRecord` endpoint | Request body không validate → metrics null | P1 |
| 14.6 | **Sửa** | Stored XSS potential | `diagnosis`, `hospitalName` không sanitize | P1 |
| 14.7 | **Sửa** | No pagination trên `getRecordsByProfile` | Load ALL records → memory pressure | P1 |
| 14.8 | **Sửa** | 5s cache TTL cho status quá ngắn | DB load cao dưới polling OCR | P1 |
| 14.9 | **Sửa** | Admin TOTP token brute-force trong window 15 phút | TOTP setup/verify endpoints không rate limit | P2 |
| 14.10 | **Sửa** | User-facing 500 khi `IllegalArgumentException` | Spring mặc định map 500 cho RuntimeException | P2 |

---

## 15. Frontend: React Anti-patterns

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 15.1 | **Sửa** | `useEffect` không cleanup (multiple files) | `setTimeout`, `setInterval`, async fetch không cleanup → state update on unmounted component | P1 |
| 15.2 | **Sửa** | `useEffect` missing dependencies (`UploadButton`) | `profileId`, `searchParams` không trong dep array | P1 |
| 15.3 | **Sửa** | `useAuthBootstrap` unstable `user` dependency | Zustand persist rehydration → reference change mỗi lần | P1 |
| 15.4 | **Tối ưu** | `navItems` tạo lại mỗi render | Không `useMemo` | P2 |
| 15.5 | **Tối ưu** | `isNavItemActive` function tạo lại mỗi render | Không `useCallback` | P2 |
| 15.6 | **Tối ưu** | `ProfileCard` missing `React.memo` | Lists re-render không cần thiết | P2 |
| 15.7 | **Tối ưu** | `providers.tsx` `QueryClient` module scope | Cache leakage giữa các tests | P2 |
| 15.8 | **Sửa** | `ConsentModal` toast ID dùng `Date.now()` | 2 toast cùng ms → duplicate ID | P2 |

---

## 16. Frontend: TypeScript & Dead Code

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 16.1 | **Sửa** | `sessionStorage` không try/catch (7 locations) | Crash trong private browsing | P0 |
| 16.2 | **Sửa** | No-op union type `"view" \| "edit" \| string` | 4 locations → mất type safety | P1 |
| 16.3 | **Sửa** | `as unknown as` double assertion (`history/page.tsx`) | `HistoryItem` type thiếu field | P1 |
| 16.4 | **Tối ưu** | `extractApiDetail` duplicate (3 files) | Copy-paste cần shared utility | P2 |
| 16.5 | **Tối ưu** | `mapSharedStatusToCardStatus` duplicate (2 files) | Copy-paste | P2 |
| 16.6 | **Tối ưu** | `pendingAccessUpdates` state never read (2 files) | Dead code | P2 |
| 16.7 | **Tối ưu** | `Link` import unused (`settings/profile`) | Dead import | P2 |
| 16.8 | **Tối ưu** | 4 barrel files export nothing | `stores/index.ts`, `hooks/index.ts`, `components/*/index.ts` | P3 |
| 16.9 | **Tối ưu** | `API_TIMEOUT` export nhưng không dùng | Dead re-export | P3 |
| 16.10 | **Tối ưu** | `console.error` redundant với user-facing feedback | `profile/page.tsx:79`, `delete-account/page.tsx:87` | P3 |
| 16.11 | **Sửa** | Potential `name[-1]` khi email starts with `@` | `CancelDeletionClient.tsx:121` | P2 |

---

## 17. Frontend: Data Flow & Error Handling

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 17.1 | **Sửa** | API Client: Refresh token failure log out unnecessarily | Refresh endpoint 500 → user logout; nên retry | P1 |
| 17.2 | **Sửa** | API Client: 403 redirect dùng `/dashboard` hardcoded | Route thực tế là `/home` | P1 |
| 17.3 | **Sửa** | API Client: Không AbortController integration | Request không cancel khi route change | P2 |
| 17.4 | **Sửa** | API Client: Không retry transient network failure | Chỉ 401 retry, 5xx pass-through | P2 |
| 17.5 | **Sửa** | Admin API Client: Empty catch trên 401 redirect | Không return → double error handling | P1 |
| 17.6 | **Sửa** | Admin API Client: Không refresh token mechanism | Token expire → logout ngay | P2 |
| 17.7 | **Sửa** | UploadButton: Empty catch block | 3 API steps nhưng error lost hoàn toàn | P0 |
| 17.8 | **Sửa** | UploadButton: S3 PUT success, confirm fail → không retry | File trên S3 nhưng record không process | P0 |
| 17.9 | **Sửa** | UploadButton: Không upload cancellation button | User không cancel large file upload | P2 |
| 17.10 | **Sửa** | UploadButton: Success redirect skip success message | "Tải lên thành công" chỉ hiện fraction of second | P2 |
| 17.11 | **Sửa** | `alert()` 10+ locations | Thay toast notification | P1 |
| 17.12 | **Sửa** | Register: Raw `axios.post` bypass `apiClient` | Không centralized error handling | P2 |
| 17.13 | **Sửa** | Login: consent fetch failure silent | User login xong nhưng consent không load | P2 |

---

## 18. Mobile App

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 18.1 | **Thêm** | TOÀN BỘ MOBILE APP | 10+ screens planned, ZERO implemented | P1 |
| 18.2 | **Thêm** | Auth screens (Login, Register, Onboarding) | | P1 |
| 18.3 | **Thêm** | Dashboard, Upload OCR, Results, Profiles | | P1 |
| 18.4 | **Thêm** | Settings, Family Sharing, Camera OCR | | P2 |
| 18.5 | **Sửa** | Missing `@healthlens/shared` dependency | `tsconfig.json` có path nhưng `package.json` không | P0 |
| 18.6 | **Sửa** | Zero test files | | P2 |
| 18.7 | **Sửa** | State management empty (`stores/index.ts`, `hooks/index.ts`) | Export `{}` | P2 |

---

## 19. CI / CD & Docker

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 19.1 | **Sửa** | Web Dockerfile dùng pnpm@9 | Project dùng pnpm@10.13.1 | P1 |
| 19.2 | **Sửa** | PostgreSQL missing từ base `compose.yml` | Chỉ có trong `compose.dev.yml` | P1 |
| 19.3 | **Thêm** | Mobile CI (EAS Build, test) | Hiện không có | P2 |
| 19.4 | **Thêm** | Staging deployment workflow | Chỉ có production deploy | P2 |
| 19.5 | **Thêm** | E2E tests (Playwright) | Không có test nào | P2 |
| 19.6 | **Thêm** | Security scanning (Trivy, Snyk) | Không trong CI | P2 |
| 19.7 | **Cải tiến** | Frontend test coverage (5 files) | Critical paths không test | P2 |
| 19.8 | **Cải tiến** | 30 env vars undocumented | Dùng trong `application.yml` nhưng không trong `.env.example` | P2 |
| 19.9 | **Sửa** | OCR Dockerfile redundant builder stage | `builder` stage unused | P3 |
| 19.10 | **Sửa** | No Nginx/Traefik config | `compose.prod.yml` có nginx commented out | P2 |

---

## 20. Cross-cutting

| # | Loại | Vấn đề | Chi tiết | Mức |
|---|------|--------|----------|-----|
| 20.1 | **Cải tiến** | `alert()` thay toast notification | 10+ locations | P1 |
| 20.2 | **Thêm** | Loading skeleton cho tất cả pages | Hiện tại chỉ spinner đơn giản | P2 |
| 20.3 | **Sửa** | Exam date không validate tương lai | Có thể chọn ngày trong tương lai | P2 |
| 20.4 | **Sửa** | Global exception handler không handle 405 | `HttpRequestMethodNotSupportedException` → HTML error page | P2 |
| 20.5 | **Thêm** | Accessibility: Missing `htmlFor`/`id` | Filter controls, form labels | P3 |
| 20.6 | **Cải tiến** | `eslint-disable @next/next/no-img-element` (5 locations) | Dùng `<img>` thay `<Image>` | P3 |
| 20.7 | **Tối ưu** | `@radix-ui/themes` full import | Chỉ dùng `Theme` + `Callout` → có thể tree-shake | P3 |
| 20.8 | **Thêm** | Error boundary trên auth pages | Login, Register silent crash | P2 |
| 20.9 | **Sửa** | Không offline detection | Mọi request fail đều không phân biệt network error | P2 |
| 20.10 | **Tối ưu** | Không request deduplication | Multiple components fetch cùng 1 endpoint | P2 |

---

---

## 22. Xác thực Email (verify-email)

### VE-001 — P1 — Token leaked in URL referrer + browser history
**`apps/web/src/app/(auth)/verify-email/page.tsx:24`** — Token persists in address bar, browser history, and sent as `Referer` header. Call `window.history.replaceState(null, "", "/verify-email")` after extraction. Add `<meta name="referrer" content="no-referrer" />`.

### VE-002 — P1 — No rate limiting on verify-email endpoint
**`apps/api/.../controller/AuthController.java:85-96`** — No IP/email rate limiting. Attacker can brute-force UUID tokens. Add 10 attempts/IP/minute.

### VE-003 — P1 — Error messages leak token validity enumeration
**`apps/api/.../service/AuthService.java:112-114`** — Different messages for "invalid" vs "expired/used" tokens. Unify to same message.

### VE-004 — P2 — TOCTOU race condition on concurrent verify
**`apps/api/.../service/AuthService.java:107-120`** — Two concurrent POSTs can both pass `usedAt == null` check. Use atomic SQL update with affected-rows check.

### VE-005 — P2 — No audit logging on verify-email
**`apps/api/.../service/AuthService.java:107-120`** — Zero logging for verification attempts. Add structured logging.

### VE-006 — P2 — Frontend displays raw backend `detail` without sanitization
**`apps/web/src/app/(auth)/verify-email/page.tsx:46-47`** — Backend error messages shown verbatim to user. Map to frontend-controlled messages.

### VE-007 — P2 — Unsafe type assertion on Axios error response
**`apps/web/src/app/(auth)/verify-email/page.tsx:45`** — `as { data?: { detail?: string } }` bypasses type safety. Use `isAxiosError` + typed interface.

### VE-008 — P2 — Error message leaks token state at frontend level
**`apps/web/src/app/(auth)/verify-email/page.tsx:47`** — Backend `detail` reveals whether token existed or expired. Always use opaque message.

### VE-009 — P2 — No loading skeleton in Suspense fallback
**`apps/web/src/app/(auth)/verify-email/page.tsx:17`** — Blank teal screen on slow connections. Use skeleton matching card layout.

### VE-010 — P3 — No resend-verification action
**`apps/web/src/app/(auth)/verify-email/page.tsx`** — Expired/used token error shows no way to request new email. Add "Gửi lại email xác thực" button.

### VE-011 — P3 — `useEffect` has no cleanup / AbortController
**`apps/web/src/app/(auth)/verify-email/page.tsx:29-54`** — State update on unmounted component possible. Add AbortController.

### VE-012 — P3 — Missing `aria-live` region for async status
**`apps/web/src/app/(auth)/verify-email/page.tsx:56-76`** — Screen readers won't announce transition. Add `role="status"`.

### VE-013 — P3 — Token not removed from URL after extraction
**`apps/web/src/app/(auth)/verify-email/page.tsx:24-25`** — Token persists even after success. Call `replaceState` immediately.

### VE-014 — P3 — Re-verify of already-verified email shows error
**`apps/api/.../service/AuthService.java:113-114`** — Already-verified user clicking link gets 400 error. Return success if already verified.

### VE-015 — P3 — No HTTP status differentiation in catch block
**`apps/web/src/app/(auth)/verify-email/page.tsx:40-52`** — 400/429/500 all show same message. Check `error.response?.status`.

### VE-016 — P3 — `withCredentials: true` unnecessary for verify-email
**`apps/web/src/lib/api/apiClient.ts:15`** — Cookies sent despite token-based auth. Set per-request instead.

---

## 23. Chấp nhận lời mời (invitations/accept)

### IA-01 — P0 — Invitation token in URL query param (leakage via referrer/logs)
**`apps/api/.../service/ProfileShareService.java:167`** + **`apps/web/.../invitations/accept/page.tsx:15`** — Token transmitted as URL query param, leaked in server logs, browser history, Referer header. Use POST body instead, add `Referrer-Policy: no-referrer`.

### IA-02 — P0 — `sessionStorage` accessed without try/catch
**`apps/web/.../invitations/accept/page.tsx:81,85`** — `sessionStorage.getItem/setItem` without try/catch crashes in iOS private browsing, disabled storage, quota exceeded. Wrap in try/catch.

### IA-03 — P1 — Expired token silently redirects to `/profiles`
**`apps/web/.../invitations/accept/page.tsx:71-73`** — Backend returns `outcome: "expired"` with `redirectUrl: "/profiles"`, frontend follows unconditionally. Check `outcome` before redirect.

### IA-04 — P1 — `AcceptResult.outcome` type mismatch
**`apps/web/.../invitations/accept/page.tsx:30`** — Frontend type has `"require-register"` but backend returns `"require-login"`. Fix type.

### IA-05 — P1 — No rate limiting on permitAll accept endpoint
**`apps/api/.../config/SecurityConfig.java:56`** — Endpoint is `permitAll()`, no rate limiting. Token enumeration oracle via 404 vs 200 differentiation. Add rate limiting + blind responses.

### IA-06 — P1 — Race condition: concurrent accept + revoke can bypass revoke
**`apps/api/.../service/ProfileShareService.java:140-175`** — Revoke checks after accept creates share leads to 404. Use `@Lock(PESSIMISTIC_WRITE)` or advisory locks.

### IA-07 — P1 — Missing 409 Conflict error handling
**`apps/web/.../invitations/accept/page.tsx:75-95`** — 409 falls through to generic error. Add dedicated handler.

### IA-08 — P2 — `window.location.replace` used for all outcomes including expired
**`apps/web/.../invitations/accept/page.tsx:72`** — Full page navigation loses React state. Use `router.push` for accepted, show error for expired.

### IA-09 — P2 — No audit log for invitation acceptance
**`apps/api/.../service/ProfileShareService.java:160-173`** — Revoke writes audit log, accept does not. Add `writeAcceptAuditLog`.

### IA-10 — P2 — `revokeShare` fallback uses wrong ID (`findById(viewerId)`)
**`apps/api/.../service/ProfileShareService.java:198`** — Looks up ProfileShare by viewer user ID instead of share ID. Remove fallback.

### IA-11 — P2 — 403 fallback message misleading for non-email-mismatch cases
**`apps/web/.../invitations/accept/page.tsx:79-81`** — 403 can be "account pending deletion" or "consent required". Check `errorCode`/`type`.

### IA-12 — P2 — Unsafe type cast without runtime validation
**`apps/web/.../invitations/accept/page.tsx:68`** — `response.data?.data as AcceptResult`. Use zod or shape guard.

### IA-13 — P2 — No timeout on API request (loading stuck forever)
**`apps/web/.../invitations/accept/page.tsx:47-56`** — No AbortController/timeout. User sees spinner forever on network hang.

### IA-14 — P2 — `incomingInvitations` query not invalidated after accept
**`apps/web/.../profiles/page.tsx:67-79`** — Accepted invitation banner persists 30s until next refetch. Invalidate on mount.

### IA-15 — P2 — No notification to owner when invitation accepted
**`apps/api/.../service/ProfileShareService.java:140-173`** — Owner unaware of active share. Send email notification.

### IA-16 — P2 — `useEffect` missing cleanup (setState on unmounted component)
**`apps/web/.../invitations/accept/page.tsx:63-93`** — No `cancelled` flag. Add cleanup.

### IA-17 — P3 — Token visible in address bar until API responds
**`apps/web/.../invitations/accept/page.tsx:72`** — Token in URL from page load to redirect. Replace history state immediately.

### IA-18 — P3 — No i18n on accept page
**`apps/web/.../invitations/accept/page.tsx`** — All text hardcoded Vietnamese. Use i18n if app multi-language.

### IA-19 — P3 — No page title / OpenGraph metadata
**`apps/web/.../invitations/accept/page.tsx:1`** — No `<title>` or OG tags for email link previews.

### IA-20 — P3 — Theoretical unhandled promise rejection
**`apps/web/.../invitations/accept/page.tsx:92`** — `void run()` inside try/catch so low risk.

### IA-21 — P3 — Component re-mount (not a bug, included for completeness)

---

## 24. Hủy yêu cầu xóa tài khoản (cancel-deletion)

### CD-01 — P1 — Email exposed in URL query params
**`apps/api/.../service/DataDeletionService.java:165`** — `&email=` in cancellation link URL. Remove email param; derive from API response.

### CD-02 — P1 — Cancellation token in URL (sensitive operation)
**`apps/api/.../service/DataDeletionService.java:161-164`** — Token as query param in `DELETE` request. Use POST with body or fragment identifier.

### CD-03 — P1 — Token replay mitigation incomplete
**`apps/api/.../service/DataDeletionService.java:179-215`** — Token set to CANCELLED after use, but no IP/user-agent logged for audit. Log + alert email on cancellation.

### CD-04 — P1 — Client-side expiry gate blocks valid backend cancellations
**`apps/web/.../CancelDeletionClient.tsx:131-133`** — Frontend calculates `isExpired` locally and blocks button. Backend is sole authority; remove client-side early return.

### CD-05 — P1 — Test coverage critically insufficient
**`apps/web/.../CancelDeletionClient.test.tsx`** — Only 1 test (missing token error). Missing: happy path, 409, 500, 429, isExpired, countdown, email masking, loading, double-click.

### CD-06 — P2 — 409 silently treated as identical success
**`apps/web/.../CancelDeletionClient.tsx:154-159`** — User sees "tài khoản đã được khôi phục" even if attacker cancelled first. Show distinct message.

### CD-07 — P2 — 401/403/500/429 all show same generic error
**`apps/web/.../CancelDeletionClient.tsx:162-169`** — No distinction between token invalid, rate limited, server error. Switch on status code.

### CD-08 — P2 — Token normalization mismatch (Java `URLEncoder` vs JS `decodeURIComponent`)
**`apps/web/.../CancelDeletionClient.tsx:16-22`** — `decodeURIComponent` doesn't decode `+` as space. Use consistent encoding or Base64 URL-safe tokens only.

### CD-09 — P2 — `parseTimestamp` fragile timezone regex
**`apps/web/.../CancelDeletionClient.tsx:24-28`** — Regex converts `"2024-01-15 10:30:00"` to UTC+10:30. Handle only ISO-8601 or use date library.

### CD-10 — P2 — No initial check for missing token (user must click button to discover error)
**`apps/web/.../CancelDeletionClient.tsx:103-112`** — With no token, user sees ready state with button. Add `useEffect` to set error immediately if no token.

### CD-11 — P2 — `useMemo` on `maskedEmail` re-runs every render
**`apps/web/.../CancelDeletionClient.tsx:94-102`** — `searchParams` from `useSearchParams()` is new instance every render. Extract string value.

### CD-12 — P2 — Test assertion for `DELETE_TOKEN_INVALID` is tautological
**`apps/web/.../CancelDeletionClient.test.tsx:43`** — String doesn't appear anywhere in component. Always passes. Remove or replace with real assertion.

### CD-13 — P2 — No mock reset between tests
**`apps/web/.../CancelDeletionClient.test.tsx:10`** — `vi.fn()` state leaks between tests. Add `beforeEach(() => vi.clearAllMocks())`.

### CD-14 — P3 — No rate limiting on public cancel endpoint
**`apps/api/.../controller/UserController.java:67-72`** — Public endpoint with no rate limiting. Add `@RateLimiter` or IP-based limit.

### CD-15 — P3 — Race window cancellation/deletion scheduler (properly handled, informational)

### CD-16 — P3 — Countdown timer may lag when tab backgrounded
**`apps/web/.../CancelDeletionClient.tsx:64-68,72-81`** — Browser throttles timers in hidden tabs. Use `document.visibilityState` or accept imprecision.

### CD-17 — P3 — 72-hour fallback undocumented
**`apps/web/.../CancelDeletionClient.tsx:42-48`** — Fallback to `requestedAt + 72h` when `scheduledDeletionAt` missing. Read from config/API.

### CD-18 — P3 — Loading state replaces entire page view (jarring)
**`apps/web/.../CancelDeletionClient.tsx:205-212`** — Clicking cancel replaces form with spinner. Show spinner in button instead.

### CD-19 — P3 — Missing `role="alert"` / `aria-live` on dynamic content
**`apps/web/.../CancelDeletionClient.tsx:207-211`** — Screen readers won't announce state transitions.

### CD-20 — P3 — `catch` uses `as` cast instead of runtime type check
**`apps/web/.../CancelDeletionClient.tsx:149`** — `err as AxiosError`. Use `isAxiosError(err)`.

---

## 21. Ma trận ưu tiên (updated)

### P0 — Critical (Làm ngay, 24 items)

```
Priority  Mô tả
─────────────────────────────────────────────────────────────
P0.1      [Backend] JWT secret hardcoded -> forge token
P0.2      [Backend] Spring Boot 4.x pre-release -> stability risk
P0.3      [Backend] Audit writes trong read-only transaction
P0.4      [Backend] purgeSoftDeletedRecords() không @Transactional
P0.5      [Backend] createProfile() race -> >10 profiles
P0.6      [Backend] Refresh token rotation race -> stolen token
P0.7      [Backend] OcrJobConsumer: stream ack không đồng bộ DB
P0.8      [Backend] No DLQ cho OCR poison messages
P0.9      [Backend] Deletion request/cancel race -> data loss
P0.10     [Backend] Shared editor sửa User entity -> privilege escalation
P0.11     [Backend] Xoá user không cleanup profile_shares/invitations
P0.12     [Backend] Thiếu rate limiting register, verify-email
P0.13     [Backend] Populate Qdrant RAG data
P0.14     [Frontend] Review: Xoá nút Chia sẻ, chuyển AI lên đầu, gộp giải thích
P0.15     [Frontend] Review: Preview bị mất (processing/ocr_failed/done)
P0.16     [Frontend] Upload Button: Empty catch block -> mất error
P0.17     [Frontend] Upload Button: S3 success + confirm fail -> không retry
P0.18     [Frontend] Register: window access during SSR -> crash
P0.19     [Frontend] Optimistic updates không rollback (3 locations)
P0.20     [Frontend] Dashboard: StatCard hardcoded fake data
P0.21     [Frontend] sessionStorage không try/catch (7 locations)
P0.22     [Frontend] Mobile: Missing @healthlens/shared dependency
P0.23     [Invitations] Token trong URL query param (IA-01)
P0.24     [Invitations] sessionStorage không try/catch invitations/accept (IA-02)
```

### P1 — High (Sprint này, 43 items)

```
Khu vực       Mô tả
─────────────────────────────────────────────────────────────
Backend       @Valid confirmRecord, pagination records, 5s cache TTL
Backend       externalize LLM prompts, retry for EasyOCR calls
Backend       AWS Textract stub fix hoặc bỏ
Backend       No auth/rate limit trên OCR endpoint
Backend       LLM JSON truncation repair fix
Backend       Email verification lost khi Redis down
Backend       forgotPassword silent swallow error
Backend       S3 deletion best-effort khi xoá user
Backend       Orphaned ProfileInvitation khi viewer user deleted
Backend       Duplicate ProfileShare khi accept concurrent
Backend       @Auditable implement, share lifecycle logs
Backend       Audit log unbounded growth (enrichMetric)
Backend       Email trong URL cancellation
Backend       MinIO credentials hardcoded, stored XSS
Frontend      Toast thay alert() (10+ locations)
Frontend      Confirm/fullDoc modals extract component
Frontend      Review page: Empty catch blocks fix
Frontend      Review page: Polling cleanup khi unmount
Frontend      Review page: fileUrl empty check
Frontend      Review page: Array index key fix
Frontend      History page: resolveHistoryStatus unsafe cast
Frontend      Health records hub: lastUpdated mapping fix
Frontend      Auth: Pending deletion text matching fix
Frontend      Auth: setTimeout cleanup (reset-password, settings)
Frontend      Form reset before API complete (Create/EditProfileModal)
Frontend      useAuthBootstrap unstable user dependency
Frontend      InviteMemberModal no-op union type fix
Frontend      Register handle 429, inviteToken gửi API
Frontend      Admin API client empty catch
Family        Block self-invite, fix revokeShare fallback
VerifyEmail   Token leaked in URL referrer + history (VE-001)
VerifyEmail   No rate limiting verify-email (VE-002)
VerifyEmail   Error messages leak token validity (VE-003)
Invitations   Expired token silent redirect (IA-03)
Invitations   AcceptResult type mismatch (IA-04)
Invitations   No rate limit on accept endpoint (IA-05)
Invitations   Accept + revoke race condition (IA-06)
Invitations   Missing 409 error handling (IA-07)
CancelDel     Email exposed in URL params (CD-01)
CancelDel     Cancellation token in URL (CD-02)
CancelDel     Token replay mitigation incomplete (CD-03)
CancelDel     Client-side expiry gate blocks valid cancels (CD-04)
CancelDel     Test coverage critically insufficient (CD-05)
```

### P2 — Medium (Sprint sau, 53 items)

```
Khu vực       Mô tả
─────────────────────────────────────────────────────────────
Feature       Family Dashboard, Admin Dashboard metrics, Audit Log
Feature       Settings: Change password, notification prefs, 2FA
Feature       Profile avatar upload, existing conditions fields
Feature       RAG: knowledge base documents ingestion
Backend       Same-class @Transactional proxy bypass
Backend       confirmUpload retry race fileKey overwrite
Backend       cancelInvitation hard-delete -> soft-delete
Backend       Gender normalization drop non-standard values
Backend       Global exception handler 405
Frontend      extractApiDetail shared utility (DRY)
Frontend      mapSharedStatusToCardStatus shared utility (DRY)
Frontend      ProfileCard React.memo
Frontend      navItems useMemo, isNavItemActive useCallback
Frontend      pendingAccessUpdates dead code (2 files)
Frontend      Link import unused (settings/profile)
Frontend      OCR service: Language detection, regex parser
Frontend      Filter "Chưa xác thực" fix, sort records
Frontend      Exam date validate tương lai
Frontend      Error boundary auth pages
Frontend      Offline detection
Frontend      Request deduplication
Frontend      Loading skeleton
Mobile        CI/CD, EAS Build
Docker        pnpm version, Postgres in base compose
CI/CD         Staging deployment
CI/CD         E2E tests setup
CI/CD         Security scanning
CI/CD         Frontend test coverage
VerifyEmail   TOCTOU race on concurrent verify (VE-004)
VerifyEmail   No audit logging verify-email (VE-005)
VerifyEmail   Backend error message shown verbatim (VE-006)
VerifyEmail   Unsafe type assertion Axios error (VE-007)
VerifyEmail   Error leaks token state frontend (VE-008)
VerifyEmail   No loading skeleton Suspense (VE-009)
Invitations   window.location.replace for all outcomes (IA-08)
Invitations   No audit log for invitation accept (IA-09)
Invitations   revokeShare fallback wrong ID (IA-10)
Invitations   Misleading 403 fallback message (IA-11)
Invitations   Unsafe type cast no runtime validation (IA-12)
Invitations   No timeout on API request (IA-13)
Invitations   incomingInvitations not invalidated (IA-14)
Invitations   No notification to owner on accept (IA-15)
Invitations   useEffect missing cleanup (IA-16)
CancelDel     409 treated as identical success (CD-06)
CancelDel     401/403/500/429 same generic error (CD-07)
CancelDel     Token normalization mismatch (CD-08)
CancelDel     parseTimestamp fragile timezone regex (CD-09)
CancelDel     No initial check for missing token (CD-10)
CancelDel     useMemo re-runs every render (CD-11)
CancelDel     Tautological test assertion (CD-12)
CancelDel     No mock reset between tests (CD-13)
```

### P3 — Low (Backlog, 34 items)

```
Khu vực       Mô tả
─────────────────────────────────────────────────────────────
Feature       Consent dedicated page, PDF download
Feature       Remember me checkbox
Feature       Register real-time validation
Feature       Admin CSV import, PDF export, timeline
Admin         About/Support pages
UI            Reusable HealthMetricsGrid, ReferenceRangeIndicator
UI            Empty state illustration
UI            Loading skeleton cho tất cả pages
UI            Accessibility: htmlFor/id
UI            @next/next/no-img-element (5 locations)
Code          @radix-ui/themes tree-shake
Code          4 barrel files export nothing
Code          API_TIMEOUT dead re-export
Code          console.error redundant
Code          Undocumented env vars
VerifyEmail   No resend-verification action (VE-010)
VerifyEmail   useEffect no cleanup/AbortController (VE-011)
VerifyEmail   Missing aria-live region (VE-012)
VerifyEmail   Token not removed from URL (VE-013)
VerifyEmail   Re-verify already-verified shows error (VE-014)
VerifyEmail   No HTTP status differentiation (VE-015)
VerifyEmail   withCredentials:true unnecessary (VE-016)
Invitations   Token visible in address bar (IA-17)
Invitations   No i18n on accept page (IA-18)
Invitations   No page title/OG metadata (IA-19)
Invitations   Unhandled promise rejection (IA-20)
CancelDel     No rate limiting public cancel endpoint (CD-14)
CancelDel     Countdown timer may lag tab backgrounded (CD-16)
CancelDel     72-hour fallback undocumented (CD-17)
CancelDel     Loading state replaces entire page view (CD-18)
CancelDel     Missing role=alert/aria-live (CD-19)
CancelDel     catch uses as cast (CD-20)
```

---

> **End of report — 154 findings total**
>
> File này sẽ được cập nhật khi có thay đổi.
