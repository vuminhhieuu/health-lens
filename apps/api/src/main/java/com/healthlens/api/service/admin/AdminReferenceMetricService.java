package com.healthlens.api.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.audit.UnifiedAuditCoordinator;
import com.healthlens.api.audit.UnifiedAuditSnapshot;
import com.healthlens.api.dto.admin.ReferenceMetricAdminDto;
import com.healthlens.api.dto.request.UpdateReferenceMetricDisplayRequest;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ReferenceMetricRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReferenceMetricService {

    private final ReferenceMetricRepository referenceMetricRepository;
    private final UnifiedAuditCoordinator unifiedAuditCoordinator;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ReferenceMetricAdminDto> listAllMetrics() {
        return referenceMetricRepository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public ReferenceMetricAdminDto updateDisplay(
            UUID adminId,
            UUID metricId,
            UpdateReferenceMetricDisplayRequest request
    ) {
        Objects.requireNonNull(adminId, "adminId");

        ReferenceMetric metric = referenceMetricRepository
                .findById(metricId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chỉ số tham chiếu"));

        final UnifiedAuditSnapshot.Payload auditPayload;
        try {
            String oldJson = objectMapper.writeValueAsString(snapshot(metric));
            metric.setDisplayNameVi(request.displayNameVi().trim());
            referenceMetricRepository.save(metric);
            auditPayload = new UnifiedAuditSnapshot.Payload(
                    metricId,
                    oldJson,
                    objectMapper.writeValueAsString(snapshot(metric))
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize reference metric audit snapshot", e);
        }

        try {
            unifiedAuditCoordinator.persist(
                    adminId,
                    AuditActions.UPDATE_REFERENCE_METRIC_DISPLAY,
                    AuditResourceTypes.REFERENCE_DATA,
                    auditPayload
            );
            return toDto(metric);
        } finally {
            UnifiedAuditSnapshot.clear();
        }
    }

    private Map<String, Object> snapshot(ReferenceMetric metric) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", metric.getId().toString());
        map.put("name", metric.getName());
        map.put("displayNameVi", metric.getDisplayNameVi());
        map.put("unit", metric.getUnit());
        return map;
    }

    private ReferenceMetricAdminDto toDto(ReferenceMetric metric) {
        return new ReferenceMetricAdminDto(
                metric.getId(),
                metric.getName(),
                metric.getDisplayNameVi(),
                metric.getUnit()
        );
    }
}
