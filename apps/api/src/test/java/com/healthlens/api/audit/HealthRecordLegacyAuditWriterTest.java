package com.healthlens.api.audit;

import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.HealthRecordAuditLog;
import com.healthlens.api.repository.HealthRecordAuditLogRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HealthRecordLegacyAuditWriterTest {

    @Mock
    private HealthRecordAuditLogRepository healthRecordAuditLogRepository;

    @Test
    @DisplayName("record lưu userId, action, recordId")
    void record_persistsSimpleRow() {
        UUID userId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        HealthRecordLegacyAuditWriter writer = new HealthRecordLegacyAuditWriter(healthRecordAuditLogRepository);

        writer.record(userId, AuditActions.DELETE_HEALTH_RECORD, recordId);

        ArgumentCaptor<HealthRecordAuditLog> captor = ArgumentCaptor.forClass(HealthRecordAuditLog.class);
        verify(healthRecordAuditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActions.DELETE_HEALTH_RECORD);
        assertThat(captor.getValue().getRecordId()).isEqualTo(recordId);
    }
}
