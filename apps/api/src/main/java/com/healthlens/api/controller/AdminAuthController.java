package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.AdminLoginRequest;
import com.healthlens.api.dto.request.AdminTotpVerifyRequest;
import com.healthlens.api.dto.response.AdminLoginResponse;
import com.healthlens.api.dto.response.AdminTotpSetupResponse;
import com.healthlens.api.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.ADMIN_AUTH_BASE)
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    /**
     * Admin login: validate email + password + optional TOTP code.
     * POST /api/v1/admin/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok(Map.of("data", response));
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
            @Valid @RequestBody AdminTotpVerifyRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        AdminLoginResponse response = adminAuthService.verifyTotp(userId, request.code());
        return ResponseEntity.ok(Map.of("data", response));
    }
}
