package com.healthlens.api.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditOutcome;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.admin.AuditLogPageDto;
import com.healthlens.api.entity.AuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.ReferenceMetric;
import com.healthlens.api.persistence.json.PostgreSqlJsonPathExpressions;
import com.healthlens.api.repository.AuditLogRepository;
import com.healthlens.api.repository.ReferenceMetricRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminAuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ReferenceMetricRepository referenceMetricRepository;

    @Mock
    private PostgreSqlJsonPathExpressions jsonPathExpressions;

    private AdminAuditLogService adminAuditLogService;

    @BeforeEach
    void setUp() {
        adminAuditLogService = new AdminAuditLogService(
                auditLogRepository,
                referenceMetricRepository,
                jsonPathExpressions,
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
    @DisplayName("query theo correlationId dùng thứ tự thời gian tăng dần để xem trace end-to-end")
    void query_withCorrelationIdUsesChronologicalTraceSort() {
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction(AuditActions.OCR_JOB_SUCCEEDED);
        row.setResourceType(AuditResourceTypes.OCR_JOB);
        row.setCorrelationId("corr-123");
        row.setRequestId("req-123");
        row.setTraceId("trace-123");
        row.setOutcome(AuditOutcome.SUCCESS);
        row.setMetadataJson("{\"jobId\":\"job-1\"}");
        row.setCreatedAt(Instant.parse("2026-05-20T01:00:00Z"));

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));

        AuditLogPageDto page = adminAuditLogService.query(
                null,
                null,
                null,
                null,
                "corr-123",
                null,
                null,
                0,
                20
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id")));
        assertThat(page.content().getFirst().correlationId()).isEqualTo("corr-123");
        assertThat(page.content().getFirst().requestId()).isEqualTo("req-123");
        assertThat(page.content().getFirst().traceId()).isEqualTo("trace-123");
        assertThat(page.content().getFirst().metadataJson()).isEqualTo("{\"jobId\":\"job-1\"}");
    }

    @Test
    @DisplayName("query dùng outcome persisted và metadata fallback cho event-style audit")
    void query_usesPersistedOutcomeAndMetadataPayloadFallback() {
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction(AuditActions.LOGIN);
        row.setResourceType(AuditResourceTypes.AUTH);
        row.setOutcome(AuditOutcome.FAILURE);
        row.setMetadataJson("{\"email\":\"unknown@example.com\",\"reason\":\"bad_credentials\"}");
        row.setCreatedAt(Instant.parse("2026-05-10T10:00:00Z"));

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));

        AuditLogPageDto page = adminAuditLogService.query(null, null, null, null, null, null, 0, 50);

        assertThat(page.content().getFirst().outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(page.content().getFirst().actorEmail()).isEqualTo("unknown@example.com");
        assertThat(page.content().getFirst().entityLabel()).isEqualTo("Phiên đăng nhập");
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
        when(referenceMetricRepository.findAllByOrderByNameAsc()).thenReturn(List.of(metric));
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
    @DisplayName("writeCsv dùng keyset pagination (luôn page 0, sort createdAt + id DESC)")
    void writeCsv_usesKeysetPaginationAcrossBatches() throws Exception {
        Instant base = Instant.parse("2026-05-01T12:00:00Z");
        List<AuditLog> firstBatch = IntStream.range(0, 500)
                .mapToObj(i -> auditRow(base.minusSeconds(i), "batch-a-" + i))
                .toList();
        AuditLog secondBatchRow = auditRow(base.minusSeconds(500), "batch-b-tail");

        when(referenceMetricRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        AtomicInteger calls = new AtomicInteger();
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    if (calls.incrementAndGet() == 1) {
                        return new PageImpl<>(firstBatch);
                    }
                    return new PageImpl<>(List.of(secondBatchRow));
                });

        StringWriter writer = new StringWriter();
        adminAuditLogService.writeCsv(null, null, null, null, null, null, null, writer, 10_000);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository, times(2)).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getAllValues())
                .allMatch(p -> p.getPageNumber() == 0)
                .allMatch(p -> p.getPageSize() == 500)
                .allMatch(p -> p.getSort().equals(Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))));

        long dataLines = writer.toString().lines().filter(line -> line.contains("batch-")).count();
        assertThat(dataLines).isEqualTo(501);
        verify(referenceMetricRepository, times(1)).findAllByOrderByNameAsc();
    }

    private static AuditLog auditRow(Instant createdAt, String marker) {
        AuditLog row = new AuditLog();
        row.setId(UUID.randomUUID());
        row.setAction("LOGIN");
        row.setResourceType(AuditResourceTypes.AUTH);
        row.setCreatedAt(createdAt);
        User actor = new User();
        actor.setEmail(marker + "@healthlens.vn");
        row.setActor(actor);
        return row;
    }

    @Test
    @DisplayName("query với actorEmail — OR actor join + email trong new/old/metadata JSON, chuẩn hóa lowercase")
    void query_withActorEmailFilter_matchesActorJoinAndJsonEmailPaths() {
        ArgumentCaptor<Specification<AuditLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        Root<AuditLog> root = mockCriteriaRoot();
        CriteriaQuery<AuditLog> query = mockCriteriaQuery();
        CriteriaBuilder cb = mockCriteriaBuilder();
        stubActorEmailSpecMocks(root, query, cb);

        when(auditLogRepository.findAll(specCaptor.capture(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    specCaptor.getValue().toPredicate(root, query, cb);
                    return new PageImpl<>(List.of());
                });

        adminAuditLogService.query(
                null,
                null,
                "  Admin@Example.COM  ",
                null,
                null,
                null,
                0,
                20
        );

        verify(jsonPathExpressions).extractPathText(root, cb, "newValueJson", "email");
        verify(jsonPathExpressions).extractPathText(root, cb, "oldValueJson", "email");
        verify(jsonPathExpressions).extractPathText(root, cb, "metadataJson", "email");
        verify(cb, atLeast(4)).equal(any(Expression.class), eq("admin@example.com"));
    }

    @Test
    @DisplayName("query không có actorEmail — không dựng predicate JSON email")
    void query_withoutActorEmailFilter_skipsJsonEmailPaths() {
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adminAuditLogService.query(null, null, null, null, null, null, 0, 20);

        verify(jsonPathExpressions, never()).extractPathText(any(), any(), anyString(), eq("email"));
    }

    @Test
    @DisplayName("writeCsv với actorEmail — cùng predicate email như query")
    void writeCsv_withActorEmailFilter_appliesJsonEmailPaths() throws Exception {
        ArgumentCaptor<Specification<AuditLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        Root<AuditLog> root = mockCriteriaRoot();
        CriteriaQuery<AuditLog> query = mockCriteriaQuery();
        CriteriaBuilder cb = mockCriteriaBuilder();
        stubActorEmailSpecMocks(root, query, cb);

        when(referenceMetricRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(auditLogRepository.findAll(specCaptor.capture(), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    specCaptor.getValue().toPredicate(root, query, cb);
                    return new PageImpl<>(List.of());
                });

        StringWriter writer = new StringWriter();
        adminAuditLogService.writeCsv(
                null,
                null,
                "anon@example.com",
                null,
                null,
                null,
                null,
                writer,
                100
        );

        verify(jsonPathExpressions).extractPathText(root, cb, "newValueJson", "email");
        verify(jsonPathExpressions).extractPathText(root, cb, "oldValueJson", "email");
        verify(jsonPathExpressions).extractPathText(root, cb, "metadataJson", "email");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Root<AuditLog> mockCriteriaRoot() {
        return mock(Root.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CriteriaQuery<AuditLog> mockCriteriaQuery() {
        CriteriaQuery<AuditLog> query = mock(CriteriaQuery.class);
        when(query.getResultType()).thenReturn(AuditLog.class);
        return query;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubActorEmailSpecMocks(Root<AuditLog> root, CriteriaQuery<AuditLog> query, CriteriaBuilder cb) {
        Join actorJoin = mock(Join.class);
        Path emailPath = mock(Path.class);
        Expression<String> lowerExpr = mock(Expression.class);
        Predicate leafPredicate = mock(Predicate.class);
        Predicate orPredicate = mock(Predicate.class);
        Predicate conjunction = mock(Predicate.class);

        when(root.fetch("actor", JoinType.LEFT)).thenReturn(mock(Fetch.class));
        when(root.join("actor", JoinType.LEFT)).thenReturn(actorJoin);
        when(actorJoin.get("email")).thenReturn(emailPath);
        when(cb.lower(any(Expression.class))).thenReturn(lowerExpr);
        when(cb.equal(any(), any())).thenReturn(leafPredicate);
        when(cb.or(any(Predicate[].class))).thenReturn(orPredicate);
        when(cb.conjunction()).thenReturn(conjunction);
        doReturn(lowerExpr).when(jsonPathExpressions).extractPathText(any(), any(), eq("newValueJson"), eq("email"));
        doReturn(lowerExpr).when(jsonPathExpressions).extractPathText(any(), any(), eq("oldValueJson"), eq("email"));
        doReturn(lowerExpr).when(jsonPathExpressions).extractPathText(any(), any(), eq("metadataJson"), eq("email"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CriteriaBuilder mockCriteriaBuilder() {
        return mock(CriteriaBuilder.class);
    }

    @Test
    @DisplayName("writeCsv với correlationId và maxRows nhỏ chỉ ghi tối đa maxRows dòng dữ liệu")
    void writeCsv_respectsMaxRowsCapAndCorrelationFilter() throws Exception {
        Instant base = Instant.parse("2026-05-01T12:00:00Z");
        List<AuditLog> batch = IntStream.range(0, 20)
                .mapToObj(i -> auditRow(base.minusSeconds(i), "cap-" + i))
                .toList();

        when(referenceMetricRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(batch));

        StringWriter writer = new StringWriter();
        adminAuditLogService.writeCsv(
                null,
                null,
                null,
                null,
                "corr-export-1",
                null,
                null,
                writer,
                5
        );

        long dataLines = writer.toString().lines().filter(line -> line.contains("cap-")).count();
        assertThat(dataLines).isEqualTo(5);
    }

    @Test
    @DisplayName("writeCsv không có dữ liệu vẫn ghi header CSV")
    void writeCsv_emptyResult_writesHeaderOnly() throws Exception {
        when(referenceMetricRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        StringWriter writer = new StringWriter();
        adminAuditLogService.writeCsv(null, null, null, null, null, null, null, writer, 100);

        String csv = writer.toString();
        assertThat(csv).contains("actorEmail");
        assertThat(csv.lines().filter(line -> line.contains("@healthlens.vn")).count()).isZero();
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
