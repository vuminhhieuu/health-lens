package com.healthlens.api.service;

import com.healthlens.api.dto.request.FollowUpReminderRequest;
import com.healthlens.api.dto.response.FollowUpReminderResponse;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.FollowUpReminder;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.exception.ResourceNotFoundException;
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
    private final EmailService emailService;

    @Autowired
    @Lazy
    private FollowUpReminderService selfProxy;

    public FollowUpReminderService(
            FollowUpReminderRepository reminderRepository,
            ProfileRepository profileRepository,
            EmailService emailService
    ) {
        this.reminderRepository = reminderRepository;
        this.profileRepository = profileRepository;
        this.emailService = emailService;
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
                if (transactionalSelf().sendClaimedReminderEmail(reminderId, today)) {
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

    public boolean sendClaimedReminderEmail(UUID reminderId) {
        return sendClaimedReminderEmail(reminderId, LocalDate.now(VN_ZONE));
    }

    public boolean sendClaimedReminderEmail(UUID reminderId, LocalDate today) {
        if (today == null) {
            throw new IllegalArgumentException("Ngày nhắc là bắt buộc");
        }
        Instant claimedAt = Instant.now();
        Instant claimCutoff = claimedAt.minus(EMAIL_CLAIM_TIMEOUT);

        int claimed = transactionalSelf().claimDueReminderForEmail(reminderId, today, claimedAt, claimCutoff);
        if (claimed == 0) {
            return false;
        }

        FollowUpReminder reminder = transactionalSelf().findReminderForEmail(reminderId)
                .orElse(null);
        if (reminder == null || reminder.getProfile() == null || reminder.getProfile().getUser() == null
                || reminder.getProfile().getUser().getAccountStatus() != AccountStatus.ACTIVE) {
            transactionalSelf().releaseEmailClaim(reminderId);
            return false;
        }

        try {
            boolean sent = emailService.sendFollowUpReminderEmail(reminder);
            if (sent) {
                transactionalSelf().markEmailSent(reminderId, Instant.now());
                return true;
            }
            log.warn("Follow-up reminder email was not sent; reminderId={} will be retried after claim timeout", reminderId);
        } catch (Exception ex) {
            log.error(
                    "Failed to send follow-up reminder email reminderId={} profileId={}",
                    reminder.getId(),
                    reminder.getProfile() != null ? reminder.getProfile().getId() : null,
                    ex
            );
        }

        return false;
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

    private Profile requireOwnedProfile(UUID userId, UUID profileId) {
        return profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ"));
    }

    private void sendAfterCommitIfDue(FollowUpReminder reminder) {
        if (reminder.getEmailSentAt() != null) {
            return;
        }
        LocalDate today = LocalDate.now(VN_ZONE);
        if (reminder.getReminderDate().isAfter(today)) {
            return;
        }

        Runnable sendEmail = () -> transactionalSelf().sendClaimedReminderEmail(reminder.getId());
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

    private FollowUpReminderResponse mapToResponse(FollowUpReminder reminder) {
        return new FollowUpReminderResponse(
                reminder.getId(),
                reminder.getProfile().getId(),
                reminder.getReminderDate(),
                reminder.getReminderType(),
                reminder.getNote(),
                reminder.getEmailSentAt(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
