package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.CreateUploadUrlRequest;
import com.healthlens.api.dto.request.ConfirmRecordRequest;
import com.healthlens.api.dto.request.InviteHealthRecordRequest;
import com.healthlens.api.dto.request.UpdateMetricsRequest;
import com.healthlens.api.dto.response.ConfirmUploadResponse;
import com.healthlens.api.dto.response.DownloadHealthRecordPdfResponse;
import com.healthlens.api.dto.response.HealthRecordInvitationResponse;
import com.healthlens.api.dto.response.HealthRecordDetailResponse;
import com.healthlens.api.dto.response.HealthRecordStatusResponse;
import com.healthlens.api.dto.response.MetricExplanationResponse;
import com.healthlens.api.dto.response.RecommendationsResponse;
import com.healthlens.api.dto.response.SharedHealthRecordResponse;
import com.healthlens.api.dto.response.UploadUrlResponse;
import com.healthlens.api.service.HealthRecordService;
import com.healthlens.api.service.HealthRecordShareService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final HealthRecordShareService healthRecordShareService;

    public HealthRecordController(
            HealthRecordService healthRecordService,
            HealthRecordShareService healthRecordShareService
    ) {
        this.healthRecordService = healthRecordService;
        this.healthRecordShareService = healthRecordShareService;
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

    @GetMapping(value = "/{recordId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(
            Authentication authentication,
            @PathVariable UUID recordId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        DownloadHealthRecordPdfResponse response = healthRecordService.downloadHealthRecordPdf(userId, recordId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + response.filename() + "\"")
                .body(response.bytes());
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

    @GetMapping("/{recordId}/recommendations")
    public ResponseEntity<Map<String, Object>> getRecommendations(
            Authentication authentication,
            @PathVariable UUID recordId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        RecommendationsResponse response = healthRecordService.getRecommendations(userId, recordId);
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
        return ResponseEntity.ok(buildResponseBody(Map.of("message", "Đã xác nhận kết quả khám thành công")));
    }

    @PutMapping("/{recordId}/metrics")
    public ResponseEntity<Map<String, Object>> updateMetrics(
            Authentication authentication,
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateMetricsRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        healthRecordService.updateMetrics(userId, recordId, request);
        return ResponseEntity.ok(buildResponseBody(Map.of("message", "Đã cập nhật chỉ số thành công")));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Map<String, Object>> deleteRecord(
            Authentication authentication,
            @PathVariable UUID recordId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        healthRecordService.deleteHealthRecord(userId, recordId);
        return ResponseEntity.ok(buildResponseBody(Map.of("message", "Đã xóa kết quả khám thành công")));
    }

    @PostMapping("/{recordId}/invitations")
    public ResponseEntity<Map<String, Object>> inviteHealthRecordViewer(
            Authentication authentication,
            @PathVariable UUID recordId,
            @Valid @RequestBody InviteHealthRecordRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        HealthRecordInvitationResponse response =
                healthRecordShareService.inviteByEmail(userId, recordId, request.email(), request.accessLevel());
        return ResponseEntity.status(201).body(buildResponseBody(response));
    }

    @GetMapping("/{recordId}/invitations")
    public ResponseEntity<Map<String, Object>> getHealthRecordInvitations(
            Authentication authentication,
            @PathVariable UUID recordId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        java.util.List<HealthRecordInvitationResponse> response =
                healthRecordShareService.listInvitations(userId, recordId);
        return ResponseEntity.ok(buildResponseBody(response));
    }

    @DeleteMapping("/{recordId}/shares/{viewerId}")
    public ResponseEntity<Void> revokeHealthRecordShare(
            Authentication authentication,
            @PathVariable UUID recordId,
            @PathVariable UUID viewerId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        healthRecordShareService.revokeShare(userId, recordId, viewerId);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/shared")
    public ResponseEntity<Map<String, Object>> getSharedHealthRecords(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        java.util.List<SharedHealthRecordResponse> response = healthRecordService.getSharedHealthRecords(userId);
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
