package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.request.AdminReferenceMetricRequest;
import com.healthlens.api.dto.response.AdminReferenceChangeSetResponse;
import com.healthlens.api.dto.response.AdminReferenceMetricResponse;
import com.healthlens.api.service.ReferenceDataAdminService;
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
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Map<String, Object>> deactivateMetric(@PathVariable UUID metricId) {
        AdminReferenceMetricResponse response = referenceDataAdminService.deactivateMetric(metricId);
        return ResponseEntity.ok(Map.of("data", response));
    }

    @PostMapping("/metrics/{metricId}/reactivate")
    public ResponseEntity<Map<String, Object>> reactivateMetric(@PathVariable UUID metricId) {
        AdminReferenceMetricResponse response = referenceDataAdminService.reactivateMetric(metricId);
        return ResponseEntity.ok(Map.of("data", response));
    }
}
