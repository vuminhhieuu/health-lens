package com.healthlens.api.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.healthlens.api.correlation.CorrelationContext;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharingAuditSupportTest {

    @Mock
    private AuditEventRecorder auditEventRecorder;

    private SharingAuditSupport sharingAuditSupport;

    @BeforeEach
    void setUp() {
        sharingAuditSupport = new SharingAuditSupport(auditEventRecorder);
        CorrelationContext.ensure("corr-test-123", "req-1", "trace-1");
    }

    @AfterEach
    void tearDown() {
        CorrelationContext.clear();
    }

    @Test
    void recordSuccess_includesMaskedEmailAndCorrelationId() {
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        sharingAuditSupport.recordSuccess(
                actorId,
                AuditActions.INVITE_PROFILE_SHARE,
                profileId,
                SharingAuditSupport.SharingAuditFields.builder()
                        .ownerId(ownerId)
                        .inviteeEmail("viewer@healthlens.vn")
                        .build()
        );

        verify(auditEventRecorder).recordEvent(
                eq(actorId),
                eq(AuditActions.INVITE_PROFILE_SHARE),
                eq(AuditResourceTypes.PROFILE),
                eq(profileId),
                argThat((Map<String, ?> payload) ->
                        "v***@healthlens.vn".equals(payload.get("inviteeEmailMasked"))
                                && !payload.containsKey("inviteeEmail")
                                && "corr-test-123".equals(payload.get("correlationId"))
                                && AuditOutcome.SUCCESS.equals(payload.get("outcome"))
                                && ownerId.toString().equals(payload.get("ownerId"))
                )
        );
    }

    @Test
    void recordAccessDenied_setsFailureOutcome() {
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        sharingAuditSupport.recordAccessDenied(
                actorId, profileId, UUID.randomUUID(), null, null, null, "not_profile_owner");

        verify(auditEventRecorder).recordEvent(
                eq(actorId),
                eq(AuditActions.PROFILE_SHARE_ACCESS_DENIED_FAILED),
                eq(AuditResourceTypes.PROFILE),
                eq(profileId),
                argThat((Map<String, ?> payload) ->
                        AuditOutcome.FAILURE.equals(payload.get("outcome"))
                                && "not_profile_owner".equals(payload.get("reason"))
                )
        );
    }
}
