# 🗂️ HealthLens — Folder Organization: Full-Repo Audit & Concrete Solution

> **Ngày**: 2026-05-28
> **Phạm vi**: TOÀN BỘ repo — `apps/api`, `apps/web`, `apps/mobile`, `services/ocr-service`, `packages/shared`, `docker/`, `docs/`, `infisical/`, root-level configs.
> **Mục tiêu**: (1) Bổ sung những phần còn thiếu so với 2 review trước (chỉ tập trung api+web), (2) Làm rõ **nội bộ của mỗi feature package** trong giải pháp package-by-feature.

---

## 0. 🧭 Hiện trạng tổng quan (chụp nhanh repo)

```
health-lens/
├── apps/
│   ├── api/                 ← Spring Boot (Java 21, Gradle Kotlin DSL)
│   ├── web/                 ← Next.js 15 (App Router, TS)
│   └── mobile/              ← Expo Router (RN, TS)
├── services/
│   └── ocr-service/         ← FastAPI (Python) — 1 file app.py
├── packages/
│   └── shared/              ← Cross-platform TS package (constants, types, schemas, config)
├── docker/                  ← compose.{yml,dev.yml,prod.yml} + scripts/
├── docs/                    ← 17 markdown files (architecture, runbooks…)
├── infisical/               ← secret manager scripts
├── _bmad/, _bmad-output/    ← tooling outputs (BMAD workflow)
├── .agent/, .agents/, .codex/, .cursor/, .opencode/  ← 5 AI tooling dirs
├── .env, .env.example, .env.production, .env.staging, .env.staging.api
├── pnpm-workspace.yaml, package.json, pnpm-lock.yaml
├── settings.gradle.kts (root Gradle settings)
└── README.md
```

2 review trước đã đi sâu `apps/api` và `apps/web`. Báo cáo này **đi rộng** ra các phần còn lại + làm rõ **micro-layout** bên trong từng feature.

---

## 1. ❓ Câu hỏi cốt lõi: "Bên trong mỗi feature package nên có gì?"

Bạn đã đúng khi nhận xét **đề xuất package-by-feature ở review #1 chưa clear**. Câu hỏi thật sự là: **trong `healthrecord/`, ta vẫn cần `dto/`, `service/`, `repository/`, hay đặt phẳng?**

### 1.1 Có **3 trường phái** được dùng rộng rãi trong industry

| Trường phái | Cấu trúc | Khi nào dùng |
|-------------|----------|--------------|
| **A. Flat per feature** (Spring Pet Clinic style) | Tất cả file của feature đặt phẳng trong package, dùng class name suffix để phân biệt (`HealthRecord.java`, `HealthRecordController.java`, `HealthRecordService.java`, `HealthRecordRepository.java`) | Feature **nhỏ** (<10 files), domain đơn giản |
| **B. Sub-package by layer inside feature** (Reflectoring/JavaCodeGeeks recommended) | `feature/web/`, `feature/service/`, `feature/repository/`, `feature/domain/`, `feature/dto/` | Feature **trung bình–lớn** (>15 files), nhiều DTO/entity |
| **C. Hexagonal / Ports & Adapters** | `feature/domain/`, `feature/application/`, `feature/adapter/in/web/`, `feature/adapter/out/persistence/` | Feature **critical**, cần test-isolation cao, có khả năng swap adapter |

> **HealthLens fit**: Feature `healthrecord` có **1,710 dòng service + 5 DTO request + 12 DTO response + 6 entities liên quan** → **trường phái B là sweet spot**. Trường phái C overkill ở thời điểm hiện tại; trường phái A sẽ nhanh chóng tạo lại flat-hell.

### 1.2 Sub-package CỤ THỂ cho mỗi feature (HealthLens convention)

Cấu trúc dưới đây áp dụng cho **mọi feature package** trong `com.healthlens.api`:

```
healthrecord/                                  ← Feature root
├── HealthRecordController.java                ← (1) PUBLIC API — REST endpoints
├── api/                                       ← Optional: thêm khi >1 controller
│   ├── HealthRecordController.java            (user-facing)
│   ├── HealthRecordAdminController.java       (admin-facing)
│   └── HealthRecordInternalController.java    (cross-service)
│
├── service/                                   ← (2) USE-CASES — Application services
│   ├── HealthRecordUploadService.java
│   ├── HealthRecordQueryService.java
│   ├── HealthRecordConfirmService.java
│   ├── HealthRecordExportService.java
│   └── HealthRecordAccessControl.java
│
├── domain/                                    ← (3) ENTITIES + VALUE OBJECTS + ENUMS
│   ├── HealthRecord.java                      (JPA entity)
│   ├── HealthRecordShare.java
│   ├── HealthRecordStatus.java                (enum + state machine)
│   ├── HealthRecordAuditLog.java
│   └── HealthRecordInvitation.java
│
├── repository/                                ← (4) DATA ACCESS — Spring Data interfaces
│   ├── HealthRecordRepository.java
│   ├── HealthRecordShareRepository.java
│   └── projection/                            ← Custom projections nếu nhiều
│       └── HealthRecordSummaryProjection.java
│
├── dto/                                       ← (5) BOUNDARY TYPES
│   ├── request/
│   │   ├── CreateUploadUrlRequest.java
│   │   ├── ConfirmRecordRequest.java
│   │   └── UpdateMetricsRequest.java
│   └── response/
│       ├── HealthRecordStatusResponse.java
│       ├── HealthRecordDetailResponse.java
│       └── HealthRecordHistoryItemResponse.java
│
├── mapper/                                    ← (6) ENTITY ↔ DTO MAPPERS
│   └── HealthRecordMapper.java                (MapStruct preferred)
│
├── event/                                     ← (7) FEATURE EVENTS (publisher only)
│   ├── HealthRecordConfirmedEvent.java
│   └── HealthRecordSharedEvent.java
│
└── exception/                                 ← (8) FEATURE-LOCAL EXCEPTIONS
    ├── HealthRecordNotFoundException.java
    └── InvalidStatusTransitionException.java
```

