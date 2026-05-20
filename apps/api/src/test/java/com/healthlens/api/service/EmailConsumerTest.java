package com.healthlens.api.service;

import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.FollowUpReminder;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.events.ApplicationStreamPublisher;
import com.healthlens.api.events.RedisStreamConsumerSupport;
import com.healthlens.api.events.email.EmailEvent;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowUpReminderService followUpReminderService;

    @Mock
    private RedisStreamConsumerSupport streamConsumerSupport;

    @Mock
    private ApplicationStreamPublisher streamPublisher;

    private EmailConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new EmailConsumer(
                redisTemplate,
                emailService,
                userRepository,
                followUpReminderService,
                streamConsumerSupport,
                streamPublisher,
                "email.events",
                "email.events.dlq",
                "email-consumers",
                "test-email-consumer"
        );
    }

    @Test
    void handleRecord_routesPasswordResetEventToEmailService() {
        UUID userId = UUID.randomUUID();

        consumer.handleRecord(record(Map.of(
                "eventType", EmailEvent.Type.PASSWORD_RESET.streamValue(),
                "userId", userId.toString(),
                "email", "user@healthlens.vn",
                "fullName", "Nguyen Van A",
                "token", "reset-token"
        )));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailService).sendPasswordResetEmail(userCaptor.capture(), eq("reset-token"));
        assertThat(userCaptor.getValue().getId()).isEqualTo(userId);
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@healthlens.vn");
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Nguyen Van A");
    }

    @Test
    void handleRecord_routesDeletionConfirmationWithScheduledDeadline() {
        UUID userId = UUID.randomUUID();
        Instant scheduledDeletionAt = Instant.parse("2026-05-23T07:00:00Z");

        consumer.handleRecord(record(Map.of(
                "eventType", EmailEvent.Type.DELETION_CONFIRMATION.streamValue(),
                "userId", userId.toString(),
                "email", "user@healthlens.vn",
                "displayName", "Nguyen Van A",
                "scheduledDeletionAt", scheduledDeletionAt.toString(),
                "cancellationLink", "http://localhost:3000/cancel-deletion?token=abc"
        )));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<DataDeletionRequest> requestCaptor = ArgumentCaptor.forClass(DataDeletionRequest.class);
        verify(emailService).sendDeletionConfirmationEmail(
                userCaptor.capture(),
                requestCaptor.capture(),
                eq("http://localhost:3000/cancel-deletion?token=abc")
        );
        assertThat(userCaptor.getValue().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(requestCaptor.getValue().getScheduledDeletionAt()).isEqualTo(scheduledDeletionAt);
    }

    @Test
    void handleRecord_routesInvitationAndReminderEventsThroughUnifiedConsumer() {
        UUID inviterId = UUID.randomUUID();
        UUID reminderId = UUID.randomUUID();
        FollowUpReminder reminder = reminder(reminderId);
        when(followUpReminderService.findReminderForEmail(reminderId)).thenReturn(Optional.of(reminder));
        when(emailService.sendFollowUpReminderEmail(reminder)).thenReturn(true);

        consumer.handleRecord(record(Map.of(
                "eventType", EmailEvent.Type.PROFILE_INVITATION.streamValue(),
                "userId", inviterId.toString(),
                "email", "invitee@healthlens.vn",
                "inviterName", "Owner",
                "invitationLink", "http://localhost:3000/invitations/accept?token=abc"
        )));
        consumer.handleRecord(record(Map.of(
                "eventType", EmailEvent.Type.HEALTH_RECORD_INVITATION.streamValue(),
                "userId", inviterId.toString(),
                "email", "record-invitee@healthlens.vn",
                "inviterName", "Owner",
                "invitationLink", "http://localhost:3000/health-record-invitations/accept?token=abc"
        )));
        consumer.handleRecord(record(Map.of(
                "eventType", EmailEvent.Type.FOLLOW_UP_REMINDER.streamValue(),
                "reminderId", reminderId.toString()
        )));

        verify(emailService).sendProfileInvitationEmail(
                any(User.class),
                eq("invitee@healthlens.vn"),
                eq("http://localhost:3000/invitations/accept?token=abc")
        );
        verify(emailService).sendHealthRecordInvitationEmail(
                any(User.class),
                eq("record-invitee@healthlens.vn"),
                eq("http://localhost:3000/health-record-invitations/accept?token=abc")
        );
        verify(emailService).sendFollowUpReminderEmail(reminder);
        verify(followUpReminderService).markEmailSent(eq(reminderId), any(Instant.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumeEmailEvents_acknowledgesOnlyAfterSuccessfulHandling() {
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@healthlens.vn");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        MapRecord<String, Object, Object> record = record(Map.of(
                "eventType", EmailEvent.Type.VERIFICATION.streamValue(),
                "userId", userId.toString(),
                "token", "verify-token"
        ));
        when(record.getId()).thenReturn(RecordId.of("1-0"));
        when(streamConsumerSupport.readPendingThenNew(
                eq(streamOps), eq("email.events"), eq("email-consumers"), eq("test-email-consumer"), eq(10), any(Duration.class)
        )).thenReturn(List.of(record));

        consumer.consumeEmailEvents();

        verify(emailService).sendVerificationEmail(user, "verify-token");
        verify(streamOps).acknowledge("email.events", "email-consumers", RecordId.of("1-0"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumeEmailEvents_leavesFailedRecordPendingForRetry() {
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@healthlens.vn");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        MapRecord<String, Object, Object> record = record(Map.of(
                "eventType", EmailEvent.Type.VERIFICATION.streamValue(),
                "userId", userId.toString(),
                "token", "verify-token"
        ));
        when(record.getId()).thenReturn(RecordId.of("1-0"));
        when(streamConsumerSupport.readPendingThenNew(
                eq(streamOps), eq("email.events"), eq("email-consumers"), eq("test-email-consumer"), eq(10), any(Duration.class)
        )).thenReturn(List.of(record));
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down"))
                .when(emailService).sendVerificationEmail(user, "verify-token");

        consumer.consumeEmailEvents();

        verify(streamOps, never()).acknowledge(any(String.class), any(String.class), any(RecordId.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumeEmailEvents_deadLettersInvalidPayloadAndAcknowledgesIt() {
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        MapRecord<String, Object, Object> record = record(Map.of(
                "eventType", EmailEvent.Type.VERIFICATION.streamValue(),
                "userId", "not-a-uuid",
                "token", "verify-token"
        ));
        when(record.getId()).thenReturn(RecordId.of("1-0"));
        when(streamConsumerSupport.readPendingThenNew(
                eq(streamOps), eq("email.events"), eq("email-consumers"), eq("test-email-consumer"), eq(10), any(Duration.class)
        )).thenReturn(List.of(record));

        consumer.consumeEmailEvents();

        verify(streamPublisher).publish(eq("email.events.dlq"), org.mockito.ArgumentMatchers.argThat(payload ->
                "invalid_userId".equals(payload.get("failureReason"))
                        && "1-0".equals(payload.get("sourceRecordId"))));
        verify(streamOps).acknowledge("email.events", "email-consumers", RecordId.of("1-0"));
    }

    @Test
    void handleRecord_skipsAlreadySentReminderWithoutDuplicateSmtpSend() {
        UUID reminderId = UUID.randomUUID();
        FollowUpReminder reminder = reminder(reminderId);
        reminder.setEmailSentAt(Instant.parse("2026-05-20T08:00:00Z"));
        when(followUpReminderService.findReminderForEmail(reminderId)).thenReturn(Optional.of(reminder));

        consumer.handleRecord(record(Map.of(
                "eventType", EmailEvent.Type.FOLLOW_UP_REMINDER.streamValue(),
                "reminderId", reminderId.toString()
        )));

        verify(emailService, never()).sendFollowUpReminderEmail(any());
        verify(followUpReminderService, never()).markEmailSent(any(UUID.class), any(Instant.class));
    }

    // --- Copilot review fixes ---

    @Test
    void handleRecord_verification_userNotFound_throwsInvalidEmailEvent() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                consumer.handleRecord(record(Map.of(
                        "eventType", EmailEvent.Type.VERIFICATION.streamValue(),
                        "userId", userId.toString(),
                        "token", "verify-token"
                ))));

        // InvalidEmailEventException is private — verify by message
        assertThat(ex.getMessage()).isEqualTo("user_not_found");
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void handleRecord_routesProfileShareAcceptedEventToEmailService() {
        UUID ownerId = UUID.randomUUID();

        consumer.handleRecord(record(Map.of(
                "eventType", EmailEvent.Type.PROFILE_SHARE_ACCEPTED.streamValue(),
                "userId", ownerId.toString(),
                "email", "owner@healthlens.vn",
                "viewerName", "Nguyen Van B",
                "profileDisplayName", "Ba",
                "profilesLink", "http://localhost:3000/profiles"
        )));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailService).sendProfileShareAcceptedEmail(
                userCaptor.capture(),
                eq("Nguyen Van B"),
                eq("Ba"),
                eq("http://localhost:3000/profiles")
        );
        assertThat(userCaptor.getValue().getId()).isEqualTo(ownerId);
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("owner@healthlens.vn");
    }

    @Test
    void handleRecord_profileShareAccepted_missingProfilesLink_throwsInvalidEmailEvent() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                consumer.handleRecord(record(Map.of(
                        "eventType", EmailEvent.Type.PROFILE_SHARE_ACCEPTED.streamValue(),
                        "userId", UUID.randomUUID().toString(),
                        "email", "owner@healthlens.vn",
                        "viewerName", "Nguyen Van B",
                        "profileDisplayName", "Ba"
                ))));

        assertThat(ex.getMessage()).isEqualTo("missing_profilesLink");
        verify(emailService, never()).sendProfileShareAcceptedEmail(any(), any(), any(), any());
    }

    @Test
    void handleRecord_profileInvitation_inviterUserDoesNotCarryRecipientEmail() {
        UUID inviterId = UUID.randomUUID();

        consumer.handleRecord(record(Map.of(
                "eventType", EmailEvent.Type.PROFILE_INVITATION.streamValue(),
                "userId", inviterId.toString(),
                "email", "invitee@healthlens.vn",
                "inviterName", "Inviter Name",
                "invitationLink", "http://localhost:3000/invitations/accept?token=abc"
        )));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailService).sendProfileInvitationEmail(
                userCaptor.capture(),
                eq("invitee@healthlens.vn"),
                eq("http://localhost:3000/invitations/accept?token=abc")
        );
        User capturedInviter = userCaptor.getValue();
        assertThat(capturedInviter.getId()).isEqualTo(inviterId);
        assertThat(capturedInviter.getFullName()).isEqualTo("Inviter Name");
        assertThat(capturedInviter.getEmail()).isNull();
    }

    @Test
    void handleRecord_followUpReminder_userInactive_throwsInvalidEmailEvent() {
        UUID reminderId = UUID.randomUUID();
        FollowUpReminder reminder = reminder(reminderId);
        reminder.getProfile().getUser().setAccountStatus(AccountStatus.DELETED);
        when(followUpReminderService.findReminderForEmail(reminderId)).thenReturn(Optional.of(reminder));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                consumer.handleRecord(record(Map.of(
                        "eventType", EmailEvent.Type.FOLLOW_UP_REMINDER.streamValue(),
                        "reminderId", reminderId.toString()
                ))));

        assertThat(ex.getMessage()).isEqualTo("reminder_not_deliverable");
        verify(followUpReminderService).releaseEmailClaim(reminderId);
        verify(emailService, never()).sendFollowUpReminderEmail(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumeEmailEvents_deadLetterRedactsSensitiveFields() {
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", EmailEvent.Type.VERIFICATION.streamValue());
        payload.put("userId", "not-a-uuid");
        payload.put("token", "secret-verification-token");
        payload.put("cancellationLink", "https://healthlens.vn/cancel?token=abc");
        payload.put("invitationLink", "https://healthlens.vn/invite?token=xyz");
        MapRecord<String, Object, Object> record = record(payload);
        when(record.getId()).thenReturn(RecordId.of("2-0"));
        when(streamConsumerSupport.readPendingThenNew(
                eq(streamOps), eq("email.events"), eq("email-consumers"), eq("test-email-consumer"), eq(10), any(Duration.class)
        )).thenReturn(List.of(record));

        consumer.consumeEmailEvents();

        verify(streamPublisher).publish(eq("email.events.dlq"), org.mockito.ArgumentMatchers.<Map<String, String>>argThat(dlq -> {
            // Sensitive fields must be redacted
            boolean noToken = !dlq.containsKey("token");
            boolean noCancelLink = !dlq.containsKey("cancellationLink");
            boolean noInviteLink = !dlq.containsKey("invitationLink");
            // Non-sensitive metadata must be preserved
            boolean hasEventType = EmailEvent.Type.VERIFICATION.streamValue().equals(dlq.get("eventType"));
            boolean hasReason = "invalid_userId".equals(dlq.get("failureReason"));
            boolean hasSourceId = "2-0".equals(dlq.get("sourceRecordId"));
            return noToken && noCancelLink && noInviteLink && hasEventType && hasReason && hasSourceId;
        }));
        verify(streamOps).acknowledge("email.events", "email-consumers", RecordId.of("2-0"));
    }

    @SuppressWarnings("unchecked")
    private MapRecord<String, Object, Object> record(Map<String, Object> value) {
        MapRecord<String, Object, Object> record = mock(MapRecord.class);
        when(record.getValue()).thenReturn((Map<Object, Object>) (Map<?, ?>) value);
        return record;
    }

    private FollowUpReminder reminder(UUID reminderId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@healthlens.vn");
        user.setAccountStatus(AccountStatus.ACTIVE);
        Profile profile = new Profile();
        profile.setId(UUID.randomUUID());
        profile.setUser(user);
        FollowUpReminder reminder = new FollowUpReminder();
        reminder.setId(reminderId);
        reminder.setProfile(profile);
        return reminder;
    }
}
