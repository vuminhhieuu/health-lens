package com.healthlens.api.service.admin;

import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.admin.AuditLogPageDto;
import com.healthlens.api.entity.AuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.repository.AuditLogRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ReferenceMetricRepository referenceMetricRepository;

    @InjectMocks
    private AdminAuditLogService adminAuditLogService;

    @Test
    @DisplayName("query áp dụng pagination và map dữ liệu audit sang DTO")
    void query_withFiltersAndPagination() {
        UUID metricId = UUID.randomUUID();
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction("UPDATE_REFERENCE_METRIC_DISPLAY");
        row.setResourceType(AuditResourceTypes.REFERENCE_DATA);
        row.setResourceId(metricId);
        row.setOldValueJson("{\"displayNameVi\":\"Đường huyết\"}");
        row.setNewValueJson("{\"displayNameVi\":\"Glucose\"}");
        row.setIpAddress("203.0.113.10");
        row.setCreatedAt(Instant.parse("2026-05-10T09:15:00Z"));
        User actor = new User();
        actor.setEmail("admin@healthlens.vn");
        row.setActor(actor);

        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(metricId);
        metric.setName("Glucose");
        metric.setDisplayNameVi("Đường huyết");
        when(referenceMetricRepository.findById(metricId)).thenReturn(Optional.of(metric));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));

        AuditLogPageDto page = adminAuditLogService.query(
                AuditResourceTypes.REFERENCE_DATA,
                metricId,
                "admin@healthlens.vn",
                null,
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-31T23:59:59Z"),
                0,
                50
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().actorEmail()).isEqualTo("admin@healthlens.vn");
        assertThat(page.content().getFirst().entityLabel()).isEqualTo("Đường huyết (Glucose)");
    }

    @Test
    @DisplayName("startOfUtcDay/endOfUtcDayInclusive trả đúng biên ngày UTC")
    void utcDayBoundaries() {
        LocalDate day = LocalDate.of(2026, 5, 10);
        assertThat(AdminAuditLogService.startOfUtcDay(day))
                .isEqualTo(Instant.parse("2026-05-10T00:00:00Z"));
        assertThat(AdminAuditLogService.endOfUtcDayInclusive(day))
                .isEqualTo(Instant.parse("2026-05-10T23:59:59.999999999Z"));
    }
}
