package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.request.ConfirmRecordRequest;
import com.healthlens.api.dto.request.UpdateMetricsRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.HealthRecordDetailResponse;
import com.healthlens.api.dto.response.HealthRecordStatusResponse;
import com.healthlens.api.dto.response.MetricExplanationResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
import com.healthlens.api.service.HealthRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/{recordId}")
    public ResponseEntity<Map<String, Object>> getDetail(
            Authentication authentication,
            @PathVariable UUID recordId,
            @RequestParam(required = false) UUID profileId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        HealthRecordDetailResponse response = healthRecordService.getDetail(userId, recordId, profileId);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    /**
     * @deprecated Prefer query-param route ({@code /metrics/explanation?metricName=...}).
     * Path-parameter route can fail for metric names containing "/" depending on encoded slash handling.
     */
    @Deprecated(since = "4.3", forRemoval = false)
    @GetMapping("/{recordId}/metrics/{metricName}/explanation")
    public ResponseEntity<Map<String, Object>> getMetricExplanation(
            Authentication authentication,
            @PathVariable UUID recordId,
            @PathVariable String metricName
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        MetricExplanationResponse response = healthRecordService.getMetricExplanation(userId, recordId, metricName);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @GetMapping("/{recordId}/metrics/explanation")
    public ResponseEntity<Map<String, Object>> getMetricExplanationByQuery(
            Authentication authentication,
            @PathVariable UUID recordId,
            @RequestParam String metricName
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        MetricExplanationResponse response = healthRecordService.getMetricExplanation(userId, recordId, metricName);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @PostMapping("/{recordId}/confirm")
    public ResponseEntity<Map<String, Object>> confirmRecord(
            Authentication authentication,
            @PathVariable UUID recordId,
            @RequestBody(required = false) ConfirmRecordRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        healthRecordService.confirmRecord(userId, recordId, request);
        return ResponseEntity.ok(buildResponseBody(Map.of("message", "Confirmed successfully")));
    }

    @PutMapping("/{recordId}/metrics")
    public ResponseEntity<Map<String, Object>> updateMetrics(
            Authentication authentication,
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateMetricsRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        healthRecordService.updateMetrics(userId, recordId, request);
        return ResponseEntity.ok(buildResponseBody(Map.of("message", "Metrics updated successfully")));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Map<String, Object>> deleteRecord(
            Authentication authentication,
            @PathVariable UUID recordId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        healthRecordService.deleteHealthRecord(userId, recordId);
        return ResponseEntity.ok(buildResponseBody(Map.of("message", "Deleted successfully")));
    }

    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<Map<String, Object>> getRecordsByProfile(
            Authentication authentication,
            @PathVariable UUID profileId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        java.util.List<HealthRecordStatusResponse> response = healthRecordService.getRecordsByProfile(userId, profileId);
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
