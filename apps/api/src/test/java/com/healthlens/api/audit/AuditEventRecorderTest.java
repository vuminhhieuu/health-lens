package com.healthlens.api.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventRecorderTest {

    @Mock
    private UnifiedAuditLogWriter unifiedAuditLogWriter;

    private AuditEventRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new AuditEventRecorder(unifiedAuditLogWriter, new ObjectMapper());
    }

    @Test
    void recordAnonymous_delegatesToWriterWithoutActor() {
        recorder.recordAnonymous(
                AuditActions.LOGIN_FAILED,
                AuditResourceTypes.AUTH,
                null,
                Map.of("email", "user@example.com")
        );

        verify(unifiedAuditLogWriter).recordWithoutActor(
                eq(AuditActions.LOGIN_FAILED),
                eq(AuditResourceTypes.AUTH),
                isNull(),
                isNull(),
                isNull(),
                eq("{\"email\":\"user@example.com\"}")
        );
    }

    @Test
    void recordEvent_serializesDetails() {
        UUID userId = UUID.randomUUID();
        recorder.recordEvent(
                userId,
                AuditActions.LOGIN,
                AuditResourceTypes.AUTH,
                userId,
                Map.of("email", "user@example.com")
        );

        verify(unifiedAuditLogWriter).record(
                eq(userId),
                eq(AuditActions.LOGIN),
                eq(AuditResourceTypes.AUTH),
                eq(userId),
                isNull(),
                isNull(),
                eq("{\"email\":\"user@example.com\"}")
        );
    }

    @Test
    void recordEvent_redactsSensitiveDetailsBeforeSerializing() {
        UUID userId = UUID.randomUUID();
        recorder.recordEvent(
                userId,
                AuditActions.CREATE_HEALTH_RECORD,
                AuditResourceTypes.HEALTH_RECORD,
                UUID.randomUUID(),
                Map.of(
                        "fileKey", "users/%s/report.pdf".formatted(userId),
                        "rawOcrText", "secret OCR body",
                        "authorization", "Bearer secret",
                        "downloadUrl", "https://storage.local/report.pdf?token=secret&email=user@example.com&expires=123"
                )
        );

        verify(unifiedAuditLogWriter).record(
                eq(userId),
                eq(AuditActions.CREATE_HEALTH_RECORD),
                eq(AuditResourceTypes.HEALTH_RECORD),
                org.mockito.ArgumentMatchers.any(UUID.class),
                isNull(),
                isNull(),
                org.mockito.ArgumentMatchers.argThat(json -> json.contains("fileKey")
                        && json.contains("expires=123")
                        && !json.contains("rawOcrText")
                        && !json.contains("Bearer secret")
                        && !json.contains("token=secret")
                        && !json.contains("email=user@example.com"))
        );
    }
}
