# 🔬 HealthLens — Deep Code Review: SOLID, Clean Code & Architecture

> **Ngày**: 2026-05-28 | **Tập trung**: SOLID violations, adapter replaceability, service decomposition, concrete refactoring solutions

---

## 1. 🏛️ SOLID Violations — Chi tiết

### 1.1 Single Responsibility Principle (SRP) — 🔴 Nghiêm trọng

#### `HealthRecordService.java` — 1,710 dòng, 18 dependencies

File này đảm nhận **ít nhất 7 trách nhiệm riêng biệt**:

| # | Trách nhiệm | Methods | Dòng ước tính |
|---|-------------|---------|---------------|
| 1 | Upload URL + Confirm Upload | `createUploadUrl`, `confirmUpload` | ~200 |
| 2 | Status Query + Caching (Redis) | `getStatus`, `getDetail` | ~150 |
| 3 | OCR State Management | `markOcrCompleted`, `markOcrFailed` | ~120 |
| 4 | Record Confirmation + Metric Update | `confirmRecord`, `updateMetrics` | ~200 |
| 5 | PDF Generation + Download | `downloadHealthRecordPdf`, `downloadOriginalDocument` | ~200 |
| 6 | Explanation + Recommendations (LLM) | `getMetricExplanation`, `getRecommendations` | ~300 |
| 7 | Sharing + Access Control | `getSharedHealthRecords`, `getRecordsByProfile`, permission checks | ~300 |
| 8 | History + Pagination | `getProfileHistory` | ~100 |
| 9 | Utility: slugify, caching, mapping | Various private methods | ~200 |

**Proposed Split:**

```
HealthRecordUploadService      ← #1 (upload URL, confirm upload, reservation)
HealthRecordQueryService       ← #2, #8 (status, detail, history, caching)
HealthRecordOcrLifecycle       ← #3 (markOcrCompleted, markOcrFailed)
HealthRecordConfirmService     ← #4 (confirm, updateMetrics)
HealthRecordExportService      ← #5 (PDF, original document download)
HealthRecordExplanationService ← #6 (LLM explanation, recommendations)
HealthRecordAccessControl      ← #7 (permission checks, sharing queries)
```

#### `OcrService.java` — 1,082 dòng, 2 hoàn toàn khác biệt trách nhiệm

**Vấn đề chính**: `OcrService` **MIX** 2 concerns hoàn toàn khác nhau:

1. **OCR Pipeline** (routing, fallback, metrics) — lines 1-625
2. **LLM Metric Parsing** (`parseMetrics`, regex fallback, OpenRouter call) — lines 632-1082

LLM parsing **KHÔNG phải OCR**. Nó nhận text đã OCR xong rồi extract structured data bằng AI.

```diff
- OcrService.java (1,082 lines — OCR + LLM mixed)
+ OcrPipelineService.java     ← processImage, processDocument, processPdf
+ OcrMetricExtractor.java     ← parseMetrics (LLM + regex fallback)
+ OpenRouterLlmClient.java    ← callOpenRouterFallback (tách thành adapter)
```

**Bonus violation**: `OcrService` contains **inner DTOs** (`EasyOcrResponse`, `EasyOcrRequest`, `OcrExtractionResult`) — nên extract ra file riêng.

#### `EmailService.java` — 550 dòng, **HTML hardcoded**

Mỗi email method chứa **inline HTML strings dài 20-50 dòng**. Một số dùng Thymeleaf template, một số dùng `String.formatted()` — **inconsistent strategy**.

```java
// ❌ Pattern hiện tại: 6 methods, mỗi method có HTML riêng
public void sendPasswordResetEmail(...) {
    String htmlContent = """
        <html><body style="...">
        <h2>HealthLens - Đặt lại mật khẩu</h2>
        ...
    """.formatted(user.getFullName(), resetLink);
}

// ✅ Nên: TẤT CẢ đều dùng template engine
public void sendPasswordResetEmail(...) {
    Context ctx = new Context();
    ctx.setVariable("name", user.getFullName());
    ctx.setVariable("resetLink", resetLink);
    sendEmail(user.getEmail(), "password-reset-subject", "email/password-reset", ctx);
}
```

### 1.2 Open/Closed Principle (OCP) — 🟡 Trung bình

#### LLM Provider — Closed for Extension

```java
// OcrService.java — line 778-833
private String callPrimaryLlm(String prompt) {
    return chatClient.prompt().user(prompt).call().content();
}
private String callOpenRouterFallback(String prompt) {
    // 30 dòng hardcoded HTTP call đến OpenRouter
    String url = openRouterBaseUrl + "/chat/completions";
    Map<String, Object> payload = Map.of("model", openRouterModel, ...);
    headers.setBearerAuth(openRouterApiKey);
    ...
}
```

