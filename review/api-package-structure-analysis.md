# 🔬 HealthLens API — Package Structure Analysis & Proposal

**Date:** 2025-05-21  
**Scope:** `apps/api/src/main/java/com/healthlens/api/`  
**Total Java files:** 291

---

## 1. Hiện Trạng: Cấu Trúc Hiện Tại

```text
com.healthlens.api/
├── HealthLensApplication.java
├── activity/           ← 1 file (UserActivityEventType enum)
├── ai/                 ← Feature-based ✅
│   ├── chat/           (LlmService, PromptTemplateRenderer)
│   ├── embedding/      (EmbeddingService, VectorStoreService)
│   └── rag/            (Ingestion, Retrieval, Corpus, Online RAG)
├── annotation/         ← Cross-cutting (2 files)
├── aspect/             ← Cross-cutting (1 file)
├── audit/              ← Feature-based ✅ (9 files)
├── config/             ← Cross-cutting (15 files, mixed concerns!)
├── constants/          ← Cross-cutting (3 files)
├── controller/         ← Layer-based ⚠️ (15 files + admin/)
├── correlation/        ← Cross-cutting (2 files)
├── dto/                ← Layer-based ⚠️ (8 files + admin/ + request/ + response/)
├── entity/             ← Layer-based ⚠️ (34 files, flat!)
├── events/             ← Feature-based ✅ (4 files + email/ + ocr/)
├── exception/          ← Layer-based ⚠️ (17 files)
├── ocr/                ← Feature-based ✅ (3 files + provider/)
├── persistence/        ← Unclear purpose (json/ → 2 files)
├── repository/         ← Layer-based ⚠️ (29 files + projection/)
├── security/           ← Feature-based ✅ (10 files)
├── service/            ← Layer-based ⚠️ (20 files + admin/)
└── util/               ← Cross-cutting (1 file)
```

---

## 2. Phân Tích Vấn Đề

### 🔴 Vấn đề 1: Hybrid Inconsistency (Nghiêm trọng)

Dự án đang **trộn lẫn hai triết lý** tổ chức code:

| Triết lý | Packages đang dùng |
|---|---|
| **Package-by-Layer** | `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `exception/` |
| **Package-by-Feature** | `ocr/`, `ai/`, `audit/`, `events/`, `security/`, `correlation/` |

**Hệ quả:** Khi muốn thay đổi feature "Health Record", bạn phải nhảy qua **6+ packages** khác nhau:
- `controller/HealthRecordController.java`
- `service/HealthRecordService.java` (77KB! 🔥)
- `entity/HealthRecord.java`, `entity/HealthRecordAuditLog.java`, `entity/HealthRecordShare.java`, `entity/HealthRecordInvitation.java`
- `repository/HealthRecordRepository.java`, `repository/HealthRecordShareRepository.java`, `repository/HealthRecordAuditLogRepository.java`, `repository/HealthRecordInvitationRepository.java`
- `dto/response/HealthRecordDetailResponse.java`, `dto/response/HealthRecordStatusResponse.java`, ...
- `dto/request/ConfirmRecordRequest.java`, `dto/request/UpdateMetricsRequest.java`, ...

### 🔴 Vấn đề 2: God Service Files

| File | Size | Ghi chú |
|---|---|---|
| `HealthRecordService.java` | **77 KB** | Quá lớn, nên tách thành nhiều service |
| `LlmService.java` | **67 KB** | Quá lớn |
| `ReferenceDataAdminService.java` | **66 KB** | Quá lớn |
| `OcrService.java` | **47 KB** | Khá lớn |
| `AdminAuditLogService.java` | **36 KB** | Khá lớn |

> [!WARNING]
> Một class Java > 500 dòng đã là cảnh báo. Các file trên có thể > 1000–2000 dòng, vi phạm nghiêm trọng Single Responsibility Principle.

### 🟡 Vấn đề 3: `persistence/` Package Mơ Hồ

```text
persistence/
└── json/
    ├── JsonPathExpressions.java      (interface)
    └── PostgreSqlJsonPathExpressions.java  (implementation)
