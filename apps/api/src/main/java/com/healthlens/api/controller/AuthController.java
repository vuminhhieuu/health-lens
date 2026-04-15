package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.dto.response.LoginResponse;
import com.healthlens.api.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.AUTH_BASE)
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days in seconds

    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService, @Value("${app.cookie.secure:true}") boolean cookieSecure) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        UUID userId = authService.register(request);

        Map<String, Object> body = Map.of(
                "data", Map.of(
                        "message", "Tai khoan da tao. Kiem tra email de xac thuc.",
                        "userId", userId
                ),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * POST /api/v1/auth/login
     * AC #1: email/password → access token + refresh token (HttpOnly cookie)
     * AC #5: generic error on bad credentials
     * AC #6: rate limiting after 5 failed attempts
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthService.LoginResult result = authService.login(request);

        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, result.rawRefreshToken());

        Map<String, Object> body = Map.of(
                "data", Map.of(
                        "accessToken", result.response().accessToken(),
                        "user", Map.of(
                                "id", result.response().user().id(),
                                "email", result.response().user().email(),
                                "role", result.response().user().role()
                        )
                ),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );

        return ResponseEntity.ok(body);
    }

    /**
     * POST /api/v1/auth/refresh
     * AC #2: use refresh token (HttpOnly cookie) to get new access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawRefreshToken = extractRefreshTokenFromCookie(request);

        if (rawRefreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "type", "https://healthlens.vn/errors/unauthorized",
                            "title", "Unauthorized",
                            "status", 401,
                            "detail", "Refresh token khong ton tai"
                    ));
        }

        AuthService.LoginResult result = authService.refresh(rawRefreshToken);

        // Set new refresh token cookie (rotation)
        setRefreshTokenCookie(response, result.rawRefreshToken());

        Map<String, Object> body = Map.of(
                "data", Map.of(
                        "accessToken", result.response().accessToken()
                ),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );

        return ResponseEntity.ok(body);
    }

    /**
     * POST /api/v1/auth/logout
     * AC #4: blacklist access token in Redis, clear refresh token cookie
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            authService.logout(accessToken);
        }

        // Clear refresh token cookie
        clearRefreshTokenCookie(response);

        return ResponseEntity.noContent().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(ApiRoutes.AUTH_REFRESH);
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(ApiRoutes.AUTH_REFRESH);
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
