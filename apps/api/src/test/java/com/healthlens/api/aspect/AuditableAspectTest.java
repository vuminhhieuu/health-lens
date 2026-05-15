package com.healthlens.api.aspect;

import com.healthlens.api.annotation.Auditable;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.UnifiedAuditLogWriter;
import com.healthlens.api.audit.UnifiedAuditSnapshot;
import com.healthlens.api.entity.HealthRecordAuditLog;
import com.healthlens.api.repository.HealthRecordAuditLogRepository;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditableAspectTest {

    @Mock
    private HealthRecordAuditLogRepository healthRecordAuditLogRepository;

    @Mock
    private UnifiedAuditLogWriter unifiedAuditLogWriter;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @AfterEach
    void clearSecurityAndSnapshot() {
        SecurityContextHolder.clearContext();
        UnifiedAuditSnapshot.take();
    }

    @Test
    @DisplayName("writeAuditLog ghi bản ghi audit khi args hợp lệ")
    void writeAuditLog_success() throws Exception {
        AuditableAspect aspect = new AuditableAspect(healthRecordAuditLogRepository, unifiedAuditLogWriter);
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        when(joinPoint.getArgs()).thenReturn(new Object[]{userId, recordId});

        Method method = DummyService.class.getDeclaredMethod("deleteHealthRecord", UUID.class, UUID.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        aspect.writeAuditLog(joinPoint, auditable);

        ArgumentCaptor<HealthRecordAuditLog> captor = ArgumentCaptor.forClass(HealthRecordAuditLog.class);
        verify(healthRecordAuditLogRepository).save(captor.capture());
        verify(unifiedAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getRecordId()).isEqualTo(recordId);
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActions.DELETE_HEALTH_RECORD);
    }

    @Test
    @DisplayName("writeAuditLog bỏ qua khi method signature không hợp lệ")
    void writeAuditLog_skipInvalidArgs() throws Exception {
        AuditableAspect aspect = new AuditableAspect(healthRecordAuditLogRepository, unifiedAuditLogWriter);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"wrong", UUID.randomUUID()});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("DummyService.deleteHealthRecord(..)");

        Method method = DummyService.class.getDeclaredMethod("deleteHealthRecord", UUID.class, UUID.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        aspect.writeAuditLog(joinPoint, auditable);

        verify(healthRecordAuditLogRepository, never()).save(any());
        verify(unifiedAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("unified resource — ghi audit_logs khi có snapshot")
    void writeUnified_auditLogs() throws Exception {
        UUID metricId = UUID.randomUUID();
        UnifiedAuditSnapshot.set(new UnifiedAuditSnapshot.Payload(metricId, "{\"x\":1}", "{\"x\":2}"));

        AuditableAspect aspect = new AuditableAspect(healthRecordAuditLogRepository, unifiedAuditLogWriter);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AdminService.update(..)");

        Method method = DummyService.class.getDeclaredMethod("unifiedMutation");
        Auditable auditable = method.getAnnotation(Auditable.class);

        aspect.writeAuditLog(joinPoint, auditable);

        verify(unifiedAuditLogWriter).record(
                isNull(),
                eq("UPDATE_REFERENCE_METRIC_DISPLAY"),
                eq("REFERENCE_DATA"),
                eq(metricId),
                eq("{\"x\":1}"),
                eq("{\"x\":2}")
        );
        verify(healthRecordAuditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("unified health record delete — ghi cả audit_logs và legacy health_record audit")
    void writeUnified_healthRecordDelete() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UnifiedAuditSnapshot.set(new UnifiedAuditSnapshot.Payload(recordId, "{\"id\":\"1\"}", "{\"deleted\":true}"));

        AuditableAspect aspect = new AuditableAspect(healthRecordAuditLogRepository, unifiedAuditLogWriter);
        when(joinPoint.getArgs()).thenReturn(new Object[]{userId, recordId});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("HealthRecordService.delete(..)");

        Method method = DummyService.class.getDeclaredMethod("deleteHealthRecordUnified", UUID.class, UUID.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        aspect.writeAuditLog(joinPoint, auditable);

        verify(unifiedAuditLogWriter).record(
                isNull(),
                eq(AuditActions.DELETE_HEALTH_RECORD),
                eq("HEALTH_RECORD"),
                eq(recordId),
                eq("{\"id\":\"1\"}"),
                eq("{\"deleted\":true}")
        );
        verify(healthRecordAuditLogRepository).save(any());
    }

    private static class DummyService {

        @Auditable(action = AuditActions.DELETE_HEALTH_RECORD)
        @SuppressWarnings("unused")
        public void deleteHealthRecord(UUID userId, UUID recordId) {
        }

        @Auditable(
                action = AuditActions.DELETE_HEALTH_RECORD,
                unifiedResourceType = "HEALTH_RECORD"
        )
        public void deleteHealthRecordUnified(UUID userId, UUID recordId) {
        }

        @Auditable(
                action = "UPDATE_REFERENCE_METRIC_DISPLAY",
                unifiedResourceType = "REFERENCE_DATA"
        )
        public void unifiedMutation() {}
    }
}
