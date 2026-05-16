package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.FollowUpReminderRequest;
import com.healthlens.api.dto.response.FollowUpReminderResponse;
import com.healthlens.api.service.FollowUpReminderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.PROFILE_FOLLOW_UP_REMINDERS)
public class FollowUpReminderController {

    private final FollowUpReminderService followUpReminderService;

    public FollowUpReminderController(FollowUpReminderService followUpReminderService) {
        this.followUpReminderService = followUpReminderService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            Authentication authentication,
            @PathVariable UUID profileId
    ) {
        UUID userId = extractUserId(authentication);
        List<FollowUpReminderResponse> reminders = followUpReminderService.list(userId, profileId);
        return ResponseEntity.ok(buildResponseBody(reminders));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            Authentication authentication,
            @PathVariable UUID profileId,
            @Valid @RequestBody FollowUpReminderRequest request
    ) {
        UUID userId = extractUserId(authentication);
        FollowUpReminderResponse reminder = followUpReminderService.create(userId, profileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildResponseBody(reminder));
    }

    @PutMapping("/{reminderId}")
    public ResponseEntity<Map<String, Object>> update(
            Authentication authentication,
            @PathVariable UUID profileId,
            @PathVariable UUID reminderId,
            @Valid @RequestBody FollowUpReminderRequest request
    ) {
        UUID userId = extractUserId(authentication);
        FollowUpReminderResponse reminder =
                followUpReminderService.update(userId, profileId, reminderId, request);
        return ResponseEntity.ok(buildResponseBody(reminder));
    }

    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable UUID profileId,
            @PathVariable UUID reminderId
    ) {
        UUID userId = extractUserId(authentication);
        followUpReminderService.delete(userId, profileId, reminderId);
        return ResponseEntity.noContent().build();
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
