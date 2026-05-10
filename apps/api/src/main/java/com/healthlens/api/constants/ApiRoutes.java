package com.healthlens.api.constants;

/**
 * Central registry of all API route paths (Backend).
 * 
 * Mirrors packages/shared/constants/api.ts for consistency.
 * This file must be kept in sync with the frontend constants.
 * 
 * Usage:
 * - Controllers: @RequestMapping(ApiRoutes.AUTH_BASE)
 * - Security: SecurityConfig permitAll patterns
 * - Tests: Integration test paths
 */
public final class ApiRoutes {

    private ApiRoutes() {
        // Utility class
    }

    // =========================================
    // API Version
    // =========================================
    public static final String API = "/api";
    public static final String API_V1 = "/api/v1";

    // =========================================
    // Auth Paths (Frontend: ApiPaths.AUTH)
    // =========================================
    public static final String AUTH_BASE = API_V1 + "/auth";
    public static final String AUTH_REGISTER = AUTH_BASE + "/register";
    public static final String AUTH_LOGIN = AUTH_BASE + "/login";
    public static final String AUTH_VERIFY_EMAIL = AUTH_BASE + "/verify-email";
    public static final String AUTH_REFRESH = AUTH_BASE + "/refresh";
    public static final String AUTH_LOGOUT = AUTH_BASE + "/logout";
    public static final String AUTH_CHANGE_PASSWORD = AUTH_BASE + "/change-password";
    public static final String AUTH_FORGOT_PASSWORD = AUTH_BASE + "/forgot-password";
    public static final String AUTH_RESET_PASSWORD = AUTH_BASE + "/reset-password";

    // =========================================
    // Dev Paths (Frontend: ApiPaths.DEV)
    // =========================================
    public static final String DEV_BASE = API_V1 + "/dev";
    public static final String DEV_VERIFY_EMAIL = DEV_BASE + "/verify-email/{email}";

    // =========================================
    // User Paths (Frontend: ApiPaths.USERS)
    // =========================================
    public static final String USERS_BASE = API_V1 + "/users";

    // =========================================
    // Profile Paths (Frontend: ApiPaths.PROFILES)
    // =========================================
    public static final String PROFILES_BASE = API_V1 + "/profiles";
    public static final String PROFILE_BY_ID = PROFILES_BASE + "/{id}";
    public static final String PROFILE_ENSURE_DEFAULT = PROFILES_BASE + "/ensure-default";
    public static final String PROFILE_SET_DEFAULT = PROFILES_BASE + "/{id}/set-default";
    public static final String PROFILE_INVITATIONS = PROFILES_BASE + "/{profileId}/invitations";
    public static final String SHARED_PROFILES = API_V1 + "/shared-profiles";

    // Invitation accept paths
    public static final String INVITATIONS_BASE = API_V1 + "/invitations";
    public static final String INVITATIONS_ACCEPT = INVITATIONS_BASE + "/accept";
    public static final String INVITATIONS_INCOMING = INVITATIONS_BASE + "/incoming";
    public static final String INVITATIONS_REJECT = INVITATIONS_BASE + "/{invitationId}/reject";

    // =========================================
    // Health Record Paths (Frontend: ApiPaths.HEALTH_RECORDS)
    // =========================================
    public static final String HEALTH_RECORDS_BASE = API_V1 + "/health-records";
    public static final String HEALTH_RECORDS_UPLOAD_URL = HEALTH_RECORDS_BASE + "/upload-url";
    public static final String HEALTH_RECORD_BY_ID = HEALTH_RECORDS_BASE + "/{id}";
    public static final String HEALTH_RECORD_IMAGE = HEALTH_RECORDS_BASE + "/{id}/image";
    public static final String HEALTH_RECORD_ANALYZE = HEALTH_RECORDS_BASE + "/{id}/analyze";
    public static final String HEALTH_RECORD_ANALYSIS = HEALTH_RECORDS_BASE + "/{id}/analysis";
    public static final String HEALTH_RECORD_METRIC_EXPLANATION = HEALTH_RECORDS_BASE
            + "/{recordId}/metrics/{metricName}/explanation";
    public static final String HEALTH_RECORD_RECOMMENDATIONS = HEALTH_RECORDS_BASE + "/{recordId}/recommendations";

