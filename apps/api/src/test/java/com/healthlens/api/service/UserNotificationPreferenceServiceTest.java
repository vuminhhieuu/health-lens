package com.healthlens.api.service;

import com.healthlens.api.dto.request.UpdateNotificationPreferencesRequest;
import com.healthlens.api.dto.response.NotificationPreferenceResponse;
import com.healthlens.api.entity.User;
import com.healthlens.api.entity.UserNotificationPreference;
import com.healthlens.api.notification.NotificationEmailCategory;
import com.healthlens.api.repository.UserNotificationPreferenceRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationPreferenceServiceTest {

    @Mock
    private UserNotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowUpReminderService followUpReminderService;

    private UserNotificationPreferenceService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UserNotificationPreferenceService(
                preferenceRepository, userRepository, followUpReminderService);
    }

    @Test
    void getPreferences_createsDefaultsWhenMissing() {
        when(preferenceRepository.findById(userId)).thenReturn(Optional.empty());
        when(preferenceRepository.saveAndFlush(any(UserNotificationPreference.class))).thenAnswer(invocation -> {
            UserNotificationPreference preference = invocation.getArgument(0);
            return preference;
        });

        NotificationPreferenceResponse response = service.getPreferences(userId);

        assertThat(response.shareInvite()).isTrue();
        assertThat(response.shareAccepted()).isTrue();
        assertThat(response.followUpReminder()).isTrue();
        assertThat(response.security()).isTrue();
        verify(followUpReminderService).dispatchDueReminderEmailsIfEnabled(userId);
    }

    @Test
    void updatePreferences_forcesSecurityOnAndPersistsOptOuts() {
        UserNotificationPreference existing = new UserNotificationPreference();
        existing.setUserId(userId);
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(UserNotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceResponse response = service.updatePreferences(
                userId,
                new UpdateNotificationPreferencesRequest(false, false, false, false)
        );

        assertThat(response.shareInvite()).isFalse();
        assertThat(response.shareAccepted()).isFalse();
        assertThat(response.followUpReminder()).isFalse();
        assertThat(response.security()).isTrue();
        verify(followUpReminderService, never()).dispatchDueReminderEmailsIfEnabled(userId);

        ArgumentCaptor<UserNotificationPreference> captor = ArgumentCaptor.forClass(UserNotificationPreference.class);
        verify(preferenceRepository).save(captor.capture());
        assertThat(captor.getValue().isSecurity()).isTrue();
    }

    @Test
    void createDefaultPreferences_isIdempotent() {
        when(preferenceRepository.existsById(userId)).thenReturn(true);

        service.createDefaultPreferences(userId);

        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void isEmailEnabledForRecipient_returnsTrueWhenUserMissing() {
        when(userRepository.findByEmailIgnoreCase("new@healthlens.vn")).thenReturn(Optional.empty());

        assertThat(service.isEmailEnabledForRecipient("new@healthlens.vn", NotificationEmailCategory.SHARE_INVITE))
                .isTrue();
    }

    @Test
    void isEmailEnabledForUser_respectsStoredPreference() {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUserId(userId);
        preference.setFollowUpReminder(false);
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(preference));

        assertThat(service.isEmailEnabledForUser(userId, NotificationEmailCategory.FOLLOW_UP_REMINDER))
                .isFalse();
        assertThat(service.isEmailEnabledForUser(userId, NotificationEmailCategory.SECURITY))
                .isTrue();
    }

    @Test
    void updatePreferences_whenFollowUpEnabled_retriesDueReminderEmails() {
        UserNotificationPreference existing = new UserNotificationPreference();
        existing.setUserId(userId);
        existing.setFollowUpReminder(false);
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any(UserNotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updatePreferences(
                userId,
                new UpdateNotificationPreferencesRequest(true, true, true, true)
        );

        verify(followUpReminderService).dispatchDueReminderEmailsIfEnabled(userId);
    }

    @Test
    void isEmailEnabledForRecipient_usesRegisteredUserPreference() {
        User user = new User();
        user.setId(userId);
        user.setEmail("viewer@healthlens.vn");

        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUserId(userId);
        preference.setShareInvite(false);

        when(userRepository.findByEmailIgnoreCase("viewer@healthlens.vn")).thenReturn(Optional.of(user));
        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(preference));

        assertThat(service.isEmailEnabledForRecipient("viewer@healthlens.vn", NotificationEmailCategory.SHARE_INVITE))
                .isFalse();
    }
}
