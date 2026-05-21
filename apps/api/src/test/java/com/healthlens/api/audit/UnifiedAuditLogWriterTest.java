package com.healthlens.api.audit;

import com.healthlens.api.correlation.CorrelationContext;
import com.healthlens.api.entity.AuditLog;
import com.healthlens.api.entity.User;
import com.healthlens.api.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnifiedAuditLogWriterTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        CorrelationContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void record_persistsCanonicalCorrelationOutcomeWithoutDuplicatingMetadata() {
        UUID actorId = UUID.randomUUID();
        User actor = new User();
        actor.setId(actorId);
        when(entityManager.getReference(User.class, actorId)).thenReturn(actor);
        CorrelationContext.ensure("corr-123", "req-456", "trace-789");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/reference-data");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UnifiedAuditLogWriter writer = new UnifiedAuditLogWriter(auditLogRepository, entityManager);
        writer.record(
                actorId,
                AuditActions.UPDATE_REFERENCE_METRIC,
                AuditResourceTypes.REFERENCE_DATA,
                UUID.randomUUID(),
                null,
                "{\"metricName\":\"Glucose\"}"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog row = captor.getValue();
        assertThat(row.getActor()).isSameAs(actor);
        assertThat(row.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(row.getCorrelationId()).isEqualTo("corr-123");
        assertThat(row.getRequestId()).isEqualTo("req-456");
        assertThat(row.getTraceId()).isEqualTo("trace-789");
        assertThat(row.getNewValueJson()).isEqualTo("{\"metricName\":\"Glucose\"}");
        assertThat(row.getMetadataJson()).isNull();
        assertThat(row.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(row.getUserAgent()).isEqualTo("JUnit");
    }

    @Test
    void record_withMetadata_persistsSafeMetadataSeparately() {
        UUID actorId = UUID.randomUUID();
        User actor = new User();
        actor.setId(actorId);
        when(entityManager.getReference(User.class, actorId)).thenReturn(actor);

        UnifiedAuditLogWriter writer = new UnifiedAuditLogWriter(auditLogRepository, entityManager);
        writer.record(
                actorId,
                AuditActions.LLM_CALL_FAILED,
                AuditResourceTypes.LLM_CALL,
                null,
                null,
                null,
                "{\"purpose\":\"metric_explanation\"}"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog row = captor.getValue();
        assertThat(row.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(row.getNewValueJson()).isNull();
        assertThat(row.getMetadataJson()).isEqualTo("{\"purpose\":\"metric_explanation\"}");
    }

    @Test
    void recordWithoutActor_persistsRowWithNoActor() {
        UnifiedAuditLogWriter writer = new UnifiedAuditLogWriter(auditLogRepository, entityManager);
        writer.recordWithoutActor(
                AuditActions.LOGIN_FAILED,
                AuditResourceTypes.AUTH,
                null,
                null,
                "{\"email\":\"anon@example.com\",\"reason\":\"bad_credentials\"}"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog row = captor.getValue();
        assertThat(row.getActor()).isNull();
        assertThat(row.getAction()).isEqualTo(AuditActions.LOGIN_FAILED);
        assertThat(row.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(row.getNewValueJson()).contains("anon@example.com");
    }

    @Test
    void record_withoutActorIdOrSecurityContext_skipsPersist() {
        UnifiedAuditLogWriter writer = new UnifiedAuditLogWriter(auditLogRepository, entityManager);
        writer.record(
                null,
                AuditActions.UPDATE_REFERENCE_METRIC,
                AuditResourceTypes.REFERENCE_DATA,
                UUID.randomUUID(),
                null,
                null
        );

        verify(auditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
