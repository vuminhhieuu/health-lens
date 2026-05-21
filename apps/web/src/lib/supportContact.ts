/**
 * Public support + contact for authenticated settings and marketing surfaces.
 * Re-exports marketing contact constants; route targets align with `remaining-2-6` (/help).
 */
export { marketingContact as supportContact } from "@/lib/marketing/contact";

/** Canonical public help route ( `/support` permanent-redirects here ). */
export const PUBLIC_SUPPORT_HREF = "/help" as const;
