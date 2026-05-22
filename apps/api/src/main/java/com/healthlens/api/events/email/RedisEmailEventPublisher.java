package com.healthlens.api.events.email;

import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.User;
import com.healthlens.api.events.ApplicationStreamNames;
import com.healthlens.api.events.ApplicationStreamPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RedisEmailEventPublisher implements EmailEventPublisher {

    private final ApplicationStreamPublisher streamPublisher;
    private final AuditEventRecorder auditEventRecorder;
    private final String emailEventStream;

    public RedisEmailEventPublisher(
            ApplicationStreamPublisher streamPublisher,
            AuditEventRecorder auditEventRecorder,
            @Value("${app.stream.email-events:" + ApplicationStreamNames.EMAIL_EVENTS + "}") String emailEventStream
    ) {
        this.streamPublisher = streamPublisher;
        this.auditEventRecorder = auditEventRecorder;
        this.emailEventStream = emailEventStream;
    }

    @Override
    public void publishVerification(User user, String token) {
        publish(EmailEvent.of(
                EmailEvent.Type.VERIFICATION,
                user.getId(),
                user.getEmail(),
                Map.of("token", token)
        ));
    }

    @Override
    public void publishPasswordReset(User user, String token) {
        publish(EmailEvent.of(
                EmailEvent.Type.PASSWORD_RESET,
                user.getId(),
                user.getEmail(),
                Map.of("token", token, "fullName", safe(user.getFullName()))
        ));
    }

    @Override
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

    @Override
    public void publishDeletionCancellation(User user) {
        publish(EmailEvent.of(
                EmailEvent.Type.DELETION_CANCELLATION,
                user.getId(),
                user.getEmail(),
                Map.of("fullName", safe(user.getFullName()))
        ));
    }

    @Override
    public void publishDeletionCompletion(User user) {
        publish(EmailEvent.of(
                EmailEvent.Type.DELETION_COMPLETION,
                user.getId(),
                user.getEmail(),
                Map.of()
        ));
    }

    @Override
    public void publishProfileInvitation(User inviter, String inviteeEmail, String invitationLink) {
        publish(EmailEvent.of(
                EmailEvent.Type.PROFILE_INVITATION,
                inviter.getId(),
                inviteeEmail,
                Map.of("inviterName", safe(inviter.getFullName()), "invitationLink", invitationLink)
        ));
    }

    /** Notifies the profile owner that an invitee accepted sharing access. */
    @Override
    public void publishProfileShareAccepted(User owner, User viewer, String profileDisplayName, String profilesLink) {
        publish(EmailEvent.of(
                EmailEvent.Type.PROFILE_SHARE_ACCEPTED,
                owner.getId(),
                owner.getEmail(),
                Map.of(
                        "viewerName", safe(viewer.getFullName()),
                        "profileDisplayName", profileDisplayName == null ? "" : profileDisplayName,
                        "profilesLink", profilesLink
                )
        ));
    }

    @Override
    public void publishHealthRecordInvitation(User inviter, String inviteeEmail, String invitationLink) {
        publish(EmailEvent.of(
                EmailEvent.Type.HEALTH_RECORD_INVITATION,
                inviter.getId(),
                inviteeEmail,
                Map.of("inviterName", safe(inviter.getFullName()), "invitationLink", invitationLink)
        ));
    }

    @Override
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
            streamPublisher.publish(emailEventStream, emailEvent.toStreamMap());
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
            if (EmailEvent.Type.FOLLOW_UP_REMINDER.streamValue().equals(emailEvent.eventType())) {
                throw new IllegalStateException(
                        "Failed to publish email event: " + emailEvent.eventType(),
                        ex);
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