**Vấn đề**: Muốn thêm provider mới (Anthropic, Gemini) phải **sửa OcrService** — vi phạm OCP.

**Giải pháp**: Extract `LlmProvider` interface:

```java
public interface LlmProvider {
    String name();
    int priority();
    String complete(String prompt);
    boolean isAvailable();
}

// Implementations:
@Component class SpringAiLlmProvider implements LlmProvider { ... }
@Component class OpenRouterLlmProvider implements LlmProvider { ... }
// Tương lai: AnthropicLlmProvider, GeminiLlmProvider

@Component
public class LlmProviderChain {
    private final List<LlmProvider> providers; // sorted by priority
    public String complete(String prompt) {
        for (var p : providers) {
            if (!p.isAvailable()) continue;
            String result = p.complete(prompt);
            if (result != null && !result.isBlank()) return result;
        }
        return null;
    }
}
```

#### Storage — Tightly Coupled to S3

```java
// StorageService.java — line 47
public class StorageService {
    private final S3Client s3Client;        // ❌ Concrete dependency
    private final S3Presigner presigner;    // ❌ Concrete dependency
```

**Vấn đề**: Muốn đổi sang GCS, Azure Blob, hoặc local filesystem → phải rewrite toàn bộ. Không có interface.

**Giải pháp**:
```java
public interface StoragePort {
    String generateUploadUrl(String key, Duration ttl, String contentType);
    String generateDownloadUrl(String key, Duration ttl);
    void uploadObject(String key, byte[] bytes, String contentType);
    byte[] downloadObjectBytes(String key);
    void deleteObject(String key);
    int deleteObjectsByPrefix(String prefix);
}

@Component @Profile("minio")
class MinioStorageAdapter implements StoragePort { ... }

@Component @Profile("s3")  
class AwsS3StorageAdapter implements StoragePort { ... }
```

### 1.3 Liskov Substitution Principle (LSP) — ✅ OK

`OcrProvider` interface + implementations (`EasyOcrProviderAdapter`, `TextractOcrProvider`, `GoogleCloudVisionOcrProvider`, `PdfTextOcrProvider`) tuân thủ LSP tốt. Mỗi provider substitutable mà không phá vỡ contract.

### 1.4 Interface Segregation Principle (ISP) — 🟡 Vi phạm

**Vấn đề**: Không có interface cho **bất kỳ service nào** ngoài `OcrProvider`:

```
87 test files   ← Nhưng services concrete, không mock được qua interface
0 service interfaces (ngoài OcrProvider)
```

| Service | Dependencies | Có interface? |
|---------|-------------|--------------|
| `HealthRecordService` | 18 | ❌ |
| `AuthService` | 15 | ❌ |
| `OcrService` | 8 | ❌ |
| `StorageService` | 0 | ❌ |
| `EmailService` | 2 | ❌ |
| `LlmService` | ? | ❌ |

Tất cả services đều là **concrete classes** — test phải dùng `@SpringBootTest` thay vì unit test với mock interfaces.

### 1.5 Dependency Inversion Principle (DIP) — 🔴 Vi phạm nặng

```java
// HealthRecordController.java
private final HealthRecordService healthRecordService; // ← Depend on CONCRETE class

// OcrService.java
private final RestTemplate ocrRestTemplate;  // ← Depend on infrastructure
private final ChatClient chatClient;         // ← Depend on infrastructure

// EasyOcrProviderAdapter.java — line 80-82
return callEasyOcrRequest(OcrService.EasyOcrRequest.fromBase64(job.imageBase64()));
//                        ^^^^^^^^^ Adapter depend on Service's inner DTO!
```

**Nghiêm trọng nhất**: `EasyOcrProviderAdapter` import `OcrService.EasyOcrRequest` — adapter phụ thuộc vào service chứa nó. Dependency **NGƯỢC**.

---

## 2. 🔌 Adapter/Provider Pattern — Replaceability Analysis

### 2.1 OCR Providers — ✅ Tốt (nhưng có vấn đề nhỏ)

```
OcrProvider (interface)
├── EasyOcrProviderAdapter    ← ✅ Spring @Component
├── TextractOcrProvider       ← ✅ Spring @Component  
├── GoogleCloudVisionOcrProvider ← ✅ Spring @Component
└── PdfTextOcrProvider        ← ✅ Spring @Component

OcrProviderRegistry           ← ✅ Routes by capability + config
```

