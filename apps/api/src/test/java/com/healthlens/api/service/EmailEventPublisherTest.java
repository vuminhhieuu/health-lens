package com.healthlens.api.service;

import com.healthlens.api.dto.event.EmailEvent;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailEventPublisherTest {

    @Test
    void emailEvent_serializesTypedPayloadWithoutDroppingFields() {
        UUID userId = UUID.randomUUID();
        EmailEvent event = EmailEvent.of(
                EmailEvent.Type.DELETION_CONFIRMATION,
                userId,
                "user@healthlens.vn",
                Map.of(
                        "cancellationLink", "http://localhost:3000/cancel-deletion?token=abc",
                        "scheduledDeletionAt", Instant.parse("2026-05-23T07:00:00Z").toString()
                )
        );

        Map<String, String> streamMap = event.toStreamMap();

        assertThat(streamMap)
                .containsEntry("eventType", "deletion_confirmation")
                .containsEntry("userId", userId.toString())
                .containsEntry("email", "user@healthlens.vn")
                .containsEntry("cancellationLink", "http://localhost:3000/cancel-deletion?token=abc")
                .containsEntry("scheduledDeletionAt", "2026-05-23T07:00:00Z");
    }

    @Test
    @SuppressWarnings("unchecked")
    void publisherWritesSupportedEmailEventsToSingleStream() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);

        EmailEventPublisher publisher = new EmailEventPublisher(redisTemplate, auditEventRecorder, "email.events");
        UUID reminderId = UUID.randomUUID();

        publisher.publishFollowUpReminder(reminderId);

        verify(streamOps).add(eq("email.events"), argThat(payload ->
                "follow_up_reminder".equals(payload.get("eventType"))
                        && reminderId.toString().equals(payload.get("reminderId"))));
        verifyNoInteractions(auditEventRecorder);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishDeletionConfirmationSerializesScheduledDeletionInstant() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@healthlens.vn");
        user.setFullName("Nguyen Van A");
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setScheduledDeletionAt(Instant.parse("2026-05-23T07:00:00Z"));

        EmailEventPublisher publisher = new EmailEventPublisher(redisTemplate, auditEventRecorder, "email.events");
        publisher.publishDeletionConfirmation(user, deletionRequest, "http://localhost:3000/cancel-deletion?token=abc");

        verify(streamOps).add(eq("email.events"), argThat(payload ->
                "deletion_confirmation".equals(payload.get("eventType"))
                        && "2026-05-23T07:00:00Z".equals(payload.get("scheduledDeletionAt"))
                        && "Nguyen Van A".equals(payload.get("displayName"))
                        && "http://localhost:3000/cancel-deletion?token=abc".equals(payload.get("cancellationLink"))));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishFailureRecordsAuditWithoutThrowing() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(streamOps).add(eq("email.events"), org.mockito.ArgumentMatchers.any(Map.class));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@healthlens.vn");

        EmailEventPublisher publisher = new EmailEventPublisher(redisTemplate, auditEventRecorder, "email.events");
        publisher.publishPasswordReset(user, "reset-token");

        verify(auditEventRecorder).recordEvent(
                eq(user.getId()),
                eq(com.healthlens.api.audit.AuditActions.EMAIL_PROVIDER_FAILURE),
                eq(com.healthlens.api.audit.AuditResourceTypes.AUTH),
                eq(user.getId()),
                argThat(payload -> "password_reset".equals(payload.get("flow"))
                        && "IllegalStateException".equals(payload.get("failureClass"))
                        && "publish".equals(payload.get("stage")))
        );
    }
}
