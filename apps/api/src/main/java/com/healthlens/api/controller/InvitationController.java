package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.response.AcceptInvitationResultResponse;

import com.healthlens.api.dto.response.IncomingProfileInvitationResponse;
import com.healthlens.api.service.ProfileShareService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.INVITATIONS_BASE)
public class InvitationController {

    private final ProfileShareService profileShareService;

    public InvitationController(ProfileShareService profileShareService) {
        this.profileShareService = profileShareService;
    }

    @PostMapping("/accept")
    public ResponseEntity<Map<String, Object>> acceptInvitation(
            @RequestParam("token") String token,
            Authentication authentication
    ) {
        UUID userId = resolveUserId(authentication);
        AcceptInvitationResultResponse result = profileShareService.acceptInvitation(token, userId);
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
        List<IncomingProfileInvitationResponse> list = profileShareService.listIncomingInvitations(userId);
        return ResponseEntity.ok(Map.of(
                "data", list,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        ));
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Map<String, Object>> rejectIncomingInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        profileShareService.rejectIncomingInvitation(invitationId, userId);
        return ResponseEntity.ok(Map.of(
                "data", Map.of("rejected", true),
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
