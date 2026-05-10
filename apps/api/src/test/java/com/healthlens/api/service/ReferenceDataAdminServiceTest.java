package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.dto.request.AdminReferenceMetricRequest;
import com.healthlens.api.dto.request.AdminReferenceRangeRequest;
import com.healthlens.api.dto.response.AdminChangeSetDetailResponse;
import com.healthlens.api.dto.response.AdminReferenceChangeSetResponse;
import com.healthlens.api.dto.response.AdminReferenceMetricResponse;
import com.healthlens.api.entity.ReferenceDataChangeSet;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.entity.ReferenceRange;
import com.healthlens.api.entity.ReferenceRangeAuditLog;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.ReferenceDataChangeSetRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import com.healthlens.api.repository.ReferenceRangeAuditLogRepository;
import com.healthlens.api.repository.ReferenceRangeRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Mock
    private ReferenceRangeAuditLogRepository referenceRangeAuditLogRepository;

    @Mock
    private UserRepository userRepository;

    private ReferenceDataAdminService referenceDataAdminService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        referenceDataAdminService = new ReferenceDataAdminService(
                referenceMetricRepository,
                referenceRangeRepository,
                referenceDataChangeSetRepository,
                referenceRangeAuditLogRepository,
                userRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("createMetric tao metric pending va luu change set trong multi-admin mode")
    void createMetric_createsPendingMetric_MultiAdmin() {
        AdminReferenceMetricRequest request = buildRequest("Glucose");
        UUID adminId = UUID.randomUUID();

        // Multi-admin mode
        when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(2L);
        when(referenceMetricRepository.findByNameIgnoreCase("Glucose")).thenReturn(Optional.empty());
        when(referenceMetricRepository.save(any(ReferenceMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReferenceMetricResponse response = referenceDataAdminService.createMetric(adminId, request);

        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.rangesCount()).isEqualTo(1);

        ArgumentCaptor<ReferenceMetric> metricCaptor = ArgumentCaptor.forClass(ReferenceMetric.class);
        verify(referenceMetricRepository).save(metricCaptor.capture());
        assertThat(metricCaptor.getValue().getStatus()).isEqualTo("pending");
    }

    @Test
    @DisplayName("updateMetric tao pending change set trong multi-admin mode")
    void updateMetric_createsPendingChangeSet_MultiAdmin() {
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

        // Multi-admin mode
        when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(2L);
        when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.findByNameIgnoreCase("Glucose")).thenReturn(Optional.of(metric));
        when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class))).thenReturn(savedChangeSet);

        AdminReferenceChangeSetResponse response = referenceDataAdminService.updateMetric(adminId, metricId, request);

        assertThat(response.changeSetId()).isEqualTo(savedChangeSet.getId());
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.resultType()).isEqualTo("change-set-submitted");
        verify(referenceDataChangeSetRepository).save(any(ReferenceDataChangeSet.class));
    }

    @Test
    @DisplayName("updateMetric sua truc tiep metric trong single-admin mode")
    void updateMetric_updatesDirectly_SingleAdmin() {
        UUID adminId = UUID.randomUUID();
        UUID metricId = UUID.randomUUID();
        AdminReferenceMetricRequest request = buildRequest("Glucose");
        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(metricId);
        metric.setName("Glucose");
        metric.setDisplayNameVi("Duong huyet");
        metric.setUnit("mmol/L");
        metric.setStatus("active");

        // Single-admin mode
        when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(1L);
        when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.findByNameIgnoreCase("Glucose")).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.save(any(ReferenceMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId)).thenReturn(List.of());
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReferenceChangeSetResponse response = referenceDataAdminService.updateMetric(adminId, metricId, request);

        assertThat(response.changeSetId()).isNull();
        assertThat(response.resultType()).isEqualTo("updated");
        assertThat(response.status()).isEqualTo("active");
        verify(referenceMetricRepository).save(any(ReferenceMetric.class));
    }

    @Test
    @DisplayName("deactivateMetric soft delete metric va ranges trong single-admin mode")
    void deactivateMetric_marksMetricAndRangesDeactivated() {
        UUID adminId = UUID.randomUUID();
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

        // Single-admin mode
        when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(1L);
        when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
        when(referenceMetricRepository.save(any(ReferenceMetric.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId)).thenReturn(List.of(range));
        when(referenceRangeRepository.save(any(ReferenceRange.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReferenceChangeSetResponse response = referenceDataAdminService.deactivateMetric(adminId, metricId);

        assertThat(response.changeSetId()).isNull();
        assertThat(response.status()).isEqualTo("deactivated");
        assertThat(response.message()).isEqualTo("Đã ngưng áp dụng chỉ số trực tiếp.");
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

    // ========================================================================
    // Approval Workflow Tests (Story 7.4)
    // ========================================================================

    @Nested
    @DisplayName("Approval Workflow - Story 7.4")
    class ApprovalWorkflowTest {

        @Test
        @DisplayName("approveChangeSet applies data to production and sets status approved (AC #1)")
        void approveChangeSet_appliesDataAndSetsApproved() throws Exception {
            UUID changeSetId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            UUID metricId = UUID.randomUUID();

            ReferenceMetric metric = new ReferenceMetric();
            metric.setId(metricId);
            metric.setName("Glucose");
            metric.setDisplayNameVi("Đường huyết");
            metric.setUnit("mmol/L");
            metric.setStatus("active");

            Map<String, Object> snapshot = Map.of(
                    "name", "Glucose Updated",
                    "displayNameVi", "Đường huyết cập nhật",
                    "unit", "mg/dL",
                    "status", "draft",
                    "ranges", List.of(Map.of(
                            "minValue", 70,
                            "maxValue", 100,
                            "attentionMin", 60,
                            "attentionMax", 110,
                            "gender", "male",
                            "minAge", 18
                    ))
            );

            ReferenceDataChangeSet cs = new ReferenceDataChangeSet();
            cs.setId(changeSetId);
            cs.setAdminId(UUID.randomUUID());
            cs.setEntityType("METRIC");
            cs.setEntityId(metricId);
            cs.setOperation("UPDATE");
            cs.setChangesJson(objectMapper.writeValueAsString(snapshot));
            cs.setStatus("pending");
            cs.setCreatedAt(Instant.now());

            ReferenceRange range = new ReferenceRange();
            range.setId(UUID.randomUUID());
            range.setMetric(metric);

            when(referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "pending"))
                    .thenReturn(Optional.of(cs));
            when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
            when(referenceMetricRepository.save(any(ReferenceMetric.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId))
                    .thenReturn(List.of(range)); // Mock range for audit log
            when(referenceRangeRepository.save(any(ReferenceRange.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceRangeAuditLogRepository.save(any(ReferenceRangeAuditLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AdminReferenceChangeSetResponse response = referenceDataAdminService.approveChangeSet(changeSetId, reviewerId);

            assertThat(response.status()).isEqualTo("approved");
            assertThat(response.resultType()).isEqualTo("approved");

            // Verify metric was updated
            ArgumentCaptor<ReferenceMetric> metricCaptor = ArgumentCaptor.forClass(ReferenceMetric.class);
            verify(referenceMetricRepository).save(metricCaptor.capture());
            ReferenceMetric savedMetric = metricCaptor.getValue();
            assertThat(savedMetric.getName()).isEqualTo("Glucose Updated");
            assertThat(savedMetric.getUnit()).isEqualTo("mg/dL");
            assertThat(savedMetric.getStatus()).isEqualTo("active");

            // Verify new range was created (actually 2 calls: one for audit log lookup, one for saving new range)
            verify(referenceRangeRepository, times(1)).save(any(ReferenceRange.class));

            // Verify change set was updated
            ArgumentCaptor<ReferenceDataChangeSet> csCaptor = ArgumentCaptor.forClass(ReferenceDataChangeSet.class);
            verify(referenceDataChangeSetRepository).save(csCaptor.capture());
            assertThat(csCaptor.getValue().getStatus()).isEqualTo("approved");
            assertThat(csCaptor.getValue().getReviewerId()).isEqualTo(reviewerId);
            assertThat(csCaptor.getValue().getApprovedAt()).isNotNull();

            // Verify audit log was written
            verify(referenceRangeAuditLogRepository).save(any(ReferenceRangeAuditLog.class));
        }

        @Test
        @DisplayName("rejectChangeSet sets status rejected and does not change data (AC #2)")
        void rejectChangeSet_setsRejectedStatusAndDoesNotApply() {
            UUID changeSetId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            String reason = "Dữ liệu chưa chính xác";

            ReferenceDataChangeSet cs = new ReferenceDataChangeSet();
            cs.setId(changeSetId);
            cs.setAdminId(UUID.randomUUID());
            cs.setEntityType("METRIC");
            cs.setEntityId(UUID.randomUUID());
            cs.setOperation("UPDATE");
            cs.setChangesJson("{}");
            cs.setStatus("pending");
            cs.setCreatedAt(Instant.now());

            ReferenceRange range = new ReferenceRange();
            range.setId(UUID.randomUUID());

            when(referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "pending"))
                    .thenReturn(Optional.of(cs));
            when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(any()))
                    .thenReturn(List.of(range)); // Mock range for audit log
            when(referenceRangeAuditLogRepository.save(any(ReferenceRangeAuditLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AdminReferenceChangeSetResponse response = referenceDataAdminService.rejectChangeSet(
                    changeSetId, reviewerId, reason);

            assertThat(response.status()).isEqualTo("rejected");
            assertThat(response.resultType()).isEqualTo("rejected");

            // Verify change set was saved with rejection info
            ArgumentCaptor<ReferenceDataChangeSet> csCaptor = ArgumentCaptor.forClass(ReferenceDataChangeSet.class);
            verify(referenceDataChangeSetRepository).save(csCaptor.capture());
            ReferenceDataChangeSet savedCs = csCaptor.getValue();
            assertThat(savedCs.getStatus()).isEqualTo("rejected");
            assertThat(savedCs.getReviewerId()).isEqualTo(reviewerId);
            assertThat(savedCs.getRejectionReason()).isEqualTo(reason);

            // Verify NO metric changes were applied
            verify(referenceMetricRepository, never()).save(any(ReferenceMetric.class));
            verify(referenceRangeRepository, never()).save(any(ReferenceRange.class));

            // Verify audit log was still written
            verify(referenceRangeAuditLogRepository).save(any(ReferenceRangeAuditLog.class));
        }

        @Test
        @DisplayName("approveChangeSet throws when change set not found or not pending")
        void approveChangeSet_throwsWhenNotFound() {
            UUID changeSetId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();

            when(referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "pending"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> referenceDataAdminService.approveChangeSet(changeSetId, reviewerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không tìm thấy change set đang chờ duyệt");
        }

        @Test
        @DisplayName("rejectChangeSet throws when change set not found or not pending")
        void rejectChangeSet_throwsWhenNotFound() {
            UUID changeSetId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();

            when(referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "pending"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> referenceDataAdminService.rejectChangeSet(changeSetId, reviewerId, "lý do"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không tìm thấy change set đang chờ duyệt");
        }

        @Test
        @DisplayName("listPendingChangeSets returns only pending change sets")
        void listPendingChangeSets_returnsPendingOnly() {
            ReferenceDataChangeSet cs1 = new ReferenceDataChangeSet();
            cs1.setId(UUID.randomUUID());
            cs1.setAdminId(UUID.randomUUID());
            cs1.setEntityType("METRIC");
            cs1.setEntityId(UUID.randomUUID());
            cs1.setOperation("CREATE");
            cs1.setChangesJson("{}");
            cs1.setStatus("pending");
            cs1.setCreatedAt(Instant.now());

            when(referenceDataChangeSetRepository.findAllByStatusOrderByCreatedAtDesc("pending"))
                    .thenReturn(List.of(cs1));

            List<AdminChangeSetDetailResponse> result = referenceDataAdminService.listPendingChangeSets();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(cs1.getId());
            assertThat(result.get(0).status()).isEqualTo("pending");
        }

        @Test
        @DisplayName("submitChangeSetForApproval transitions draft to pending")
        void submitChangeSetForApproval_transitionsDraftToPending() {
            UUID changeSetId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            ReferenceDataChangeSet cs = new ReferenceDataChangeSet();
            cs.setId(changeSetId);
            cs.setAdminId(adminId);
            cs.setEntityType("METRIC");
            cs.setEntityId(UUID.randomUUID());
            cs.setOperation("UPDATE");
            cs.setChangesJson("{}");
            cs.setStatus("draft");
            cs.setCreatedAt(Instant.now());

            when(referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "draft"))
                    .thenReturn(Optional.of(cs));
            when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            // Submit requires multi-admin mode
            when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(2L);

            AdminReferenceChangeSetResponse response = referenceDataAdminService.submitChangeSetForApproval(changeSetId, adminId);

            assertThat(response.status()).isEqualTo("pending");
            assertThat(response.resultType()).isEqualTo("submitted");

            ArgumentCaptor<ReferenceDataChangeSet> captor = ArgumentCaptor.forClass(ReferenceDataChangeSet.class);
            verify(referenceDataChangeSetRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("pending");
        }

        @Test
        @DisplayName("approveChangeSet with CREATE operation activates draft metric (AC #3)")
        void approveChangeSet_createOperation_activatesDraftMetric() throws Exception {
            UUID changeSetId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            UUID metricId = UUID.randomUUID();

            ReferenceMetric draftMetric = new ReferenceMetric();
            draftMetric.setId(metricId);
            draftMetric.setName("HbA1c");
            draftMetric.setDisplayNameVi("HbA1c");
            draftMetric.setUnit("%");
            draftMetric.setStatus("draft");

            ReferenceRange draftRange = new ReferenceRange();
            draftRange.setId(UUID.randomUUID());
            draftRange.setMetric(draftMetric);
            draftRange.setMinValue(BigDecimal.valueOf(4.0));
            draftRange.setMaxValue(BigDecimal.valueOf(5.6));
            draftRange.setAttentionMin(BigDecimal.valueOf(3.5));
            draftRange.setAttentionMax(BigDecimal.valueOf(6.5));
            draftRange.setStatus("draft");

            Map<String, Object> snapshot = Map.of(
                    "name", "HbA1c",
                    "displayNameVi", "HbA1c",
                    "unit", "%",
                    "status", "draft",
                    "ranges", List.of()
            );

            ReferenceDataChangeSet cs = new ReferenceDataChangeSet();
            cs.setId(changeSetId);
            cs.setAdminId(UUID.randomUUID());
            cs.setEntityType("METRIC");
            cs.setEntityId(metricId);
            cs.setOperation("CREATE");
            cs.setChangesJson(objectMapper.writeValueAsString(snapshot));
            cs.setStatus("pending");
            cs.setCreatedAt(Instant.now());

            when(referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "pending"))
                    .thenReturn(Optional.of(cs));
            when(referenceMetricRepository.findById(metricId))
                    .thenReturn(Optional.of(draftMetric));
            when(referenceMetricRepository.save(any(ReferenceMetric.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId))
                    .thenReturn(List.of(draftRange));
            when(referenceRangeRepository.save(any(ReferenceRange.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceRangeAuditLogRepository.save(any(ReferenceRangeAuditLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AdminReferenceChangeSetResponse response = referenceDataAdminService.approveChangeSet(changeSetId, reviewerId);

            assertThat(response.status()).isEqualTo("approved");

            // Verify metric was activated
            ArgumentCaptor<ReferenceMetric> metricCaptor = ArgumentCaptor.forClass(ReferenceMetric.class);
            verify(referenceMetricRepository).save(metricCaptor.capture());
            assertThat(metricCaptor.getValue().getStatus()).isEqualTo("active");

            // Verify range was activated
            ArgumentCaptor<ReferenceRange> rangeCaptor = ArgumentCaptor.forClass(ReferenceRange.class);
            verify(referenceRangeRepository).save(rangeCaptor.capture());
            assertThat(rangeCaptor.getValue().getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("publishChangeSet directly activates draft in single-admin mode")
        void publishChangeSet_activatesDraftInSingleAdminMode() throws Exception {
            UUID changeSetId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();
            UUID metricId = UUID.randomUUID();

            // Single admin mode
            when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(1L);

            ReferenceMetric metric = new ReferenceMetric();
            metric.setId(metricId);
            metric.setName("Glucose");
            metric.setDisplayNameVi("Đường huyết");
            metric.setUnit("mmol/L");
            metric.setStatus("draft");

            ReferenceRange range = new ReferenceRange();
            range.setId(UUID.randomUUID());

            Map<String, Object> snapshot = Map.of(
                    "name", "Glucose",
                    "displayNameVi", "Đường huyết",
                    "unit", "mmol/L",
                    "status", "draft",
                    "ranges", List.of()
            );

            ReferenceDataChangeSet cs = new ReferenceDataChangeSet();
            cs.setId(changeSetId);
            cs.setAdminId(adminId);
            cs.setEntityType("METRIC");
            cs.setEntityId(metricId);
            cs.setOperation("CREATE");
            cs.setChangesJson(objectMapper.writeValueAsString(snapshot));
            cs.setStatus("draft");
            cs.setCreatedAt(Instant.now());

            when(referenceDataChangeSetRepository.findById(changeSetId))
                    .thenReturn(Optional.of(cs));
            when(referenceMetricRepository.findById(metricId))
                    .thenReturn(Optional.of(metric));
            when(referenceMetricRepository.save(any(ReferenceMetric.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceRangeRepository.findAllByMetric_IdOrderByGenderAscMinAgeAscMaxAgeAsc(metricId))
                    .thenReturn(List.of(range)); // For audit log and range activation
            when(referenceRangeRepository.save(any(ReferenceRange.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceDataChangeSetRepository.save(any(ReferenceDataChangeSet.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(referenceRangeAuditLogRepository.save(any(ReferenceRangeAuditLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AdminReferenceChangeSetResponse response = referenceDataAdminService.publishChangeSet(changeSetId, adminId);

            assertThat(response.status()).isEqualTo("approved");
            assertThat(response.resultType()).isEqualTo("published");

            // Verify metric was activated
            ArgumentCaptor<ReferenceMetric> metricCaptor = ArgumentCaptor.forClass(ReferenceMetric.class);
            verify(referenceMetricRepository).save(metricCaptor.capture());
            assertThat(metricCaptor.getValue().getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("approveChangeSet blocks self-approval in multi-admin mode (AC #4)")
        void approveChangeSet_blocksSelfApprovalInMultiAdminMode() throws Exception {
            UUID adminId = UUID.randomUUID();
            UUID changeSetId = UUID.randomUUID();

            // Multi-admin mode (2 admins)
            when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(2L);

            ReferenceDataChangeSet cs = new ReferenceDataChangeSet();
            cs.setId(changeSetId);
            cs.setAdminId(adminId); // Same admin created and tries to approve
            cs.setEntityType("METRIC");
            cs.setEntityId(UUID.randomUUID());
            cs.setOperation("UPDATE");
            cs.setChangesJson("{}");
            cs.setStatus("pending");
            cs.setCreatedAt(Instant.now());

            when(referenceDataChangeSetRepository.findByIdAndStatus(changeSetId, "pending"))
                    .thenReturn(Optional.of(cs));

            assertThatThrownBy(() -> referenceDataAdminService.approveChangeSet(changeSetId, adminId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Không thể phê duyệt change set do chính bạn tạo");
        }

        @Test
        @DisplayName("publishChangeSet blocks in multi-admin mode")
        void publishChangeSet_blocksInMultiAdminMode() {
            UUID changeSetId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            // Multi-admin mode
            when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(2L);

            assertThatThrownBy(() -> referenceDataAdminService.publishChangeSet(changeSetId, adminId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("multi-admin");
        }

        @Test
        @DisplayName("submitChangeSetForApproval blocks in single-admin mode")
        void submitChangeSetForApproval_blocksInSingleAdminMode() {
            UUID changeSetId = UUID.randomUUID();
            UUID adminId = UUID.randomUUID();

            // Single-admin mode
            when(userRepository.countByRole(UserRole.ROLE_ADMIN)).thenReturn(1L);

            assertThatThrownBy(() -> referenceDataAdminService.submitChangeSetForApproval(changeSetId, adminId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("single-admin");
        }
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
