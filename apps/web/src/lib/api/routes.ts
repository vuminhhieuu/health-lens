/**
 * Central registry of all API route paths (frontend).
 * Mirrors backend ApiRoutes.java for consistency.
 */
export const API_ROUTES = {
  // Auth
  AUTH: {
    REGISTER: "/api/v1/auth/register",
    LOGIN: "/api/v1/auth/login",
    REFRESH: "/api/v1/auth/refresh",
    LOGOUT: "/api/v1/auth/logout",
  },

  // Dev (dev only)
  DEV: {
    VERIFY_EMAIL: (email: string) => `/api/v1/dev/verify-email/${email}`,
  },
} as const;
