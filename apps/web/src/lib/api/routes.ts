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
  ADMIN_LOGIN: '/admin/login',
  ADMIN_DASHBOARD: '/admin',
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
    ME: ApiPaths.USERS.ME,
    ME_AVATAR: ApiPaths.USERS.ME_AVATAR,
    ME_NOTIFICATION_PREFERENCES: ApiPaths.USERS.ME_NOTIFICATION_PREFERENCES,
    ME_TOTP: ApiPaths.USERS.ME_TOTP,
    ME_TOTP_SETUP: ApiPaths.USERS.ME_TOTP_SETUP,
    ME_TOTP_VERIFY: ApiPaths.USERS.ME_TOTP_VERIFY,
    ME_HEALTH_CONTEXT: ApiPaths.USERS.ME_HEALTH_CONTEXT,
    DELETION_REQUEST: ApiPaths.USERS.DELETION_REQUEST,
    /** Public cancel (no JWT): `DELETE` + query `token` from email — path is `/users/deletion-requests/...`, not `/users/me/...`. */
    CANCEL_DELETION: ApiPaths.USERS.CANCEL_DELETION,
  },
  PROFILES: ApiPaths.PROFILES,
  NOTIFICATIONS: ApiPaths.NOTIFICATIONS,
  ADMIN_AUTH: ApiPaths.ADMIN_AUTH,
  ADMIN_ANALYTICS: ApiPaths.ADMIN_ANALYTICS,
  ADMIN_REFERENCE_DATA: ApiPaths.ADMIN_REFERENCE_DATA,
} as const;
