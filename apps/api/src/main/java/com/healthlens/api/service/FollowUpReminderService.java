package com.healthlens.api.service;

import com.healthlens.api.dto.request.FollowUpReminderRequest;
import com.healthlens.api.dto.response.FollowUpReminderResponse;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.FollowUpReminder;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.events.email.EmailEventPublisher;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.notification.NotificationEmailCategory;
import com.healthlens.api.repository.FollowUpReminderRepository;
import com.healthlens.api.repository.ProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FollowUpReminderService {

    private static final int MAX_TYPE_LENGTH = 50;
    private static final int MAX_NOTE_LENGTH = 500;
    private static final int EMAIL_BATCH_SIZE = 100;
    private static final int MAX_EMAILS_PER_RUN = 500;
    private static final Duration EMAIL_CLAIM_TIMEOUT = Duration.ofMinutes(30);
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> ALLOWED_REMINDER_TYPES = Set.of(
            "Tái khám",
            "Xét nghiệm lại",
            "Theo dõi chỉ số",
            "Khác"
    );

    private final FollowUpReminderRepository reminderRepository;
    private final ProfileRepository profileRepository;
    private final EmailEventPublisher emailEventPublisher;
    private final UserNotificationPreferenceService notificationPreferenceService;

    @Autowired
    @Lazy
    private FollowUpReminderService selfProxy;

    public FollowUpReminderService(
            FollowUpReminderRepository reminderRepository,
            ProfileRepository profileRepository,
            EmailEventPublisher emailEventPublisher,
            UserNotificationPreferenceService notificationPreferenceService
    ) {
        this.reminderRepository = reminderRepository;
        this.profileRepository = profileRepository;
        this.emailEventPublisher = emailEventPublisher;
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @Transactional(readOnly = true)
    public List<FollowUpReminderResponse> list(UUID userId, UUID profileId) {
        requireOwnedProfile(userId, profileId);
        return reminderRepository.findAllByProfileIdOrderByReminderDateAscCreatedAtAsc(profileId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public FollowUpReminderResponse create(UUID userId, UUID profileId, FollowUpReminderRequest request) {
        Profile profile = requireOwnedProfile(userId, profileId);
        FollowUpReminder reminder = new FollowUpReminder();
        reminder.setProfile(profile);
        applyRequest(reminder, request);
        FollowUpReminder savedReminder = reminderRepository.save(reminder);
        sendAfterCommitIfDue(savedReminder);
        return mapToResponse(savedReminder);
    }

    @Transactional
    public FollowUpReminderResponse update(
            UUID userId,
            UUID profileId,
            UUID reminderId,
            FollowUpReminderRequest request
    ) {
        requireOwnedProfile(userId, profileId);
        FollowUpReminder reminder = reminderRepository.findByIdAndProfileId(reminderId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhắc lịch"));

        LocalDate previousDate = reminder.getReminderDate();
        applyRequest(reminder, request);
        if (!reminder.getReminderDate().equals(previousDate)) {
            reminder.setEmailSentAt(null);
            reminder.setEmailSkippedOptOutAt(null);
        }

        FollowUpReminder savedReminder = reminderRepository.save(reminder);
        sendAfterCommitIfDue(savedReminder);
        return mapToResponse(savedReminder);
    }

    @Transactional
    public void delete(UUID userId, UUID profileId, UUID reminderId) {
        requireOwnedProfile(userId, profileId);
        FollowUpReminder reminder = reminderRepository.findByIdAndProfileId(reminderId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhắc lịch"));
        reminderRepository.delete(reminder);
    }

    public int sendDueReminderEmails(LocalDate today) {
        int sentCount = 0;
        int processedCount = 0;

        while (processedCount < MAX_EMAILS_PER_RUN) {
            int limit = Math.min(EMAIL_BATCH_SIZE, MAX_EMAILS_PER_RUN - processedCount);
            List<UUID> dueReminderIds = transactionalSelf().findDueReminderIdsForEmail(today, limit);
            if (dueReminderIds.isEmpty()) {
                break;
            }

            processedCount += dueReminderIds.size();
            for (UUID reminderId : dueReminderIds) {
                if (transactionalSelf().publishClaimedReminderEmail(reminderId, today)) {
                    sentCount++;
                }
            }

            if (dueReminderIds.size() < limit) {
                break;
            }
        }

        return sentCount;
    }

    @Transactional(readOnly = true)
    public List<UUID> findDueReminderIdsForEmail(LocalDate today, int limit) {
        if (today == null) {
            throw new IllegalArgumentException("Ngày nhắc là bắt buộc");
        }
        Instant claimCutoff = Instant.now().minus(EMAIL_CLAIM_TIMEOUT);
        return reminderRepository.findDueReminderIdsForEmail(
                today,
                claimCutoff,
                AccountStatus.ACTIVE,
                PageRequest.of(0, Math.max(1, limit))
        );
    }


    public boolean publishClaimedReminderEmail(UUID reminderId, LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("Ngày nhắc là bắt buộc");
        }

        FollowUpReminder reminder = reminderRepository.findWithProfileAndUserById(reminderId).orElse(null);
        if (reminder == null || !isDeliverable(reminder, today)) {
            return false;
        }

        UUID ownerId = reminder.getProfile().getUser().getId();
        if (!notificationPreferenceService.isEmailEnabledForUser(
                ownerId, NotificationEmailCategory.FOLLOW_UP_REMINDER)) {
            log.info(
                    "Skipping follow-up reminder email publish; followUpReminder disabled for user {}",
                    ownerId);
            transactionalSelf().markEmailSkippedOptOut(reminderId, Instant.now());
            return false;
        }

        Instant claimedAt = Instant.now();
        Instant claimCutoff = claimedAt.minus(EMAIL_CLAIM_TIMEOUT);

        int claimed = transactionalSelf().claimDueReminderForEmail(reminderId, today, claimedAt, claimCutoff);
        if (claimed == 0) {
            return false;
        }

        try {
            emailEventPublisher.publishFollowUpReminder(reminderId);
            return true;
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to publish follow-up reminder email event for reminder {}; releasing claim",
                    reminderId,
                    ex);
            transactionalSelf().releaseEmailClaim(reminderId);
            return false;
        }
    }

    /**
     * Clears stale skip/claim state and re-publishes due reminder emails when the user has not opted out.
     * Uses a new transaction so callers in read-only flows (inbox, reminder list) can still dispatch.
     */
    public void dispatchDueReminderEmailsIfEnabled(UUID userId) {
        if (!notificationPreferenceService.isEmailEnabledForUser(
                userId, NotificationEmailCategory.FOLLOW_UP_REMINDER)) {
            return;
        }
        transactionalSelf().retryDueReminderEmailsForUser(userId);
    }

    /**
     * Re-attempts email dispatch for due reminders after prefs change or stale skip/claim state.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int retryDueReminderEmailsForUser(UUID userId) {
        LocalDate today = LocalDate.now(VN_ZONE);
        reminderRepository.clearEmailSkippedOptOutForUser(userId, today);
        Instant claimCutoff = Instant.now().minus(EMAIL_CLAIM_TIMEOUT);
        List<UUID> dueReminderIds = reminderRepository.findDueReminderIdsForUser(
                userId, today, claimCutoff, AccountStatus.ACTIVE);
        int dispatched = 0;
        for (UUID reminderId : dueReminderIds) {
            transactionalSelf().releaseEmailClaim(reminderId);
            if (transactionalSelf().publishClaimedReminderEmail(reminderId, today)) {
                dispatched++;
            }
        }
        return dispatched;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claimDueReminderForEmail(UUID reminderId, LocalDate today, Instant claimedAt, Instant claimCutoff) {
        return reminderRepository.claimDueReminderForEmail(
                reminderId,
                today,
                claimedAt,
                claimCutoff,
                AccountStatus.ACTIVE
        );
    }

    @Transactional(readOnly = true)
    public java.util.Optional<FollowUpReminder> findReminderForEmail(UUID reminderId) {
        return reminderRepository.findWithProfileAndUserById(reminderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEmailSent(UUID reminderId, Instant sentAt) {
        reminderRepository.markEmailSent(reminderId, sentAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseEmailClaim(UUID reminderId) {
        reminderRepository.releaseEmailClaim(reminderId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEmailSkippedOptOut(UUID reminderId, Instant skippedAt) {
        reminderRepository.markEmailSkippedOptOut(reminderId, skippedAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int clearEmailSkippedOptOutForUser(UUID userId, LocalDate today) {
        return reminderRepository.clearEmailSkippedOptOutForUser(userId, today);
    }

    private Profile requireOwnedProfile(UUID userId, UUID profileId) {
        return profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ"));
    }

    private void sendAfterCommitIfDue(FollowUpReminder reminder) {
        LocalDate today = LocalDate.now(VN_ZONE);
        if (reminder.getEmailSentAt() != null || reminder.getEmailSkippedOptOutAt() != null) {
            return;
        }
        if (reminder.getReminderDate() == null || reminder.getReminderDate().isAfter(today)) {
            return;
        }

        UUID reminderId = reminder.getId();
        Runnable sendEmail = () -> transactionalSelf().publishClaimedReminderEmail(reminderId, today);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEmail.run();
                }
            });
            return;
        }

        sendEmail.run();
    }

    private FollowUpReminderService transactionalSelf() {
        return selfProxy != null ? selfProxy : this;
    }

    private void applyRequest(FollowUpReminder reminder, FollowUpReminderRequest request) {
        if (request.reminderDate() == null) {
            throw new IllegalArgumentException("Ngày nhắc là bắt buộc");
        }

        String reminderType = normalizeRequiredText(request.reminderType(), "Loại nhắc là bắt buộc");
        if (reminderType.length() > MAX_TYPE_LENGTH) {
            throw new IllegalArgumentException("Loại nhắc tối đa 50 ký tự");
        }
        if (!ALLOWED_REMINDER_TYPES.contains(reminderType)) {
            throw new IllegalArgumentException("Loại nhắc không hợp lệ");
        }

        String note = normalizeOptionalText(request.note());
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự");
        }

        reminder.setReminderDate(request.reminderDate());
        reminder.setReminderType(reminderType);
        reminder.setNote(note);
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isDeliverable(FollowUpReminder reminder, LocalDate today) {
        if (reminder.getEmailSentAt() != null || reminder.getEmailSkippedOptOutAt() != null) {
            return false;
        }
        if (reminder.getReminderDate() == null || reminder.getReminderDate().isAfter(today)) {
            return false;
        }
        if (reminder.getProfile() == null
                || reminder.getProfile().getUser() == null
                || reminder.getProfile().getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            return false;
        }
        return true;
    }

    private FollowUpReminderResponse mapToResponse(FollowUpReminder reminder) {
        return new FollowUpReminderResponse(
                reminder.getId(),
                reminder.getProfile().getId(),
                reminder.getReminderDate(),
                reminder.getReminderType(),
                reminder.getNote(),
                reminder.getEmailSentAt(),
                reminder.getEmailSkippedOptOutAt(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
