package com.healthlens.api.constants;

public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class
    }

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String ADMIN_ACCESS_TOKEN_COOKIE = "admin_access_token";
    public static final String XSRF_COOKIE = "XSRF-TOKEN";
    public static final String XSRF_HEADER = "X-XSRF-TOKEN";
}
