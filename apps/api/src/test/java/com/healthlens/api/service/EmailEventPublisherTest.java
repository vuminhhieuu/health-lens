package com.healthlens.api.service;

import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.User;
import com.healthlens.api.events.ApplicationStreamPublisher;
import com.healthlens.api.events.email.EmailEvent;
import com.healthlens.api.events.email.RedisEmailEventPublisher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

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
    void publisherWritesSupportedEmailEventsToSingleStream() {
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);

        RedisEmailEventPublisher publisher = new RedisEmailEventPublisher(streamPublisher, auditEventRecorder, "email.events");
        UUID reminderId = UUID.randomUUID();

        publisher.publishFollowUpReminder(reminderId);

        verify(streamPublisher).publish(eq("email.events"), argThat(payload ->
                "follow_up_reminder".equals(payload.get("eventType"))
                        && reminderId.toString().equals(payload.get("reminderId"))));
        verifyNoInteractions(auditEventRecorder);
    }

    @Test
    void publishDeletionConfirmationSerializesScheduledDeletionInstant() {
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@healthlens.vn");
        user.setFullName("Nguyen Van A");
        DataDeletionRequest deletionRequest = new DataDeletionRequest();
        deletionRequest.setScheduledDeletionAt(Instant.parse("2026-05-23T07:00:00Z"));

        RedisEmailEventPublisher publisher = new RedisEmailEventPublisher(streamPublisher, auditEventRecorder, "email.events");
        publisher.publishDeletionConfirmation(user, deletionRequest, "http://localhost:3000/cancel-deletion?token=abc");

        verify(streamPublisher).publish(eq("email.events"), argThat(payload ->
                "deletion_confirmation".equals(payload.get("eventType"))
                        && "2026-05-23T07:00:00Z".equals(payload.get("scheduledDeletionAt"))
                        && "Nguyen Van A".equals(payload.get("displayName"))
                        && "http://localhost:3000/cancel-deletion?token=abc".equals(payload.get("cancellationLink"))));
    }

    @Test
    void publishProfileShareAcceptedSerializesExpectedPayload() {
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@healthlens.vn");
        User viewer = new User();
        viewer.setFullName("Nguyen Van B");

        RedisEmailEventPublisher publisher = new RedisEmailEventPublisher(streamPublisher, auditEventRecorder, "email.events");
        publisher.publishProfileShareAccepted(owner, viewer, "Ba", "http://localhost:3000/profiles");

        verify(streamPublisher).publish(eq("email.events"), argThat(payload ->
                "profile_share_accepted".equals(payload.get("eventType"))
                        && owner.getId().toString().equals(payload.get("userId"))
                        && "owner@healthlens.vn".equals(payload.get("email"))
                        && "Nguyen Van B".equals(payload.get("viewerName"))
                        && "Ba".equals(payload.get("profileDisplayName"))
                        && "http://localhost:3000/profiles".equals(payload.get("profilesLink"))));
    }

    @Test
    void publishProfileShareAccepted_nullProfileDisplayNameSerializesEmptyString() {
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@healthlens.vn");
        User viewer = new User();

        RedisEmailEventPublisher publisher = new RedisEmailEventPublisher(streamPublisher, auditEventRecorder, "email.events");
        publisher.publishProfileShareAccepted(owner, viewer, null, "http://localhost:3000/profiles");

        verify(streamPublisher).publish(eq("email.events"), argThat(payload ->
                "profile_share_accepted".equals(payload.get("eventType"))
                        && "".equals(payload.get("profileDisplayName"))
                        && "".equals(payload.get("viewerName"))));
    }

    @Test
    void publishFailureRecordsAuditWithoutThrowing() {
        ApplicationStreamPublisher streamPublisher = mock(ApplicationStreamPublisher.class);
        AuditEventRecorder auditEventRecorder = mock(AuditEventRecorder.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(streamPublisher).publish(eq("email.events"), org.mockito.ArgumentMatchers.any(Map.class));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@healthlens.vn");

        RedisEmailEventPublisher publisher = new RedisEmailEventPublisher(streamPublisher, auditEventRecorder, "email.events");
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
