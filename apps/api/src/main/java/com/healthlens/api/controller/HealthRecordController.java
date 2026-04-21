package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.HealthRecordStatusResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
import com.healthlens.api.service.HealthRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.HEALTH_RECORDS_BASE)
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    @PostMapping("/upload-url")
    public ResponseEntity<Map<String, Object>> createUploadUrl(
            Authentication authentication,
            @Valid @RequestBody CreateUploadUrlRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        UploadUrlResponse response = healthRecordService.createUploadUrl(userId, request);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @PostMapping("/{recordId}/confirm-upload")
    public ResponseEntity<Map<String, Object>> confirmUpload(
            Authentication authentication,
            @PathVariable UUID recordId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        ConfirmUploadResponse response = healthRecordService.confirmUpload(userId, recordId);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @GetMapping("/{recordId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            Authentication authentication,
            @PathVariable UUID recordId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        HealthRecordStatusResponse response = healthRecordService.getStatus(userId, recordId);
        return ResponseEntity.ok(buildResponseBody(response));
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
