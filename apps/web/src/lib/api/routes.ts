/**
 * API Routes for Frontend
 * 
 * Imports from packages/shared/constants/api.ts (Single Source of Truth)
 * This file is kept for backward compatibility and custom frontend routes.
 * 
 * For new routes, add them to packages/shared/constants/api.ts instead.
 */

import { ApiPaths, API_TIMEOUT } from '@healthlens/shared/constants';

export { ApiPaths, API_TIMEOUT };

/**
 * Custom frontend-only routes (not shared with backend)
 */
export const FRONTEND_ROUTES = {
  HOME: '/',
  DASHBOARD: '/dashboard',
  LOGIN: '/login',
  REGISTER: '/register',
  VERIFY_EMAIL: '/verify-email',
  FORGOT_PASSWORD: '/forgot-password',
  PROFILE: '/profile',
  HEALTH_RECORDS: '/health-records',
  SETTINGS: '/settings',
} as const;

/**
 * Re-export specific paths for convenience
 */
export const API_ROUTES = {
  AUTH: ApiPaths.AUTH,
  DEV: ApiPaths.DEV,
  OCR: ApiPaths.OCR,
  HEALTH: ApiPaths.HEALTH,
  USERS: {
    ME: "/api/v1/users/me",
  },
} as const;
