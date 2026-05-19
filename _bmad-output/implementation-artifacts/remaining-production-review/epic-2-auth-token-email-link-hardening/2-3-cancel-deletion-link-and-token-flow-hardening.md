# Story 2.3: Cancel Deletion Link And Token Flow Hardening

Status: done

## Execution Scope

**Phase:** Remaining production review / account deletion safety  
**Area:** Cancel deletion email link, frontend state mapping, backend token validation  
**Priority:** P0/P1

## Story

As a user cancelling account deletion,  
I want cancellation to be secure and accurately explained,  
so that I do not lose data because of URL leaks or client-side expiry bugs.

## Acceptance Criteria

1. Email and sensitive token values are not exposed in durable URLs, logs, or browser history beyond unavoidable one-time link constraints.
2. Backend remains source of truth for token expiry; client-side expiry never blocks a valid backend cancellation.
3. `409`, `401`, `403`, `429`, and `500` outcomes map to distinct frontend states.
4. Token normalization matches backend encoding behavior.
5. Cancellation flow has tests for happy path, expired token, replay, 409, 429, 500, missing token, loading, and double-submit.

## Tasks / Subtasks

- [x] Task 1 - Audit current cancel-deletion URL and token handling (AC: #1, #4)
- [x] Task 2 - Move expiry authority to backend result (AC: #2)
- [x] Task 3 - Implement distinct frontend state mapping (AC: #3)
- [x] Task 4 - Add double-submit and replay protection UX (AC: #3, #5)
- [x] Task 5 - Add backend/frontend tests for listed outcomes (AC: #1-#5)

### Review Findings

- [x] [Review][Patch] Remove raw email from durable audit payloads [apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java:171]
- [x] [Review][Patch] Make backend cancellation replay-safe under concurrent requests [apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java:196]
- [x] [Review][Patch] Guard cancellation against scheduler/deletion deadline races [apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java:204]
- [x] [Review][Patch] Stop rendering untrusted or fabricated email identity before backend confirmation [apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx:112]
- [x] [Review][Patch] Normalize missing-token API responses through cancellation token semantics [apps/api/src/main/java/com/healthlens/api/controller/UserController.java:83]
- [x] [Review][Patch] Add explicit success and loading coverage for cancellation UI [apps/web/src/app/cancel-deletion/CancelDeletionClient.test.tsx:57] — removed afterward per requester; validate UI manually.

## Dev Notes

### Implementation Guardrails

- Never reject a token solely because the browser computed expiry locally.
- Remove sensitive query params from history as early as practical.
- Do not log full email or raw cancellation token.

### Likely Files

- `apps/web/src/app/(auth)/*cancel*`
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java`
- `apps/api/src/main/java/com/healthlens/api/service/AccountDeletionService.java`
- `apps/api/src/test/java/com/healthlens/api/*`

### References

- `_bmad-output/planning-artifacts/remaining-production-review-epics-and-stories.md` Story 2.3
- `_bmad-output/planning-artifacts/review-source/REVIEW-PRODUCTION-MASTER.md`

## Dev Agent Record

### Agent Model Used

GPT-5 Codex

### Debug Log References

- `cd apps/api && ./gradlew test --tests com.healthlens.api.service.DataDeletionServiceTest --tests com.healthlens.api.exception.GlobalExceptionHandlerTest`
- `cd apps/web && pnpm test`
- `cd apps/api && ./gradlew test`

### Completion Notes List

- Đã bỏ email khỏi cancellation link backend và giữ token chỉ trong state tạm thời của trang trước khi xóa query params khỏi browser history.
- Đã bỏ chặn hết hạn ở frontend dựa trên timestamp URL; backend là nguồn quyết định token còn hiệu lực hay không.
- Đã phân loại lỗi hủy xóa thành trạng thái UI riêng cho missing token, replay/conflict `409`, expired/invalid `401`, forbidden `403`, rate limit `429`, server `500`, và network.
- Đã thêm guard double-submit để chỉ gửi một request hủy xóa khi thao tác đang chạy.
- Đã thêm test backend cho link không chứa email, exception/status mapping, missing-token delegation, audit payload, và locked lookup behavior.
- Đã xử lý code review: audit payload không chứa raw email, cancel/delete cùng khóa row để giảm race, missing token đi qua cancellation semantics, UI không tin email query, và test success/loading được bổ sung.
- File test frontend cho trang cancel-deletion đã được xóa theo yêu cầu sau review; luồng UI cần được validate thủ công.

### File List

- apps/api/src/main/java/com/healthlens/api/exception/DeletionCancellationConflictException.java
- apps/api/src/main/java/com/healthlens/api/exception/DeletionCancellationForbiddenException.java
- apps/api/src/main/java/com/healthlens/api/exception/DeletionCancellationTokenException.java
- apps/api/src/main/java/com/healthlens/api/exception/GlobalExceptionHandler.java
- apps/api/src/main/java/com/healthlens/api/controller/UserController.java
- apps/api/src/main/java/com/healthlens/api/repository/DataDeletionRequestRepository.java
- apps/api/src/main/java/com/healthlens/api/service/DataDeletionService.java
- apps/api/src/test/java/com/healthlens/api/controller/UserControllerTest.java
- apps/api/src/test/java/com/healthlens/api/exception/GlobalExceptionHandlerTest.java
- apps/api/src/test/java/com/healthlens/api/service/DataDeletionServiceTest.java
- apps/web/src/app/cancel-deletion/CancelDeletionClient.tsx
- _bmad-output/implementation-artifacts/remaining-production-review/epic-2-auth-token-email-link-hardening/2-3-cancel-deletion-link-and-token-flow-hardening.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-05-19: Hardened cancel-deletion token flow, URL hygiene, frontend state mapping, replay/double-submit behavior, and tests.
- 2026-05-19: Addressed code review findings for audit privacy, cancellation race hardening, missing-token semantics, trusted email display, and backend guardrail coverage.
- 2026-05-19: Removed cancel-deletion frontend test file per requester.
