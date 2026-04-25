package com.healthlens.api.service;

import com.healthlens.api.dto.MetricClassificationDto;
import com.healthlens.api.entity.ReferenceMetricAlias;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.entity.ReferenceRange;
import com.healthlens.api.repository.ReferenceMetricAliasRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import com.healthlens.api.repository.ReferenceRangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    @Mock
    private ReferenceMetricRepository referenceMetricRepository;

    @Mock
    private ReferenceMetricAliasRepository referenceMetricAliasRepository;

    @Mock
    private ReferenceRangeRepository referenceRangeRepository;

    private ReferenceDataService referenceDataService;

    @BeforeEach
    void setUp() {
        referenceDataService = new ReferenceDataService(
                referenceMetricRepository,
                referenceMetricAliasRepository,
                referenceRangeRepository
        );
    }

    @Test
    @DisplayName("classifyMetric tra ve normal khi nam trong nguong chuan")
    void classifyMetric_normal() {
        mockMetricWithRange("Glucose", buildRange(3.9, 5.5, 3.5, 6.9));

        MetricClassificationDto result = referenceDataService.classifyMetric("Glucose", "5.2", 30, "male");

        assertThat(result.status()).isEqualTo("normal");
        assertThat(result.referenceRange()).isNotNull();
    }

    @Test
    @DisplayName("classifyMetric tra ve attention khi gan nguong")
    void classifyMetric_attention() {
        mockMetricWithRange("Glucose", buildRange(3.9, 5.5, 3.5, 6.9));

        MetricClassificationDto result = referenceDataService.classifyMetric("Glucose", "5.8", 30, "male");

        assertThat(result.status()).isEqualTo("attention");
    }

    @Test
    @DisplayName("classifyMetric tra ve abnormal khi vuot attention range")
    void classifyMetric_abnormal() {
        mockMetricWithRange("Glucose", buildRange(3.9, 5.5, 3.5, 6.9));

        MetricClassificationDto result = referenceDataService.classifyMetric("Glucose", "7.2", 30, "male");

        assertThat(result.status()).isEqualTo("abnormal");
    }

    @Test
    @DisplayName("classifyMetric tra ve no_data khi khong co reference")
    void classifyMetric_noData() {
        when(referenceMetricRepository.findByNameIgnoreCase("Unknown")).thenReturn(Optional.empty());
        when(referenceMetricAliasRepository.findByAliasNormalizedAndActiveTrue("unknown")).thenReturn(Optional.empty());

        MetricClassificationDto result = referenceDataService.classifyMetric("Unknown", "5.0", 30, "male");

        assertThat(result.status()).isEqualTo("no_data");
        assertThat(result.referenceRange()).isNull();
    }

    @Test
    @DisplayName("classifyMetric resolve alias duong huyet")
    void classifyMetric_aliasDuongHuyet() {
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(UUID.randomUUID());
        metric.setName("Glucose");
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");

        ReferenceMetricAlias alias = new ReferenceMetricAlias();
        alias.setMetric(metric);
        alias.setAliasNormalized("duonghuyet");
        alias.setActive(true);

        when(referenceMetricRepository.findByNameIgnoreCase("Duong huyet")).thenReturn(Optional.empty());
        when(referenceMetricAliasRepository.findByAliasNormalizedAndActiveTrue("duonghuyet")).thenReturn(Optional.of(alias));
        when(referenceRangeRepository.findMatchingRanges(metric.getId(), 30, "male"))
                .thenReturn(List.of(buildRange(3.9, 5.5, 3.5, 6.9)));

        MetricClassificationDto result = referenceDataService.classifyMetric("Duong huyet", "5.2", 30, "male");

        assertThat(result.status()).isEqualTo("normal");
    }

    @Test
    @DisplayName("classifyMetric resolve alias hgb")
    void classifyMetric_aliasHgb() {
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(UUID.randomUUID());
        metric.setName("Hemoglobin");
        metric.setDisplayNameVi("Huyet sac to");
        metric.setUnit("g/L");

        ReferenceMetricAlias alias = new ReferenceMetricAlias();
        alias.setMetric(metric);
        alias.setAliasNormalized("hgb");
        alias.setActive(true);

        when(referenceMetricRepository.findByNameIgnoreCase("HGB")).thenReturn(Optional.empty());
        when(referenceMetricAliasRepository.findByAliasNormalizedAndActiveTrue("hgb")).thenReturn(Optional.of(alias));
        when(referenceRangeRepository.findMatchingRanges(metric.getId(), 30, "male"))
                .thenReturn(List.of(buildRange(130.0, 170.0, 120.0, 180.0)));

        MetricClassificationDto result = referenceDataService.classifyMetric("HGB", "125", 30, "male");

        assertThat(result.status()).isEqualTo("attention");
    }

    private void mockMetricWithRange(String metricName, ReferenceRange range) {
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(UUID.randomUUID());
        metric.setName(metricName);
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");

        when(referenceMetricRepository.findByNameIgnoreCase(metricName)).thenReturn(Optional.of(metric));
        when(referenceRangeRepository.findMatchingRanges(metric.getId(), 30, "male")).thenReturn(List.of(range));
    }

    private ReferenceRange buildRange(double min, double max, double attentionMin, double attentionMax) {
        ReferenceRange range = new ReferenceRange();
        range.setMinValue(BigDecimal.valueOf(min));
        range.setMaxValue(BigDecimal.valueOf(max));
        range.setAttentionMin(BigDecimal.valueOf(attentionMin));
        range.setAttentionMax(BigDecimal.valueOf(attentionMax));
        return range;
    }
}