```

Chỉ có 2 file, mục đích không rõ ràng. Nên thuộc về `common/persistence/` hoặc `infrastructure/`.

### 🟡 Vấn đề 4: `activity/` Package Mỏng

```text
activity/
└── UserActivityEventType.java   (1 enum)
```

Chỉ có 1 enum. Service liên quan (`UserActivityService`) lại nằm trong `service/`. Filter liên quan (`UserActivityRecordingFilter`) lại nằm trong `security/`.

### 🟡 Vấn đề 5: `config/` Package Chứa Quá Nhiều Concerns

15 file config trộn lẫn:
- AI config (`AiChatConfig`, `AiChatProviderEnvironmentPostProcessor`)
- Security config (`SecurityConfig`, `SecurityStartupValidator`)
- OCR config (`OcrServiceConfig`)
- Storage config (`StorageBucketInitializer`)
- Embedding config (`QdrantVectorStoreEnvironmentPostProcessor`, `EmbeddingVectorStoreStartupValidator`)
- Scheduling config (`DeletionScheduler`, `FollowUpReminderScheduler`)
- General config (`ClockConfig`, `JacksonConfig`, `OpenApiConfig`)

### 🟡 Vấn đề 6: `entity/` Flat với 34 Files

34 entity files trong một flat directory, khó tìm entity liên quan đến feature nào.

---

## 3. Đề Xuất: Package-by-Feature (Hybrid)

> [!IMPORTANT]
> Triết lý: **Feature ở top-level, Layer ở bên trong feature.** Cross-cutting concerns nằm trong `common/`.

### Cấu Trúc Đề Xuất

```text
com.healthlens.api/
├── HealthLensApplication.java
│
├── auth/                           ← 🔑 Authentication & Authorization
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   └── AuthService.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── RefreshToken.java
│   │   ├── EmailVerificationToken.java
│   │   └── PasswordResetToken.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── RefreshTokenRepository.java
│   │   ├── EmailVerificationTokenRepository.java
│   │   └── PasswordResetTokenRepository.java
│   └── dto/
│       ├── request/  (LoginRequest, RegisterRequest, ...)
│       └── response/ (LoginResponse, RefreshResponse, ...)
│
├── profile/                        ← 👤 User Profiles
│   ├── controller/
│   │   └── ProfileController.java
│   ├── service/
│   │   ├── ProfileService.java
│   │   └── ProfileShareService.java
│   ├── entity/
│   │   ├── Profile.java
│   │   ├── ProfileShare.java
│   │   ├── ProfileInvitation.java
│   │   └── ProfileShareAuditLog.java
│   ├── repository/
│   └── dto/
│
├── healthrecord/                   ← 📋 Health Records (core domain)
│   ├── controller/
│   │   ├── HealthRecordController.java
│   │   └── HealthRecordInvitationController.java
│   ├── service/
│   │   ├── HealthRecordQueryService.java      ← tách từ HealthRecordService
│   │   ├── HealthRecordCommandService.java    ← tách từ HealthRecordService
│   │   ├── HealthRecordShareService.java
│   │   └── HealthRecordPdfService.java
│   ├── entity/
│   │   ├── HealthRecord.java
│   │   ├── HealthRecordAuditLog.java
│   │   ├── HealthRecordShare.java
│   │   └── HealthRecordInvitation.java
│   ├── repository/
│   └── dto/
│
├── ocr/                            ← 📷 OCR Processing (giữ nguyên ✅)
│   ├── OcrService.java
│   ├── OcrJobConsumer.java
│   ├── OcrJobStateService.java
│   ├── controller/
│   │   └── OcrController.java        ← di chuyển từ controller/
│   ├── config/
│   │   └── OcrServiceConfig.java     ← di chuyển từ config/
│   ├── entity/
│   │   ├── OcrDeadLetter.java        ← di chuyển từ entity/
│   │   ├── OcrJobExecution.java
│   │   └── OcrJobState.java
│   ├── repository/
│   │   ├── OcrDeadLetterRepository.java
│   │   └── OcrJobExecutionRepository.java
│   ├── dto/
│   │   ├── OcrResult.java
│   │   └── OcrMetricResponse.java
│   ├── event/
│   │   ├── OcrJobEvent.java
│   │   ├── OcrJobEventPublisher.java
│   │   └── RedisOcrJobEventPublisher.java
│   └── provider/                     (giữ nguyên ✅)
│       ├── OcrProvider.java
│       ├── OcrProviderRegistry.java
│       ├── EasyOcrProviderAdapter.java
│       ├── GoogleCloudVisionOcrProvider.java
│       ├── PdfTextOcrProvider.java
│       └── TextractOcrProvider.java
│
├── ai/                             ← 🤖 AI Services (giữ nguyên ✅)
│   ├── chat/
│   │   ├── LlmService.java
│   │   ├── PromptTemplateRenderer.java
│   │   └── config/
│   │       └── AiChatConfig.java
│   ├── embedding/
│   │   ├── EmbeddingService.java
│   │   ├── VectorStoreService.java
│   │   └── config/
│   │       └── QdrantVectorStoreConfig.java
│   └── rag/
│       ├── MetricExplanationIngestionService.java
│       ├── MetricExplanationRetrievalService.java
│       ├── RagCorpusGovernanceService.java
│       ├── TrustedOnlineRagSourceAdapter.java
│       └── entity/
│           ├── OnlineRagAnswerCitation.java
│           ├── OnlineRagSourceSnapshot.java
│           └── RagCorpusVersion.java
│
├── referencedata/                  ← 📊 Reference Data Management
│   ├── controller/
│   │   └── ReferenceDataController.java
│   ├── service/
│   │   └── ReferenceDataService.java
│   ├── entity/
│   │   ├── ReferenceMetric.java
│   │   ├── ReferenceMetricAlias.java
│   │   ├── ReferenceRange.java
│   │   └── ReferenceDataChangeSet.java
│   ├── repository/
│   └── dto/
│
├── consent/                        ← ✅ Consent Management
│   ├── controller/
│   │   └── ConsentController.java
│   ├── service/
│   │   └── ConsentService.java
│   ├── entity/
│   │   └── ConsentLog.java
│   └── annotation/
│       ├── RequiresConsent.java
│       └── ConsentAspect.java
│
├── reminder/                       ← ⏰ Follow-up Reminders
│   ├── controller/
│   │   └── FollowUpReminderController.java
│   ├── service/
│   │   └── FollowUpReminderService.java
│   ├── entity/
│   │   └── FollowUpReminder.java
│   └── scheduler/
│       └── FollowUpReminderScheduler.java
│
├── deletion/                       ← 🗑️ Data Deletion
│   ├── service/
│   │   └── DataDeletionService.java
│   ├── entity/
│   │   └── DataDeletionRequest.java
│   └── scheduler/
│       └── DeletionScheduler.java
│
├── email/                          ← 📧 Email Delivery
│   ├── service/
│   │   ├── EmailService.java
│   │   └── EmailConsumer.java
│   └── event/
│       ├── EmailEvent.java
│       ├── EmailEventPublisher.java
│       └── RedisEmailEventPublisher.java
│
├── admin/                          ← 🛡️ Admin Features
│   ├── auth/
│   │   ├── AdminAuthController.java
│   │   ├── AdminAuthService.java
│   │   └── entity/AdminTotpSecret.java
│   ├── analytics/
│   │   ├── AdminAnalyticsController.java
│   │   └── AnalyticsService.java
│   ├── audit/
│   │   ├── AdminAuditLogController.java
│   │   └── AdminAuditLogService.java
│   ├── referencedata/
│   │   ├── AdminReferenceDataController.java
│   │   ├── AdminReferenceMetricController.java
│   │   ├── ReferenceDataAdminService.java
│   │   └── AdminReferenceMetricService.java
│   └── citation/
│       ├── AdminOnlineRagCitationController.java
│       └── AdminOnlineRagCitationService.java
│
└── common/                         ← 🔧 Cross-Cutting Concerns
    ├── audit/                      (giữ nguyên audit/ hiện tại)
    │   ├── AuditActions.java
    │   ├── AuditEventRecorder.java
    │   ├── AuditOutcome.java
    │   ├── AuditRedactor.java
    │   ├── UnifiedAuditCoordinator.java
    │   └── ...
    ├── correlation/
    │   ├── CorrelationContext.java
    │   └── CorrelationIdFilter.java
    ├── event/                      (event infrastructure)
    │   ├── ApplicationStreamNames.java
    │   ├── ApplicationStreamPublisher.java
    │   ├── RedisApplicationStreamPublisher.java
    │   └── RedisStreamConsumerSupport.java
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   ├── BusinessException.java
    │   ├── ResourceNotFoundException.java
    │   └── ...
    ├── persistence/
    │   └── json/
    │       ├── JsonPathExpressions.java
    │       └── PostgreSqlJsonPathExpressions.java
    ├── security/
    │   ├── JwtAuthenticationFilter.java
    │   ├── CustomUserDetailsService.java
    │   ├── LoginRateLimiter.java
    │   └── ...
    ├── config/                     (chỉ general configs)
    │   ├── ClockConfig.java
    │   ├── JacksonConfig.java
    │   ├── OpenApiConfig.java
    │   └── SecurityConfig.java
    ├── annotation/
    │   └── Auditable.java
    ├── constants/
    │   ├── ApiRoutes.java
    │   └── SecurityConstants.java
    └── util/
        └── JwtUtil.java
