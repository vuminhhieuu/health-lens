package com.healthlens.api.service;

import com.healthlens.api.dto.MetricClassificationDto;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.ReferenceMetricAlias;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.entity.ReferenceRange;
import com.healthlens.api.repository.ReferenceMetricAliasRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import com.healthlens.api.repository.ReferenceRangeAuditLogRepository;
import com.healthlens.api.repository.ReferenceRangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

    @Mock
    private ReferenceMetricRepository referenceMetricRepository;

    @Mock
    private ReferenceMetricAliasRepository referenceMetricAliasRepository;

    @Mock
    private ReferenceRangeRepository referenceRangeRepository;

    @Mock
    private ReferenceRangeAuditLogRepository referenceRangeAuditLogRepository;

    private ReferenceDataService referenceDataService;

    @BeforeEach
    void setUp() {
        referenceDataService = new ReferenceDataService(
                referenceMetricRepository,
                referenceMetricAliasRepository,
                referenceRangeRepository,
                referenceRangeAuditLogRepository
        );
    }

    @Test
    @DisplayName("classifyMetric tra ve normal khi nam trong nguong chuan")
    void classifyMetric_normal() {
        mockMetricWithRange("Glucose", buildRange(3.9, 5.5, 3.5, 6.9, "male", 18, 65));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Glucose",
                "5.2",
                buildProfile(30, "male"),
                LocalDate.now()
        );

        assertThat(result.status()).isEqualTo("normal");
        assertThat(result.referenceRange()).isNotNull();
    }

    @Test
    @DisplayName("classifyMetric tra ve attention khi gan nguong")
    void classifyMetric_attention() {
        mockMetricWithRange("Glucose", buildRange(3.9, 5.5, 3.5, 6.9, "male", 18, 65));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Glucose",
                "5.8",
                buildProfile(30, "male"),
                LocalDate.now()
        );

        assertThat(result.status()).isEqualTo("attention");
    }

    @Test
    @DisplayName("classifyMetric tra ve abnormal khi vuot attention range")
    void classifyMetric_abnormal() {
        mockMetricWithRange("Glucose", buildRange(3.9, 5.5, 3.5, 6.9, "male", 18, 65));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Glucose",
                "7.2",
                buildProfile(30, "male"),
                LocalDate.now()
        );

        assertThat(result.status()).isEqualTo("abnormal");
    }

    @Test
    @DisplayName("classifyMetric tra ve no_data khi khong co reference")
    void classifyMetric_noData() {
        when(referenceMetricRepository.findByNameIgnoreCase("Unknown")).thenReturn(Optional.empty());
        when(referenceMetricAliasRepository.findByAliasNormalizedAndActiveTrue("unknown")).thenReturn(Optional.empty());

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Unknown",
                "5.0",
                buildProfile(30, "male"),
                LocalDate.now()
        );

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
        when(referenceRangeRepository.findActiveRangesByMetricId(metric.getId()))
                .thenReturn(List.of(buildRange(3.9, 5.5, 3.5, 6.9, "male", 18, 65)));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Duong huyet",
                "5.2",
                buildProfile(30, "male"),
                LocalDate.now()
        );

        assertThat(result.status()).isEqualTo("normal");
    }

    @Test
    @DisplayName("classifyMetric resolve alias co dau tieng Viet")
    void classifyMetric_aliasCoDauTiengViet() {
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(UUID.randomUUID());
        metric.setName("Glucose");
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");

        ReferenceMetricAlias alias = new ReferenceMetricAlias();
        alias.setMetric(metric);
        alias.setAliasNormalized("duonghuyet");
        alias.setActive(true);

        when(referenceMetricRepository.findByNameIgnoreCase("Đường huyết")).thenReturn(Optional.empty());
        when(referenceMetricAliasRepository.findByAliasNormalizedAndActiveTrue("duonghuyet")).thenReturn(Optional.of(alias));
        when(referenceRangeRepository.findActiveRangesByMetricId(metric.getId()))
                .thenReturn(List.of(buildRange(3.9, 5.5, 3.5, 6.9, null, 18, 65)));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Đường huyết",
                "5.2",
                buildProfile(30, "female"),
                LocalDate.now()
        );

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
        when(referenceRangeRepository.findActiveRangesByMetricId(metric.getId()))
                .thenReturn(List.of(buildRange(130.0, 170.0, 120.0, 180.0, "male", 18, 65)));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "HGB",
                "125",
                buildProfile(30, "male"),
                LocalDate.now()
        );

        assertThat(result.status()).isEqualTo("attention");
    }

    @Test
    @DisplayName("classifyMetric uu tien range gender+age truoc default")
    void classifyMetric_priorityGenderAndAge() {
        ReferenceRange defaultRange = buildRange(4.0, 6.0, 3.5, 6.5, null, null, null);
        ReferenceRange genderAgeRange = buildRange(3.9, 5.5, 3.5, 6.9, "female", 18, 45);
        mockMetricWithRanges("Glucose", List.of(defaultRange, genderAgeRange));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Glucose",
                "5.2",
                buildProfile(30, "female"),
                LocalDate.now()
        );

        assertThat(result.referenceRange()).isNotNull();
        assertThat(result.referenceRange().max()).isEqualByComparingTo("5.5");
        assertThat(result.rangeContext()).isNotNull();
        assertThat(result.rangeContext().gender()).isEqualTo("female");
        assertThat(result.rangeContext().ageRange()).isEqualTo("18-45");
    }

    @Test
    @DisplayName("classifyMetric fallback ve default khi thieu birthDate")
    void classifyMetric_fallbackDefaultWhenAgeMissing() {
        ReferenceRange defaultRange = buildRange(4.0, 6.0, 3.5, 6.5, null, null, null);
        ReferenceRange ageSpecific = buildRange(3.9, 5.5, 3.5, 6.9, null, 18, 45);
        mockMetricWithRanges("Glucose", List.of(defaultRange, ageSpecific));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Glucose",
                "5.2",
                buildProfile(null, "female"),
                LocalDate.now()
        );

        assertThat(result.referenceRange()).isNotNull();
        assertThat(result.referenceRange().max()).isEqualByComparingTo("6.0");
    }

    @Test
    @DisplayName("classifyMetric fallback ve default khi thieu gender")
    void classifyMetric_fallbackDefaultWhenGenderMissing() {
        ReferenceRange defaultRange = buildRange(4.0, 6.0, 3.5, 6.5, null, null, null);
        ReferenceRange genderSpecific = buildRange(3.9, 5.5, 3.5, 6.9, "female", null, null);
        mockMetricWithRanges("Glucose", List.of(defaultRange, genderSpecific));

        MetricClassificationDto result = referenceDataService.classifyMetric(
                "Glucose",
                "5.2",
                buildProfile(30, null),
                LocalDate.now()
        );

        assertThat(result.referenceRange()).isNotNull();
        assertThat(result.referenceRange().max()).isEqualByComparingTo("6.0");
        verify(referenceRangeAuditLogRepository).save(org.mockito.ArgumentMatchers.any());
    }

    private void mockMetricWithRange(String metricName, ReferenceRange range) {
        mockMetricWithRanges(metricName, List.of(range));
    }

    private void mockMetricWithRanges(String metricName, List<ReferenceRange> ranges) {
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(UUID.randomUUID());
        metric.setName(metricName);
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");

        when(referenceMetricRepository.findByNameIgnoreCase(metricName)).thenReturn(Optional.of(metric));
        when(referenceRangeRepository.findActiveRangesByMetricId(metric.getId())).thenReturn(ranges);
    }

    private ReferenceRange buildRange(
            double min,
            double max,
            double attentionMin,
            double attentionMax,
            String gender,
            Integer minAge,
            Integer maxAge
    ) {
        ReferenceRange range = new ReferenceRange();
        range.setId(UUID.randomUUID());
        range.setMinValue(BigDecimal.valueOf(min));
        range.setMaxValue(BigDecimal.valueOf(max));
        range.setAttentionMin(BigDecimal.valueOf(attentionMin));
        range.setAttentionMax(BigDecimal.valueOf(attentionMax));
        range.setGender(gender);
        range.setMinAge(minAge);
        range.setMaxAge(maxAge);
        return range;
    }

    private Profile buildProfile(Integer age, String gender) {
        Profile profile = new Profile();
        if (age != null) {
            profile.setBirthDate(LocalDate.now().minusYears(age));
        }
        profile.setGender(gender);
        return profile;
    }
}
