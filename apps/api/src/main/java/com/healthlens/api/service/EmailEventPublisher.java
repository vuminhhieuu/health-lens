package com.healthlens.api.service;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.event.EmailEvent;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class EmailEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final AuditEventRecorder auditEventRecorder;
    private final String emailEventStream;

    public EmailEventPublisher(
            StringRedisTemplate redisTemplate,
            AuditEventRecorder auditEventRecorder,
            @Value("${app.stream.email-events:email.events}") String emailEventStream
    ) {
        this.redisTemplate = redisTemplate;
        this.auditEventRecorder = auditEventRecorder;
        this.emailEventStream = emailEventStream;
    }

    public void publishVerification(User user, String token) {
        publish(EmailEvent.of(
                EmailEvent.Type.VERIFICATION,
                user.getId(),
                user.getEmail(),
                Map.of("token", token)
        ));
    }

    public void publishPasswordReset(User user, String token) {
        publish(EmailEvent.of(
                EmailEvent.Type.PASSWORD_RESET,
                user.getId(),
                user.getEmail(),
                Map.of("token", token, "fullName", safe(user.getFullName()))
        ));
    }

    public void publishDeletionConfirmation(User user, DataDeletionRequest deletionRequest, String cancellationLink) {
        publish(EmailEvent.of(
                EmailEvent.Type.DELETION_CONFIRMATION,
                user.getId(),
                user.getEmail(),
                Map.of(
                        "displayName", displayName(user),
                        "scheduledDeletionAt", deletionRequest.getScheduledDeletionAt() == null
                                ? ""
                                : deletionRequest.getScheduledDeletionAt().toString(),
                        "cancellationLink", cancellationLink
                )
        ));
    }

    public void publishDeletionCancellation(User user) {
        publish(EmailEvent.of(
                EmailEvent.Type.DELETION_CANCELLATION,
                user.getId(),
                user.getEmail(),
                Map.of("fullName", safe(user.getFullName()))
        ));
    }

    public void publishDeletionCompletion(User user) {
        publish(EmailEvent.of(
                EmailEvent.Type.DELETION_COMPLETION,
                user.getId(),
                user.getEmail(),
                Map.of()
        ));
    }

    public void publishProfileInvitation(User inviter, String inviteeEmail, String invitationLink) {
        publish(EmailEvent.of(
                EmailEvent.Type.PROFILE_INVITATION,
                inviter.getId(),
                inviteeEmail,
                Map.of("inviterName", safe(inviter.getFullName()), "invitationLink", invitationLink)
        ));
    }

    public void publishHealthRecordInvitation(User inviter, String inviteeEmail, String invitationLink) {
        publish(EmailEvent.of(
                EmailEvent.Type.HEALTH_RECORD_INVITATION,
                inviter.getId(),
                inviteeEmail,
                Map.of("inviterName", safe(inviter.getFullName()), "invitationLink", invitationLink)
        ));
    }

    public void publishFollowUpReminder(UUID reminderId) {
        publish(EmailEvent.of(
                EmailEvent.Type.FOLLOW_UP_REMINDER,
                null,
                null,
                Map.of("reminderId", reminderId.toString())
        ));
    }

    private void publish(EmailEvent emailEvent) {
        Runnable publishAction = () -> publishNow(emailEvent);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
            return;
        }
        publishAction.run();
    }

    private void publishNow(EmailEvent emailEvent) {
        try {
            redisTemplate.opsForStream().add(emailEventStream, emailEvent.toStreamMap());
        } catch (Exception ex) {
            log.warn("[EmailEventPublisher] Cannot publish {} email event for user {}",
                    emailEvent.eventType(), emailEvent.userId(), ex);
            if (emailEvent.userId() != null) {
                auditEventRecorder.recordEvent(
                        emailEvent.userId(),
                        AuditActions.EMAIL_PROVIDER_FAILURE,
                        AuditResourceTypes.AUTH,
                        emailEvent.userId(),
                        Map.of(
                                "flow", emailEvent.eventType(),
                                "failureClass", ex.getClass().getSimpleName(),
                                "stage", "publish"
                        )
                );
            }
        }
    }

    private static String displayName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? "bạn" : user.getFullName();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
