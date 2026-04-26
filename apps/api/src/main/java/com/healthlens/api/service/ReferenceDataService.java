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
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.UUID;

@Service
public class ReferenceDataService {
    private static final Map<String, String> RELATION_HINTS = Map.ofEntries(
            Map.entry("ALT", "gan và nguy cơ tổn thương tế bào gan"),
            Map.entry("AST", "gan, cơ và đánh giá tổn thương mô"),
            Map.entry("GPT", "gan và nguy cơ tổn thương tế bào gan"),
            Map.entry("GOT", "gan, cơ và đánh giá tổn thương mô"),
            Map.entry("GLUCOSE", "đường huyết và nguy cơ rối loạn chuyển hóa"),
            Map.entry("HBA1C", "kiểm soát đường huyết dài hạn"),
            Map.entry("HDL", "mức bảo vệ tim mạch"),
            Map.entry("LDL", "nguy cơ xơ vữa mạch và tim mạch"),
            Map.entry("TRIGLYCERIDE", "rối loạn mỡ máu và tim mạch"),
            Map.entry("CHOL", "mỡ máu tổng và tim mạch"),
            Map.entry("EO", "dị ứng, miễn dịch và ký sinh trùng"),
            Map.entry("EOS", "dị ứng, miễn dịch và ký sinh trùng"),
            Map.entry("BASO", "viêm, dị ứng và miễn dịch"),
            Map.entry("BAS", "viêm, dị ứng và miễn dịch"),
            Map.entry("WBC", "hệ miễn dịch và nhiễm trùng"),
            Map.entry("RBC", "vận chuyển oxy và tình trạng thiếu máu"),
            Map.entry("HGB", "vận chuyển oxy và thiếu máu"),
            Map.entry("PLT", "đông máu và nguy cơ chảy máu"),
            Map.entry("HBSAG", "sàng lọc nhiễm virus viêm gan B")
    );

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

    public String buildMetricKnowledgeSnippet(String metricName, String status, ReferenceRangeDto referenceRange) {
        String safeMetricName = metricName == null || metricName.isBlank() ? "chỉ số xét nghiệm" : metricName.trim();
        String normalizedKey = normalizeMetricName(safeMetricName).toUpperCase(Locale.ROOT);

        String displayName = resolveMetric(safeMetricName)
                .map(metric -> metric.getDisplayNameVi() != null ? metric.getDisplayNameVi() : metric.getName())
                .orElse(safeMetricName);

        String relation = RELATION_HINTS.getOrDefault(normalizedKey,
                "cân bằng miễn dịch, chuyển hóa và chức năng cơ quan tùy loại xét nghiệm");

        String rangeText = "không có";
        if (referenceRange != null && referenceRange.min() != null && referenceRange.max() != null) {
            String unit = referenceRange.unit() != null ? " " + referenceRange.unit() : "";
            rangeText = referenceRange.min().toPlainString() + " - " + referenceRange.max().toPlainString() + unit;
        }

        String impact = switch (status == null ? "unknown" : status.toLowerCase(Locale.ROOT)) {
            case "normal" -> "Khi trong ngưỡng, chỉ số này thường chưa gợi ý bất thường rõ.";
            case "attention" -> "Khi hơi lệch ngưỡng, nên theo dõi thêm cùng các chỉ số liên quan.";
            case "abnormal" -> "Khi lệch rõ ngưỡng, nguy cơ vấn đề sức khỏe liên quan có thể tăng.";
            default -> "Cần đối chiếu thêm với bối cảnh sức khỏe và các chỉ số liên quan.";
        };

        return """
                Metric identity: %s (%s).
                Clinical relation: chỉ số này thường liên quan đến %s.
                Out-of-range impact: %s
                Reference range context: %s.
                """.formatted(displayName, safeMetricName, relation, impact, rangeText);
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
