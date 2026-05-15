package com.healthlens.api.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.annotation.Auditable;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.audit.UnifiedAuditSnapshot;
import com.healthlens.api.dto.admin.ReferenceMetricAdminDto;
import com.healthlens.api.dto.request.UpdateReferenceMetricDisplayRequest;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ReferenceMetricRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReferenceMetricService {

    private final ReferenceMetricRepository referenceMetricRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ReferenceMetricAdminDto> listAllMetrics() {
        return referenceMetricRepository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    @Auditable(
            action = AuditActions.UPDATE_REFERENCE_METRIC_DISPLAY,
            unifiedResourceType = AuditResourceTypes.REFERENCE_DATA
    )
    public ReferenceMetricAdminDto updateDisplay(UUID metricId, UpdateReferenceMetricDisplayRequest request) {

        ReferenceMetric metric = referenceMetricRepository
                .findById(metricId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chỉ số tham chiếu"));

        try {
            String oldJson = objectMapper.writeValueAsString(snapshot(metric));
            metric.setDisplayNameVi(request.displayNameVi().trim());
            referenceMetricRepository.save(metric);

            UnifiedAuditSnapshot.set(
                    new UnifiedAuditSnapshot.Payload(
                            metricId,
                            oldJson,
                            objectMapper.writeValueAsString(snapshot(metric))
                    )
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize reference metric audit snapshot", e);
        }

        return toDto(metric);
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
