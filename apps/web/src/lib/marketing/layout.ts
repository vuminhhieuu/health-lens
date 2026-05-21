import {
  authenticatedContentShell,
  authenticatedHeaderInnerClassName,
} from "@/lib/layout/shell";

/** Public marketing pages share the same horizontal shell as /home (max-w-7xl + px-6). */
export const marketingShell = authenticatedContentShell;

/** Guest marketing header — same px-6 edge padding as authenticated /home header. */
export const publicMarketingHeaderClassName =
  "sticky top-0 z-50 w-full border-b border-[#bcc9c6]/50 bg-[#f6fbfa]/90 px-6 py-3 backdrop-blur-md";

export const publicMarketingHeaderInnerClassName = authenticatedHeaderInnerClassName;

export const marketingBand = {
  mint: "w-full bg-[#f6fbfa]",
  white: "w-full bg-white",
  mintDeep: "w-full bg-[#effcf9]",
  teal: "w-full bg-[#00685f] text-white",
} as const;
