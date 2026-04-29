package com.healthlens.api.aspect;

import com.healthlens.api.annotation.Auditable;
import com.healthlens.api.entity.HealthRecordAuditLog;
import com.healthlens.api.repository.HealthRecordAuditLogRepository;
import java.lang.reflect.Method;
import java.util.UUID;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditableAspectTest {

    @Mock
    private HealthRecordAuditLogRepository healthRecordAuditLogRepository;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Test
    @DisplayName("writeAuditLog ghi bản ghi audit khi args hợp lệ")
    void writeAuditLog_success() throws Exception {
        AuditableAspect aspect = new AuditableAspect(healthRecordAuditLogRepository);
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        when(joinPoint.getArgs()).thenReturn(new Object[]{userId, recordId});

        Method method = DummyService.class.getDeclaredMethod("deleteHealthRecord", UUID.class, UUID.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        aspect.writeAuditLog(joinPoint, auditable);

        ArgumentCaptor<HealthRecordAuditLog> captor = ArgumentCaptor.forClass(HealthRecordAuditLog.class);
        verify(healthRecordAuditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getRecordId()).isEqualTo(recordId);
        assertThat(captor.getValue().getAction()).isEqualTo("DELETE_HEALTH_RECORD");
    }

    @Test
    @DisplayName("writeAuditLog bỏ qua khi method signature không hợp lệ")
    void writeAuditLog_skipInvalidArgs() throws Exception {
        AuditableAspect aspect = new AuditableAspect(healthRecordAuditLogRepository);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"wrong", UUID.randomUUID()});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("DummyService.deleteHealthRecord(..)");

        Method method = DummyService.class.getDeclaredMethod("deleteHealthRecord", UUID.class, UUID.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        aspect.writeAuditLog(joinPoint, auditable);

        verify(healthRecordAuditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static class DummyService {
        @Auditable(action = "DELETE_HEALTH_RECORD")
        @SuppressWarnings("unused")
        public void deleteHealthRecord(UUID userId, UUID recordId) {
        }
    }
}
