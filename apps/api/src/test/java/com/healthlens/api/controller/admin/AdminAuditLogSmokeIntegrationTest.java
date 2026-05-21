package com.healthlens.api.controller.admin;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditOutcome;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.admin.AuditLogPageDto;
import com.healthlens.api.entity.AuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserRole;
import com.healthlens.api.repository.AuditLogRepository;
import com.healthlens.api.repository.UserRepository;
import com.healthlens.api.service.admin.AdminAuditLogService;
import com.healthlens.api.support.PostgresTestContainerBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.StringWriter;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automated smoke for story 6.4 API: filters, export content, Vietnamese labels (DB + service).
 * UTF-8 BOM is asserted in {@link AdminAuditLogControllerTest}.
 */
@SpringBootTest
class AdminAuditLogSmokeIntegrationTest extends PostgresTestContainerBase {

    private static final String SMOKE_ADMIN_EMAIL = "audit-smoke-admin@healthlens.test";

    @Autowired
    private AdminAuditLogService adminAuditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private StreamOperations<String, Object, Object> streamOperations;

    private User admin;

    @BeforeEach
    @Transactional
    void seedAuditData() {
        persistSmokeRows();
    }

    @Test
    @DisplayName("smoke: query REFERENCE_DATA + actorEmail")
    @Transactional
    void query_referenceScopeAndActorEmail() {
        AuditLogPageDto page = adminAuditLogService.query(
                AuditResourceTypes.REFERENCE_DATA,
                null,
                SMOKE_ADMIN_EMAIL,
                null,
                null,
                null,
                0,
                20
        );
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.content().getFirst().entityLabel()).isNotBlank();
    }

    @Test
    @DisplayName("smoke: writeCsv chứa header schema và dòng dữ liệu")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void writeCsv_headerAndDataRows() throws Exception {
        transactionTemplate.executeWithoutResult(status -> persistSmokeRows());

        try {
            StringWriter writer = new StringWriter();
            adminAuditLogService.writeCsv(
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
            assertThat(csv).contains("entityLabel");
            assertThat(csv).contains("detailSummary");
            assertThat(csv).contains("actorEmail");
            long lineCount = csv.lines().filter(line -> !line.isBlank()).count();
            assertThat(lineCount).as("CSV should include header plus at least one row: %s", csv).isGreaterThan(1);
            assertThat(csv).contains(SMOKE_ADMIN_EMAIL);
        } finally {
            transactionTemplate.executeWithoutResult(status -> auditLogRepository.deleteAll());
        }
    }

    @Test
    @DisplayName("smoke: actorEmail JSON path — anonymous login failed")
    @Transactional
    void query_actorEmailMatchesJsonEmail() {
        AuditLogPageDto page = adminAuditLogService.query(
                null,
                null,
                "anon-smoke@example.com",
                null,
                null,
                null,
                0,
                20
        );
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.content().getFirst().actorEmail()).isEqualToIgnoringCase("anon-smoke@example.com");
    }

    @Test
    @DisplayName("smoke: correlationId trace sort")
    @Transactional
    void query_correlationIdChronological() {
        AuditLogPageDto page = adminAuditLogService.query(
                null,
                null,
                null,
                null,
                "corr-smoke-6-4",
                null,
                null,
                0,
                20
        );
        assertThat(page.content()).isNotEmpty();
        assertThat(page.content().getFirst().correlationId()).isEqualTo("corr-smoke-6-4");
    }

    private void persistSmokeRows() {
        admin = userRepository.findByEmailIgnoreCase(SMOKE_ADMIN_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(SMOKE_ADMIN_EMAIL);
            u.setPasswordHash(passwordEncoder.encode("SmokeAdmin1!"));
            u.setRole(UserRole.ROLE_ADMIN);
            u.setEmailVerified(true);
            u.setFullName("Audit Smoke");
            u.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
            return userRepository.save(u);
        });

        auditLogRepository.deleteAll();

        AuditLog referenceRow = new AuditLog();
        referenceRow.setAction(AuditActions.UPDATE_REFERENCE_METRIC_DISPLAY);
        referenceRow.setResourceType(AuditResourceTypes.REFERENCE_DATA);
        referenceRow.setResourceId(java.util.UUID.randomUUID());
        referenceRow.setActor(admin);
        referenceRow.setOutcome(AuditOutcome.SUCCESS);
        referenceRow.setNewValueJson("{\"displayNameVi\":\"Đường huyết\",\"name\":\"Glucose\"}");
        referenceRow.setCreatedAt(Instant.parse("2026-05-10T09:00:00Z"));
        auditLogRepository.save(referenceRow);

        AuditLog anonAuth = new AuditLog();
        anonAuth.setAction(AuditActions.LOGIN_FAILED);
        anonAuth.setResourceType(AuditResourceTypes.AUTH);
        anonAuth.setOutcome(AuditOutcome.FAILURE);
        anonAuth.setNewValueJson("{\"email\":\"anon-smoke@example.com\",\"reason\":\"bad_credentials\"}");
        anonAuth.setCorrelationId("corr-smoke-6-4");
        anonAuth.setCreatedAt(Instant.parse("2026-05-10T10:00:00Z"));
        auditLogRepository.save(anonAuth);
    }
}
