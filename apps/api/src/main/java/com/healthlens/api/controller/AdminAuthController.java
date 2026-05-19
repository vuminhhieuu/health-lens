package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.constants.SecurityConstants;
import com.healthlens.api.dto.request.AdminLoginRequest;
import com.healthlens.api.dto.request.AdminTotpVerifyRequest;
import com.healthlens.api.dto.response.AdminLoginResponse;
import com.healthlens.api.dto.response.AdminTotpSetupResponse;
import com.healthlens.api.service.AdminAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.ADMIN_AUTH_BASE)
public class AdminAuthController {
    private static final int ADMIN_ACCESS_TOKEN_MAX_AGE = 15 * 60; // 15 minutes

    private final AdminAuthService adminAuthService;
    private final boolean cookieSecure;

    public AdminAuthController(
            AdminAuthService adminAuthService,
            @Value("${app.cookie.secure:true}") boolean cookieSecure) {
        this.adminAuthService = adminAuthService;
        this.cookieSecure = cookieSecure;
    }

    /**
     * Admin login: validate email + password + optional TOTP code.
     * POST /api/v1/admin/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletResponse response) {
        AdminLoginResponse loginResponse = adminAuthService.login(request);
        setAdminAccessTokenCookie(response, loginResponse.accessToken());
        return ResponseEntity.ok(Map.of("data", buildLoginPayload(loginResponse)));
    }

    /**
     * TOTP setup: generate secret + QR code URL.
     * Requires a valid admin session (password verified, totpVerified=false is OK).
     * POST /api/v1/admin/auth/totp/setup
     */
    @PostMapping("/totp/setup")
    public ResponseEntity<Map<String, Object>> setupTotp(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        AdminTotpSetupResponse response = adminAuthService.setupTotp(userId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    /**
     * Verify TOTP code after setup.
     * POST /api/v1/admin/auth/totp/verify
     */
    @PostMapping("/totp/verify")
    public ResponseEntity<Map<String, Object>> verifyTotp(
            Authentication authentication,
            @Valid @RequestBody AdminTotpVerifyRequest request,
            HttpServletResponse response) {
        UUID userId = UUID.fromString(authentication.getName());
        AdminLoginResponse verifyResponse = adminAuthService.verifyTotp(userId, request.code());
        setAdminAccessTokenCookie(response, verifyResponse.accessToken());
        return ResponseEntity.ok(Map.of("data", buildLoginPayload(verifyResponse)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = resolveAdminAccessToken(request);
        if (accessToken != null && !accessToken.isBlank()) {
            adminAuthService.logout(accessToken);
        }
        clearAdminAccessTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(Map.of("data", Map.of(
                "userId", userId.toString(),
                "email", extractAuthenticationStringDetail(authentication, "email"),
                "authenticated", true,
                "totpVerified", extractAuthenticationBooleanDetail(authentication, "totpVerified")
        )));
    }

    private void setAdminAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(SecurityConstants.ADMIN_ACCESS_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(ApiRoutes.ADMIN_BASE);
        cookie.setMaxAge(ADMIN_ACCESS_TOKEN_MAX_AGE);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearAdminAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SecurityConstants.ADMIN_ACCESS_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(ApiRoutes.ADMIN_BASE);
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(SecurityConstants.ADMIN_ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build()
                .toString());
    }

    private Map<String, Object> buildLoginPayload(AdminLoginResponse loginResponse) {
        return Map.of(
                "totpRequired", loginResponse.totpRequired(),
                "totpSetupRequired", loginResponse.totpSetupRequired(),
                "email", loginResponse.email()
        );
    }

    private String resolveAdminAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SecurityConstants.ADMIN_ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private boolean extractAuthenticationBooleanDetail(Authentication authentication, String key) {
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> detailMap) {
            Object value = ((Map<String, Object>) detailMap).get(key);
            if (value instanceof Boolean booleanValue) {
                return booleanValue;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private String extractAuthenticationStringDetail(Authentication authentication, String key) {
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> detailMap) {
            Object value = ((Map<String, Object>) detailMap).get(key);
            if (value instanceof String stringValue) {
                return stringValue;
            }
        }
        return null;
    }
}