**Vấn đề 1**: Provider implementations nằm **ngoài** `service/ocr/` package:
```
service/ocr/OcrProvider.java           ← Interface ở sub-package
service/ocr/OcrProviderRegistry.java   ← Registry ở sub-package
service/EasyOcrProviderAdapter.java    ← ❌ Implementation ở PARENT package
service/TextractOcrProvider.java       ← ❌ Implementation ở PARENT package
service/GoogleCloudVisionOcrProvider.java ← ❌
service/PdfTextOcrProvider.java        ← ❌
```

**Vấn đề 2**: `EasyOcrProviderAdapter` phụ thuộc `OcrService.EasyOcrRequest` — circular-like dependency.

### 2.2 LLM Providers — ❌ Không có abstraction

Hiện tại LLM fallback là **procedural code hardcoded** trong `OcrService`:
```
callPrimaryLlm() → chatClient.prompt()      ← Spring AI (Groq)
callOpenRouterFallback() → RestTemplate POST ← Manual HTTP
```

Không có interface, không swap được. Muốn thêm Gemini phải sửa `OcrService`.

### 2.3 Storage — ❌ Concrete S3 chỉ

Không interface. `StorageService` dùng `S3Client` trực tiếp. 16 services depend on `StorageService` concrete.

### 2.4 Email — ❌ No port/adapter

`EmailService` dùng `JavaMailSender` trực tiếp. Muốn đổi sang SendGrid/SES → rewrite toàn bộ.

---

## 3. 📊 Health Record Status — Cần State Machine

### 3.1 Current State: Magic Strings Everywhere

**88 chỗ** dùng hardcoded Vietnamese exception messages. **23 chỗ** so sánh status bằng raw strings:

```java
// Scattered across 6 files:
"processing"       → HealthRecordService, HealthRecord entity
"review_required"  → HealthRecordService (15 occurrences)
"done"             → HealthRecordService, ProfileService, AnalyticsService  
"ocr_failed"       → HealthRecordService, AnalyticsService, ProfileService
"failed"           → ProfileService (DIFFERENT from "ocr_failed"!)
```

### 3.2 Solution: Java Enum + State Machine

```java
public enum HealthRecordStatus {
    PROCESSING,
    REVIEW_REQUIRED,
    DONE,
    OCR_FAILED;

    private static final Map<HealthRecordStatus, Set<HealthRecordStatus>> TRANSITIONS = Map.of(
        PROCESSING,      Set.of(REVIEW_REQUIRED, OCR_FAILED),
        REVIEW_REQUIRED, Set.of(DONE, OCR_FAILED),
        OCR_FAILED,      Set.of(PROCESSING, DONE),  // retry or manual confirm
        DONE,            Set.of()                     // terminal
    );

    public boolean canTransitionTo(HealthRecordStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public HealthRecordStatus transitionTo(HealthRecordStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateTransitionException(this, target);
        }
        return target;
    }

    // DB value mapping
    @JsonValue
    public String toDbValue() {
        return name().toLowerCase();
    }
}
```

---

## 4. 🗺️ DTO Mapping — Manual Everywhere

### 4.1 Problem

Mapping hiện tại là **manual inline** trong service methods — ví dụ `getRecordsByProfile()` (40 dòng mapping):

```java
return new HealthRecordStatusResponse(
    record.getId(),
    isOwner,
    canEdit,
    record.getStatus(),
    metricsList,
    containsLowConfidenceMetrics(metricsList),
    resolveOcrFailureReason(record),
    record.getExamDate() != null ? record.getExamDate().toString() : null,
    record.getRecordType(),
    record.getHospitalName(),
    record.getDiagnosis(),
    record.getAnalyzerModel(),
    // ... 3 more fields
);
```

### 4.2 Solution: MapStruct or dedicated Mapper classes

```java
@Mapper(componentModel = "spring")
public interface HealthRecordMapper {
    @Mapping(target = "owner", source = "isOwner")
    @Mapping(target = "examDate", expression = "java(record.getExamDate() != null ? record.getExamDate().toString() : null)")
    HealthRecordStatusResponse toStatusResponse(HealthRecord record, boolean isOwner, boolean canEdit, ...);

    HealthRecordDetailResponse toDetailResponse(HealthRecord record, ...);
    HealthRecordHistoryItemResponse toHistoryItem(HealthRecord record, ...);
}
```

---

## 5. ⚠️ Exception Hierarchy — Cần cải thiện

### 5.1 Current: `IllegalArgumentException` for everything

**30+ chỗ** dùng `IllegalArgumentException` cho business errors — GlobalExceptionHandler map tất cả thành **400 Bad Request**:

```java
// Tất cả cùng exception type, khác nhau chỉ ở message string
throw new IllegalArgumentException("Kết quả khám không tồn tại");      // → 404
throw new IllegalArgumentException("Ngày nhắc là bắt buộc");           // → 400
throw new IllegalArgumentException("Tên chỉ số đã tồn tại");           // → 409
throw new IllegalArgumentException("Không thể đọc nội dung file CSV."); // → 422
```

**Vấn đề**: Phân biệt 404/400/409/422 dựa vào **nội dung Vietnamese string** → fragile.

### 5.2 Proposed Exception Hierarchy

```java
public abstract class HealthLensException extends RuntimeException {
    private final ApiErrorCode errorCode;
    private final HttpStatus httpStatus;
    // ...
}

// Specific exceptions:
class EntityNotFoundException extends HealthLensException { ... }    // 404
class DuplicateEntityException extends HealthLensException { ... }  // 409
class InvalidInputException extends HealthLensException { ... }     // 400
class InvalidStateException extends HealthLensException { ... }     // 409
class InfrastructureException extends HealthLensException { ... }   // 500
```

---

## 6. 🖥️ Frontend — Component Decomposition

### 6.1 Top 5 Oversized Components

| File | Lines | States | Proposed Split |
|------|-------|--------|---------------|
| `review/[recordId]/page.tsx` | **2,782** | 30+ `useState` | 7 sub-components + 3 hooks |
| `admin/reference-data/approvals/page.tsx` | **1,098** | — | 3 sub-components |
| `admin/reference-data/page.tsx` | **1,025** | — | 3 sub-components |
| `(dashboard)/home/page.tsx` | **744** | — | 4 feature sections |
| `admin/login/page.tsx` | **691** | — | 2 sub-components |

### 6.2 `ReviewRecordPage` Decomposition

```
apps/web/src/app/(dashboard)/health-records/review/[recordId]/
├── page.tsx                    ← Shell (< 100 lines, composes sub-components)
├── components/
│   ├── MetricsTable.tsx        ← Editable metrics table
│   ├── MetricExplanation.tsx   ← Explanation modal/drawer
│   ├── RecordHeader.tsx        ← Title, status badge, exam date
│   ├── RecordActions.tsx       ← Confirm, PDF, Share buttons
│   ├── RecommendationsPanel.tsx← Recommendations section
│   ├── DocumentPreview.tsx     ← Original document iframe/image
│   └── ShareDialog.tsx         ← Sharing invitation dialog
└── hooks/
    ├── useHealthRecordDetail.ts    ← fetches record + metrics
    ├── useMetricEditing.ts         ← edit/confirm metric state
    └── useRecordActions.ts         ← confirm, delete, share mutations
```

### 6.3 Missing Custom Hooks

Current `apps/web/src/hooks/` has only **7 hooks** — mostly auth-related. Domain hooks are **missing**:

```
Needed but missing:
├── useHealthRecordUpload.ts
├── useHealthRecordDetail.ts
├── useMetricExplanation.ts
├── useProfileList.ts
├── useFollowUpReminders.ts
└── useReferenceData.ts
```

### 6.4 Inline Types in Page Files

`review/page.tsx` defines **7 interfaces inline** (lines 72-178):

```typescript
// ❌ Defined inside page.tsx
interface MetricDto { ... }
interface ReviewRecordData { ... }
interface RecommendationsData { ... }
// ... 4 more
```

→ Nên move vào `@healthlens/shared/types/` hoặc `src/types/health-record.ts`.

---

## 7. 🔧 Concrete Actions — Priority Ordered

### Phase 1: Foundation (Tuần 1-2)

| # | Action | Impact | Effort |
|---|--------|--------|--------|
| 1 | **Create `HealthRecordStatus` enum** (Java) | Eliminate 23+ magic string comparisons | S |
| 2 | **Extract OCR inner DTOs** out of `OcrService` | Fix circular dependency | S |
| 3 | **Create `StoragePort` interface** | Enable storage swap | S |
| 4 | **Create `LlmProvider` interface** + extract `LlmProviderChain` | Enable LLM swap | M |
| 5 | **Create domain exception hierarchy** | Fix 400/404/409 ambiguity | M |

### Phase 2: Service Decomposition (Tuần 3-4)

| # | Action | Impact | Effort |
|---|--------|--------|--------|
| 6 | **Split `HealthRecordService`** into 5-6 focused services | SRP compliance | L |
| 7 | **Extract `OcrMetricExtractor`** from `OcrService` | SRP, testability | M |
| 8 | **Move all email HTML to Thymeleaf templates** | Consistency, maintainability | M |
| 9 | **Move OCR providers into `service/ocr/` package** | Package cohesion | S |

