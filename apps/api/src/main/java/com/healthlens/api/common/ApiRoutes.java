package com.healthlens.api.common;

/**
 * Central registry of all API route paths.
 * Used by controllers and SecurityConfig to avoid hardcoded strings.
 */
public final class ApiRoutes {

    private ApiRoutes() {
    }

    public static final String API_BASE = "/api/v1";

    // ===== Auth =====
    public static final String AUTH_BASE = API_BASE + "/auth";
    public static final String AUTH_REGISTER = AUTH_BASE + "/register";
    public static final String AUTH_LOGIN = AUTH_BASE + "/login";
    public static final String AUTH_REFRESH = AUTH_BASE + "/refresh";
    public static final String AUTH_LOGOUT = AUTH_BASE + "/logout";

    // ===== Dev (dev/docker profile only) =====
    public static final String DEV_BASE = API_BASE + "/dev";
    public static final String DEV_VERIFY_EMAIL = DEV_BASE + "/verify-email/{email}";

    // ===== Users =====
    public static final String USERS_BASE = API_BASE + "/users";

    // ===== Patterns (for SecurityConfig permitAll) =====
    public static final String AUTH_PATTERN = AUTH_BASE + "/**";
    public static final String DEV_PATTERN = DEV_BASE + "/**";
    public static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
    public static final String SWAGGER_HTML = "/swagger-ui.html";
    public static final String API_DOCS_PATTERN = "/v3/api-docs/**";
    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_HEALTH_PATTERN = "/actuator/health/**";
}
