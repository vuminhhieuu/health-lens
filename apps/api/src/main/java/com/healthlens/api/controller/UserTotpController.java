package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.UserTotpDisableRequest;
import com.healthlens.api.dto.request.UserTotpVerifyRequest;
import com.healthlens.api.dto.response.UserTotpSetupResponse;
import com.healthlens.api.dto.response.UserTotpStatusResponse;
import com.healthlens.api.service.UserTotpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.USERS_ME_TOTP_BASE)
public class UserTotpController {

    private final UserTotpService userTotpService;

    public UserTotpController(UserTotpService userTotpService) {
        this.userTotpService = userTotpService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserTotpStatusResponse status = userTotpService.getStatus(userId);
        return ResponseEntity.ok(wrap(status));
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserTotpSetupResponse response = userTotpService.setup(userId);
        return ResponseEntity.ok(wrap(response));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            Authentication authentication,
            @Valid @RequestBody UserTotpVerifyRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        userTotpService.verifySetup(userId, request.code());
        return ResponseEntity.ok(wrap(Map.of("message", "Xác thực hai yếu tố đã được bật.")));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> disable(
            Authentication authentication,
            @Valid @RequestBody UserTotpDisableRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        userTotpService.disable(userId, request);
        return ResponseEntity.ok(wrap(Map.of("message", "Xác thực hai yếu tố đã được tắt.")));
    }

    private Map<String, Object> wrap(Object data) {
        return Map.of(
                "data", data,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );
    }
}
