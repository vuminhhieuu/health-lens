/** Public routes safe for unauthenticated auth pages (not dashboard /help). */
export const AUTH_PUBLIC_HELP_HREF = "/help" as const;

export const AUTH_PUBLIC_FOOTER_LINKS = [
  { href: "/privacy", label: "Quy định bảo mật" },
  { href: "/terms", label: "Điều khoản sử dụng" },
  { href: AUTH_PUBLIC_HELP_HREF, label: "Trợ giúp" },
] as const;

export type AuthPublicFooterLink = (typeof AUTH_PUBLIC_FOOTER_LINKS)[number];

export function authPublicFooterHrefs(): string[] {
  return AUTH_PUBLIC_FOOTER_LINKS.map((link) => link.href);
}