```

---

## 4. So Sánh: Trước vs Sau

| Tiêu chí | Hiện tại (Hybrid lộn xộn) | Đề xuất (Feature-first) |
|---|---|---|
| **Tìm code liên quan Health Record** | Nhảy 6+ packages | 1 package `healthrecord/` |
| **Tìm code liên quan OCR** | `ocr/` + `controller/OcrController` + `config/OcrServiceConfig` + `entity/OcrDeadLetter` + `repository/OcrDeadLetterRepository` + `events/ocr/` + `dto/OcrResult` | 1 package `ocr/` |
| **Thêm feature mới** | Tạo file ở 4-6 packages khác nhau | Tạo 1 package mới |
| **Visibility control** | Mọi thứ phải `public` | Có thể dùng package-private |
| **Cohesion** | Thấp (entity/34 files flat) | Cao (entity gần service/controller) |
| **Coupling** | Cao (cross-package dependencies) | Thấp hơn (rõ boundaries) |

---

## 5. Lộ Trình Migration (Phased)

> [!TIP]
> Không nên refactor tất cả cùng lúc. Ưu tiên những module đã có hình dáng feature (ocr, ai) trước.

### Phase 1 — Quick Wins (1-2 ngày)

Consolidate các module **đã gần đúng** feature-based:

- [x] `ocr/` → Chỉ cần di chuyển `OcrController`, `OcrServiceConfig`, OCR entities, OCR repositories, OCR DTOs, OCR events vào
- [x] `ai/` → Di chuyển `AiChatConfig`, AI-related entities/repos vào
- [x] Tạo `common/` cho cross-cutting: `correlation/`, `persistence/`, `util/`, annotation `Auditable`

### Phase 2 — Core Features (3-5 ngày)

Tách các domain lớn:

- [ ] Tạo `auth/` → Di chuyển auth controller/service/entities/DTOs
- [ ] Tạo `healthrecord/` → Di chuyển và **tách** `HealthRecordService` (77KB → Query + Command + Share + Pdf)
- [ ] Tạo `profile/` → Di chuyển profile-related code
- [ ] Tạo `referencedata/` → Di chuyển reference data code
- [ ] Tạo `email/` → Di chuyển email service + events

### Phase 3 — Supporting Features (2-3 ngày)

- [ ] Tạo `consent/`, `reminder/`, `deletion/`
- [ ] Tạo `admin/` với sub-packages
- [ ] Consolidate `config/` → Phân tán config về feature packages, giữ general configs ở `common/config/`

### Phase 4 — Cleanup (1 ngày)

- [ ] Xóa packages cũ rỗng
- [ ] Cập nhật `source-tree-analysis.md` và `backend-ai-ocr-rag-package-map.md`
- [ ] Cập nhật `architecture.md`
- [ ] Verify: `./gradlew build` passes
- [ ] Verify: all tests green

---

## 6. Nguyên Tắc Áp Dụng Lâu Dài

1. **Feature mới = Package mới** — Không thêm file vào `service/` hay `controller/` flat nữa
2. **Config nằm gần feature** — `OcrServiceConfig` thuộc về `ocr/config/`, không phải `config/`
3. **Cross-cutting → `common/`** — Chỉ những thứ **thực sự** dùng chung mới vào `common/`
4. **Package-private by default** — Chỉ expose public API cần thiết
5. **Tách God Service khi refactor** — Bất kỳ service > 500 dòng nên được review để tách

---

## 7. Trả Lời Câu Hỏi Cụ Thể

> **"Tổ chức thành folder ocr, ai có hợp lý không?"**

✅ **Rất hợp lý!** Đây chính là Package-by-Feature — best practice hiện tại cho Spring Boot. `ocr/` và `ai/` là hai module tốt nhất trong project hiện tại.

> **"Persistence, activity, correlation thì sao?"**

| Package | Đánh giá | Đề xuất |
|---|---|---|
| `persistence/` | ⚠️ Tên mơ hồ, chỉ 2 file JSON path expressions | → `common/persistence/json/` |
| `activity/` | ⚠️ Chỉ 1 enum, service ở nơi khác | → Merge enum vào `common/` hoặc tạo `activity/` đầy đủ (chuyển cả service + filter vào) |
| `correlation/` | ✅ Hợp lý nhưng là cross-cutting | → `common/correlation/` |

> **"Cách tổ chức đang không tốt ở đâu?"**

Vấn đề chính là **sự thiếu nhất quán**: `ocr/` và `ai/` theo feature-based rất tốt, nhưng phần còn lại (`controller/`, `service/`, `entity/`, `repository/`, `dto/`) vẫn theo layer-based truyền thống, tạo ra sự phân tán code.