    // =========================================
    // Document Paths (Frontend: ApiPaths.DOCUMENTS)
    // =========================================
    public static final String DOCUMENTS_BASE = API_V1 + "/documents";
    public static final String DOCUMENT_BY_ID = DOCUMENTS_BASE + "/{id}";
    public static final String DOCUMENTS_UPLOAD = DOCUMENTS_BASE + "/upload";
    public static final String DOCUMENT_DOWNLOAD = DOCUMENTS_BASE + "/{id}/download";

    // =========================================
    // Reference Data Paths (Frontend: ApiPaths.REFERENCE_DATA)
    // =========================================
    public static final String REFERENCE_DATA_BASE = API_V1 + "/reference-data";
    public static final String REFERENCE_DATA_INDEX = REFERENCE_DATA_BASE + "/{id}/index";
    public static final String REFERENCE_DATA_SEARCH = REFERENCE_DATA_BASE + "/search";
    public static final String REFERENCE_DATA_RANGES = REFERENCE_DATA_BASE + "/ranges";
    public static final String REFERENCE_DATA_SYNC = REFERENCE_DATA_BASE + "/sync";
    public static final String REFERENCE_DATA_METRICS = REFERENCE_DATA_BASE + "/metrics";

    // =========================================
    // Health Record Metric Paths (Story 3.4)
    // =========================================
    public static final String HEALTH_RECORD_METRICS = HEALTH_RECORDS_BASE + "/{recordId}/metrics";

    // =========================================
    // OCR Paths (Frontend: ApiPaths.OCR)
    // =========================================
    public static final String OCR_BASE = "/api/ocr";
    public static final String OCR_EXTRACT = OCR_BASE + "/extract";
    public static final String OCR_HEALTH = OCR_BASE + "/health";
    public static final String OCR_STATUS = OCR_BASE + "/status";

    // =========================================
    // User Deletion Paths (Story 1.6 - AC #5)
    // Create: POST .../users/me/deletion-request (authenticated).
    // Cancel: DELETE .../users/deletion-requests/cancel?token=... — public
    // (permitAll); NOT under /me/ because the email cancellation token is the
    // credential (no JWT on this call).
    // =========================================
    public static final String USERS_DELETION_BASE = API_V1 + "/users/deletion-requests";
    public static final String USERS_DELETION_CANCEL = USERS_DELETION_BASE + "/cancel";

    // =========================================
    // Admin Auth Paths
    // =========================================
    public static final String ADMIN_BASE = API_V1 + "/admin";
    public static final String ADMIN_AUTH_BASE = ADMIN_BASE + "/auth";
    public static final String ADMIN_REFERENCE_DATA_BASE = ADMIN_BASE + "/reference-data";
    public static final String ADMIN_REFERENCE_DATA_REACTIVATE = ADMIN_REFERENCE_DATA_BASE + "/metrics/{metricId}/reactivate";
    public static final String ADMIN_AUTH_LOGIN = ADMIN_AUTH_BASE + "/login";
    public static final String ADMIN_AUTH_TOTP_SETUP = ADMIN_AUTH_BASE + "/totp/setup";
    public static final String ADMIN_AUTH_TOTP_VERIFY = ADMIN_AUTH_BASE + "/totp/verify";

    // =========================================
    // Patterns (for SecurityConfig permitAll)
    // =========================================
    public static final String AUTH_PATTERN = AUTH_BASE + "/**";
    public static final String DEV_PATTERN = DEV_BASE + "/**";
    public static final String USERS_DELETION_PATTERN = USERS_DELETION_BASE + "/**";
    public static final String INVITATIONS_ACCEPT_PATTERN = INVITATIONS_ACCEPT + "/**";
    public static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
    public static final String SWAGGER_HTML = "/swagger-ui.html";
    public static final String API_DOCS_PATTERN = "/v3/api-docs/**";
    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_HEALTH_PATTERN = "/actuator/health/**";
    public static final String ADMIN_AUTH_PATTERN = ADMIN_AUTH_BASE + "/**";
    public static final String ADMIN_PATTERN = ADMIN_BASE + "/**";
}
