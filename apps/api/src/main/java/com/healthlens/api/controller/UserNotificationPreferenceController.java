package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.UpdateNotificationPreferencesRequest;
import com.healthlens.api.dto.response.NotificationPreferenceResponse;
import com.healthlens.api.service.UserNotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.USERS_ME_NOTIFICATION_PREFERENCES)
public class UserNotificationPreferenceController {

    private final UserNotificationPreferenceService preferenceService;

    public UserNotificationPreferenceController(UserNotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPreferences(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        NotificationPreferenceResponse response = preferenceService.getPreferences(userId);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request) {
        UUID userId = extractUserId(authentication);
        NotificationPreferenceResponse response = preferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    private UUID extractUserId(Authentication authentication) {
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
