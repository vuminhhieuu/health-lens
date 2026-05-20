package com.healthlens.api.service;

import com.healthlens.api.entity.DataDeletionRequest;
import com.healthlens.api.entity.FollowUpReminder;
import com.healthlens.api.entity.User;
import com.healthlens.api.events.ApplicationStreamPublisher;
import com.healthlens.api.events.RedisStreamConsumerSupport;
import com.healthlens.api.events.email.EmailEvent;
import com.healthlens.api.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class EmailConsumer {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final FollowUpReminderService followUpReminderService;
    private final RedisStreamConsumerSupport streamConsumerSupport;
    private final ApplicationStreamPublisher streamPublisher;
    private final String emailEventStream;
    private final String emailDeadLetterStream;
    private final String consumerGroup;
    private final String consumerName;

    public EmailConsumer(
            StringRedisTemplate redisTemplate,
            EmailService emailService,
            UserRepository userRepository,
            @Lazy FollowUpReminderService followUpReminderService,
            RedisStreamConsumerSupport streamConsumerSupport,
            ApplicationStreamPublisher streamPublisher,
            @Value("${app.stream.email-events:email.events}") String emailEventStream,
            @Value("${app.stream.email-dead-letter-events:email.events.dlq}") String emailDeadLetterStream,
            @Value("${app.stream.email-consumer-group:email-consumers}") String consumerGroup,
            @Value("${app.stream.email-consumer-name:api-email-consumer}") String consumerName) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.followUpReminderService = followUpReminderService;
        this.streamConsumerSupport = streamConsumerSupport;
        this.streamPublisher = streamPublisher;
        this.emailEventStream = emailEventStream;
        this.emailDeadLetterStream = emailDeadLetterStream;
        this.consumerGroup = consumerGroup;
        this.consumerName = consumerName;
    }

    @PostConstruct
    public void initConsumerGroup() {
        ensureConsumerGroup();
    }

    private void ensureConsumerGroup() {
        streamConsumerSupport.ensureConsumerGroup(emailEventStream, consumerGroup, "EmailConsumer");
    }

    @Scheduled(fixedDelayString = "${app.stream.email-poll-delay-ms:1000}")
    public void consumeEmailEvents() {
        StreamOperations<String, Object, Object> streamOps = redisTemplate.opsForStream();
        if (streamOps == null) {
            log.debug("[EmailConsumer] Redis stream operations unavailable; skipping poll");
            return;
        }
        List<MapRecord<String, Object, Object>> records;
        try {
            records = streamConsumerSupport.readPendingThenNew(
                    streamOps,
                    emailEventStream,
                    consumerGroup,
                    consumerName,
                    10,
                    Duration.ofMillis(500));
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("NOGROUP")) {
                ensureConsumerGroup();
            }
            log.warn("[EmailConsumer] Failed reading stream {}", emailEventStream, ex);
            return;
        }

        if (records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                handleRecord(record);
                streamOps.acknowledge(emailEventStream, consumerGroup, record.getId());
            } catch (InvalidEmailEventException ex) {
                deadLetterInvalidRecord(streamOps, record, ex);
                streamOps.acknowledge(emailEventStream, consumerGroup, record.getId());
            } catch (Exception ex) {
                log.error("[EmailConsumer] Failed processing record {}. It will remain pending for retry.", record.getId(), ex);
            }
        }
    }

    void handleRecord(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        String eventType = stringVal(value.get("eventType"));

        if (EmailEvent.Type.VERIFICATION.streamValue().equals(eventType)) {
            User user = userRepository.findById(requiredUuid(value, "userId"))
                    .orElseThrow(() -> new InvalidEmailEventException("user_not_found"));
            emailService.sendVerificationEmail(user, required(value, "token"));
            return;
        }

        if (EmailEvent.Type.PASSWORD_RESET.streamValue().equals(eventType)) {
            User user = eventUser(value);
            emailService.sendPasswordResetEmail(user, required(value, "token"));
            return;
        }

        if (EmailEvent.Type.DELETION_CONFIRMATION.streamValue().equals(eventType)) {
            DataDeletionRequest deletionRequest = new DataDeletionRequest();
            String scheduledDeletionAt = stringVal(value.get("scheduledDeletionAt"));
            if (!scheduledDeletionAt.isBlank()) {
                try {
                    deletionRequest.setScheduledDeletionAt(Instant.parse(scheduledDeletionAt));
                } catch (Exception ex) {
                    throw new InvalidEmailEventException("invalid_scheduled_deletion_at", ex);
                }
            }
            emailService.sendDeletionConfirmationEmail(
                    eventUser(value),
                    deletionRequest,
                    required(value, "cancellationLink")
            );
            return;
        }

        if (EmailEvent.Type.DELETION_CANCELLATION.streamValue().equals(eventType)) {
            emailService.sendCancellationConfirmationEmail(eventUser(value));
            return;
        }

        if (EmailEvent.Type.DELETION_COMPLETION.streamValue().equals(eventType)) {
            emailService.sendDeletionCompletionEmail(eventUser(value));
            return;
        }

        if (EmailEvent.Type.PROFILE_INVITATION.streamValue().equals(eventType)) {
            emailService.sendProfileInvitationEmail(
                    inviterUser(value),
                    required(value, "email"),
                    required(value, "invitationLink")
            );
            return;
        }

        if (EmailEvent.Type.HEALTH_RECORD_INVITATION.streamValue().equals(eventType)) {
            emailService.sendHealthRecordInvitationEmail(
                    inviterUser(value),
                    required(value, "email"),
                    required(value, "invitationLink")
            );
            return;
        }

        if (EmailEvent.Type.FOLLOW_UP_REMINDER.streamValue().equals(eventType)) {
            sendClaimedReminderEmail(requiredUuid(value, "reminderId"));
            return;
        }

        throw new InvalidEmailEventException("unknown_event_type");
    }

    private User eventUser(Map<Object, Object> value) {
        User user = new User();
        String userId = stringVal(value.get("userId"));
        if (!userId.isBlank()) {
            user.setId(parseUuid(userId, "userId"));
        }
        user.setEmail(required(value, "email"));
        String fullName = stringVal(value.get("fullName"));
        if (fullName.isBlank()) {
            fullName = stringVal(value.get("displayName"));
        }
        if (fullName.isBlank()) {
            fullName = stringVal(value.get("inviterName"));
        }
        user.setFullName(fullName);
        return user;
    }

    private User inviterUser(Map<Object, Object> value) {
        User user = new User();
        String userId = stringVal(value.get("userId"));
        if (!userId.isBlank()) {
            user.setId(parseUuid(userId, "userId"));
        }
        String inviterName = stringVal(value.get("inviterName"));
        if (inviterName.isBlank()) {
            inviterName = stringVal(value.get("fullName"));
        }
        if (inviterName.isBlank()) {
            inviterName = stringVal(value.get("displayName"));
        }
        user.setFullName(inviterName);
        return user;
    }

    private void sendClaimedReminderEmail(UUID reminderId) {
        FollowUpReminder reminder = followUpReminderService.findReminderForEmail(reminderId)
                .orElse(null);
        if (reminder == null || reminder.getEmailSentAt() != null) {
            return;
        }
        if (reminder.getProfile() == null || reminder.getProfile().getUser() == null
                || reminder.getProfile().getUser().getAccountStatus() != com.healthlens.api.entity.AccountStatus.ACTIVE) {
            followUpReminderService.releaseEmailClaim(reminderId);
            throw new InvalidEmailEventException("reminder_not_deliverable");
        }
        boolean sent = emailService.sendFollowUpReminderEmail(reminder);
        if (!sent) {
            throw new IllegalStateException("Follow-up reminder email was not sent: " + reminderId);
        }
        followUpReminderService.markEmailSent(reminderId, Instant.now());
    }

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "token", "cancellationLink", "invitationLink");

    private void deadLetterInvalidRecord(
            StreamOperations<String, Object, Object> streamOps,
            MapRecord<String, Object, Object> record,
            InvalidEmailEventException ex
    ) {
        try {
            Map<String, String> deadLetter = record.getValue().entrySet().stream()
                    .filter(entry -> !SENSITIVE_FIELDS.contains(String.valueOf(entry.getKey())))
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            entry -> stringVal(entry.getValue()),
                            (left, right) -> left
                    ));
            deadLetter.put("failureReason", ex.reason);
            deadLetter.put("sourceRecordId", record.getId().getValue());
            streamPublisher.publish(emailDeadLetterStream, deadLetter);
            log.warn("[EmailConsumer] Invalid email event moved to DLQ. recordId={} reason={}",
                    record.getId(), ex.reason);
        } catch (Exception dlqEx) {
            log.error("[EmailConsumer] Invalid email event could not be written to DLQ. recordId={}",
                    record.getId(), dlqEx);
            throw dlqEx;
        }
    }

    private UUID requiredUuid(Map<Object, Object> value, String field) {
        return parseUuid(required(value, field), field);
    }

    private UUID parseUuid(String raw, String field) {
        try {
            return UUID.fromString(raw);
        } catch (Exception ex) {
            throw new InvalidEmailEventException("invalid_" + field, ex);
        }
    }

    private String required(Map<Object, Object> value, String field) {
        String raw = stringVal(value.get(field));
        if (raw.isBlank()) {
            throw new InvalidEmailEventException("missing_" + field);
        }
        return raw;
    }

    private String stringVal(Object value) {
        return value == null ? "" : value.toString();
    }

    private static class InvalidEmailEventException extends RuntimeException {
        private final String reason;

        private InvalidEmailEventException(String reason) {
            super(reason);
            this.reason = reason;
        }

        private InvalidEmailEventException(String reason, Throwable cause) {
            super(reason, cause);
            this.reason = reason;
        }
    }
}
