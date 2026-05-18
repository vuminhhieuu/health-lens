package com.healthlens.api.audit;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnifiedAuditCoordinatorTest {

    @Mock
    private UnifiedAuditLogWriter unifiedAuditLogWriter;

    @AfterEach
    void tearDown() {
        UnifiedAuditSnapshot.clear();
    }

    @Test
    @DisplayName("persistAndClear ghi audit và xóa ThreadLocal")
    void persistAndClear_writesAndClearsThreadLocal() {
        UUID metricId = UUID.randomUUID();
        UnifiedAuditSnapshot.set(new UnifiedAuditSnapshot.Payload(metricId, "{\"x\":1}", "{\"x\":2}"));

        UUID actorId = UUID.randomUUID();
        UnifiedAuditCoordinator coordinator = new UnifiedAuditCoordinator(unifiedAuditLogWriter);
        coordinator.persistAndClear(
                actorId,
                "UPDATE_REFERENCE_METRIC_DISPLAY",
                "REFERENCE_DATA",
                new UnifiedAuditSnapshot.Payload(metricId, "{\"a\":1}", "{\"a\":2}")
        );

        verify(unifiedAuditLogWriter).record(
                eq(actorId),
                eq("UPDATE_REFERENCE_METRIC_DISPLAY"),
                eq("REFERENCE_DATA"),
                eq(metricId),
                eq("{\"a\":1}"),
                eq("{\"a\":2}")
        );
        assertThat(UnifiedAuditSnapshot.take()).isNull();
    }
}
