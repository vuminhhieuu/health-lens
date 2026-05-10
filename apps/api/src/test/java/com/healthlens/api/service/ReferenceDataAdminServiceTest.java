package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.request.AdminReferenceMetricRequest;
import com.healthlens.api.dto.request.AdminReferenceRangeRequest;
import com.healthlens.api.dto.response.AdminReferenceChangeSetResponse;
import com.healthlens.api.dto.response.AdminReferenceMetricResponse;
import com.healthlens.api.entity.ReferenceDataChangeSet;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.entity.ReferenceRange;
import com.healthlens.api.repository.ReferenceDataChangeSetRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import com.healthlens.api.repository.ReferenceRangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataAdminServiceTest {

    @Mock
    private ReferenceMetricRepository referenceMetricRepository;

    @Mock
    private ReferenceRangeRepository referenceRangeRepository;

    @Mock
    private ReferenceDataChangeSetRepository referenceDataChangeSetRepository;

    private ReferenceDataAdminService referenceDataAdminService;

    @BeforeEach
    void setUp() {
        referenceDataAdminService = new ReferenceDataAdminService(
                referenceMetricRepository,
                referenceRangeRepository,
                referenceDataChangeSetRepository,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("createMetric tao metric draft va luu change set")
    void createMetric_createsDraftMetric() {
        AdminReferenceMetricRequest request = buildRequest("Glucose");
        UUID adminId = UUID.randomUUID();

        when(referenceMetricRepository.findByNameIgnoreCase("Glucose")).thenReturn(Optional.empty());
        when(referenceMetricRepository.save(any(ReferenceMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReferenceMetricResponse response = referenceDataAdminService.createMetric(adminId, request);

        assertThat(response.status()).isEqualTo("draft");
        assertThat(response.rangesCount()).isEqualTo(1);

        ArgumentCaptor<ReferenceMetric> metricCaptor = ArgumentCaptor.forClass(ReferenceMetric.class);
        verify(referenceMetricRepository).save(metricCaptor.capture());
        assertThat(metricCaptor.getValue().getStatus()).isEqualTo("draft");
    }

    @Test
    @DisplayName("updateMetric tao draft change set khong sua production")
    void updateMetric_createsChangeSet() {
        UUID adminId = UUID.randomUUID();
        UUID metricId = UUID.randomUUID();
        AdminReferenceMetricRequest request = buildRequest("Glucose");
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(metricId);
        metric.setName("Glucose");
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");
        metric.setStatus("active");

        ReferenceDataChangeSet savedChangeSet = new ReferenceDataChangeSet();
        savedChangeSet.setId(UUID.randomUUID());

        when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.findByNameIgnoreCase("Glucose")).thenReturn(Optional.of(metric));
        when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class))).thenReturn(savedChangeSet);

        AdminReferenceChangeSetResponse response = referenceDataAdminService.updateMetric(adminId, metricId, request);

        assertThat(response.changeSetId()).isEqualTo(savedChangeSet.getId());
        assertThat(response.status()).isEqualTo("draft");
        assertThat(response.resultType()).isEqualTo("change-set-created");
        verify(referenceDataChangeSetRepository).save(any(ReferenceDataChangeSet.class));
    }

    @Test
    @DisplayName("updateMetric sua truc tiep metric draft")
    void updateMetric_updatesDraftDirectly() {
        UUID adminId = UUID.randomUUID();
        UUID metricId = UUID.randomUUID();
        AdminReferenceMetricRequest request = new AdminReferenceMetricRequest(
                "Glucose",
                "Đường huyết",
                "mmol/L",
                List.of(
                        new AdminReferenceRangeRequest(
                                BigDecimal.valueOf(3.9),
                                BigDecimal.valueOf(5.5),
                                BigDecimal.valueOf(3.5),
                                BigDecimal.valueOf(6.9),
                                "male",
                                18,
                                null
                        ),
                        new AdminReferenceRangeRequest(
                                BigDecimal.valueOf(4.0),
                                BigDecimal.valueOf(6.0),
                                BigDecimal.valueOf(3.5),
                                BigDecimal.valueOf(7.0),
                                "female",
                                18,
                                null
                        )
                )
        );
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(metricId);
        metric.setName("Glucose");
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");
        metric.setStatus("draft");

        when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.findByNameIgnoreCase("Glucose")).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.save(any(ReferenceMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId)).thenReturn(List.of());
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReferenceChangeSetResponse response = referenceDataAdminService.updateMetric(adminId, metricId, request);

        assertThat(response.changeSetId()).isNull();
        assertThat(response.resultType()).isEqualTo("draft-updated");
        verify(referenceRangeRepository, times(2)).save(any(ReferenceRange.class));
    }

    @Test
    @DisplayName("deactivateMetric soft delete metric va ranges")
    void deactivateMetric_marksMetricAndRangesDeactivated() {
        UUID metricId = UUID.randomUUID();
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(metricId);
        metric.setName("Glucose");
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");
        metric.setStatus("active");

        ReferenceRange range = new ReferenceRange();
        range.setId(UUID.randomUUID());
        range.setMetric(metric);
        range.setMinValue(BigDecimal.valueOf(3.9));
        range.setMaxValue(BigDecimal.valueOf(5.5));
        range.setAttentionMin(BigDecimal.valueOf(3.5));
        range.setAttentionMax(BigDecimal.valueOf(6.9));
        range.setStatus("active");

        when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.save(any(ReferenceMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId)).thenReturn(List.of(range));
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReferenceMetricResponse response = referenceDataAdminService.deactivateMetric(metricId);

        assertThat(response.status()).isEqualTo("deactivated");
        assertThat(response.ranges()).allMatch(item -> "deactivated".equals(item.status()));
    }

    @Test
    @DisplayName("reactivateMetric kich hoat lai metric va ranges")
    void reactivateMetric_marksMetricAndRangesActive() {
        UUID metricId = UUID.randomUUID();
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(metricId);
        metric.setName("Glucose");
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");
        metric.setStatus("deactivated");

        ReferenceRange range = new ReferenceRange();
        range.setId(UUID.randomUUID());
        range.setMetric(metric);
        range.setMinValue(BigDecimal.valueOf(3.9));
        range.setMaxValue(BigDecimal.valueOf(5.5));
        range.setAttentionMin(BigDecimal.valueOf(3.5));
        range.setAttentionMax(BigDecimal.valueOf(6.9));
        range.setStatus("deactivated");

        when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.save(any(ReferenceMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId)).thenReturn(List.of(range));
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReferenceMetricResponse response = referenceDataAdminService.reactivateMetric(metricId);

        assertThat(response.status()).isEqualTo("active");
        assertThat(response.ranges()).allMatch(item -> "active".equals(item.status()));
    }

    @Test
    @DisplayName("validateMetric reject min lon hon max")
    void validateMetric_rejectsInvalidRangeOrder() {
        AdminReferenceMetricRequest request = new AdminReferenceMetricRequest(
                "Glucose",
                "Duong huyet",
                "mmol/L",
                List.of(new AdminReferenceRangeRequest(
                        BigDecimal.valueOf(6.0),
                        BigDecimal.valueOf(5.0),
                        BigDecimal.valueOf(3.5),
                        BigDecimal.valueOf(6.9),
                        null,
                        18,
                        null
                ))
        );

        assertThatThrownBy(() -> referenceDataAdminService.createMetric(UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Giá trị tối thiểu phải nhỏ hơn giá trị tối đa");
    }

    @Test
    @DisplayName("validateMetric reject negative glucose")
    void validateMetric_rejectsNegativeGlucoseRange() {
        AdminReferenceMetricRequest request = new AdminReferenceMetricRequest(
                "Glucose",
                "Duong huyet",
                "mmol/L",
                List.of(new AdminReferenceRangeRequest(
                        BigDecimal.valueOf(-1.0),
                        BigDecimal.valueOf(5.0),
                        BigDecimal.valueOf(-2.0),
                        BigDecimal.valueOf(6.9),
                        null,
                        18,
                        null
                ))
        );

        assertThatThrownBy(() -> referenceDataAdminService.createMetric(UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không cho phép ngưỡng âm");
    }

    private AdminReferenceMetricRequest buildRequest(String name) {
        return new AdminReferenceMetricRequest(
                name,
                "Duong huyet",
                "mmol/L",
                List.of(new AdminReferenceRangeRequest(
                        BigDecimal.valueOf(3.9),
                        BigDecimal.valueOf(5.5),
                        BigDecimal.valueOf(3.5),
                        BigDecimal.valueOf(6.9),
                        null,
                        18,
                        null
                ))
        );
    }
}