**8 sub-packages, mỗi cái có 1 trách nhiệm rõ ràng.** Đây là layout đã được Reflectoring / Mark Heckler / Java Geeks khuyến nghị cho Spring Boot vertical-slice.

### 1.3 Quy tắc chốt — khi nào tạo sub-package nào?

| Sub-package | Bắt buộc? | Khi nào tạo |
|-------------|-----------|-------------|
| `domain/` (entity, enum, VO) | ✅ Luôn có | Mọi feature đều có entity |
| `repository/` | ✅ Luôn có | Mọi feature đều có data access |
| `dto/request/` + `dto/response/` | ✅ Luôn có | Mọi feature đều có boundary |
| `service/` | ✅ Luôn có | Business logic |
| Controller (`api/` hoặc flat) | ✅ Luôn có | REST endpoints |
| `mapper/` | ⚠️ Khi có MapStruct hoặc >2 mapping pattern lặp lại | — |
| `event/` | ⚠️ Khi feature publish events | — |
| `exception/` | ⚠️ Khi >1 domain exception | Otherwise dùng `common/exception/` |

### 1.4 Khi feature chỉ có 3–5 file → flat OK

Ví dụ `consent/` (chỉ có 1 controller, 1 service, 2 DTO, 1 constants) → để **phẳng**:

```
consent/
├── ConsentController.java
├── ConsentService.java
├── ConsentRequest.java
├── ConsentResponse.java
└── ConsentConstants.java
```

→ **Quy tắc 7-files rule**: Feature ≤7 files → flat. >7 files → sub-package theo §1.2.

---

## 2. 🏗️ Giải pháp toàn diện cho `apps/api`

### 2.1 Mapping hiện tại → đích

| Hiện tại (54 files chia 6 nhóm) | Sau refactor | Ghi chú |
|----------------------------------|--------------|---------|
| `controller/HealthRecordController.java` | `healthrecord/HealthRecordController.java` | Di chuyển nguyên file |
| `controller/HealthRecordInvitationController.java` | `healthrecord/api/HealthRecordInvitationController.java` hoặc gộp `invitation/` | Tùy theo invitation có cross-feature không |
| `service/HealthRecordService.java` (1,710 dòng) | **TÁCH** thành 5 file trong `healthrecord/service/` | Xem review #2 §1.1 |
| `service/HealthRecordPdfService.java` | `healthrecord/service/HealthRecordExportService.java` | Đổi tên cho clear |
| `service/HealthRecordShareService.java` | `healthrecord/service/HealthRecordShareService.java` | — |
| `entity/HealthRecord.java`, `HealthRecordShare.java`, `HealthRecordAuditLog.java`, `HealthRecordInvitation.java` | `healthrecord/domain/` | — |
| `repository/HealthRecordRepository.java`, `HealthRecordShareRepository.java`, `HealthRecordAuditLogRepository.java`, `HealthRecordInvitationRepository.java` | `healthrecord/repository/` | — |
| `dto/request/CreateUploadUrlRequest.java`, `ConfirmRecordRequest.java`, `UpdateMetricsRequest.java`, `InviteHealthRecordRequest.java` | `healthrecord/dto/request/` | — |
| `dto/response/HealthRecord*.java` (12 file) | `healthrecord/dto/response/` | — |

### 2.2 Phân nhóm 17+ feature roots cho `apps/api`

Dựa trên 37 entity + 19 controller + 34 repo hiện tại:

