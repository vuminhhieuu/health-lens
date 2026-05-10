package com.healthlens.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.request.AdminReferenceMetricRequest;
import com.healthlens.api.dto.request.AdminReferenceRangeRequest;
import com.healthlens.api.dto.response.AdminPendingChangeSetSummary;
import com.healthlens.api.dto.response.AdminReferenceChangeSetResponse;
import com.healthlens.api.dto.response.AdminReferenceMetricResponse;
import com.healthlens.api.dto.response.AdminReferenceRangeResponse;
import com.healthlens.api.entity.ReferenceDataChangeSet;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.entity.ReferenceRange;
import com.healthlens.api.repository.ReferenceDataChangeSetRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import com.healthlens.api.repository.ReferenceRangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReferenceDataAdminService {

    private static final List<String> NON_NEGATIVE_METRICS = List.of(
            "glucose", "hba1c", "cholesterol", "triglycerides", "hdl", "ldl"
    );

    private final ReferenceMetricRepository referenceMetricRepository;
    private final ReferenceRangeRepository referenceRangeRepository;
    private final ReferenceDataChangeSetRepository referenceDataChangeSetRepository;
    private final ObjectMapper objectMapper;

    public ReferenceDataAdminService(
            ReferenceMetricRepository referenceMetricRepository,
            ReferenceRangeRepository referenceRangeRepository,
            ReferenceDataChangeSetRepository referenceDataChangeSetRepository,
            ObjectMapper objectMapper
    ) {
        this.referenceMetricRepository = referenceMetricRepository;
        this.referenceRangeRepository = referenceRangeRepository;
        this.referenceDataChangeSetRepository = referenceDataChangeSetRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AdminReferenceMetricResponse> listMetrics() {
        List<ReferenceMetric> allMetrics = referenceMetricRepository.findAllByOrderByNameAsc();
        Map<UUID, List<AdminReferenceRangeResponse>> rangesByMetricId = referenceRangeRepository
                .findAllOrderByMetricIdAscGenderAscMinAgeAsc()
                .stream()
                .collect(Collectors.groupingBy(
                        range -> range.getMetric().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(this::toRangeResponse, Collectors.toList())
                ));

        List<UUID> metricIds = allMetrics.stream().map(ReferenceMetric::getId).toList();
        Map<UUID, AdminPendingChangeSetSummary> pendingByMetricId = buildPendingChangeSetsMap(metricIds);

        return allMetrics.stream()
                .map(metric -> {
                    List<AdminReferenceRangeResponse> ranges = rangesByMetricId.getOrDefault(metric.getId(), List.of());
                    return new AdminReferenceMetricResponse(
                            metric.getId(),
                            metric.getName(),
                            metric.getDisplayNameVi(),
                            metric.getUnit(),
                            metric.getStatus(),
                            ranges.size(),
                            ranges,
                            pendingByMetricId.get(metric.getId())
                    );
                })
                .toList();
    }

    @Transactional
    public AdminReferenceMetricResponse createMetric(UUID adminId, AdminReferenceMetricRequest request) {
        validateMetricRequest(request);
        referenceMetricRepository.findByNameIgnoreCase(request.name().trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tên chỉ số đã tồn tại");
                });

        ReferenceMetric metric = new ReferenceMetric();
        metric.setName(request.name().trim());
        metric.setDisplayNameVi(request.displayNameVi().trim());
        metric.setUnit(request.unit().trim());
        metric.setStatus("draft");
        ReferenceMetric savedMetric = referenceMetricRepository.save(metric);

        List<AdminReferenceRangeResponse> ranges = request.ranges().stream()
                .map(range -> toRangeEntity(savedMetric, range, "draft"))
                .map(referenceRangeRepository::save)
                .map(this::toRangeResponse)
                .toList();

        persistChangeSet(adminId, savedMetric.getId(), "METRIC", "CREATE", buildSnapshot(savedMetric, request, "draft"));

        return new AdminReferenceMetricResponse(
                savedMetric.getId(),
                savedMetric.getName(),
                savedMetric.getDisplayNameVi(),
                savedMetric.getUnit(),
                savedMetric.getStatus(),
                ranges.size(),
                ranges
        );
    }

    @Transactional
    public AdminReferenceChangeSetResponse updateMetric(UUID adminId, UUID metricId, AdminReferenceMetricRequest request) {
        ReferenceMetric metric = referenceMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chỉ số cần cập nhật"));
        validateMetricRequest(request);

        referenceMetricRepository.findByNameIgnoreCase(request.name().trim())
                .filter(existing -> !existing.getId().equals(metricId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Tên chỉ số đã tồn tại");
                });

        if ("draft".equals(metric.getStatus())) {
            metric.setName(request.name().trim());
            metric.setDisplayNameVi(request.displayNameVi().trim());
            metric.setUnit(request.unit().trim());
            referenceMetricRepository.save(metric);
            replaceRanges(metric, request.ranges(), "draft");
            return new AdminReferenceChangeSetResponse(
                    null,
                    "draft",
                    "Đã cập nhật trực tiếp bản nháp của chỉ số.",
                    "draft-updated"
            );
        }

        UUID changeSetId = persistChangeSet(
                adminId,
                metricId,
                "METRIC",
                "UPDATE",
                buildSnapshot(metric, request, "draft")
        );

        return new AdminReferenceChangeSetResponse(
                changeSetId,
                "draft",
                "Đã tạo bản thay đổi nháp. Dữ liệu đang áp dụng chưa bị thay đổi.",
                "change-set-created"
        );
    }

    @Transactional
    public AdminReferenceMetricResponse deactivateMetric(UUID metricId) {
        ReferenceMetric metric = referenceMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chỉ số cần xóa"));

        metric.setStatus("deactivated");
        ReferenceMetric savedMetric = referenceMetricRepository.save(metric);
        List<AdminReferenceRangeResponse> ranges = referenceRangeRepository
                .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId)
                .stream()
                .map(range -> {
                    range.setStatus("deactivated");
                    return referenceRangeRepository.save(range);
                })
                .map(this::toRangeResponse)
                .toList();

        return new AdminReferenceMetricResponse(
                savedMetric.getId(),
                savedMetric.getName(),
                savedMetric.getDisplayNameVi(),
                savedMetric.getUnit(),
                savedMetric.getStatus(),
                ranges.size(),
                ranges
        );
    }

    @Transactional
    public AdminReferenceMetricResponse reactivateMetric(UUID metricId) {
        ReferenceMetric metric = referenceMetricRepository.findById(metricId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chỉ số cần kích hoạt lại"));

        metric.setStatus("active");
        ReferenceMetric savedMetric = referenceMetricRepository.save(metric);
        List<AdminReferenceRangeResponse> ranges = referenceRangeRepository
                .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId)
                .stream()
                .map(range -> {
                    range.setStatus("active");
                    return referenceRangeRepository.save(range);
                })
                .map(this::toRangeResponse)
                .toList();

        return new AdminReferenceMetricResponse(
                savedMetric.getId(),
                savedMetric.getName(),
                savedMetric.getDisplayNameVi(),
                savedMetric.getUnit(),
                savedMetric.getStatus(),
                ranges.size(),
                ranges
        );
    }

    private void validateMetricRequest(AdminReferenceMetricRequest request) {
        String metricName = request.name().trim();
        request.ranges().forEach(range -> validateRange(metricName, range));
        validateRangesNoOverlap(request.ranges());
    }

    private void validateRangesNoOverlap(List<AdminReferenceRangeRequest> ranges) {
        if (ranges.size() < 2) {
            return;
        }
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                AdminReferenceRangeRequest a = ranges.get(i);
                AdminReferenceRangeRequest b = ranges.get(j);
                if (genderSegmentsOverlap(a.gender(), b.gender())
                        && ageSegmentsOverlap(a.minAge(), a.maxAge(), b.minAge(), b.maxAge())) {
                    throw new IllegalArgumentException(
                            "Ngưỡng #" + (i + 1) + " và #" + (j + 1)
                                    + " trùng phân đoạn giới tính/độ tuổi. "
                                    + "Hãy điều chỉnh để các ngưỡng không chồng lấn."
                    );
                }
            }
        }
    }

    private boolean genderSegmentsOverlap(String genderA, String genderB) {
        if (genderA == null || genderA.isBlank() || genderB == null || genderB.isBlank()) {
            return true;
        }
        return genderA.equalsIgnoreCase(genderB);
    }

    private boolean ageSegmentsOverlap(Integer minA, Integer maxA, Integer minB, Integer maxB) {
        int effectiveMinA = minA != null ? minA : 0;
        int effectiveMaxA = maxA != null ? maxA : Integer.MAX_VALUE;
        int effectiveMinB = minB != null ? minB : 0;
        int effectiveMaxB = maxB != null ? maxB : Integer.MAX_VALUE;
        return effectiveMinA <= effectiveMaxB && effectiveMinB <= effectiveMaxA;
    }

    private void validateRange(String metricName, AdminReferenceRangeRequest range) {
        if (range.minValue().compareTo(range.maxValue()) >= 0) {
            throw new IllegalArgumentException("Giá trị tối thiểu phải nhỏ hơn giá trị tối đa");
        }
        if (range.attentionMin().compareTo(range.attentionMax()) >= 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo tối thiểu phải nhỏ hơn ngưỡng cảnh báo tối đa");
        }
        if (range.attentionMin().compareTo(range.minValue()) > 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo tối thiểu không được lớn hơn ngưỡng tối thiểu bình thường");
        }
        if (range.attentionMax().compareTo(range.maxValue()) < 0) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo tối đa không được nhỏ hơn ngưỡng tối đa bình thường");
        }
        if (range.minAge() != null && range.maxAge() != null && range.minAge() > range.maxAge()) {
            throw new IllegalArgumentException("Tuổi tối thiểu không được lớn hơn tuổi tối đa");
        }
        if (isNonNegativeMetric(metricName)
                && (range.minValue().compareTo(BigDecimal.ZERO) < 0
                || range.maxValue().compareTo(BigDecimal.ZERO) < 0
                || range.attentionMin().compareTo(BigDecimal.ZERO) < 0
                || range.attentionMax().compareTo(BigDecimal.ZERO) < 0)) {
            throw new IllegalArgumentException("Không cho phép ngưỡng âm cho chỉ số này");
        }
    }

    private boolean isNonNegativeMetric(String metricName) {
        return NON_NEGATIVE_METRICS.contains(normalizeMetricName(metricName));
    }

    private ReferenceRange toRangeEntity(ReferenceMetric metric, AdminReferenceRangeRequest range, String status) {
        ReferenceRange entity = new ReferenceRange();
        entity.setMetric(metric);
        entity.setMinValue(range.minValue());
        entity.setMaxValue(range.maxValue());
        entity.setAttentionMin(range.attentionMin());
        entity.setAttentionMax(range.attentionMax());
        entity.setGender(range.gender());
        entity.setMinAge(range.minAge());
        entity.setMaxAge(range.maxAge());
        entity.setStatus(status);
        return entity;
    }

    private AdminReferenceRangeResponse toRangeResponse(ReferenceRange range) {
        return new AdminReferenceRangeResponse(
                range.getId(),
                range.getMinValue(),
                range.getMaxValue(),
                range.getAttentionMin(),
                range.getAttentionMax(),
                range.getGender(),
                range.getMinAge(),
                range.getMaxAge(),
                range.getStatus()
        );
    }

    private UUID persistChangeSet(UUID adminId, UUID entityId, String entityType, String operation, Map<String, Object> changes) {
        ReferenceDataChangeSet changeSet = new ReferenceDataChangeSet();
        changeSet.setAdminId(adminId);
        changeSet.setEntityType(entityType);
        changeSet.setEntityId(entityId);
        changeSet.setOperation(operation);
        changeSet.setStatus("draft");
        changeSet.setChangesJson(writeJson(changes));
        return referenceDataChangeSetRepository.save(changeSet).getId();
    }

    private void replaceRanges(ReferenceMetric metric, List<AdminReferenceRangeRequest> ranges, String status) {
        List<ReferenceRange> existingRanges = referenceRangeRepository
                .findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metric.getId());
        if (!existingRanges.isEmpty()) {
            referenceRangeRepository.deleteAll(existingRanges);
        }
        ranges.stream()
                .map(range -> toRangeEntity(metric, range, status))
                .forEach(referenceRangeRepository::save);
    }

    private Map<String, Object> buildSnapshot(ReferenceMetric metric, AdminReferenceMetricRequest request, String targetStatus) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("metricId", metric.getId());
        payload.put("name", request.name().trim());
        payload.put("displayNameVi", request.displayNameVi().trim());
        payload.put("unit", request.unit().trim());
        payload.put("status", targetStatus);
        payload.put("ranges", request.ranges().stream().map(range -> {
            Map<String, Object> rangePayload = new LinkedHashMap<>();
            rangePayload.put("minValue", range.minValue());
            rangePayload.put("maxValue", range.maxValue());
            rangePayload.put("attentionMin", range.attentionMin());
            rangePayload.put("attentionMax", range.attentionMax());
            rangePayload.put("gender", range.gender());
            rangePayload.put("minAge", range.minAge());
            rangePayload.put("maxAge", range.maxAge());
            rangePayload.put("status", targetStatus);
            return rangePayload;
        }).toList());
        return payload;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể lưu change set", ex);
        }
    }

    private String normalizeMetricName(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, AdminPendingChangeSetSummary> buildPendingChangeSetsMap(List<UUID> metricIds) {
        if (metricIds.isEmpty()) {
            return Map.of();
        }
        List<ReferenceDataChangeSet> draftSets = referenceDataChangeSetRepository
                .findAllByEntityIdInAndStatusOrderByCreatedAtDesc(metricIds, "draft");
        Map<UUID, AdminPendingChangeSetSummary> result = new LinkedHashMap<>();
        for (ReferenceDataChangeSet cs : draftSets) {
            if (result.containsKey(cs.getEntityId())) {
                continue; // only the latest draft per metric
            }
            try {
                Map<String, Object> snapshot = objectMapper.readValue(cs.getChangesJson(), Map.class);
                String proposedName = snapshot.getOrDefault("name", "").toString();
                String proposedDisplayNameVi = snapshot.getOrDefault("displayNameVi", "").toString();
                String proposedUnit = snapshot.getOrDefault("unit", "").toString();
                Object rangesObj = snapshot.get("ranges");
                int proposedRangesCount = rangesObj instanceof List<?> rangesList ? rangesList.size() : 0;
                result.put(cs.getEntityId(), new AdminPendingChangeSetSummary(
                        cs.getId(),
                        cs.getOperation(),
                        cs.getCreatedAt(),
                        proposedName,
                        proposedDisplayNameVi,
                        proposedUnit,
                        proposedRangesCount
                ));
            } catch (JsonProcessingException ignored) {
                // corrupted JSON — skip
            }
        }
        return result;
    }
}
