/**
 * API Constants for HealthLens
 * 
 * Single Source of Truth cho tất cả API routes phía frontend.
 * Sử dụng Google Resource Naming Convention.
 * 
 * @see https://cloud.google.com/apis/design/resource_names
 */

export const API_VERSION = 'v1' as const;

/**
 * API Base Paths
 */
export const ApiPaths = {
  /** Authentication endpoints */
  AUTH: {
    BASE: `/api/${API_VERSION}/auth`,
    REGISTER: `/api/${API_VERSION}/auth/register`,
    LOGIN: `/api/${API_VERSION}/auth/login`,
    CSRF: `/api/${API_VERSION}/auth/csrf`,
    VERIFY_EMAIL: `/api/${API_VERSION}/auth/verify-email`,
    REFRESH: `/api/${API_VERSION}/auth/refresh`,
    LOGOUT: `/api/${API_VERSION}/auth/logout`,
    CHANGE_PASSWORD: `/api/${API_VERSION}/auth/change-password`,
    FORGOT_PASSWORD: `/api/${API_VERSION}/auth/forgot-password`,
    RESET_PASSWORD: `/api/${API_VERSION}/auth/reset-password`,
  },

  /** Consent (requires authenticated session for all routes below) */
  CONSENT: {
    ME: `/api/${API_VERSION}/users/me/consent`,
    ME_ACTIVE_VERSION: `/api/${API_VERSION}/users/me/consent/active-version`,
  },

  /** Dev endpoints (dev profile only) */
  DEV: {
    BASE: `/api/${API_VERSION}/dev`,
    VERIFY_EMAIL: (email: string) => `/api/${API_VERSION}/dev/verify-email/${email}`,
  },

  /** User/Profile management endpoints */
  USERS: {
    ME: `/api/${API_VERSION}/users/me`,
    ME_AVATAR: `/api/${API_VERSION}/users/me/avatar`,
    DELETION_REQUEST: `/api/${API_VERSION}/users/me/deletion-request`,
    CANCEL_DELETION: `/api/${API_VERSION}/users/deletion-requests/cancel`,
  },

  /** Profile management endpoints */
  PROFILES: {
    BASE: `/api/${API_VERSION}/profiles`,
    LIST: `/api/${API_VERSION}/profiles`,
    GET: (id: string) => `/api/${API_VERSION}/profiles/${id}`,
    CREATE: `/api/${API_VERSION}/profiles`,
    /** Tạo hồ sơ mặc định từ thông tin user nếu chưa có hồ sơ nào (idempotent). */
    ENSURE_DEFAULT: `/api/${API_VERSION}/profiles/ensure-default`,
    UPDATE: (id: string) => `/api/${API_VERSION}/profiles/${id}`,
    DELETE: (id: string) => `/api/${API_VERSION}/profiles/${id}`,
    SET_DEFAULT: (id: string) => `/api/${API_VERSION}/profiles/${id}/set-default`,
    HEALTH_RECORDS: (id: string) => `/api/${API_VERSION}/profiles/${id}/health-records`,
    INVITATIONS: (id: string) => `/api/${API_VERSION}/profiles/${id}/invitations`,
    INVITATION_BY_ID: (profileId: string, invitationId: string) =>
      `/api/${API_VERSION}/profiles/${profileId}/invitations/${invitationId}`,
    REVOKE_SHARE: (profileId: string, viewerId: string) =>
      `/api/${API_VERSION}/profiles/${profileId}/shares/${viewerId}`,
    INVITATION_RESEND: (profileId: string, invitationId: string) =>
      `/api/${API_VERSION}/profiles/${profileId}/invitations/${invitationId}/resend`,
    SHARE_BY_VIEWER: (profileId: string, viewerId: string) =>
      `/api/${API_VERSION}/profiles/${profileId}/shares/${viewerId}`,
    FOLLOW_UP_REMINDERS: (profileId: string) =>
      `/api/${API_VERSION}/profiles/${profileId}/follow-up-reminders`,
    FOLLOW_UP_REMINDER_BY_ID: (profileId: string, reminderId: string) =>
      `/api/${API_VERSION}/profiles/${profileId}/follow-up-reminders/${reminderId}`,
  },

  INVITATIONS: {
    ACCEPT: (token: string) => `/api/${API_VERSION}/invitations/accept?token=${encodeURIComponent(token)}`,
    /** Lời mời pending gửi đến email của user đang đăng nhập (thông báo trong app) */
    INCOMING: `/api/${API_VERSION}/invitations/incoming`,
    REJECT: (invitationId: string) => `/api/${API_VERSION}/invitations/${invitationId}/reject`,
  },

  SHARED_PROFILES: {
    LIST: `/api/${API_VERSION}/shared-profiles`,
  },

  /** Health record endpoints */
  HEALTH_RECORDS: {
    BASE: `/api/${API_VERSION}/health-records`,
    LIST: `/api/${API_VERSION}/health-records`,
    SHARED: `/api/${API_VERSION}/health-records/shared`,
    GET: (id: string) => `/api/${API_VERSION}/health-records/${id}`,
    CREATE: `/api/${API_VERSION}/health-records`,
    UPDATE: (id: string) => `/api/${API_VERSION}/health-records/${id}`,
    DELETE: (id: string) => `/api/${API_VERSION}/health-records/${id}`,
    UPLOAD_URL: `/api/${API_VERSION}/health-records/upload-url`,
    CONFIRM_UPLOAD: (id: string) => `/api/${API_VERSION}/health-records/${id}/confirm-upload`,
    CONFIRM_RECORD: (id: string) => `/api/${API_VERSION}/health-records/${id}/confirm`,
    UPDATE_METRICS: (id: string) => `/api/${API_VERSION}/health-records/${id}/metrics`,
    STATUS: (id: string) => `/api/${API_VERSION}/health-records/${id}/status`,
    /**
     * @deprecated Prefer `EXPLANATION`, which sends `metricName` as a query parameter.
     * Path-segment routing may fail for metric names containing `/` on some server configs.
     * Kept for backward compatibility with older callers.
     */
    EXPLANATION_PATH: (id: string, metricName: string) =>
      `/api/${API_VERSION}/health-records/${id}/metrics/${encodeURIComponent(metricName)}/explanation`,
    /** Preferred route: pass `metricName` as query parameter to avoid encoded-slash path issues. */
    EXPLANATION: (id: string, metricName: string) =>
      `/api/${API_VERSION}/health-records/${id}/metrics/explanation?metricName=${encodeURIComponent(metricName)}`,
    RECOMMENDATIONS: (id: string) => `/api/${API_VERSION}/health-records/${id}/recommendations`,
    ORIGINAL_DOCUMENT: (id: string) => `/api/${API_VERSION}/health-records/${id}/original-document`,
    DOWNLOAD_PDF: (id: string) => `/api/${API_VERSION}/health-records/${id}/pdf`,
    UPLOAD_IMAGE: (id: string) => `/api/${API_VERSION}/health-records/${id}/image`,
    ANALYZE: (id: string) => `/api/${API_VERSION}/health-records/${id}/analyze`,
    GET_ANALYSIS: (id: string) => `/api/${API_VERSION}/health-records/${id}/analysis`,
    INVITATIONS: (id: string) => `/api/${API_VERSION}/health-records/${id}/invitations`,
    REVOKE_SHARE: (id: string, viewerId: string) => `/api/${API_VERSION}/health-records/${id}/shares/${viewerId}`,
  },

  HEALTH_RECORD_INVITATIONS: {
    ACCEPT: (token: string) =>
      `/api/${API_VERSION}/health-record-invitations/accept?token=${encodeURIComponent(token)}`,
    INCOMING: `/api/${API_VERSION}/health-record-invitations/incoming`,
  },

  /** Unified notification inbox (aggregates invitations + read state) */
  NOTIFICATIONS: {
    INBOX: `/api/${API_VERSION}/notifications/inbox`,
    INBOX_READ: `/api/${API_VERSION}/notifications/inbox/read`,
    INBOX_READ_ALL: `/api/${API_VERSION}/notifications/inbox/read-all`,
  },

  /** Document management endpoints */
  DOCUMENTS: {
    BASE: `/api/${API_VERSION}/documents`,
    LIST: `/api/${API_VERSION}/documents`,
    GET: (id: string) => `/api/${API_VERSION}/documents/${id}`,
    UPLOAD: `/api/${API_VERSION}/documents/upload`,
    DELETE: (id: string) => `/api/${API_VERSION}/documents/${id}`,
    DOWNLOAD: (id: string) => `/api/${API_VERSION}/documents/${id}/download`,
  },

  /** Admin (requires ROLE_ADMIN in JWT — login lại sau khi đổi role trong DB) */
  ADMIN: {
    AUDIT_LOGS: `/api/${API_VERSION}/admin/audit-logs`,
    AUDIT_LOGS_EXPORT: `/api/${API_VERSION}/admin/audit-logs/export`,
    ONLINE_RAG_CITATIONS: `/api/${API_VERSION}/admin/online-rag-citations`,
    ONLINE_RAG_CITATIONS_EXPORT: `/api/${API_VERSION}/admin/online-rag-citations/export`,
    REFERENCE_METRICS: `/api/${API_VERSION}/admin/reference-metrics`,
    REFERENCE_METRIC_DISPLAY_PATCH: (id: string) =>
      `/api/${API_VERSION}/admin/reference-metrics/${id}/display`,
  },

  /** Reference data endpoints (medical terms, etc.) */
  REFERENCE_DATA: {
    BASE: `/api/${API_VERSION}/reference-data`,
    INDEX: (id: string) => `/api/${API_VERSION}/reference-data/${id}/index`,
    SEARCH: `/api/${API_VERSION}/reference-data/search`,
    RANGES: `/api/${API_VERSION}/reference-data/ranges`,
    SYNC: `/api/${API_VERSION}/reference-data/sync`,
    METRICS: `/api/${API_VERSION}/reference-data/metrics`,
  },

  /** OCR service endpoints */
  OCR: {
    BASE: `/api/ocr`,
    EXTRACT: `/api/ocr/extract`,
    HEALTH: `/api/ocr/health`,
    STATUS: `/api/ocr/status`,
  },

  /** Health check endpoints */
  HEALTH: {
    LIVENESS: `/actuator/health/liveness`,
    READINESS: `/actuator/health/readiness`,
    INFO: `/actuator/info`,
  },

  /** Admin authentication endpoints */
  ADMIN_AUTH: {
    BASE: `/api/${API_VERSION}/admin/auth`,
    LOGIN: `/api/${API_VERSION}/admin/auth/login`,
    LOGOUT: `/api/${API_VERSION}/admin/auth/logout`,
    SESSION: `/api/${API_VERSION}/admin/auth/session`,
    TOTP_SETUP: `/api/${API_VERSION}/admin/auth/totp/setup`,
    TOTP_VERIFY: `/api/${API_VERSION}/admin/auth/totp/verify`,
  },

  ADMIN_ANALYTICS: {
    BASE: `/api/${API_VERSION}/admin/analytics`,
    USERS: `/api/${API_VERSION}/admin/analytics/users`,
    UPLOAD_QUALITY: `/api/${API_VERSION}/admin/analytics/upload-quality`,
    UPLOAD_HISTORY: `/api/${API_VERSION}/admin/analytics/upload-history`,
    ACTIVITY: `/api/${API_VERSION}/admin/analytics/activity`,
  },

  ADMIN_REFERENCE_DATA: {
    BASE: `/api/${API_VERSION}/admin/reference-data`,
    METRICS: `/api/${API_VERSION}/admin/reference-data/metrics`,
    IMPORT_PREVIEW: `/api/${API_VERSION}/admin/reference-data/import/preview`,
    IMPORT_CONFIRM: `/api/${API_VERSION}/admin/reference-data/import/confirm`,
    METRIC_BY_ID: (id: string) => `/api/${API_VERSION}/admin/reference-data/metrics/${id}`,
    REACTIVATE: (id: string) => `/api/${API_VERSION}/admin/reference-data/metrics/${id}/reactivate`,
    CHANGE_SETS: `/api/${API_VERSION}/admin/reference-data/change-sets`,
    APPROVE_CHANGE_SET: (id: string) => `/api/${API_VERSION}/admin/reference-data/change-sets/${id}/approve`,
    REJECT_CHANGE_SET: (id: string) => `/api/${API_VERSION}/admin/reference-data/change-sets/${id}/reject`,
    SUBMIT_CHANGE_SET: (id: string) => `/api/${API_VERSION}/admin/reference-data/change-sets/${id}/submit`,
    PUBLISH_CHANGE_SET: (id: string) => `/api/${API_VERSION}/admin/reference-data/change-sets/${id}/publish`,
    CONFIG: `/api/${API_VERSION}/admin/reference-data/config`,
  },
} as const;

/**
 * API Timeout configurations (in milliseconds)
 */
export const API_TIMEOUT = {
  DEFAULT: 30_000,
  UPLOAD: 60_000,
  OCR: 60_000,
  AI_ANALYSIS: 120_000,
  AUTH: 15_000,
} as const;

/**
 * API Pagination defaults
 */
export const API_PAGINATION = {
  DEFAULT_PAGE: 0,
  DEFAULT_SIZE: 20,
  MAX_SIZE: 100,
} as const;

/**
 * API Retry configuration
 */
export const API_RETRY = {
  MAX_ATTEMPTS: 3,
  INITIAL_DELAY_MS: 1_000,
  MAX_DELAY_MS: 10_000,
  BACKOFF_MULTIPLIER: 2,
} as const;

export type ApiPaths = typeof ApiPaths;
export type API_TIMEOUT = typeof API_TIMEOUT;
export type API_PAGINATION = typeof API_PAGINATION;
