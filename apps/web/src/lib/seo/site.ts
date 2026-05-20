export const SITE_NAME = "HealthLens";

/** Canonical production origin; override with NEXT_PUBLIC_SITE_URL in deploy env. */
export const DEFAULT_SITE_URL = "https://healthlens.vn";

const DEV_SITE_URL = "http://localhost:3000";

/** Normalize env value to a fully-qualified http(s) origin (no trailing slash). */
export function normalizeSiteUrl(raw: string): string | null {
  const trimmed = raw.trim().replace(/\/$/, "");
  if (!trimmed) {
    return null;
  }

  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;

  try {
    const url = new URL(withScheme);
    if (url.protocol !== "http:" && url.protocol !== "https:") {
      return null;
    }
    return `${url.protocol}//${url.host}`;
  } catch {
    return null;
  }
}

export function getSiteUrl(): string {
  const configured = process.env.NEXT_PUBLIC_SITE_URL?.trim();
  if (configured) {
    const normalized = normalizeSiteUrl(configured);
    if (normalized) {
      return normalized;
    }
  }
  if (process.env.NODE_ENV === "development") {
    return DEV_SITE_URL;
  }
  return DEFAULT_SITE_URL;
}

/** Public marketing URLs included in sitemap.xml (/support redirects to /help). */
export const PUBLIC_SITEMAP_PATHS = ["/", "/privacy", "/terms", "/help"] as const;

/** Authenticated, admin, and auth flows — must not be indexed. */
export const ROBOTS_DISALLOW_PREFIXES = [
  "/home",
  "/admin",
  "/profiles",
  "/settings",
  "/health-records",
  "/follow-up-reminders",
  "/visit-summary",
  "/guide",
  "/login",
  "/register",
  "/forgot-password",
  "/reset-password",
  "/verify-email",
  "/invitations",
  "/health-record-invitations",
  "/cancel-deletion",
] as const;
