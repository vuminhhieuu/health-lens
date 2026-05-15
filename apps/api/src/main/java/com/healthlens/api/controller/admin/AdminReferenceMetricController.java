package com.healthlens.api.controller.admin;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.dto.admin.ReferenceMetricAdminDto;
import com.healthlens.api.dto.request.UpdateReferenceMetricDisplayRequest;
import com.healthlens.api.service.admin.AdminReferenceMetricService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.ADMIN_REFERENCE_METRICS_BASE)
@RequiredArgsConstructor
@Validated
public class AdminReferenceMetricController {

    private final AdminReferenceMetricService adminReferenceMetricService;

    @GetMapping
    public ResponseEntity<List<ReferenceMetricAdminDto>> listMetrics() {
        return ResponseEntity.ok(adminReferenceMetricService.listAllMetrics());
    }

    @PatchMapping("/{metricId}/display")
    public ResponseEntity<ReferenceMetricAdminDto> updateDisplay(
            @PathVariable UUID metricId,
            @Valid @RequestBody UpdateReferenceMetricDisplayRequest request
    ) {
        return ResponseEntity.ok(adminReferenceMetricService.updateDisplay(metricId, request));
    }
}
