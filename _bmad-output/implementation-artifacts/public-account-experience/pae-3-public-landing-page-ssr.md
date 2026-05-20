# Story 3: Public Landing Page SSR

Status: done

## Execution Scope

**Epic:** Public Account Experience | **Priority:** P1 | **Depends on:** `pae-2`

## Story

As a prospective user,  
I want a clear Vietnamese landing page explaining HealthLens,  
so that I understand the product before registering.

## Acceptance Criteria

1. Sections: hero, how-it-works (3 steps), trust/compliance (NĐ 13, medical disclaimer), feature highlights, CTAs (Đăng ký / Đăng nhập).
2. Server-rendered (RSC/SSR); no auth required.
3. Mobile-responsive; touch-friendly CTAs.
4. Teal brand aligned with auth pages; no decorative background blobs (per auth hardening guidance).
5. No fake patient metrics or misleading demo health data.

## Tasks / Subtasks

- [x] Implement `apps/web/src/app/(marketing)/page.tsx` (or root marketing page).
- [x] Reuse design tokens / Tailwind classes from auth dashboard palette.
- [x] Link CTAs to `/register`, `/login`.
- [x] Footer links to `/privacy`, `/terms`, `/support` (routes from `pae-5` + `remaining-2-6`).
- [x] Optional: snapshot/visual test for critical sections.

### Review Findings

- [x] [Review][Decision] Inline CTA (`LandingGuestCtas`) vs chính sách pae-2 — Resolved A: giữ CTA inline cho khách (hero + band); user đã đăng nhập dùng header. Cập nhật ghi chú pae-2.

- [x] [Review][Patch] Ghi chú minh họa trong `aria-hidden` — Resolved: chuyển sang `<figcaption>` ngoài vùng ẩn.

- [x] [Review][Patch] CTA hero CLS khi bootstrap — Resolved: skeleton `landing-guest-ctas-skeleton` thay `null` lúc loading.

- [x] [Review][Patch] Band «Sẵn sàng bắt đầu?» trống khi đã login — Resolved: tách `LandingGuestClosingBand`, ẩn cả section khi authenticated.

- [x] [Review][Patch] `/support` thiếu metadata — Resolved: `metadata` + `permanentRedirect("/help")`.

- [x] [Review][Defer] Test landing chỉ assert chuỗi trong file nguồn [`marketing-landing.test.ts`] — Cùng pattern `routing-policy.test.ts`; không bắt regression render/runtime.

- [x] [Review][Defer] Footer trùng `/support` và `/help` — Cả hai link cùng đích sau redirect; có thể gọn sau khi có trang support thật.

## Dev Notes

- PRD: SSR landing + mobile-responsive landing.
- Stitch: Display/hero specs in `STITCH-AI-SPECIFICATIONS.md` (Phase 2+ reference).

### Likely Files

- `apps/web/src/app/(marketing)/page.tsx`
- `apps/web/src/components/marketing/` (optional extract)

## Dev Agent Record

### Agent Model Used

Composer

### Implementation Plan

- Mở rộng `MarketingHome` (RSC) với hero, 3 bước, tin cậy (NĐ 13 + disclaimer), điểm nổi bật, band CTA cuối trang.
- Tách `LandingGuestCtas` (client) để hiện Đăng ký/Đăng nhập chỉ khi chưa đăng nhập — phù hợp quyết định pae-2 (header cho auth, CTA inline cho khách).
- Thay minh họa hero bằng wireframe trừu tượng, ghi chú “không phải dữ liệu bệnh nhân thật”.
- Thêm `/support` redirect → `/help`; footer có `/privacy`, `/terms`, `/support`.
- Test nguồn: `marketing-landing.test.ts` + cập nhật `routing-policy.test.ts`.

### Completion Notes

- Landing `/` đủ 5 AC: nội dung tiếng Việt, teal palette `#00685f`/`#f6fbfa`, responsive grid, không blob trang trí.
- `MarketingHome` không có `"use client"`; CTAs qua `LandingGuestCtas` + `LandingGuestClosingBand`.
- Code review: decision A (guest inline CTA); patch a11y figcaption, CTA skeleton, ẩn closing band khi auth, support metadata + 308.
- `pnpm test` pass sau review fixes.

### File List

- apps/web/src/app/(marketing)/MarketingHome.tsx
- apps/web/src/app/(marketing)/marketing-landing.test.ts
- apps/web/src/app/(marketing)/support/page.tsx
- apps/web/src/components/marketing/LandingGuestCtas.tsx
- apps/web/src/components/marketing/LandingGuestClosingBand.tsx
- apps/web/src/components/marketing/MarketingFooter.tsx
- apps/web/src/components/features/consent/ConsentModal.tsx
- apps/web/src/app/routing-policy.test.ts
- _bmad-output/implementation-artifacts/public-account-experience/pae-3-public-landing-page-ssr.md
- _bmad-output/implementation-artifacts/sprint-status.yaml

### Change Log

- 2026-05-20: Implemented full public landing SSR with trust sections, guest CTAs, support route, and guardrail tests.
- 2026-05-20: Code review fixes — figcaption a11y, CTA skeleton, guest-only closing band, support permanent redirect + metadata.