```
com.healthlens.api/
├── HealthLensApplication.java                ← Main class
│
├── common/                                    ← Cross-cutting (NOT a feature)
│   ├── config/                                ← JacksonConfig, ClockConfig, OpenApiConfig, SecurityConfig…
│   ├── exception/                             ← HealthLensException hierarchy + GlobalExceptionHandler
│   ├── security/                              ← JwtAuthenticationFilter, rate limiters, ClientIpResolver
│   ├── audit/                                 ← AuditEventRecorder, UnifiedAuditCoordinator (cross-feature)
│   ├── correlation/                           ← CorrelationIdFilter
│   ├── persistence/                           ← JsonPathExpressions
│   ├── web/                                   ← ApiRoutes, common response envelopes, pagination
│   ├── event/                                 ← ApplicationStreamPublisher (Redis Streams base)
│   └── util/                                  ← JwtUtil
│
├── auth/                                      ← Login, register, JWT, password reset, email verify
│   ├── AuthController.java
│   ├── service/ AuthService.java
│   ├── domain/ User.java, RefreshToken.java, PasswordResetToken.java, EmailVerificationToken.java, UserRole.java, AccountStatus.java
│   ├── repository/ UserRepository.java, RefreshTokenRepository.java, …
│   ├── dto/request/ LoginRequest, RegisterRequest, ResetPasswordRequest, …
│   ├── dto/response/ LoginResponse, RefreshResponse, …
│   └── exception/ AccountLockedException, EmailAlreadyExistsException, WeakPasswordException, UserNotFoundException
│
├── user/                                      ← User profile mgmt, settings, TOTP, activity
│   ├── UserController.java, UserTotpController.java
│   ├── service/ UserService.java, UserTotpService.java, UserActivityService.java, TotpSecretCryptoService.java
│   ├── domain/ UserTotpSecret.java, UserActivityEvent.java, UserActivityEventType.java
│   ├── repository/ UserTotpSecretRepository.java, UserActivityEventRepository.java
│   ├── dto/{request,response}/
│   └── (FailureReasonNormalizer.java vào lib nội bộ)
│
├── profile/                                   ← Patient profiles (multi-patient per user)
│   ├── ProfileController.java, SharedProfileController.java
│   ├── service/ ProfileService.java, ProfileShareService.java
│   ├── domain/ Profile.java, ProfileShare.java, ProfileShareAuditLog.java, ProfileInvitation.java
│   ├── repository/
│   └── dto/{request,response}/
│
├── healthrecord/                              ← (theo §1.2 chi tiết ở trên)
│   ├── HealthRecordController.java, HealthRecordInvitationController.java
│   ├── service/ HealthRecordUploadService, QueryService, ConfirmService, ExportService, AccessControl, PdfService
│   ├── domain/ HealthRecord, HealthRecordShare, HealthRecordStatus (enum), HealthRecordAuditLog, HealthRecordInvitation
│   ├── repository/
│   ├── dto/{request,response}/
│   ├── mapper/ HealthRecordMapper.java (MapStruct)
│   └── exception/ HealthRecordNotFoundException, InvalidStatusTransitionException, ProfileAccessRevokedException
│
├── ocr/                                       ← OCR pipeline (NOT including LLM parsing)
│   ├── OcrController.java
│   ├── service/ OcrPipelineService.java, OcrJobConsumer.java, OcrJobStateService.java
│   ├── provider/                              ← OcrProvider interface + implementations
│   │   ├── OcrProvider.java                   (interface)
│   │   ├── OcrProviderRegistry.java
│   │   ├── EasyOcrProviderAdapter.java
│   │   ├── TextractOcrProvider.java
│   │   ├── GoogleCloudVisionOcrProvider.java
│   │   └── PdfTextOcrProvider.java
│   ├── client/                                ← Low-level HTTP clients
│   │   ├── AwsTextractClient.java
│   │   └── GoogleCloudVisionClient.java
│   ├── domain/ OcrJobExecution, OcrJobState, OcrDeadLetter, OcrCapability, OcrJob
│   ├── repository/ OcrJobExecutionRepository, OcrDeadLetterRepository
│   ├── event/ OcrJobEvent, OcrJobEventPublisher, RedisOcrJobEventPublisher
│   ├── dto/{request,response}/                ← EasyOcrRequest/Response extracted from inner classes!
│   ├── config/ OcrServiceConfig.java          ← Property binding cụ thể feature
│   └── exception/ OcrProcessingException
│
├── llm/                                       ← LLM provider chain (Spring AI + OpenRouter)
│   ├── service/ LlmService.java, OcrMetricExtractor.java
│   ├── provider/                              ← (NEW) Interface + adapters
│   │   ├── LlmProvider.java
│   │   ├── LlmProviderChain.java
│   │   ├── SpringAiLlmProvider.java
│   │   └── OpenRouterLlmProvider.java
│   ├── prompt/ PromptTemplateRenderer.java
│   └── config/ AiChatConfig.java, AiChatProviderEnvironmentPostProcessor.java
│
├── rag/                                       ← Retrieval-Augmented Generation
│   ├── service/ MetricExplanationIngestionService.java, MetricExplanationRetrievalService.java, MetricExplanationIngestionJob.java, EmbeddingService.java, VectorStoreService.java, RagCorpusGovernanceService.java
│   ├── client/ OnlineRagHttpClient.java, RestTemplateOnlineRagHttpClient.java, TrustedOnlineRagSourceAdapter.java
│   ├── policy/ TrustedOnlineRagSourcePolicy.java
│   ├── domain/ OnlineRagAnswerCitation, OnlineRagSourceSnapshot, OnlineRagReviewStatus, RagCorpusVersion
│   ├── repository/
│   ├── dto/{request,response}/                ← OnlineRagCitationResponse, RetrievalTraceResponse
│   └── config/ AiRagHealthIndicator.java, QdrantVectorStoreEnvironmentPostProcessor.java, EmbeddingVectorStoreStartupValidator.java
│
├── storage/                                   ← S3/MinIO upload + presigned URL
│   ├── service/ StorageService.java
│   ├── port/ StoragePort.java                 ← (NEW interface — review #2 §1.2)
│   ├── adapter/ MinioStorageAdapter.java, AwsS3StorageAdapter.java
│   └── config/ StorageBucketInitializer.java
│
├── notification/                              ← Email + in-app notifications
│   ├── NotificationController.java, UserNotificationPreferenceController.java
│   ├── service/ EmailService.java, EmailConsumer.java, NotificationInboxService.java, UserNotificationPreferenceService.java
│   ├── port/ EmailPort.java                   ← (NEW) for swap-out
│   ├── adapter/ SmtpEmailAdapter.java
│   ├── domain/ NotificationInboxReadState, UserNotificationPreference, NotificationEmailCategory
│   ├── repository/
│   ├── event/ EmailEvent, EmailEventPublisher, RedisEmailEventPublisher
│   └── dto/{request,response}/
│
├── reminder/                                  ← Follow-up reminders (1 controller + 1 entity)
│   ├── FollowUpReminderController.java
│   ├── service/ FollowUpReminderService.java
│   ├── domain/ FollowUpReminder.java
│   ├── repository/
│   ├── dto/{request,response}/
│   └── scheduler/ FollowUpReminderScheduler.java
│
├── consent/                                   ← Small feature → flat OK (5 files)
│   ├── ConsentController.java
│   ├── ConsentService.java
│   ├── ConsentAspect.java
│   ├── domain/ ConsentLog.java
│   ├── repository/ ConsentLogRepository.java
│   ├── ConsentConstants.java
│   └── dto/  (ConsentRequest, ConsentResponse)
│
├── invitation/                                ← Có thể gộp profile-invitation + healthrecord-invitation
│   ├── InvitationController.java
│   ├── service/
│   └── (chia tiếp nếu cần)
│
├── deletion/                                  ← Account deletion (GDPR-like)
│   ├── service/ DataDeletionService.java
│   ├── domain/ DataDeletionRequest.java, DeletionRequestStatus.java
│   ├── repository/
│   ├── dto/{request,response}/
│   ├── scheduler/ DeletionScheduler.java
│   └── exception/ DeletionCancellation*Exception.java (3 file)
│
├── reference/                                 ← Reference data (ranges, metrics)
│   ├── ReferenceDataController.java
│   ├── service/ ReferenceDataService.java
│   ├── domain/ ReferenceData, ReferenceMetric, ReferenceMetricAlias, ReferenceRange, ReferenceRangeAuditLog, ReferenceDataChangeSet
│   ├── repository/
│   └── dto/{request,response}/
│
├── analytics/                                 ← Đo lường
│   ├── service/ AnalyticsService.java, UserActivityService (đã ở user/)
│   ├── repository/ AnalyticsRepository.java
│   └── dto/response/ ActivityAnalyticsResponse, UserAnalyticsResponse, UploadQualityResponse
│
└── admin/                                     ← Admin-specific endpoints — vẫn cần TÁCH theo sub-feature
    ├── auth/                                  ← admin-only auth (AdminAuthService, AdminAuthController, AdminTotp*)
    ├── analytics/                             ← AdminAnalyticsController
    ├── audit/                                 ← AdminAuditLogController, AdminAuditLogService
    ├── reference/                             ← AdminReferenceDataController, ReferenceDataAdminService
    └── rag/                                   ← AdminOnlineRagCitationController, AdminOnlineRagCitationService
```

