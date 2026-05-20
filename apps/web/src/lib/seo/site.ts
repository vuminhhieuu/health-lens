export const SITE_NAME = "HealthLens";

/** Canonical production origin; override with NEXT_PUBLIC_SITE_URL in deploy env. */
export const DEFAULT_SITE_URL = "https://healthlens.vn";

const DEV_SITE_URL = "http://localhost:3000";

export function getSiteUrl(): string {
  const configured = process.env.NEXT_PUBLIC_SITE_URL?.trim();
  if (configured && configured.length > 0) {
    return configured.replace(/\/$/, "");
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
