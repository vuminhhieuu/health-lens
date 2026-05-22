package com.healthlens.api.service;

import com.healthlens.api.dto.request.UpdateNotificationPreferencesRequest;
import com.healthlens.api.dto.response.NotificationPreferenceResponse;
import com.healthlens.api.entity.UserNotificationPreference;
import com.healthlens.api.notification.NotificationEmailCategory;
import com.healthlens.api.repository.UserNotificationPreferenceRepository;
import com.healthlens.api.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserNotificationPreferenceService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final FollowUpReminderService followUpReminderService;

    public UserNotificationPreferenceService(
            UserNotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository,
            @Lazy FollowUpReminderService followUpReminderService) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.followUpReminderService = followUpReminderService;
    }

    @Transactional
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        UserNotificationPreference preference = ensureRow(userId);
        if (preference.isFollowUpReminder()) {
            followUpReminderService.dispatchDueReminderEmailsIfEnabled(userId);
        }
        return toResponse(preference);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferencesRequest request) {
        UserNotificationPreference preference = ensureRow(userId);
        preference.setShareInvite(request.shareInvite());
        preference.setShareAccepted(request.shareAccepted());
        preference.setFollowUpReminder(request.followUpReminder());
        // Security notifications cannot be disabled.
        preference.setSecurity(true);
        UserNotificationPreference saved = preferenceRepository.save(preference);
        if (saved.isFollowUpReminder()) {
            followUpReminderService.dispatchDueReminderEmailsIfEnabled(userId);
        }
        return toResponse(saved);
    }

    @Transactional
    public void createDefaultPreferences(UUID userId) {
        if (preferenceRepository.existsById(userId)) {
            return;
        }
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUserId(userId);
        preferenceRepository.save(preference);
    }

    @Transactional(readOnly = true)
    public boolean isEmailEnabledForUser(UUID userId, NotificationEmailCategory category) {
        if (category == NotificationEmailCategory.SECURITY) {
            return true;
        }
        UserNotificationPreference preference = preferenceRepository.findById(userId).orElse(null);
        if (preference == null) {
            return true;
        }
        return switch (category) {
            case SHARE_INVITE -> preference.isShareInvite();
            case SHARE_ACCEPTED -> preference.isShareAccepted();
            case FOLLOW_UP_REMINDER -> preference.isFollowUpReminder();
            case SECURITY -> true;
        };
    }

    @Transactional(readOnly = true)
    public boolean isEmailEnabledForRecipient(String recipientEmail, NotificationEmailCategory category) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return true;
        }
        String normalized = recipientEmail.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmailIgnoreCase(normalized)
                .map(user -> isEmailEnabledForUser(user.getId(), category))
                .orElse(true);
    }

    private UserNotificationPreference ensureRow(UUID userId) {
        return preferenceRepository.findById(userId)
                .orElseGet(() -> {
                    UserNotificationPreference preference = new UserNotificationPreference();
                    preference.setUserId(userId);
                    return preferenceRepository.save(preference);
                });
    }

    private static NotificationPreferenceResponse toResponse(UserNotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.isShareInvite(),
                preference.isShareAccepted(),
                preference.isFollowUpReminder(),
                preference.isSecurity()
        );
    }
}
