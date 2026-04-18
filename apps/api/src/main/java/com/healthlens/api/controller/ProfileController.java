package com.healthlens.api.controller;

import com.healthlens.api.common.ApiRoutes;
import com.healthlens.api.dto.request.CreateProfileRequest;
import com.healthlens.api.dto.response.ProfileResponse;
import com.healthlens.api.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.PROFILES_BASE)
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfiles(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<ProfileResponse> profiles = profileService.getProfiles(userId);
        return ResponseEntity.ok(buildResponseBody(profiles));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProfile(
            Authentication authentication,
            @Valid @RequestBody CreateProfileRequest request) {
        
        UUID userId = extractUserId(authentication);
        ProfileResponse profile = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponseBody(profile));
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
