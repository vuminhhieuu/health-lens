package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.MarkNotificationInboxReadRequest;
import com.healthlens.api.dto.response.NotificationInboxItemResponse;
import com.healthlens.api.service.NotificationInboxService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.NOTIFICATIONS_BASE)
public class NotificationController {

    private final NotificationInboxService notificationInboxService;

    public NotificationController(NotificationInboxService notificationInboxService) {
        this.notificationInboxService = notificationInboxService;
    }

    @GetMapping("/inbox")
    public ResponseEntity<Map<String, Object>> listInbox(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<NotificationInboxItemResponse> items = notificationInboxService.listInbox(userId);
        return ResponseEntity.ok(Map.of(
                "data", items,
                "meta", Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        ));
    }

    @PostMapping("/inbox/read")
    public ResponseEntity<Map<String, Object>> markInboxItemRead(
            @Valid @RequestBody MarkNotificationInboxReadRequest request, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        notificationInboxService.markAsRead(userId, request.itemId());
        return ResponseEntity.ok(Map.of(
                "data",
                Map.of("itemId", request.itemId(), "read", true),
                "meta",
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        ));
    }

    @PostMapping("/inbox/read-all")
    public ResponseEntity<Map<String, Object>> markAllInboxItemsRead(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        int markedCount = notificationInboxService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of(
                "data",
                Map.of("markedCount", markedCount),
                "meta",
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "requestId", UUID.randomUUID().toString()
                )
        ));
    }
}
