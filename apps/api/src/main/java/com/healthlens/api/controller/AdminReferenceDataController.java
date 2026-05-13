package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.AdminReferenceMetricRequest;
import com.healthlens.api.dto.request.AdminReferenceImportConfirmRequest;
import com.healthlens.api.dto.request.AdminRejectChangeSetRequest;
import com.healthlens.api.dto.response.AdminChangeSetDetailResponse;
import com.healthlens.api.dto.response.AdminReferenceChangeSetResponse;
import com.healthlens.api.dto.response.AdminReferenceImportConfirmResponse;
import com.healthlens.api.dto.response.AdminReferenceImportPreviewResponse;
import com.healthlens.api.dto.response.AdminReferenceMetricResponse;
import com.healthlens.api.service.ReferenceDataAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiRoutes.ADMIN_REFERENCE_DATA_BASE)
public class AdminReferenceDataController {

    private final ReferenceDataAdminService referenceDataAdminService;

    public AdminReferenceDataController(ReferenceDataAdminService referenceDataAdminService) {
        this.referenceDataAdminService = referenceDataAdminService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        List<AdminReferenceMetricResponse> metrics = referenceDataAdminService.listMetrics();
        return ResponseEntity.ok(Map.of("data", metrics));
    }

    @PostMapping("/import/preview")
    public ResponseEntity<Map<String, Object>> previewImport(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminReferenceImportPreviewResponse response = referenceDataAdminService.previewImport(adminId, file);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/import/confirm")
    public ResponseEntity<Map<String, Object>> confirmImport(
            Authentication authentication,
            @Valid @RequestBody AdminReferenceImportConfirmRequest request
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminReferenceImportConfirmResponse response = referenceDataAdminService.confirmImport(adminId, request.importId());
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/metrics")
    public ResponseEntity<Map<String, Object>> createMetric(
            Authentication authentication,
            @Valid @RequestBody AdminReferenceMetricRequest request) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminReferenceMetricResponse response = referenceDataAdminService.createMetric(adminId, request);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PutMapping("/metrics/{metricId}")
    public ResponseEntity<Map<String, Object>> updateMetric(
            Authentication authentication,
            @PathVariable UUID metricId,
            @Valid @RequestBody AdminReferenceMetricRequest request) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminReferenceChangeSetResponse response = referenceDataAdminService.updateMetric(adminId, metricId, request);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @DeleteMapping("/metrics/{metricId}")
    public ResponseEntity<Map<String, Object>> deactivateMetric(
            Authentication authentication,
            @PathVariable UUID metricId) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminReferenceChangeSetResponse response = referenceDataAdminService.deactivateMetric(adminId, metricId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/metrics/{metricId}/reactivate")
    public ResponseEntity<Map<String, Object>> reactivateMetric(@PathVariable UUID metricId) {
        AdminReferenceMetricResponse response = referenceDataAdminService.reactivateMetric(metricId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    // ========================================================================
    // Approval Workflow Endpoints (Story 7.4)
    // ========================================================================

    @GetMapping("/change-sets")
    public ResponseEntity<Map<String, Object>> listPendingChangeSets() {
        List<AdminChangeSetDetailResponse> changeSets = referenceDataAdminService.listPendingChangeSets();
        return ResponseEntity.ok(Map.of("data", changeSets));
    }

    @PostMapping("/change-sets/{changeSetId}/approve")
    public ResponseEntity<Map<String, Object>> approveChangeSet(
            Authentication authentication,
            @PathVariable UUID changeSetId) {
        UUID reviewerId = UUID.fromString(authentication.getName());
        AdminReferenceChangeSetResponse response = referenceDataAdminService.approveChangeSet(changeSetId, reviewerId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/change-sets/{changeSetId}/reject")
    public ResponseEntity<Map<String, Object>> rejectChangeSet(
            Authentication authentication,
            @PathVariable UUID changeSetId,
            @Valid @RequestBody AdminRejectChangeSetRequest request) {
        UUID reviewerId = UUID.fromString(authentication.getName());
        AdminReferenceChangeSetResponse response = referenceDataAdminService.rejectChangeSet(
                changeSetId, reviewerId, request.reason());
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/change-sets/{changeSetId}/submit")
    public ResponseEntity<Map<String, Object>> submitChangeSetForApproval(
            Authentication authentication,
            @PathVariable UUID changeSetId) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminReferenceChangeSetResponse response = referenceDataAdminService.submitChangeSetForApproval(
                changeSetId, adminId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/change-sets/{changeSetId}/publish")
    public ResponseEntity<Map<String, Object>> publishChangeSet(
            Authentication authentication,
            @PathVariable UUID changeSetId) {
        UUID adminId = UUID.fromString(authentication.getName());
        AdminReferenceChangeSetResponse response = referenceDataAdminService.publishChangeSet(changeSetId, adminId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getAdminConfig() {
        boolean multiAdmin = referenceDataAdminService.isMultiAdminMode();
        return ResponseEntity.ok(Map.of("data", Map.of("multiAdminMode", multiAdmin)));
    }
}
