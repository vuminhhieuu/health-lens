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
  DELETE_ACCOUNT: '/settings/delete-account',
  CANCEL_DELETION: '/cancel-deletion',
  EMAIL_PREVIEW_DELETION_REQUEST: '/email-previews/deletion-request',
} as const;

/**
 * Re-export specific paths for convenience
 */
export const API_ROUTES = {
  AUTH: ApiPaths.AUTH,
  CONSENT: ApiPaths.CONSENT,
  DEV: ApiPaths.DEV,
  OCR: ApiPaths.OCR,
  HEALTH: ApiPaths.HEALTH,
  HEALTH_RECORDS: ApiPaths.HEALTH_RECORDS,
  USERS: {
    ME: "/api/v1/users/me",
    DELETION_REQUEST: "/api/v1/users/me/deletion-request",
    CANCEL_DELETION: "/api/v1/users/deletion-requests/cancel"
  },
  PROFILES: ApiPaths.PROFILES,
} as const;
