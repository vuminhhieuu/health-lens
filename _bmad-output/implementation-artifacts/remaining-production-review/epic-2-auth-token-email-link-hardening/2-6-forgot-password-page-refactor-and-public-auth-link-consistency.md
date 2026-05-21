# Story 2.6: Forgot Password Page Refactor And Public Auth Link Consistency

Status: done

## Execution Scope

**Phase:** Remaining production review / auth UX hardening  
**Area:** Forgot password page, login footer links, public legal/help routes, auth user consistency  
**Priority:** P2

## Story

As a user recovering access to my account,  
I want the forgot-password flow to be visually and behaviorally consistent with login and public support links,  
so that I can recover safely and reach privacy, terms, and help information without dead links.

## Acceptance Criteria

1. Forgot-password page uses the same auth-page layout conventions as login/register: stable header, restrained card radius, no decorative background blobs, consistent button/input sizing, and no layout shift between form and success state.
2. Forgot-password submission keeps enumeration-safe behavior while giving clear, accessible status text for success, network failure, generic server failure, and `429` rate limit.
3. The forgot-password flow does not desynchronize user/auth state: unauthenticated users remain unauthenticated, existing logged-in state is not mutated, and any post-reset/login guidance returns to the correct public auth route.
4. Login footer links `Quy định bảo mật`, `Điều khoản sử dụng`, and `Trợ giúp` point to real routes instead of `#`; forgot-password uses the same link map and labels.
5. Public legal/help routes exist or links target existing routes, with safe behavior for unauthenticated users. Dashboard-only `/help` must not be linked directly from public auth pages if it redirects to login.
6. Header help icons on login, register, forgot-password, and reset-password either navigate to the same public help route or are removed if no public support route exists.
7. Tests cover link hrefs, forgot-password success/error/rate-limit states, accessible live regions, and absence of broken `#`/missing-route links on public auth pages.

## Tasks / Subtasks

