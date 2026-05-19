package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.DeleteAccountRequest;
import com.healthlens.api.dto.request.UpdateUserRequest;
import com.healthlens.api.dto.response.CancelDeletionResponse;
import com.healthlens.api.dto.response.DeleteAccountResponse;
import com.healthlens.api.dto.response.UserResponse;
import com.healthlens.api.security.ClientIpResolver;
import com.healthlens.api.security.PublicEndpointRateLimiter;
import com.healthlens.api.service.DataDeletionService;
import com.healthlens.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.USERS_BASE)
public class UserController {

    private final UserService userService;
    private final DataDeletionService dataDeletionService;
    private final PublicEndpointRateLimiter publicEndpointRateLimiter;

    public UserController(
            UserService userService,
            DataDeletionService dataDeletionService,
            PublicEndpointRateLimiter publicEndpointRateLimiter) {
        this.userService = userService;
        this.dataDeletionService = dataDeletionService;
        this.publicEndpointRateLimiter = publicEndpointRateLimiter;
    }

    @GetMapping(ApiRoutes.USERS_ME_REL)
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        UserResponse response = userService.getCurrentUser(userId);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @PutMapping(ApiRoutes.USERS_ME_REL)
    public ResponseEntity<Map<String, Object>> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request) {
        
        UUID userId = extractUserId(authentication);
        UserResponse response = userService.updateCurrentUser(userId, request);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @PutMapping(ApiRoutes.USERS_ME_AVATAR_REL)
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        UUID userId = extractUserId(authentication);
        UserResponse response = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @DeleteMapping(ApiRoutes.USERS_ME_AVATAR_REL)
    public ResponseEntity<Map<String, Object>> removeAvatar(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        UserResponse response = userService.removeAvatar(userId);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @PostMapping("/me/deletion-request")
    public ResponseEntity<Map<String, Object>> requestDeletion(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest request) {
        
        UUID userId = extractUserId(authentication);
        DeleteAccountResponse response = dataDeletionService.createDeletionRequest(userId, request);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @DeleteMapping("/deletion-requests/cancel")
    public ResponseEntity<Map<String, Object>> cancelDeletion(
            @RequestParam(required = false) String token,
            HttpServletRequest request) {
        publicEndpointRateLimiter.consumeCancelDeletion(ClientIpResolver.resolve(request), token);
        CancelDeletionResponse response = dataDeletionService.cancelDeletionRequest(token);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    private UUID extractUserId(Authentication authentication) {
        // JwtAuthenticationFilter sets UserDetails with username as userId
        return UUID.fromString(authentication.getName());
    }

    private Map<String, Object> buildResponseBody(Object data) {
        return Map.of(
                "data", data,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        );
    }
}