### Phase 3: Frontend Refactoring (Tuần 5-6)

| # | Action | Impact | Effort |
|---|--------|--------|--------|
| 10 | **Split `ReviewRecordPage`** into 7 components + 3 hooks | Readability, testability | L |
| 11 | **Extract shared types** from page files to `types/` | Single source of truth | S |
| 12 | **Create domain hooks** for health records, profiles | Reusability | M |
| 13 | **Merge mobile `src/` structure** | Consistency | S |

### Phase 4: Cross-Cutting (Tuần 7-8)

| # | Action | Impact | Effort |
|---|--------|--------|--------|
| 14 | **Sync status enum** between Java ↔ TypeScript shared | End-to-end type safety | S |
| 15 | **Remove `routes.ts` re-export wrapper** | Reduce indirection | S |
| 16 | **Fix file size constant mismatch** (20/10/30MB) | Correctness | S |
| 17 | **Wire `ErrorCode` system** end-to-end (shared → Java → API response) | Consistency | M |

---

## 8. 🏗️ Target Architecture Blueprint

### Backend: Package-by-Feature

```
com.healthlens.api/
├── common/
│   ├── exception/        ← HealthLensException hierarchy
│   ├── dto/              ← PaginationResponse, ApiResponse
│   ├── config/           ← Jackson, OpenAPI, Clock
│   └── security/         ← SecurityConfig, JWT, RateLimiters
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── dto/              ← LoginRequest, RegisterRequest, etc.
│   └── entity/           ← User, RefreshToken, PasswordResetToken
├── healthrecord/
│   ├── HealthRecordController.java
│   ├── HealthRecordUploadService.java
│   ├── HealthRecordQueryService.java
│   ├── HealthRecordConfirmService.java
│   ├── HealthRecordExportService.java
│   ├── HealthRecordAccessControl.java
│   ├── HealthRecordMapper.java
│   ├── HealthRecordStatus.java    ← Enum
│   ├── dto/
│   ├── entity/
│   └── repository/
├── ocr/
│   ├── OcrPipelineService.java
│   ├── OcrMetricExtractor.java
│   ├── OcrJobConsumer.java
│   ├── provider/
│   │   ├── OcrProvider.java       ← Interface
│   │   ├── OcrProviderRegistry.java
│   │   ├── EasyOcrAdapter.java
│   │   ├── TextractAdapter.java
│   │   └── GcvAdapter.java
│   └── dto/
├── llm/
│   ├── LlmProviderChain.java
│   ├── LlmProvider.java           ← Interface
│   ├── SpringAiLlmProvider.java
│   ├── OpenRouterLlmProvider.java
│   └── LlmService.java
├── storage/
│   ├── StoragePort.java            ← Interface
│   ├── MinioStorageAdapter.java
│   └── StorageBucketInitializer.java
├── email/
│   ├── EmailPort.java              ← Interface
│   ├── SmtpEmailAdapter.java
│   └── templates/                  ← Thymeleaf only
├── profile/
├── notification/
├── audit/
└── admin/
```

### Frontend: Feature-Sliced

```
apps/web/src/
├── app/                 ← Next.js routes (thin shells only)
├── features/
│   ├── health-records/
│   │   ├── components/  ← MetricsTable, RecordHeader, etc.
│   │   ├── hooks/       ← useHealthRecordDetail, useMetricEditing
│   │   └── types.ts
│   ├── profiles/
│   ├── auth/
│   └── admin/
├── shared/
│   ├── components/      ← Button, Modal, Badge (generic UI)
│   ├── hooks/           ← useAuth, useNotifications
│   ├── lib/             ← apiClient, formatters
│   └── types/
└── stores/
```

---

## 📊 Summary Metrics

| Category | Current | Target | Gap |
|----------|---------|--------|-----|
| Service interfaces | 1 (`OcrProvider`) | 5+ (`Storage`, `Email`, `Llm`, `OcrProvider`, service ports) | 4+ missing |
| God classes (>500 LoC) | 5 | 0 | -5 |
| God components (>700 LoC) | 6 | 0 | -6 |
| Magic status strings | 23 comparisons | 0 (enum) | -23 |
| Hardcoded exception messages | 88 | 0 (message codes) | -88 |
| Custom hooks | 7 | 15+ | 8+ missing |
| Test files | 87 Java + 22 TS | Should increase with better testability | — |
| Status enum sync | Disconnected (Java strings ≠ TS enum) | Single source of truth | 1 fix |
