# Phase 4 — Domain Exceptions & Error Codes

> **Plan**: [INTEGRATED_MASTER_PLAN.md § Phase 4](../INTEGRATED_MASTER_PLAN.md) | **Effort**: 10h
> **Goal**: Replace 30+ `IllegalArgumentException` với domain hierarchy → đúng HTTP status code + i18n-ready.

---

## 📊 Status

- **Status**: 🔴 Not Started
- **Branch**: `phase/04-exceptions`
- **PR(s)**: —

---

## ✅ Pre-flight Checklist

- [ ] Phase 3 `🟢 Done`
- [ ] Đã đọc `deep_code_review.md` §5
- [ ] Đã đọc `project_review.md` §6.1
- [ ] Branch: `git checkout -b phase/04-exceptions main`

---

## 🎯 Tasks

### 4.1 Exception Hierarchy (2h)
- [ ] Tạo `apps/api/src/main/java/com/healthlens/api/common/exception/`:
  - `HealthLensException.java` (abstract, `ApiErrorCode` + `HttpStatus`)
  - `EntityNotFoundException.java` → 404
  - `DuplicateEntityException.java` → 409
  - `InvalidInputException.java` → 400
  - `InvalidStateException.java` → 409
  - `InfrastructureException.java` → 500
- [ ] Each exception nhận `ApiErrorCode` enum param
- [ ] Tests

### 4.2 GlobalExceptionHandler Update (2h)
- [ ] Handler `@ExceptionHandler(HealthLensException.class)`:
  - Map exception.httpStatus → ProblemDetail
  - Set `code` = `ApiErrorCode.name()`
  - Set `detail` = i18n message từ messageSource
- [ ] Keep existing handlers cho Spring exceptions
- [ ] Test `GlobalExceptionHandlerTest.java`

### 4.3 Replace IllegalArgumentException (3h)
- [ ] Search: `grep -rn "throw new IllegalArgumentException" apps/api/src/main/java/`
- [ ] Categorize:
  - Not found → `EntityNotFoundException`
  - Duplicate → `DuplicateEntityException`
  - Bad input → `InvalidInputException`
  - Bad state → `InvalidStateException`
- [ ] Replace + correct `ApiErrorCode`
- [ ] Update tests expect right type + status
- **Estimated**: ~30 replacements

### 4.4 Wire ErrorCode End-to-End (2h)
- [ ] Verify `packages/shared/constants/error-codes.ts` đủ codes
- [ ] Mirror `apps/api/.../exception/ApiErrorCode.java` enum
- [ ] GlobalExceptionHandler set `code` field
- [ ] Web `apiClient.ts` parse error code
- [ ] Error toasts dùng code-specific message
- [ ] Test: API trả ErrorCode đúng

### 4.5 i18n Message Bundle (1h)
- [ ] Tạo `apps/api/src/main/resources/messages_vi.properties`
- [ ] Extract 88 hardcoded Vietnamese strings (ưu tiên auth + healthrecord)
- [ ] Inject `MessageSource` vào exception handlers
- [ ] Lookup by `ApiErrorCode.name()`
- [ ] Add `LocaleResolver` config (single-locale `vi`)

---

## 🧪 Verification

```bash
# No IllegalArgumentException for business
grep -rn "throw new IllegalArgumentException" apps/api/src/main/java/service/
# Expect: 0 hoặc validation-related only

# Exception → correct status
./gradlew test --tests "*ExceptionHandler*"

# Error code in response
curl -X POST /api/auth/register -d '{"email":"existing@x.com"}'
# Expect: 409 { code: "AUTH_EMAIL_EXISTS", ... }

# i18n
curl -H "Accept-Language: vi" .../error-endpoint
# Expect: Vietnamese detail
```

---

## 🏁 Phase Completion Checklist

- [ ] 5 sub-task `[x]`
- [ ] 6 exception classes defined + tested
- [ ] GlobalExceptionHandler updated
- [ ] 30+ IllegalArgumentException replaced
- [ ] ErrorCode wired end-to-end
- [ ] i18n bundle scaffolded
- [ ] Tests xanh
- [ ] Manual: 400/404/409 đúng
- [ ] Update DASHBOARD

---

## 📝 Retrospective

| Metric | Before | After |
|--------|:-:|:-:|
| IllegalArgumentException | 30+ | — |
| HTTP status accuracy | Mostly 400 | — |
| Hardcoded VN strings | 88 | — |

### Effort
| Task | Est | Actual |
|------|:-:|:-:|
| 4.1 Hierarchy | 2h | — |
| 4.2 Handler | 2h | — |
| 4.3 Replace | 3h | — |
| 4.4 Wire | 2h | — |
| 4.5 i18n | 1h | — |
| **Total** | **10h** | — |
