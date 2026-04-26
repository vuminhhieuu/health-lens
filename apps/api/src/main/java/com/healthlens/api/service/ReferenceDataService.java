package com.healthlens.api.service;

import com.healthlens.api.dto.MetricClassificationDto;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.entity.ReferenceData;
import com.healthlens.api.entity.ReferenceMetricAlias;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.entity.ReferenceRange;
import com.healthlens.api.repository.ReferenceMetricAliasRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import com.healthlens.api.repository.ReferenceRangeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Optional;
import java.util.Locale;
import java.util.UUID;

@Service
public class ReferenceDataService {

    private final ReferenceMetricRepository referenceMetricRepository;
    private final ReferenceMetricAliasRepository referenceMetricAliasRepository;
    private final ReferenceRangeRepository referenceRangeRepository;

    public ReferenceDataService(
            ReferenceMetricRepository referenceMetricRepository,
            ReferenceMetricAliasRepository referenceMetricAliasRepository,
            ReferenceRangeRepository referenceRangeRepository
    ) {
        this.referenceMetricRepository = referenceMetricRepository;
        this.referenceMetricAliasRepository = referenceMetricAliasRepository;
        this.referenceRangeRepository = referenceRangeRepository;
    }

    public Optional<ReferenceData> findById(String id) {
        try {
            UUID metricId = UUID.fromString(id);
            return referenceMetricRepository.findById(metricId).map(metric -> {
                Optional<ReferenceRange> rangeOpt = referenceRangeRepository.findMatchingRanges(metric.getId(), null, null)
                        .stream()
                        .findFirst();
                ReferenceData.ReferenceDataBuilder builder = ReferenceData.builder()
                        .id(id)
                        .type("metric")
                        .name(metric.getDisplayNameVi() != null ? metric.getDisplayNameVi() : metric.getName())
                        .unit(metric.getUnit())
                        .descriptionVi("Ngưỡng tham chiếu chuẩn cho chỉ số " + metric.getName());
                rangeOpt.ifPresent(range -> builder
                        .minValue(range.getMinValue() != null ? range.getMinValue().toPlainString() : null)
                        .maxValue(range.getMaxValue() != null ? range.getMaxValue().toPlainString() : null));
                return builder.build();
            });
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public MetricClassificationDto classifyMetric(String metricName, String rawValue, Integer age, String gender) {
        Optional<ReferenceRangeWithMeta> matched = findMatchingRange(metricName, age, gender);
        if (matched.isEmpty()) {
            return new MetricClassificationDto("no_data", null, null);
        }

        ReferenceRangeWithMeta data = matched.get();
        ReferenceRange range = data.range();
        ReferenceRangeDto rangeDto = new ReferenceRangeDto(
                range.getMinValue(),
                range.getMaxValue(),
                range.getAttentionMin(),
                range.getAttentionMax(),
                data.metric().getUnit()
        );

        Optional<BigDecimal> valueOpt = parseValue(rawValue);
        if (valueOpt.isEmpty()) {
            return new MetricClassificationDto("no_data", null, data.metric().getDisplayNameVi());
        }

        BigDecimal value = valueOpt.get();
        if (value.compareTo(range.getAttentionMin()) < 0 || value.compareTo(range.getAttentionMax()) > 0) {
            return new MetricClassificationDto("abnormal", rangeDto, data.metric().getDisplayNameVi());
        }
        if (value.compareTo(range.getMinValue()) < 0 || value.compareTo(range.getMaxValue()) > 0) {
            return new MetricClassificationDto("attention", rangeDto, data.metric().getDisplayNameVi());
        }
        return new MetricClassificationDto("normal", rangeDto, data.metric().getDisplayNameVi());
    }

    public Optional<ReferenceRangeDto> findReferenceRange(String metricName, Integer age, String gender) {
        return findMatchingRange(metricName, age, gender)
                .map(data -> new ReferenceRangeDto(
                        data.range().getMinValue(),
                        data.range().getMaxValue(),
                        data.range().getAttentionMin(),
                        data.range().getAttentionMax(),
                        data.metric().getUnit()
                ));
    }

    private Optional<ReferenceRangeWithMeta> findMatchingRange(String metricName, Integer age, String gender) {
        if (metricName == null || metricName.isBlank()) {
            return Optional.empty();
        }
        Optional<ReferenceMetric> metricOpt = resolveMetric(metricName);
        if (metricOpt.isEmpty()) {
            return Optional.empty();
        }

        ReferenceMetric metric = metricOpt.get();
        return referenceRangeRepository.findMatchingRanges(metric.getId(), age, normalizeGender(gender))
                .stream()
                .findFirst()
                .map(range -> new ReferenceRangeWithMeta(metric, range));
    }

    private Optional<ReferenceMetric> resolveMetric(String metricName) {
        String trimmedName = metricName.trim();
        Optional<ReferenceMetric> byName = referenceMetricRepository.findByNameIgnoreCase(trimmedName);
        if (byName.isPresent()) {
            return byName;
        }
        String normalized = normalizeMetricName(trimmedName);
        return referenceMetricAliasRepository.findByAliasNormalizedAndActiveTrue(normalized)
                .map(ReferenceMetricAlias::getMetric);
    }

    private String normalizeMetricName(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9]", "");
    }

    private Optional<BigDecimal> parseValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawValue.trim().replace(",", ".");
        normalized = normalized.replaceAll("[^0-9.\\-]", "");
        if (normalized.isBlank() || "-".equals(normalized) || ".".equals(normalized)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(normalized));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.isBlank()) {
            return null;
        }
        String value = gender.trim().toLowerCase();
        if ("male".equals(value) || "nam".equals(value)) {
            return "male";
        }
        if ("female".equals(value) || "nu".equals(value) || "nữ".equals(value)) {
            return "female";
        }
        return null;
    }

    private record ReferenceRangeWithMeta(ReferenceMetric metric, ReferenceRange range) {
    }
}