### 2.3 Quy tắc tránh sai khi migrate

1. **Một file Java chỉ thuộc đúng MỘT feature root.** Nếu thấy mơ hồ (vd `UserActivityService` — thuộc `user/` hay `analytics/`?), chọn theo **đối tượng nó hành xử**, không phải nơi nó được gọi.
2. **`common/` không phải là cái thùng rác.** Chỉ chấp nhận: cross-cutting filters/aspects, base exception, base event publisher, util thực sự dùng ở ≥3 feature.
3. **Admin có thể là feature root riêng** (như trên), **hoặc** một sub-package trong từng feature (`healthrecord/admin/HealthRecordAdminController.java`). HealthLens hiện đã có `controller/admin/` nên giữ trường phái "admin root" cho đỡ disrupt.
4. **Không cho phép import vòng giữa các feature.** Nếu `healthrecord` cần gọi `profile`, gọi qua **interface ở common/** hoặc qua **event** — không import trực tiếp service của nhau.

---

## 3. 🌐 Giải pháp cho `apps/web` — Feature-Sliced Design (FSD-lite)

### 3.1 Vấn đề riêng (chưa được review #1 nêu)

| Vấn đề | Chi tiết |
|--------|----------|
| `apps/web/apps/web/` | **Ghost directory** chỉ chứa `node_modules` — phải xóa, sai do leftover từ workspace init |
| `apps/web/packages/shared/` | **Rỗng** (không có file) — leftover từ workspace |
| `apps/web/src/lib/` | 12 sub-folder + 8 file phẳng (`appVersion.ts`, `healthRecordHub.ts`, `notify.ts`, `profileMappings.ts`, `supportContact.ts`, `authPublicLinks.ts`) — không có quy tắc nhóm |
| Cùng tên 2 chỗ | `SettingsAccountSidebar.tsx` xuất hiện ở `components/features/settings/` **VÀ** `app/(dashboard)/settings/_components/` |
| Hooks scattered | `hooks/admin/` chỉ 3 file, không có domain hooks cho health-records / profiles / upload (xem review #2 §6.3) |
| Mixed components/ui | `HealthMetricCard.tsx` (domain) cùng chỗ với `SafeImage.tsx` (generic) trong `components/ui/` |

### 3.2 Target layout

Áp dụng **Feature-Sliced Design (FSD)** rút gọn — 4 layer thay vì 7 (bỏ widgets, pages, processes do Next.js App Router đã quản):

```
apps/web/
├── src/
│   ├── app/                          ← Next.js routes ONLY (thin shells <100 LoC)
│   │   ├── layout.tsx
│   │   ├── (auth)/login/page.tsx
│   │   ├── (dashboard)/health-records/page.tsx
│   │   ├── (dashboard)/health-records/review/[recordId]/page.tsx  ← shell only
│   │   ├── admin/…
│   │   └── (marketing)/…
│   │
│   ├── features/                     ← Domain features (each = vertical slice)
│   │   ├── health-records/
│   │   │   ├── components/           ← MetricsTable, RecordHeader, RecordActions, ShareDialog…
│   │   │   ├── hooks/                ← useHealthRecordDetail, useMetricEditing, useRecordActions
│   │   │   ├── api/                  ← record-api.ts (functions calling backend)
│   │   │   ├── types.ts              ← MetricDto, ReviewRecordData… (extracted from page.tsx)
│   │   │   └── lib/                  ← formatters, status helpers
│   │   ├── upload/
│   │   ├── profiles/
│   │   ├── consent/
│   │   ├── notifications/
│   │   ├── follow-up-reminders/
│   │   ├── visit-summary/
│   │   ├── settings/
│   │   ├── auth/                     ← login forms, register, password reset
│   │   └── admin/
│   │       ├── analytics/
│   │       ├── audit-log/
│   │       └── reference-data/
│   │
│   ├── shared/                       ← Project-agnostic
│   │   ├── ui/                       ← Button, Modal, Badge, SafeImage, StateComponents…
│   │   ├── lib/                      ← apiClient, formatters, browser/sessionStorage, i18n/messages
│   │   ├── hooks/                    ← useAuthHydrated, useAuthBootstrap (auth wiring)
│   │   ├── config/                   ← env helpers, appVersion
│   │   └── types/
│   │
│   ├── layouts/                      ← App shells (AuthPageShell, DashboardPageShell, MarketingHeader…)
│   │
│   └── stores/                       ← Zustand global stores (authStore)
│
├── public/
├── scripts/
└── package.json
```

### 3.3 Migrations cụ thể (high-leverage)

| Từ | Đến |
|-----|------|
| `apps/web/apps/` | **DELETE** (ghost dir) |
| `apps/web/packages/` | **DELETE** (empty) |
| `src/lib/admin/*` | `src/features/admin/{analytics,audit-log,reference-data}/lib/` |
| `src/lib/api/{apiClient,adminApiClient}.ts` | `src/shared/lib/api/` |
| `src/lib/api/routes.ts` | **DELETE** (re-export wrapper — import từ `@healthlens/shared` trực tiếp) |
| `src/lib/auth/*` | `src/features/auth/lib/` |
| `src/lib/browser/*` | `src/shared/lib/browser/` |
| `src/lib/consent/*` | `src/features/consent/lib/` |
| `src/lib/forms/*` | `src/shared/lib/forms/` |
| `src/lib/healthRecordHub.ts` | `src/features/health-records/lib/hub.ts` |
| `src/lib/notify.ts` | `src/shared/lib/notify.ts` |
| `src/lib/profileMappings.ts` | `src/features/profiles/lib/mappings.ts` |
| `src/lib/i18n/messages.ts` | `src/shared/lib/i18n/` |
| `src/lib/layout/*` | `src/layouts/` |
| `src/lib/marketing/*` | `src/features/marketing/lib/` hoặc gộp vào `(marketing)` |
| `src/lib/seo/*` | `src/shared/lib/seo/` |
| `src/lib/sharing/*` | `src/features/health-records/lib/sharing/` |
| `src/lib/supportContact.ts`, `appVersion.ts`, `authPublicLinks.ts` | `src/shared/config/` |
| `src/lib/utils/*` | `src/shared/lib/utils/` |
| `src/components/ui/HealthMetricCard.tsx`, `HealthMetricsGrid.tsx`, `ReferenceRangeIndicator.tsx` | `src/features/health-records/components/` |
| `src/components/ui/SafeImage.tsx`, `StateComponents.tsx` | `src/shared/ui/` |
| `src/components/features/*` | `src/features/*/components/` (đã gần đúng — bỏ tầng `components/features/` đệm) |
| `src/components/auth/*` | `src/features/auth/components/` |
| `src/components/admin/*` | `src/features/admin/components/` |
| `src/components/marketing/*` | `src/features/marketing/components/` |
| `src/components/layout/*` | `src/layouts/` |
| `src/components/notifications/ToastProvider.tsx` | `src/shared/ui/` (provider chung) |
| `src/app/(dashboard)/settings/_components/*` | Gộp với `src/features/settings/components/` (đang **trùng tên** với chỗ khác!) |
| `src/hooks/*` (admin sub-folder + 6 loose hooks) | Phân loại: auth → `features/auth/hooks/`; notification → `features/notifications/hooks/`; admin → `features/admin/.../hooks/` |
| `src/types/qrcode.react.d.ts` | `src/shared/types/` |

### 3.4 Routing rule (FSD-style)

* `app/` → import từ `features/` và `shared/` ✅
* `features/X` → import từ `features/X` (own), `shared/`, `layouts/` ✅
* `features/X` → import `features/Y` ❌ (cross-feature) — phải đi qua `shared/` hoặc `app/`-level composition
* `shared/` → KHÔNG import từ `features/` ❌

---

## 4. 📱 Giải pháp cho `apps/mobile` — Triệt để hợp nhất `src/`

### 4.1 Vấn đề riêng

Hiện tại có **4 thư mục top-level "ma"** chứa chỉ 1 file barrel rỗng:

```bash
apps/mobile/components/index.ts   →  export {};
apps/mobile/hooks/index.ts        →  export {};
apps/mobile/stores/index.ts       →  export {};
apps/mobile/lib/api/index.ts      →  export const apiBaseUrl = …
```

Trong khi đó `apps/mobile/src/` mới là nơi code thật (`src/components/`, `src/hooks/`, `src/constants/`). **tsconfig path alias đã trỏ `@/*` → `./src/*`** → các top-level dirs này **vô dụng** và gây nhầm lẫn.

### 4.2 Target layout

```
apps/mobile/
├── app/                              ← Expo Router routes (giữ ngoài src/ — convention Expo)
│   ├── _layout.tsx
│   └── index.tsx
├── src/
│   ├── features/                     ← Sao chép FSD-lite từ web
│   │   ├── health-records/
│   │   ├── profiles/
│   │   └── auth/
│   ├── shared/
│   │   ├── ui/                       ← themed-text, themed-view, animated-icon
│   │   ├── hooks/                    ← use-color-scheme, use-theme
│   │   ├── lib/api/                  ← apiBaseUrl + client
│   │   └── constants/                ← theme.ts
│   ├── layouts/                      ← app-tabs, web-badge
│   └── stores/
├── assets/                           ← Expo conventions (ngoài src/)
├── scripts/
├── app.json
├── package.json
└── tsconfig.json
```

### 4.3 Migrations cụ thể

| Hiện tại | Đích | Hành động |
|----------|------|-----------|
| `apps/mobile/components/index.ts` | — | **DELETE** (file rỗng) |
| `apps/mobile/hooks/index.ts` | — | **DELETE** (file rỗng) |
| `apps/mobile/stores/index.ts` | — | **DELETE** (file rỗng) |
| `apps/mobile/lib/api/index.ts` | `apps/mobile/src/shared/lib/api/index.ts` | Move + update import |
| `apps/mobile/src/components/{themed-text,themed-view,animated-icon,external-link,hint-row}` | `src/shared/ui/` | Move |
| `apps/mobile/src/components/{app-tabs,web-badge}` | `src/layouts/` | Move |
| `apps/mobile/src/components/ui/` | `src/shared/ui/` | Merge |
| `apps/mobile/src/hooks/` | `src/shared/hooks/` | Move |
| `apps/mobile/src/constants/theme.ts` | `src/shared/constants/` | Move |
| `apps/mobile/app/` | Giữ nguyên | Expo convention |

Lý do giữ `app/` ngoài `src/`: Expo Router docs nói **cả 2 đều OK**, nhưng project đã có `app/` ngoài rồi và tsconfig đã wire — đổi sẽ phá routing. Chỉ cần unify phần còn lại.

---

## 5. 🐍 Giải pháp cho `services/ocr-service` — Từ monolith → modules

### 5.1 Hiện trạng

```
services/ocr-service/
├── app.py             ← 455 dòng: FastAPI app + models + SSRF + image download + OCR + endpoints
├── requirements.txt
├── Dockerfile
└── README.md
```

**1 file = 6 concerns**. Không có `src/`, không có tests folder, không có config tách riêng.

### 5.2 Target layout (theo zhanymkanov/fastapi-best-practices)

```
services/ocr-service/
├── src/
│   ├── main.py                       ← FastAPI app instantiation, middleware, router include
│   ├── config.py                     ← Settings (pydantic-settings), env loading
│   │
│   ├── ocr/                          ← Domain module (chỉ 1 domain hiện tại)
│   │   ├── router.py                 ← /ocr/extract endpoint
│   │   ├── schemas.py                ← Pydantic models (request/response)
│   │   ├── service.py                ← Business logic (OCR orchestration)
│   │   ├── dependencies.py           ← FastAPI Depends() (auth, rate limit)
│   │   ├── exceptions.py             ← OcrException hierarchy
│   │   └── constants.py
│   │
│   ├── image/                        ← Image download + SSRF protection
│   │   ├── downloader.py             ← Tách phần SSRF + HTTP download
│   │   ├── validator.py              ← MIME + size validation
│   │   └── ssrf_guard.py             ← isolate SSRF logic (đã có test cho nó!)
│   │
│   ├── core/
│   │   ├── logging.py
│   │   ├── middleware.py
│   │   └── health.py                 ← /health endpoint
│   │
│   └── shared/
│       ├── http.py                   ← shared HTTPX client
│       └── retry.py
│
├── tests/                            ← pytest (hiện đã có test_ssrf_protection nhưng để root!)
│   ├── conftest.py
│   ├── test_ocr_router.py
│   ├── test_ssrf_protection.py       ← Move ra đây
│   └── test_image_validator.py
│
├── requirements/
│   ├── base.txt
│   ├── dev.txt                       ← + pytest, ruff, mypy
│   └── prod.txt
│
├── pyproject.toml                    ← Thay/bổ sung requirements.txt
├── Dockerfile
└── README.md
```

> **Quy tắc**: Khi service Python chỉ có **1 domain** (OCR), không cần `domain/` đa nhánh — nhưng **bắt buộc** tách `router/schema/service/dependencies` thành file riêng. Khi thêm domain mới (ví dụ chấm chất lượng ảnh), copy module structure.

---

## 6. 📦 Giải pháp cho `packages/shared`

### 6.1 Hiện trạng — phân tán không có `src/`

```
packages/shared/
├── index.ts                          ← Barrel root
├── package.json
├── tsconfig.json
├── config/    env.ts, index.ts
├── constants/ api.ts, consent.ts, error-codes.ts, index.ts, status.ts
├── schemas/   auth.ts, profile.ts, user.ts, index.ts
└── types/     index.ts
```

3 vấn đề (kế thừa từ 2 review trước):
1. **Constants duplication**: `Gender`, `Pagination`, `FileSize` định nghĩa nhiều lần (xem review #1 §3.1–3.6).
2. **No `src/`**: package thiếu tầng `src/` chuẩn — `tsconfig` exclude/include phải gymnastic.
3. **`HealthRecordStatus` enum không khớp** với strings thật trong code.

### 6.2 Target layout

```
packages/shared/
├── src/
│   ├── index.ts                      ← Re-export root
│   │
│   ├── domain/                       ← (NEW) Domain primitives — single source of truth
│   │   ├── gender.ts                 ← Hợp nhất 3 định nghĩa Gender hiện tại
│   │   ├── health-record-status.ts   ← Hợp nhất với Java enum (cùng tên + value)
│   │   ├── consent.ts                ← Phiên bản từ API (no hardcoded "1.0")
│   │   └── error-code.ts             ← ApiErrorCode mirror với Java
│   │
│   ├── api/                          ← API contract types
│   │   ├── routes.ts                 ← URL builders (đổi tên từ api.ts)
│   │   ├── pagination.ts             ← 1 nguồn cho DEFAULT_PAGE/SIZE/MAX_SIZE
│   │   └── upload.ts                 ← UPLOAD_MAX_SIZE_BYTES (1 giá trị duy nhất)
│   │
│   ├── schemas/                      ← Zod schemas (giữ nguyên)
│   │   ├── auth.ts
│   │   ├── profile.ts
│   │   └── user.ts
│   │
│   ├── config/                       ← env helpers (KHÔNG còn duplicate PAGINATION)
│   │   └── env.ts
│   │
│   └── types/                        ← Cross-platform DTO interfaces
│       └── index.ts
│
├── package.json
├── tsconfig.json
└── README.md
```

### 6.3 Quy tắc khi thêm constant mới

1. Nếu giá trị **xuất hiện ở cả backend Java**: thêm vào `packages/shared/src/domain/` **và** generate/sync với enum Java tương ứng (có thể qua một codegen script đơn giản, hoặc giữ comment `// SYNCED WITH com.healthlens.api.healthrecord.domain.HealthRecordStatus`).
2. Nếu chỉ dùng web/mobile: vẫn đặt ở `packages/shared/` để 2 client thấy giống nhau.
3. **Một concept = một file**. Đừng để `Gender` xuất hiện ở 2 file.

---

## 7. 🐳 Giải pháp cho `docker/` và root config

### 7.1 Hiện trạng

```
docker/
├── compose.yml          ← base
├── compose.dev.yml      ← dev override
├── compose.prod.yml     ← prod override
└── scripts/             ← up.sh, down.sh, logs.sh, cleanup.sh
```

→ **Đã đủ tốt**. 3 file compose theo override pattern là chuẩn Docker Compose. Không cần thay đổi.

### 7.2 Vấn đề root-level config sprawl

```
.env                  (5.3 KB, gitignored)
.env.example          (5.2 KB)
.env.production       (4.8 KB)
.env.staging          (1.4 KB)
.env.staging.api      (4.9 KB)
.infisical.json       (config secret manager)
```

**5 env file** ở root + Infisical đã có. Đề xuất:

| File | Hành động |
|------|-----------|
| `.env.example` | **GIỮ** — tài liệu hóa các biến cần thiết |
| `.env` | **GIỮ** (gitignored) — local dev |
| `.env.production` | **DELETE** — đã có Infisical |
| `.env.staging` | **DELETE** — đã có Infisical |
| `.env.staging.api` | **DELETE** — đã có Infisical |
| `.infisical.json` | **GIỮ** |
| `infisical/` | **GIỮ** — scripts đã có |

→ Còn lại `.env.example` (commit), `.env` (local-only), `.infisical.json` + `infisical/scripts/` cho mọi thứ không phải local. Cleaner ×3.

### 7.3 AI tooling dir sprawl

`.agent/`, `.agents/`, `.codex/`, `.cursor/`, `.opencode/`, `_bmad/`, `_bmad-output/`, `.github/copilot-*` — **7 thư mục tooling** ở root.

Đề xuất:
1. Audit xem 5 tool dirs có còn dùng không (`.codex` có vẻ là OpenAI Codex CLI, `.cursor` cho Cursor IDE, `.opencode` không rõ).
2. Giữ **tối đa 2–3** dir nhóm đang active (vd Cursor + GitHub Copilot). Còn lại move sang `docs/agent-configs/` hoặc xóa.
3. `_bmad/` và `_bmad-output/` (BMAD workflow output) nên gitignore nếu là cache; hoặc move vào `docs/_bmad/`.

---

## 8. 🧪 Test folder consistency

### 8.1 Hiện trạng

| Module | Test location |
|--------|---------------|
| `apps/api` | `src/test/java/com/healthlens/api/` — **mirror exactly layer structure** (sẽ tự động mirror sau khi refactor) ✅ |
| `apps/web` | Test file **đặt cạnh source** (`*.test.ts(x)` colocated) ✅ |
| `apps/mobile` | Chưa có test infrastructure ❌ |
| `services/ocr-service` | `test_ssrf_protection.py` đặt **trong `__pycache__/`** (!!) — bug |

### 8.2 Khuyến nghị

* **Java**: Khi refactor sang feature packages, test cũng phải move song song để giữ mirror.
* **Web**: Giữ colocated test pattern.
* **Mobile**: Thêm `vitest` hoặc `jest-expo` theo cùng pattern web.
* **OCR service**: Tạo `tests/` folder rõ ràng, di chuyển `test_ssrf_protection.py` ra khỏi `__pycache__/`.

---

## 9. 📋 Roadmap thực hiện (ưu tiên theo ROI)

### Phase 0 — Cleanup nhanh (ngày 1, < 2h, low risk)

1. Xóa `apps/web/apps/` (ghost dir)
2. Xóa `apps/web/packages/` (empty)
3. Xóa 3 file barrel rỗng trong `apps/mobile/{components,hooks,stores}/index.ts`
4. Di chuyển `services/ocr-service/__pycache__/test_ssrf_protection*.pyc` → cleanup, move source file sang `tests/`
5. Xóa `.env.staging`, `.env.production`, `.env.staging.api` (Infisical đã thay)
6. Hợp nhất file size constant (20MB vs 10MB vs 30MB) → 1 giá trị duy nhất

### Phase 1 — Foundation enum + duplicates (tuần 1)

7. Tạo Java enum `HealthRecordStatus` + sync với `packages/shared/src/domain/health-record-status.ts`
8. Hợp nhất `Gender` thành 1 file ở `packages/shared/src/domain/gender.ts`
9. Hợp nhất `Pagination` constants
10. Xóa `apps/web/src/lib/api/routes.ts` (re-export wrapper)

### Phase 2 — Mobile + OCR-service refactor (tuần 2)

11. Mobile: unify mọi top-level dir vào `src/` theo §4.2
12. OCR-service: tách `app.py` 455 dòng thành module theo §5.2
13. OCR-service: tạo `tests/` folder

### Phase 3 — API package-by-feature (tuần 3–6)

14. Tách `healthrecord/` feature trước (rủi ro cao nhất, ROI cao nhất). Theo §1.2 và §2.2.
15. Tách `ocr/` + `llm/` (giải quyết circular dependency `EasyOcrProviderAdapter` ↔ `OcrService`)
16. Tách `auth/`, `user/`, `profile/`
17. Tách `storage/`, `notification/`, `rag/`, `reference/`, `analytics/`
18. Cuối cùng: `common/` cleanup (chỉ giữ thứ thực sự cross-cutting)

### Phase 4 — Web FSD migration (tuần 7–8)

19. Move components/lib theo §3.3
20. Extract `ReviewRecordPage` (2,783 lines) → `features/health-records/`
21. Tạo domain hooks (`useHealthRecordDetail`, `useMetricEditing`…)

---

## 10. ✅ Checklist tự đánh giá (sau khi áp dụng)

| Tiêu chí | Pass? |
|----------|-------|
| Mọi file Java thuộc về đúng 1 feature root, không file nào ở `service/` flat | ☐ |
| `common/` không chứa file của feature cụ thể | ☐ |
| Không còn import vòng giữa feature root | ☐ |
| `packages/shared/src/domain/` là single source cho mọi enum cross-platform | ☐ |
| Backend Java và TS shared dùng cùng value cho status, gender, pagination | ☐ |
| `apps/web/apps/` và `apps/web/packages/` đã xóa | ☐ |
| `apps/mobile` không còn top-level `components/hooks/stores/lib` ngoài `src/` | ☐ |
| `services/ocr-service/app.py` < 100 dòng (chỉ là entry point) | ☐ |
| Root có **tối đa 2 env file** (`.env`, `.env.example`) | ☐ |
| Mỗi feature có sub-package theo §1.2 nếu >7 file, flat nếu ≤7 file | ☐ |

---

## 📚 Tài liệu tham khảo

- [Feature-Sliced Design — Overview](https://feature-sliced.design/docs/get-started/overview) — 7 layers + slice segments (UI, API, Model, Lib, Config) cho frontend
- [zhanymkanov/fastapi-best-practices](https://github.com/zhanymkanov/fastapi-best-practices) — Domain-based module structure cho FastAPI
- [Expo Router — src/ convention](https://docs.expo.dev/router/installation/) — path alias `@/*` → `./src/*`
- **Review #1**: [project_review.md](project_review.md) — bao quát hybrid structure issues
- **Review #2**: [deep_code_review.md](deep_code_review.md) — SOLID + service decomposition
