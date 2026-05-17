package com.healthlens.api.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditOutcome;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.admin.AuditLogPageDto;
import com.healthlens.api.entity.AuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.repository.AuditLogRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ReferenceMetricRepository referenceMetricRepository;

    private AdminAuditLogService adminAuditLogService;

    @BeforeEach
    void setUp() {
        adminAuditLogService = new AdminAuditLogService(
                auditLogRepository,
                referenceMetricRepository,
                new ObjectMapper()
        );
    }

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
        when(referenceMetricRepository.findAllById(Set.of(metricId))).thenReturn(List.of(metric));
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
        assertThat(page.content().getFirst().outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(page.content().getFirst().entityLabel()).isEqualTo("Đường huyết (Glucose)");
    }

    @Test
    @DisplayName("REFERENCE_DATA — dùng label từ JSON audit, không query metric")
    void query_referenceMetricLabelFromJson() {
        UUID metricId = UUID.randomUUID();
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction("UPDATE_REFERENCE_METRIC_DISPLAY");
        row.setResourceType(AuditResourceTypes.REFERENCE_DATA);
        row.setResourceId(metricId);
        row.setNewValueJson("{\"displayNameVi\":\"Glucose\",\"name\":\"Glucose\"}");
        row.setCreatedAt(Instant.parse("2026-05-10T09:15:00Z"));

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));

        AuditLogPageDto page = adminAuditLogService.query(null, null, null, null, null, null, 0, 50);

        assertThat(page.content().getFirst().entityLabel()).isEqualTo("Glucose (Glucose)");
        verify(referenceMetricRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("AUTH LOGIN — entityLabel mô tả phiên, không lặp email actor")
    void query_authLogin_entityLabelWithoutEmail() {
        UUID userId = UUID.randomUUID();
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction(AuditActions.LOGIN);
        row.setResourceType(AuditResourceTypes.AUTH);
        row.setResourceId(userId);
        row.setNewValueJson("{\"email\":\"user@example.com\",\"role\":\"ROLE_USER\"}");
        row.setCreatedAt(Instant.parse("2026-05-10T10:00:00Z"));
        row.setIpAddress("203.0.113.10");
        row.setUserAgent("Mozilla/5.0 Chrome/120.0.0.0");
        User actor = new User();
        actor.setEmail("user@example.com");
        row.setActor(actor);

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));

        AuditLogPageDto page = adminAuditLogService.query(null, null, null, null, null, null, 0, 50);

        assertThat(page.content().getFirst().entityLabel()).isEqualTo("Phiên đăng nhập");
        assertThat(page.content().getFirst().detailSummary()).contains("Google Chrome");
        assertThat(page.content().getFirst().detailSummary()).contains("203.0.113.10");
        assertThat(page.content().getFirst().actorEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("AUTH LOGIN_FAILED — entityLabel hiển thị lý do, không lặp email")
    void query_authLoginFailed_entityLabelShowsReason() {
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction(AuditActions.LOGIN_FAILED);
        row.setResourceType(AuditResourceTypes.AUTH);
        row.setNewValueJson("{\"email\":\"unknown@example.com\",\"reason\":\"bad_credentials\"}");
        row.setCreatedAt(Instant.parse("2026-05-10T10:00:00Z"));

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));

        AuditLogPageDto page = adminAuditLogService.query(null, null, null, null, null, null, 0, 50);

        assertThat(page.content().getFirst().entityLabel()).isEqualTo("Phiên đăng nhập thất bại");
        assertThat(page.content().getFirst().outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(page.content().getFirst().detailSummary()).contains("Thất bại xác thực");
        assertThat(page.content().getFirst().detailSummary()).contains("Sai email hoặc mật khẩu");
        assertThat(page.content().getFirst().actorEmail()).isEqualTo("unknown@example.com");
    }

    @Test
    @DisplayName("USER UPDATE_USER — entityLabel dùng fullName thay vì email")
    void query_userUpdate_entityLabelUsesFullName() {
        UUID userId = UUID.randomUUID();
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction(AuditActions.UPDATE_USER);
        row.setResourceType(AuditResourceTypes.USER);
        row.setResourceId(userId);
        row.setNewValueJson("{\"email\":\"user@example.com\",\"fullName\":\"Nguyễn Văn A\"}");
        row.setCreatedAt(Instant.parse("2026-05-10T10:00:00Z"));
        User actor = new User();
        actor.setEmail("user@example.com");
        row.setActor(actor);

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));

        AuditLogPageDto page = adminAuditLogService.query(null, null, null, null, null, null, 0, 50);

        assertThat(page.content().getFirst().entityLabel()).isEqualTo("Cập nhật hồ sơ · Nguyễn Văn A");
    }

    @Test
    @DisplayName("writeCsv ghi header và một dòng dữ liệu")
    void writeCsv_includesHeaderAndRow() throws Exception {
        UUID metricId = UUID.randomUUID();
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction("UPDATE_REFERENCE_METRIC_DISPLAY");
        row.setResourceType(AuditResourceTypes.REFERENCE_DATA);
        row.setResourceId(metricId);
        row.setIpAddress("203.0.113.10");
        row.setCreatedAt(Instant.parse("2026-05-10T09:15:00Z"));
        User actor = new User();
        actor.setEmail("admin@healthlens.vn");
        row.setActor(actor);

        ReferenceMetric metric = new ReferenceMetric();
        metric.setId(metricId);
        metric.setName("Glucose");
        metric.setDisplayNameVi("Đường huyết");
        when(referenceMetricRepository.findAllById(Set.of(metricId))).thenReturn(List.of(metric));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)))
                .thenReturn(new PageImpl<>(List.of()));

        StringWriter writer = new StringWriter();
        adminAuditLogService.writeCsv(
                AuditResourceTypes.REFERENCE_DATA,
                null,
                null,
                null,
                null,
                null,
                writer,
                100
        );

        String csv = writer.toString();
        assertThat(csv).contains("outcome");
        assertThat(csv).contains("actorEmail");
        assertThat(csv).contains("admin@healthlens.vn");
        assertThat(csv).contains("UPDATE_REFERENCE_METRIC_DISPLAY");
        assertThat(csv).contains("203.0.113.10");
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