- [x] Task 1 - Define a shared public auth link map (AC: #4-#6)
  - [x] Add constants/helper for public auth footer/header links such as `/privacy`, `/terms`, and `/help` or `/support`.
  - [x] Verify each target route exists; create minimal public pages if product wants public legal/help pages.
  - [x] Do not point unauthenticated auth pages at dashboard-only `/help` unless that route is made public.
- [x] Task 2 - Refactor login footer and auth header help actions (AC: #4, #6, #7)
  - [x] Replace `href="#"` in `apps/web/src/app/(auth)/login/page.tsx`.
  - [x] Make `CircleHelp` controls actual `Link`s or accessible buttons with implemented navigation.
  - [x] Keep Vietnamese labels exactly aligned: `Quy định bảo mật`, `Điều khoản sử dụng`, `Trợ giúp`.
- [x] Task 3 - Refactor forgot-password page for consistency and accessibility (AC: #1-#3)
  - [x] Remove decorative fixed blobs and oversized/rounded visual treatment that diverges from login/register.
  - [x] Keep form and success state within a stable auth layout.
  - [x] Add `role="status"` / `aria-live` for pending and success, and `role="alert"` for errors.
  - [x] Ensure resend-request path preserves entered email when possible and does not mutate auth store.
- [x] Task 4 - Align forgot-password error semantics with backend contract (AC: #2, #3)
  - [x] Continue enumeration-safe success copy for existing and non-existing emails.
  - [x] Map `429` using `retryAfterMinutes`; map unknown server failure to generic retry copy.
  - [x] Avoid exposing whether the email exists.
- [x] Task 5 - Add focused tests and route smoke checks (AC: #1-#7)
  - [x] Test login/footer links are not `#` and target existing public routes.
  - [x] Test forgot-password success, network error, generic server error, `429`, and resend/edit-email behavior.
  - [x] Test public auth pages do not contain missing `/contact` links unless `/contact` is implemented.

### Review Findings

- [x] [Review][Patch] Login footer legal/help links are visible but use `href="#"`, so production users cannot access privacy, terms, or support content.
- [x] [Review][Patch] Forgot-password currently links to `/privacy`, `/terms`, and `/contact`, but only `/terms` directory exists and it has no page file; `/privacy` and `/contact` are missing.
- [x] [Review][UX] Auth header help icons on login/register/forgot/reset render as buttons without implemented action.
- [x] [Review][UX] Forgot-password visual treatment diverges from the login/register auth layout and includes decorative background blobs that the frontend guidance discourages.

#### Code review (2026-05-21)

- [x] [Review][Patch] Trùng nội dung thành công trên forgot-password (`FORGOT_SUCCESS_COPY` + `messageCatalog.auth.forgotPasswordSent`) — gây lặp cho người dùng và screen reader [`apps/web/src/app/(auth)/forgot-password/page.tsx:19-97`]
- [x] [Review][Patch] Sau "gửi lại yêu cầu" không focus lại ô email — keyboard/SR dễ mất ngữ cảnh [`apps/web/src/app/(auth)/forgot-password/page.tsx:64-70`]
- [x] [Review][Patch] Smoke test chỉ tìm chuỗi `footer` — dễ false positive, nên assert prop `footer` trên `AuthPageShell` [`apps/web/src/app/(auth)/public-auth-pages.test.ts:40`]
- [x] [Review][Defer] Nút ngôn ngữ trên login vẫn không có hành động — tồn tại trước story này [`apps/web/src/app/(auth)/login/page.tsx:129-135`]

## Dev Notes

### Dedup — Public Account Experience epic

- **Không** tạo story PAE riêng cho auth link map — story này là canonical owner.
- **`pae-5-public-privacy-and-terms-pages`** nên hoàn thành trước (hoặc song song) để `/privacy` và `/terms` tồn tại khi wire footer links.
- Public `/support` FAQ: implement trong story này (Task 1), không trùng `pae-*`.

### Implementation Guardrails

- Keep public auth pages safe for unauthenticated users.
- Do not introduce email enumeration in either frontend copy or backend error mapping.
- Prefer a small shared component/helper for auth public links only if it reduces real duplication across login/forgot/register/reset.
- If creating public legal/help pages, keep content factual and concise; do not invent binding legal text beyond product-approved placeholder copy.
- Do not alter token reset backend behavior in this story unless required for frontend consistency.

### Current Code Intelligence

- `apps/web/src/app/(auth)/login/page.tsx` footer links currently use `href="#"` for `Quy định bảo mật`, `Điều khoản sử dụng`, and `Trợ giúp`.
- `apps/web/src/app/(auth)/forgot-password/page.tsx` footer links target `/privacy`, `/terms`, and `/contact`; these routes are not all implemented.
- `apps/web/src/app/terms/` exists as an empty directory with no `page.tsx`.
- `apps/web/src/app/(dashboard)/help/page.tsx` exists but lives inside the authenticated dashboard group.
- `apps/web/src/lib/i18n/messages.ts` already includes forgot-password success and rate-limit copy.
- `apps/api/src/main/java/com/healthlens/api/controller/AuthController.java` exposes `/api/v1/auth/forgot-password`.
- `apps/api/src/test/java/com/healthlens/api/service/AuthServiceTest.java` already covers forgot-password user-exists, user-not-found enumeration-safe success, and rate-limit behavior.

### Likely Files

- `apps/web/src/app/(auth)/login/page.tsx`
- `apps/web/src/app/(auth)/forgot-password/page.tsx`
- `apps/web/src/app/(auth)/register/page.tsx`
- `apps/web/src/app/(auth)/reset-password/page.tsx`
- `apps/web/src/app/privacy/page.tsx`
- `apps/web/src/app/terms/page.tsx`
- `apps/web/src/app/help/page.tsx` or `apps/web/src/app/support/page.tsx`
- `apps/web/src/lib/i18n/messages.ts`
- `apps/web/src/lib/authPublicLinks.ts` or a similarly scoped helper

### References

- `_bmad-output/implementation-artifacts/epic-1/1-4-password-reset-via-email.md`
- `_bmad-output/implementation-artifacts/remaining-production-review/epic-2-auth-token-email-link-hardening/2-4-public-auth-endpoint-rate-limit-and-error-semantics.md`
- `_bmad-output/implementation-artifacts/epic-10/10-1-help-and-questions-pages.md`
- `apps/web/src/app/(auth)/login/page.tsx`
- `apps/web/src/app/(auth)/forgot-password/page.tsx`

## Dev Agent Record

### Agent Model Used

Composer

### Debug Log References

- `pnpm test` in `apps/web` — 53 tests passed

### Completion Notes List

- Added `authPublicLinks` map targeting public marketing routes `/privacy`, `/terms`, `/help` (not dashboard `/help`).
- Introduced `AuthPageHeader`, `AuthPageFooter`, and `AuthPageShell` for consistent auth chrome across login, register, forgot-password, and reset-password.
- Refactored forgot-password to login-aligned card layout, removed decorative blobs, unified form/success in one shell, and added accessible status/alert regions plus email preservation on resend.
- Wired header help icons as links to `/help`; replaced login `href="#"` footer placeholders and removed `/contact` from forgot-password.
- Added source smoke tests and RTL tests for forgot-password success, network/server/429 errors, and resend behavior.

### File List

- `apps/web/src/lib/authPublicLinks.ts`
- `apps/web/src/lib/authPublicLinks.test.ts`
- `apps/web/src/components/auth/AuthPageHeader.tsx`
- `apps/web/src/components/auth/AuthPageFooter.tsx`
- `apps/web/src/components/auth/AuthPageShell.tsx`
- `apps/web/src/app/(auth)/login/page.tsx`
- `apps/web/src/app/(auth)/register/page.tsx`
- `apps/web/src/app/(auth)/forgot-password/page.tsx`
- `apps/web/src/app/(auth)/reset-password/page.tsx`
- `apps/web/src/app/(auth)/public-auth-pages.test.ts`
- `apps/web/src/app/(auth)/forgot-password/forgot-password-page.test.tsx`

### Change Log

- 2026-05-19: Created story for forgot-password auth-page refactor and public legal/help link consistency.
- 2026-05-21: Implemented shared auth public links, auth page shell, forgot-password UX/accessibility alignment, and tests.
- 2026-05-21: Code review batch-fix — deduplicated success copy, resend email focus, stricter footer smoke test.
