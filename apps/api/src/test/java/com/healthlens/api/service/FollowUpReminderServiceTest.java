package com.healthlens.api.service;

import com.healthlens.api.dto.request.FollowUpReminderRequest;
import com.healthlens.api.dto.response.FollowUpReminderResponse;
import com.healthlens.api.entity.AccountStatus;
import com.healthlens.api.entity.FollowUpReminder;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.FollowUpReminderRepository;
import com.healthlens.api.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowUpReminderServiceTest {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private FollowUpReminderRepository reminderRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private EmailEventPublisher emailEventPublisher;

    private FollowUpReminderService service;
    private UUID userId;
    private UUID profileId;
    private Profile profile;

    @BeforeEach
    void setUp() {
        service = new FollowUpReminderService(reminderRepository, profileRepository, emailEventPublisher);
        userId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user@healthlens.vn");
        user.setFullName("Nguyen Van A");
        user.setAccountStatus(AccountStatus.ACTIVE);
        profile = new Profile();
        profile.setId(profileId);
        profile.setUser(user);
        profile.setDisplayName("Hồ sơ của tôi");
    }

    @Test
    void create_shouldPersistReminderForOwnedProfile() {
        LocalDate futureDate = LocalDate.now(VN_ZONE).plusDays(30);
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(profile));
        when(reminderRepository.save(any(FollowUpReminder.class))).thenAnswer(invocation -> {
            FollowUpReminder reminder = invocation.getArgument(0);
            reminder.setId(UUID.randomUUID());
            reminder.setCreatedAt(Instant.parse("2026-05-16T00:00:00Z"));
            reminder.setUpdatedAt(Instant.parse("2026-05-16T00:00:00Z"));
            return reminder;
        });

        FollowUpReminderResponse response = service.create(
                userId,
                profileId,
                new FollowUpReminderRequest(futureDate, "  Tái khám  ", "  Mang toa cũ  ")
        );

        assertThat(response.profileId()).isEqualTo(profileId);
        assertThat(response.reminderDate()).isEqualTo(futureDate);
        assertThat(response.reminderType()).isEqualTo("Tái khám");
        assertThat(response.note()).isEqualTo("Mang toa cũ");

        ArgumentCaptor<FollowUpReminder> captor = ArgumentCaptor.forClass(FollowUpReminder.class);
        verify(reminderRepository).save(captor.capture());
        assertThat(captor.getValue().getProfile()).isEqualTo(profile);
    }

    @Test
    void create_shouldSendEmailImmediatelyWhenReminderDateIsToday() {
        AtomicReference<FollowUpReminder> savedReminder = new AtomicReference<>();
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(profile));
        when(reminderRepository.save(any(FollowUpReminder.class))).thenAnswer(invocation -> {
            FollowUpReminder reminder = invocation.getArgument(0);
            if (reminder.getId() == null) {
                reminder.setId(UUID.randomUUID());
            }
            reminder.setCreatedAt(Instant.parse("2026-05-16T00:00:00Z"));
            reminder.setUpdatedAt(Instant.parse("2026-05-16T00:00:00Z"));
            savedReminder.set(reminder);
            return reminder;
        });
        when(reminderRepository.claimDueReminderForEmail(
                any(UUID.class),
                any(LocalDate.class),
                any(Instant.class),
                any(Instant.class),
                any(AccountStatus.class))).thenReturn(1);
        LocalDate today = LocalDate.now(VN_ZONE);

        FollowUpReminderResponse response = service.create(
                userId,
                profileId,
                new FollowUpReminderRequest(today, "Tái khám", null)
        );

        assertThat(response.profileId()).isEqualTo(profileId);
        verify(emailEventPublisher).publishFollowUpReminder(any(UUID.class));
        verify(reminderRepository, never()).markEmailSent(any(UUID.class), any(Instant.class));
    }

    @Test
    void create_shouldRejectInvalidReminderType() {
        LocalDate futureDate = LocalDate.now(VN_ZONE).plusDays(30);
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.create(
                userId,
                profileId,
                new FollowUpReminderRequest(futureDate, "Khám chuyên khoa", null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Loại nhắc không hợp lệ");

        verify(reminderRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectNullReminderDateAtServiceLayer() {
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.create(
                userId,
                profileId,
                new FollowUpReminderRequest(null, "Tái khám", null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ngày nhắc là bắt buộc");

        verify(reminderRepository, never()).save(any());
    }

    @Test
    void update_shouldResetEmailSentAtWhenDateChanges() {
        UUID reminderId = UUID.randomUUID();
        FollowUpReminder reminder = existingReminder(reminderId);
        LocalDate previousFutureDate = LocalDate.now(VN_ZONE).plusDays(30);
        LocalDate nextFutureDate = previousFutureDate.plusDays(15);
        reminder.setReminderDate(previousFutureDate);
        reminder.setEmailSentAt(Instant.parse("2026-06-20T01:00:00Z"));

        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.of(profile));
        when(reminderRepository.findByIdAndProfileId(reminderId, profileId)).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(any(FollowUpReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FollowUpReminderResponse response = service.update(
                userId,
                profileId,
                reminderId,
                new FollowUpReminderRequest(nextFutureDate, "Theo dõi chỉ số", null)
        );

        assertThat(response.reminderDate()).isEqualTo(nextFutureDate);
        assertThat(response.emailSentAt()).isNull();
    }

    @Test
    void delete_shouldRejectUnknownProfile() {
        when(profileRepository.findByIdAndUserId(profileId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, profileId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy hồ sơ");

        verify(reminderRepository, never()).delete(any());
    }

    @Test
    void sendDueReminderEmails_shouldMarkSentOnlyAfterEmailSuccess() {
        FollowUpReminder first = existingReminder(UUID.randomUUID());
        FollowUpReminder second = existingReminder(UUID.randomUUID());
        when(reminderRepository.findDueReminderIdsForEmail(
                any(LocalDate.class),
                any(Instant.class),
                any(AccountStatus.class),
                any())).thenReturn(List.of(first.getId(), second.getId()));
        when(reminderRepository.claimDueReminderForEmail(
                any(UUID.class),
                any(LocalDate.class),
                any(Instant.class),
                any(Instant.class),
                any(AccountStatus.class))).thenReturn(1);
        int dispatchedCount = service.sendDueReminderEmails(LocalDate.of(2026, 6, 20));

        assertThat(dispatchedCount).isEqualTo(2);
        verify(emailEventPublisher).publishFollowUpReminder(first.getId());
        verify(emailEventPublisher).publishFollowUpReminder(second.getId());
        verify(reminderRepository, never()).markEmailSent(any(UUID.class), any(Instant.class));
    }

    private FollowUpReminder existingReminder(UUID reminderId) {
        FollowUpReminder reminder = new FollowUpReminder();
        reminder.setId(reminderId);
        reminder.setProfile(profile);
        reminder.setReminderDate(LocalDate.of(2026, 6, 20));
        reminder.setReminderType("Tái khám");
        reminder.setNote("Ghi chú");
        reminder.setCreatedAt(Instant.parse("2026-05-16T00:00:00Z"));
        reminder.setUpdatedAt(Instant.parse("2026-05-16T00:00:00Z"));
        return reminder;
    }
}
