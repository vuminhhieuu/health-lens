package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.response.AcceptHealthRecordInvitationResultResponse;
import com.healthlens.api.dto.response.IncomingHealthRecordInvitationResponse;
import com.healthlens.api.service.HealthRecordShareService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.HEALTH_RECORD_INVITATIONS_BASE)
public class HealthRecordInvitationController {

    private final HealthRecordShareService healthRecordShareService;

    public HealthRecordInvitationController(HealthRecordShareService healthRecordShareService) {
        this.healthRecordShareService = healthRecordShareService;
    }

    @PostMapping("/accept")
    public ResponseEntity<Map<String, Object>> acceptInvitation(
            @RequestParam("token") String token,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        AcceptHealthRecordInvitationResultResponse result = healthRecordShareService.acceptInvitation(token, userId);
        return ResponseEntity.ok(Map.of(
                "data", result,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        ));
    }

    @GetMapping("/incoming")
    public ResponseEntity<Map<String, Object>> listIncomingInvitations(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<IncomingHealthRecordInvitationResponse> list = healthRecordShareService.listIncomingInvitations(userId);
        return ResponseEntity.ok(Map.of(
                "data", list,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        ));
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
