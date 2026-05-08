package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.response.SharedProfileResponse;
import com.healthlens.api.service.ProfileService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.SHARED_PROFILES)
public class SharedProfileController {

    private final ProfileService profileService;

    public SharedProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listSharedProfiles(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<SharedProfileResponse> profiles = profileService.getSharedProfiles(userId);
        return ResponseEntity.ok(Map.of(
                "data", profiles,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        ));
    }
}
