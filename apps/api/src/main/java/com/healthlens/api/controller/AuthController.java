package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.constants.SecurityConstants;
import com.healthlens.api.dto.request.ForgotPasswordRequest;
import com.healthlens.api.dto.request.LoginRequest;
import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.dto.request.ResetPasswordRequest;
import com.healthlens.api.dto.request.VerifyEmailRequest;
import com.healthlens.api.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.AUTH_BASE)
public class AuthController {

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
                        "message", "Tài khoản đã tạo. Vui lòng kiểm tra email để xác thực.",
                        "userId", userId
                ),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping(ApiRoutes.AUTH_CSRF_REL)
    public ResponseEntity<Map<String, Object>> csrf(HttpServletRequest request, CsrfToken csrfToken) {
        CsrfToken effectiveToken = csrfTokenFromRequest(request, csrfToken);
        Map<String, Object> body = Map.of(
                "data", Map.of(
                        "headerName", SecurityConstants.XSRF_HEADER,
                        "cookieName", SecurityConstants.XSRF_COOKIE,
                        "parameterName", effectiveToken.getParameterName()
                ),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );

        return ResponseEntity.ok(body);
    }

    private CsrfToken csrfTokenFromRequest(HttpServletRequest request, CsrfToken fallback) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = (CsrfToken) request.getAttribute("_csrf");
        }
        return token == null ? fallback : token;
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

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        authService.verifyEmail(request.token(), clientIp(httpRequest));

        Map<String, Object> body = Map.of(
                "data", Map.of("message", "Email đã được xác thực thành công"),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );

        return ResponseEntity.ok(body);
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * POST /api/v1/auth/refresh
     * AC #2: use refresh token (HttpOnly cookie) to get new access token
     * Returns consent information alongside user data to enable frontend
     * to update consent modal based on latest consent status.
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
                            "title", "Không được xác thực",
                            "status", 401,
                            "detail", "Refresh token không tồn tại",
                            "errorCode", "INVALID_CREDENTIALS"
                    ));
        }

        AuthService.RefreshResult result = authService.refreshWithConsent(rawRefreshToken);

        // Set new refresh token cookie (rotation)
        setRefreshTokenCookie(response, result.rawRefreshToken());

        // Map.of does not allow null values; consentVersion can legitimately be null
        // when user has not granted consent for the active version yet.
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", result.response().accessToken());
        data.put("user", Map.of(
                "id", result.response().user().id(),
                "email", result.response().user().email(),
                "role", result.response().user().role()
        ));
        data.put("consentGiven", result.response().consentGiven());
        data.put("consentVersion", result.response().consentVersion());

        Map<String, Object> body = new HashMap<>();
        body.put("data", data);
        body.put("meta", Map.of(
                "timestamp", Instant.now().toString(),
                "requestId", UUID.randomUUID().toString()
        ));

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

    /**
     * POST /api/v1/auth/forgot-password
     * AC #1: email tồn tại -> tạo reset token, gửi email
     * AC #2: email không tồn tại -> vẫn trả 200 OK (an toàn thông tin)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);

        Map<String, Object> body = Map.of(
                "data", Map.of("message", "Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu."),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );

        return ResponseEntity.ok(body);
    }

    /**
     * POST /api/v1/auth/reset-password
     * AC #3, #4, #5: validate token, cập nhật mật khẩu
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);

        Map<String, Object> body = Map.of(
                "data", Map.of("message", "Mật khẩu đã được đặt lại thành công."),
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );

        return ResponseEntity.ok(body);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(SecurityConstants.REFRESH_TOKEN_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath(ApiRoutes.AUTH_REFRESH);
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SecurityConstants.REFRESH_TOKEN_COOKIE, "");
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
                .filter(c -> SecurityConstants.REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
