/**
 * Horizontal layout shell aligned with the authenticated /home header.
 * Header bar: full-width with px-6; page content: centered max-w-7xl with px-6.
 */
export const authenticatedHeaderSurfaceClassName =
  "border-b border-[#bcc9c6]/20 bg-[#e9f6f3]/80 px-6 py-3 shadow-sm backdrop-blur-md print:hidden";

export const authenticatedHeaderInnerClassName = "flex items-center justify-between";

/** Offset below fixed AuthenticatedTopHeader (~72px: py-3 + 48px controls). */
export const authenticatedHeaderOffsetClass = "pt-[4.5rem]";

/** Sticky anchor for breadcrumb bars directly under the header. */
export const authenticatedStickyBelowHeaderClass = "sticky top-[4.5rem] z-20";

export const dashboardBreadcrumbBarSurfaceClassName =
  "border-b border-[#bcc9c6]/30 bg-[#effcf9]/95 shadow-[0_1px_0_rgba(18,30,28,0.04)] backdrop-blur-sm";

export const dashboardBreadcrumbNavClassName =
  "py-4 text-base font-medium leading-6 text-[#6d7a77]";

export const authenticatedContentShell = "mx-auto w-full max-w-7xl px-6";
